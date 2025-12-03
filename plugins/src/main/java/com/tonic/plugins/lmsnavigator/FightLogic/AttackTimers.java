package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.widgets.InventoryAPI;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight attack cooldown tracker for LMS Navigator.
 * Mirrors the semantics of the AttackTimer plugin:
 *  - When an attack animation is detected, start a timer at the weapon's tick speed.
 *  - The timer counts down once per GameTick, but not on the tick it was created.
 *  - We track separate cooldowns for the local player and their opponent.
 */
public class AttackTimers
{
    private static Client client;

    // Animation -> tick length mapping
    private static final Map<Integer, Integer> ANIMATION_TICKS = new HashMap<>();

    // Player and target cooldowns
    private static int playerCooldownTicks = 0;
    private static int targetCooldownTicks = 0;
    private static boolean playerNewThisTick = false;
    private static boolean targetNewThisTick = false;

    // Minimum player cooldown (invisible -3 window used for shark eating logic)
    private static final int MIN_PLAYER_COOLDOWN = -3;

    // Track shark count so we can extend cooldown by +3 for each shark that leaves inventory
    private static int lastSharkCount = -1;
    private static boolean suppressNextSharkDiffIncrement = false;
    private static boolean sharkEatenThisTick = false;

    static
    {
        // ===== Melee combat animation ids =====
        // Basic unarmed
        put(422, 4);   // Punch 4 ticks
        put(423, 4);   // Kick 4 ticks

        // One-handed swords / stab
        put(390, 4);   // ONEHAND_SLASH_SWORD 4 ticks (also used for claws spec)
        put(386, 5);   // Onehand Stab Sword 5 ticks

        // Scythe of Vitur
        put(8056, 5);  // Scythe of Vitur 5 ticks

        // Whip / Tentacle
        put(1658, 4);  // Whip/Tent 4 ticks
        put(1659, 4);  // Tentacle alt anim 4 ticks

        // Dharok's greataxe
        put(2067, 7);  // Dharoks Great Axe_1 7 ticks
        put(2066, 7);  // Dharoks Great Axe_2 7 ticks

        // 2h heavy swords
        put(2068, 6);  // 2H heavy swords 6 ticks
        put(7045, 6);  // 2H heavy swords / AGS wack 6 ticks

        // Armadyl godsword
        put(7644, 6);  // AGS spec/attack 6 ticks

        // Vesta's longsword
        put(7515, 5);  // Vesta spec 5 ticks

        // Ghrazi rapier
        put(8145, 4);  // Ghrazi Rapier 4 ticks

        // Ancient godsword
        put(9171, 6);  // Ancient Godsword spec 6 ticks

        // Abyssal dagger
        put(3297, 4);  // Abyssal Dagger poke 4 ticks
        put(3300, 4);  // Abyssal Dagger spec 4 ticks

        // Dragon dagger
        put(376, 4);   // DDS Wack_1 4 ticks
        put(377, 4);   // DDS Wack_2 4 ticks
        put(1062, 4);  // DDS spec 4 ticks

        // Colossal blade
        put(7516, 5);  // Colossal Blade 5 ticks

        // Voidwaker
        put(1378, 4);  // Voidwaker spec 4 ticks
        put(11275, 4); // Voidwaker spec alt 4 ticks

        // Soulreaper axe
        put(10171, 6); // Soulreaper smash 6 ticks
        put(10172, 6); // Soulreaper wack 6 ticks
        put(10173, 6); // Soulreaper spec 6 ticks

        // Dual Macuahuitl
        put(10989, 6); // Dual Macuahuitl 6 ticks

        // Granite maul
        put(1665, 5);  // Gmaul wack 5 ticks
        put(1667, 5);  // Gmaul spec 5 ticks

        // Dragon claws (wacks + spec)
        put(415, 4);   // Claw Wack_1 4 ticks
        put(393, 4);   // Claw Wack_2 4 ticks
        put(401, 4);   // Dclaws spec 4 ticks
        put(407, 4);   // Dclaws spec variant 4 ticks
        put(7514, 4);  // Dclaws spec anim 4 ticks

        // Halberd / staff bash style heavy hits
        put(440, 6);   // Halberd wack / staff bash 6 ticks
        put(414, 6);   // Staff Bash_1 6 ticks
        put(419, 6);   // Staff Bash_2 6 ticks
        put(2078, 6);  // Staff Bash_3 6 ticks

        // Leaf-bladed battleaxe
        put(7004, 5);  // Leaf-bladed battleaxe 5 ticks

        // Viggora's chainmace
        put(245, 6);   // Viggora 6 ticks

        // Misc melee defaults (no explicit comments in snippet - treat as 4 tick attacks)
        put(25979, 4);
        put(12297, 4);
        put(2062, 4);

        // ===== Ranged combat animation ids =====
        put(11057, 3); // Eclipse Atlatl 3 ticks
        put(426, 5);   // Bows 5 ticks
        put(4230, 5);  // Crossbow / Dragon Xbow spec 5 ticks
        put(7552, 5);  // Crossbow variant 5 ticks
        put(929, 2);   // Throwing knives 2 ticks
        put(8194, 2);  // Dragon knives 2 ticks
        put(5061, 3);  // Blowpipe attack 3 ticks
        put(7218, 6);  // Heavy ballista 6 ticks
        put(9964, 5);  // Webweaver spec 5 ticks
        put(9166, 5);  // Zaryte crossbow spec 5 ticks

        // Additional crossbow / bow animations from AttackTimer plugin (all 5-tick)
        int[] crossbowAnims = {9168, 9169, 9170, 7617, 7618, 9171, 9172, 4230, 7615, 7616};
        for (int anim : crossbowAnims)
        {
            putIfAbsent(anim, 5);
        }
        int[] bowAnims = {426, 7618, 7619};
        for (int anim : bowAnims)
        {
            putIfAbsent(anim, 5);
        }

        // ===== Magic animations (Ice Barrage, Blitz, Entangle, Fire Surge, etc.) =====
        // These mirror AttackTimer's MAGIC_ANIMATIONS and are treated as 5-tick spells.
        int[] magicAnims = {
            // Core ancient/standard casts from AttackTimer
            7855,  // Common magic casting / Ice Barrage / Fire Surge high anim
            7854,
            7856,
            1978,  // Ice Blitz
            1979,  // Ice Burst
            711,   // Ancient/Entangle cast
            1167,
            1162,
            428,
            763,
            7853,

            // Extra 5-tick spell animations from SPELL_ANIMATIONS
            710,
            727,
            811,
            1161,
            1820,
            8532,
            9145,
            10501,
            11423,
            11430
        };
        for (int anim : magicAnims)
        {
            putIfAbsent(anim, 5);
        }
    }

