package com.tonic.plugins.gearswapper.combat;

/*
 * Attack cooldown tracker using attack timer logic from ngraves95/attacktimer
 * 
 * Original implementation:
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
 * Copyright (c) 2024, Lexer747 <https://github.com/Lexer747>
 * Adapted for GearSwapper by Tonic
 * 
 * Licensed under BSD 2-Clause License
 */

import com.google.common.collect.ImmutableMap;
import com.tonic.plugins.gearswapper.combat.attacktimer.*;
import com.tonic.plugins.gearswapper.combat.attacktimer.variablespeed.VariableSpeed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.api.kit.KitType;

import javax.inject.Inject;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Tracks attack cooldowns using sophisticated logic from ngraves95/attacktimer
 */
@Slf4j
public class AttackCooldownTracker {

    public enum AttackState {
        NOT_ATTACKING,
        DELAYED_FIRST_TICK,
        DELAYED,
    }

    private static class CooldownInfo {
        int ticks;
        int maxTicks; // weapon speed
    }

    private final Client client;
    private final ItemManager itemManager;

    // Actor cooldowns by NAME (Actor objects are not stable across event calls)
    private final Map<String, CooldownInfo> actorCooldowns = new HashMap<>();

    // Cached target for lookups (getInteracting() can return null at render time)
    private String lastTargetName = null;
    @Getter
    private Actor lastTargetActor = null;

    // Player attack state (Self)
    @Getter
    private int playerCooldownTicks = 0;
    @Getter
    private int playerWeaponSpeed = 4;
    private AttackState playerAttackState = AttackState.NOT_ATTACKING;

    // Spellbook tracking for magic detection
    private Spellbook currentSpellBook = Spellbook.STANDARD;
    private int lastEquippingMonotonicValue = -1;
    private int soundEffectTick = -1;
    private int soundEffectId = -1;

    private boolean initialized = false;

    @Getter
    private int currentAnimation = -1;

    // Special weapon mappings
    private static final int SALAMANDER_SET_ANIM_ID = 952;
    private static final int TWINFLAME_STAFF_WEAPON_ID = 30634;
    private static final int ECHO_VENATOR_BOW_WEAPON_ID = 30434;
    private static final int VENATOR_BOW_WEAPON_ID = 27610;

    private static final Map<Integer, Integer> NON_STANDARD_MAGIC_WEAPON_SPEEDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(TWINFLAME_STAFF_WEAPON_ID, 6)
            .build();

    private static final Map<Integer, Integer> WEAPON_ID_MAPPING_WORKAROUNDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ECHO_VENATOR_BOW_WEAPON_ID, VENATOR_BOW_WEAPON_ID)
            .build();

    public static final int EQUIPPING_MONOTONIC = 384;

    @Inject
    public AttackCooldownTracker(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;
    }

    /**
     * Track spellbook changes
     */
    public void onVarbitChanged(VarbitChanged varbitChanged) {
        if (varbitChanged.getVarbitId() == Varbits.SPELLBOOK) {
            currentSpellBook = Spellbook.fromVarbit(varbitChanged.getValue());
            log.debug("Spellbook changed to: {}", currentSpellBook);
        }
    }

    /**
     * Track weapon swaps
     */
    public void onVarClientIntChanged(VarClientIntChanged varClientIntChanged) {
        final int currentVarBit = client.getVarcIntValue(EQUIPPING_MONOTONIC);
        if (currentVarBit <= lastEquippingMonotonicValue) {
            return;
        }
        lastEquippingMonotonicValue = currentVarBit;

        // Weapon swapped during attack window
        if (playerAttackState == AttackState.DELAYED_FIRST_TICK) {
            performPlayerAttack();
        }
    }

    /**
     * Track sound effects for spell detection
     */
    public void onSoundEffectPlayed(SoundEffectPlayed event) {
        soundEffectTick = client.getTickCount();
        soundEffectId = event.getSoundId();
    }

    /**
     * Handle animation changes for ALL actors
     */
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        int animationId = actor.getAnimation();

        // Skip idle animations
        if (animationId == -1) {
            return;
        }

        // Skip block/defense animations (these aren't attacks)
        if (AnimationData.isBlockListAnimation(animationId)) {
            return;
        }

        // Skip self (handled by performPlayerAttack logic)
        if (actor == client.getLocalPlayer()) {
            return;
        }

        // Get actor name - if null, can't track
        String actorName = actor.getName();
        if (actorName == null) {
            return;
        }

        // Get weapon ID for the opponent (works for Players via PlayerComposition)
        int weaponId = getWeaponId(actor);
        int speed;

        if (weaponId > 0) {
            // Look up weapon speed from item stats
            ItemStats weaponStats = itemManager.getItemStats(weaponId);
            if (weaponStats != null && weaponStats.getEquipment() != null) {
                int baseSpeed = weaponStats.getEquipment().getAspeed();
                int rangedBonus = weaponStats.getEquipment().getArange();

                // Only apply rapid reduction (-1 tick) if it's actually a ranged weapon
                // Ranged weapons have positive ranged attack bonus
                if (rangedBonus > 0 && baseSpeed > 4) {
                    // Ranged weapon on rapid style
                    speed = baseSpeed - 1;
                } else {
                    // Melee weapon (like AGS) or ranged with base <= 4
                    speed = baseSpeed;
                }
            } else {
                // Fallback to 4 (common melee speed)
                speed = 4;
            }
        } else {
            // NPC or unknown - default to 4 ticks
            speed = 4;
        }

        // Store cooldown by name
        CooldownInfo info = new CooldownInfo();
        info.ticks = speed;
        info.maxTicks = speed;
        actorCooldowns.put(actorName, info);

        // Cache the target for later lookups and rendering
        lastTargetName = actorName;
        lastTargetActor = actor;

        // Debug log
        String debugMsg = "<col=00ffff>Tracked: " + actorName + " Weapon=" + weaponId + " Speed=" + speed + "t</col>";
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", debugMsg, null);
    }

    /**
     * Main tick handler
     */
    public void onGameTick(GameTick event) {
        // Initialize spellbook state if needed
        if (!initialized) {
            try {
                currentSpellBook = Spellbook.fromVarbit(client.getVarbitValue(Varbits.SPELLBOOK));
                log.debug("Initialized spellbook to: {}", currentSpellBook);
                initialized = true;
            } catch (Exception e) {
                // Ignore if client logic not ready
            }
        }

        VariableSpeed.onGameTick(client, event);

        // Update player cooldown (Self)
        currentAnimation = client.getLocalPlayer().getAnimation();
        boolean isAttacking = isPlayerAttacking();

        switch (playerAttackState) {
            case NOT_ATTACKING:
                if (isAttacking) {
                    performPlayerAttack();
                }
                break;
            case DELAYED_FIRST_TICK:
                playerAttackState = AttackState.DELAYED;
                // fallthrough
            case DELAYED:
                if (playerCooldownTicks <= 0) {
                    if (isAttacking) {
                        performPlayerAttack();
                    } else {
                        playerAttackState = AttackState.NOT_ATTACKING;
                    }
                }
        }

        // Decrement player cooldowns
        if (playerCooldownTicks > 0) {
            playerCooldownTicks--;
        }

        // Decrement actor cooldowns
        Iterator<Map.Entry<String, CooldownInfo>> iterator = actorCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CooldownInfo> entry = iterator.next();
            CooldownInfo info = entry.getValue();
            if (info.ticks > 0) {
                info.ticks--;
            }
            // Use 0 as "ready". If < 0, maybe remove?
            // Let's keep them at 0 for a moment or remove if they are dead/gone?
            // Actually, simply keeping them at 0 is fine for "Ready" state logic.
            // But we should prune eventually. Prune if actor is dead or invalid?
            // RL Actors are reused? Safer to prune if ticks <= -5 or something?
            // For now, prune if ticks <= 0 to keep map clean (Ready state implies 0).
            // But if we want to show "Ready" overlay, we need it to stay 0.
            // But Overlay hides "Ready".
            // So we can remove if <= 0!
            if (info.ticks <= 0) {
                iterator.remove();
            }
        }
    }

    /**
     * Perform an attack (start cooldown)
     */
    private void performPlayerAttack() {
        playerAttackState = AttackState.DELAYED_FIRST_TICK;
        int weaponId = getWeaponId();
        AnimationData curAnimation = AnimationData.fromId(client.getLocalPlayer().getAnimation());
        PoweredStaves stave = PoweredStaves.getPoweredStaves(weaponId, curAnimation);
        boolean matchesSpellbook = matchesSpellbook(curAnimation);

        playerWeaponSpeed = getWeaponSpeed(client.getLocalPlayer(), weaponId, curAnimation, stave, matchesSpellbook);
        playerCooldownTicks = playerWeaponSpeed;

        log.debug("Player attack: anim={}, matchesBook={}, speed={}, weaponId={}",
                curAnimation, matchesSpellbook, playerWeaponSpeed, weaponId);
    }

    /**
     * Check if player is attacking
     */
    private boolean isPlayerAttacking() {
        int animationId = client.getLocalPlayer().getAnimation();
        if (AnimationData.isBlockListAnimation(animationId)) {
            return false;
        }

        boolean notWalking = animationId != -1 || getSalamanderAttack();
        Actor target = client.getLocalPlayer().getInteracting();

        // Allow interaction with any Actor (Player or NPC) to trigger attack state
        // checks
        if (target != null) {
            return notWalking;
        }

        // Fallback to animation detection
        AnimationData fromId = AnimationData.fromId(animationId);
        if (fromId == AnimationData.RANGED_BLOWPIPE || fromId == AnimationData.RANGED_BLAZING_BLOWPIPE) {
            return false; // These exceed their cooldown duration
        }
        return fromId != null;
    }

    /**
     * Get weapon ID for local player
     */
    private int getWeaponId() {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null) {
            return -1;
        }
        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        int weaponId = weapon != null ? weapon.getId() : -1;
        return WEAPON_ID_MAPPING_WORKAROUNDS.getOrDefault(weaponId, weaponId);
    }

    /**
     * Get weapon ID for any actor
     */
    private int getWeaponId(Actor actor) {
        if (actor instanceof Player) {
            Player p = (Player) actor;
            if (p.getPlayerComposition() != null) {
                int weaponId = p.getPlayerComposition().getEquipmentId(KitType.WEAPON);
                return WEAPON_ID_MAPPING_WORKAROUNDS.getOrDefault(weaponId, weaponId);
            }
        }
        // NPCs don't have weapons in the same way
        return -1;
    }

    /**
     * Calculate weapon speed with variable speed modifiers
     */
    private int getWeaponSpeed(Actor actor, int weaponId, AnimationData curAnimation, PoweredStaves stave,
            boolean matchesSpellbook) {
        if (stave != null && stave.getAnimations().contains(curAnimation)) {
            return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.POWERED_STAVE, 4);
        }

        // Trust manual casting if the animation is explicitly for magic, even if our
        // spellbook tracking is slightly off.
        // AnimationData.isManualCasting() ensures it's a valid magic animation.
        // For targets, matchesSpellbook is usually false, but isManualCasting logic
        // overrides it.
        if (AnimationData.isManualCasting(curAnimation)) {
            int baseSpeed = NON_STANDARD_MAGIC_WEAPON_SPEEDS.getOrDefault(weaponId, 5);
            return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.MANUAL_AUTO_CAST, baseSpeed);
        }

        ItemStats weaponStats = itemManager.getItemStats(weaponId);
        if (weaponStats == null) {
            return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.MELEE_OR_RANGE, 4);
        }
        return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.MELEE_OR_RANGE,
                weaponStats.getEquipment().getAspeed());
    }

    /**
     * Check if animation matches current spellbook
     */
    private boolean matchesSpellbook(AnimationData curAnimation) {
        if (curAnimation != null && curAnimation.matchesSpellbook(currentSpellBook)) {
            return true;
        }
        if (client.getTickCount() == soundEffectTick) {
            return CastingSoundData.getSpellBookFromId(soundEffectId) == currentSpellBook;
        }
        return false;
    }

    /**
     * Check for salamander attack
     */
    private boolean getSalamanderAttack() {
        return client.getLocalPlayer().hasSpotAnim(SALAMANDER_SET_ANIM_ID);
    }

    /**
     * Check if player can attack (cooldown is 0)
     */
    public boolean isPlayerReady() {
        return playerCooldownTicks == 0;
    }

    /**
     * Check if target can attack
     */
    public boolean isTargetReady() {
        return getTargetCooldownTicks() == 0;
    }

    // API for Overlay

    public int getTargetCooldownTicks() {
        // Use cached target name if getInteracting() is null
        Actor target = client.getLocalPlayer().getInteracting();
        String name = (target != null) ? target.getName() : lastTargetName;
        if (name == null)
            return 0;
        CooldownInfo info = actorCooldowns.get(name);
        return info != null ? info.ticks : 0;
    }

    public int getTargetWeaponSpeed() {
        Actor target = client.getLocalPlayer().getInteracting();
        String name = (target != null) ? target.getName() : lastTargetName;
        if (name == null)
            return 4;
        CooldownInfo info = actorCooldowns.get(name);
        return info != null ? info.maxTicks : 4;
    }

    /**
     * Get cached target actor for rendering
     */
    public Actor getLastTargetActor() {
        Actor current = client.getLocalPlayer().getInteracting();
        return current != null ? current : lastTargetActor;
    }

    public int getActorCooldownTicks(Actor actor) {
        if (actor == null)
            return 0;
        // Prioritize self
        if (actor == client.getLocalPlayer())
            return playerCooldownTicks;

        String name = actor.getName();
        if (name == null)
            return 0;
        CooldownInfo info = actorCooldowns.get(name);
        return info != null ? info.ticks : 0;
    }

    public int getActorMaxTicks(Actor actor) {
        if (actor == null)
            return 4;
        if (actor == client.getLocalPlayer())
            return playerWeaponSpeed;

        String name = actor.getName();
        if (name == null)
            return 4;
        CooldownInfo info = actorCooldowns.get(name);
        return info != null ? info.maxTicks : 4;
    }
}