    private static void put(int animId, int ticks)
    {
        ANIMATION_TICKS.put(animId, ticks);
    }

    private static void putIfAbsent(int animId, int ticks)
    {
        ANIMATION_TICKS.putIfAbsent(animId, ticks);
    }

    public static void setClient(Client c)
    {
        client = c;
        Logger.norm("[AttackTimers] Client reference set");
    }

    public static void onAnimationChanged(AnimationChanged event)
    {
        if (client == null)
        {
            return;
        }

        Actor actor = event.getActor();
        if (actor == null)
        {
            return;
        }

        int animationId = actor.getAnimation();
        if (animationId <= 0)
        {
            return;
        }

        Integer ticks = ANIMATION_TICKS.get(animationId);
        if (ticks == null || ticks <= 0)
        {
            return; // Not a tracked attack animation
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        if (actor == local)
        {
            playerCooldownTicks = ticks;
            playerNewThisTick = true;
            return;
        }

        // Treat as target if:
        // 1) We're attacking them (local.getInteracting() == actor), or
        // 2) They are attacking us (actor.getInteracting() == local)
        Actor ourTarget = local.getInteracting();
        Actor theirTarget = actor.getInteracting();

        boolean isOurTarget = ourTarget != null && ourTarget == actor;
        boolean isAttackingUs = theirTarget != null && theirTarget == local;

        if (isOurTarget || isAttackingUs)
        {
            targetCooldownTicks = ticks;
            targetNewThisTick = true;
        }
    }

    public static void onGameTick(GameTick event)
    {
        // Player cooldown: counts down every tick until MIN_PLAYER_COOLDOWN (-3).
        if (playerCooldownTicks > MIN_PLAYER_COOLDOWN)
        {
            if (!playerNewThisTick)
            {
                playerCooldownTicks--;
            }
            else
            {
                playerNewThisTick = false;
            }
        }
        else
        {
            // Once we reach MIN_PLAYER_COOLDOWN, just clear the new-this-tick flag.
            playerNewThisTick = false;
        }

        if (targetCooldownTicks > 0)
        {
            if (!targetNewThisTick)
            {
                targetCooldownTicks--;
            }
            else
            {
                targetNewThisTick = false;
            }
        }

        // === Shark tracking: add +3 ticks for each shark that leaves inventory ===
        if (sharkEatenThisTick)
        {
            int ticksToAdd = 3;

            // Cap the timer at 8 ticks, mirroring AttackTimerPlugin's anglerfish logic
            if (playerCooldownTicks + ticksToAdd > 8)
            {
                ticksToAdd = 8 - playerCooldownTicks;
            }

            if (ticksToAdd > 0)
            {
                playerCooldownTicks += ticksToAdd;
            }
        }

        sharkEatenThisTick = false;
    }

    public static int getPlayerCooldown()
    {
        return playerCooldownTicks;
    }

    public static int getTargetCooldown()
    {
        return targetCooldownTicks;
    }

    public static void onSharkEaten()
    {
        sharkEatenThisTick = true;
    }

    public static int getPlayerHpFromOrb()
    {
        if (client == null)
        {
            return CombatAPI.getHealth();
        }

        try
        {
            return Static.invoke(() ->
            {
                // Primary source: HP orb widget text from the minimap group (same widget ID
                // used by AttackTimerPlugin.debugHpCheck Method 6).
                Widget minimapWidget = client.getWidget(35913750);
                if (minimapWidget != null)
                {
                    Widget[] children = minimapWidget.getChildren();
                    if (children != null)
                    {
                        for (Widget child : children)
                        {
                            if (child == null || child.isHidden())
                            {
                                continue;
                            }

                            String text = child.getText();
                            if (text == null || text.isEmpty())
                            {
                                continue;
                            }

                            // HP orb usually shows either "current/max" or just "current" (<= 3 digits)
                            if (text.matches("\\d+/\\d+") || (text.matches("\\d+") && text.length() <= 3))
                            {
                                String widgetName = child.getName();
                                String lowerName = widgetName != null ? widgetName.toLowerCase() : "";

                                if (lowerName.contains("hitpoint") || lowerName.contains("health") ||
                                    lowerName.contains("hp") || lowerName.contains("orb"))
                                {
                                    String hpText = text;
                                    int slashIdx = hpText.indexOf('/');
                                    if (slashIdx >= 0)
                                    {
                                        hpText = hpText.substring(0, slashIdx);
                                    }

                                    try
                                    {
                                        return Integer.parseInt(hpText.trim());
                                    }
                                    catch (NumberFormatException ignored)
                                    {
                                        // Fall through to fallback below if parsing fails
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                // Fallback: VitaLite CombatAPI (boosted HP). This may not be LMS-accurate but
                // is better than returning 0 if the orb widget is unavailable.
                return CombatAPI.getHealth();
            });
        }
        catch (Exception e)
        {
            return CombatAPI.getHealth();
        }
    }
}
