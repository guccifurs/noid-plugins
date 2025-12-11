package com.tonic.plugins.gearswapper.combat;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Hitsplat;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks combat events and correlates animations, projectiles, and hitsplats
 * to provide comprehensive attack timing data.
 */
public class CombatAnalyticsTracker {
    private static final Logger logger = LoggerFactory.getLogger(CombatAnalyticsTracker.class);

    // Game cycles per tick (approximately)
    private static final int CYCLES_PER_TICK = 30;

    private final Client client;
    private final List<AttackRecord> attackHistory = new ArrayList<>();
    private AttackRecord currentAttack = null;
    private int maxHistorySize = 10;

    // Pending projectile tracking
    private Projectile pendingProjectile = null;
    private int pendingProjectileStartCycle = 0;
    private int pendingProjectileEndCycle = 0;

    // Pending hitsplat tracking (for on-PID melee where hitsplat fires before
    // animation)
    private Actor pendingHitsplatTarget = null;
    private Hitsplat pendingHitsplat = null;
    private int pendingHitsplatTick = -1;

    // PID status callback
    private PidStatusListener pidStatusListener;

    public interface PidStatusListener {
        void onPidStatusUpdated(PidDetector.PidStatus status);
    }

    public CombatAnalyticsTracker(Client client) {
        this.client = client;
    }

    public void setPidStatusListener(PidStatusListener listener) {
        this.pidStatusListener = listener;
    }

    /**
     * Record for a single attack with all captured data
     */
    public static class AttackRecord {
        // Cast data
        public int castTick;
        public int playerAnimationId;
        public String attackType; // "Melee", "Ranged", "Magic", "Unknown"
        public int distanceToTarget;
        public String targetName;

        // Projectile data
        public boolean hasProjectile = false;
        public int projectileId;
        public int projectileStartCycle;
        public int projectileEndCycle;
        public int projectileTravelCycles;
        public double projectileTravelTicks;

        // Hitsplat data
        public boolean hasHitsplat = false;
        public int hitsplatTick;
        public int hitsplatType;
        public int hitsplatAmount;
        public boolean isSplash = false;

        // GFX data (for splash detection - records when GFX appears on target)
        public boolean hasTargetGfx = false;
        public int targetGfxId;
        public int targetGfxTick; // The tick when the GFX appeared
        public int ticksFromCastToGfx; // Cast → GFX timing in ticks

        // PID detection
        public PidDetector.PidStatus pidStatus = PidDetector.PidStatus.UNKNOWN;

        // Calculated summary
        public int ticksFromCastToHit;
        public long timestamp;

        public AttackRecord(int castTick) {
            this.castTick = castTick;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Attack @ tick ").append(castTick);
            if (hasProjectile) {
                sb.append(" | Proj: ").append(projectileId);
                sb.append(" (").append(String.format("%.1f", projectileTravelTicks)).append(" ticks)");
            }
            if (hasHitsplat) {
                sb.append(" | ").append(isSplash ? "SPLASH" : "HIT:" + hitsplatAmount);
                sb.append(" (+").append(ticksFromCastToHit).append("t)");
            }
            return sb.toString();
        }
    }

    /**
     * Called when the local player performs an attack animation
     */
    public void onPlayerAttack(int animationId, Actor target) {
        if (target == null) {
            return;
        }

        int currentTick = client.getTickCount();

        // Create new attack record
        currentAttack = new AttackRecord(currentTick);
        currentAttack.playerAnimationId = animationId;
        currentAttack.attackType = classifyAttackType(animationId);
        currentAttack.targetName = target.getName();

        // Calculate Chebyshev distance (max of x_diff, y_diff) - required for hit delay
        // formulas
        if (client.getLocalPlayer() != null) {
            WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
            WorldPoint targetLoc = target.getWorldLocation();
            if (playerLoc != null && targetLoc != null) {
                // Chebyshev distance = max(|dx|, |dy|)
                int dx = Math.abs(playerLoc.getX() - targetLoc.getX());
                int dy = Math.abs(playerLoc.getY() - targetLoc.getY());
                currentAttack.distanceToTarget = Math.max(dx, dy);
            }
        }

        logger.debug("[Combat Analytics] Attack started: anim={}, target={}, distance={}",
                animationId, currentAttack.targetName, currentAttack.distanceToTarget);

        // Check for pending hitsplat from same tick (on-PID melee detection)
        // For on-PID melee, the hitsplat fires BEFORE the animation on the same tick
        if (pendingHitsplat != null && pendingHitsplatTick == currentTick) {
            // Check if the pending hitsplat was on our target
            if (pendingHitsplatTarget != null && currentAttack.targetName != null
                    && currentAttack.targetName.equals(pendingHitsplatTarget.getName())) {

                logger.debug("[Combat Analytics] Found pending hitsplat on same tick - On-PID melee detected!");

                currentAttack.hasHitsplat = true;
                currentAttack.hitsplatTick = pendingHitsplatTick;
                currentAttack.hitsplatType = pendingHitsplat.getHitsplatType();
                currentAttack.hitsplatAmount = pendingHitsplat.getAmount();
                currentAttack.ticksFromCastToHit = 0; // Same tick = 0 ticks delay
                currentAttack.isSplash = false; // Melee can't splash

                // Clear pending hitsplat
                pendingHitsplat = null;
                pendingHitsplatTarget = null;
                pendingHitsplatTick = -1;

                // Finalize immediately
                finalizeAttack();
            }
        }
    }

    /**
     * Called when a projectile is spawned
     */
    public void onProjectileSpawned(Projectile projectile) {
        if (currentAttack == null) {
            // No active attack - might be NPC projectile, store it anyway
            pendingProjectile = projectile;
            pendingProjectileStartCycle = projectile.getStartCycle();
            pendingProjectileEndCycle = projectile.getEndCycle();
            return;
        }

        // Check if this projectile is from local player to their target
        Actor target = projectile.getInteracting();
        if (target != null && currentAttack.targetName != null
                && currentAttack.targetName.equals(target.getName())) {

            currentAttack.hasProjectile = true;
            currentAttack.projectileId = projectile.getId();
            currentAttack.projectileStartCycle = projectile.getStartCycle();
            currentAttack.projectileEndCycle = projectile.getEndCycle();
            currentAttack.projectileTravelCycles = projectile.getEndCycle() - projectile.getStartCycle();
            currentAttack.projectileTravelTicks = (double) currentAttack.projectileTravelCycles / CYCLES_PER_TICK;

            logger.debug("[Combat Analytics] Projectile captured: id={}, cycles={}, ticks={}",
                    currentAttack.projectileId, currentAttack.projectileTravelCycles,
                    String.format("%.2f", currentAttack.projectileTravelTicks));
        }
    }

    /**
     * Called when a hitsplat appears on an actor
     */
    public void onHitsplatApplied(Actor target, Hitsplat hitsplat) {
        int currentTick = client.getTickCount();

        // If no current attack, potentially store as pending hitsplat (for on-PID melee
        // detection)
        // On-PID melee: hitsplat fires BEFORE animation on the same tick
        // Only store if it's on another player (potential PvP target), not ourselves or
        // NPCs
        if (currentAttack == null) {
            // Only store hitsplats on other players (not ourselves, not NPCs)
            if (target instanceof Player && target != client.getLocalPlayer()) {
                pendingHitsplatTarget = target;
                pendingHitsplat = hitsplat;
                pendingHitsplatTick = currentTick;
                logger.debug("[Combat Analytics] Stored pending hitsplat on tick {} for player: {}",
                        currentTick, target.getName());
            }
            return;
        }

        logger.debug("[Combat Analytics] Hitsplat received - currentAttack.target={}, hitsplat.target={}",
                currentAttack.targetName, target != null ? target.getName() : "null");

        // Check if this hitsplat is on our target
        if (target != null && currentAttack.targetName != null
                && currentAttack.targetName.equals(target.getName())) {

            currentAttack.hasHitsplat = true;
            currentAttack.hitsplatTick = currentTick;
            currentAttack.hitsplatType = hitsplat.getHitsplatType();
            currentAttack.hitsplatAmount = hitsplat.getAmount();
            currentAttack.ticksFromCastToHit = currentTick - currentAttack.castTick;

            // Splash detection - only mark as splash for MAGIC attacks with 0 damage
            // A 0 hit with melee/ranged is a "block" but not a "splash"
            // HitsplatID.BLOCK_ME (12) = blue splash block indicator
            if (hitsplat.getAmount() == 0 && "Magic".equals(currentAttack.attackType)) {
                currentAttack.isSplash = true;
            } else {
                currentAttack.isSplash = false;
            }

            logger.debug("[Combat Analytics] Hitsplat captured: type={}, amount={}, isSplash={}, delay={}t",
                    currentAttack.hitsplatType, currentAttack.hitsplatAmount,
                    currentAttack.isSplash, currentAttack.ticksFromCastToHit);

            // Finalize this attack record
            finalizeAttack();
        }
    }

    /**
     * Called when a GFX/graphic appears on an actor
     */
    public void onGraphicChanged(Actor target, int graphicId) {
        if (currentAttack == null) {
            return;
        }

        // Check if this GFX is on our target
        if (target != null && currentAttack.targetName != null
                && currentAttack.targetName.equals(target.getName())) {

            if (graphicId > 0) {
                int currentTick = client.getTickCount();

                currentAttack.hasTargetGfx = true;
                currentAttack.targetGfxId = graphicId;
                currentAttack.targetGfxTick = currentTick;
                currentAttack.ticksFromCastToGfx = currentTick - currentAttack.castTick;

                logger.debug("[Combat Analytics] Target GFX captured: id={}, tick={}, delay={}t",
                        graphicId, currentTick, currentAttack.ticksFromCastToGfx);
            }
        }
    }

    /**
     * Finalize and store the current attack record
     */
    private void finalizeAttack() {
        if (currentAttack == null) {
            return;
        }

        // Analyze PID status based on timing and distance
        if (currentAttack.hasHitsplat) {
            currentAttack.pidStatus = PidDetector.analyzeAttack(
                    currentAttack.attackType,
                    currentAttack.playerAnimationId,
                    currentAttack.distanceToTarget,
                    currentAttack.ticksFromCastToHit);
            logger.debug("[Combat Analytics] PID analysis: {} (type={}, dist={}, ticks={})",
                    currentAttack.pidStatus, currentAttack.attackType,
                    currentAttack.distanceToTarget, currentAttack.ticksFromCastToHit);

            // Notify listener of PID status update
            if (pidStatusListener != null) {
                pidStatusListener.onPidStatusUpdated(currentAttack.pidStatus);
            }
        }

        // Add to history
        attackHistory.add(0, currentAttack);

        // Trim history
        while (attackHistory.size() > maxHistorySize) {
            attackHistory.remove(attackHistory.size() - 1);
        }

        logger.info("[Combat Analytics] Attack finalized: {}", currentAttack.getSummary());

        // Clear current attack (ready for next)
        currentAttack = null;
    }

    /**
     * Get the most recent attack record
     */
    public AttackRecord getLastAttack() {
        if (attackHistory.isEmpty()) {
            return null;
        }
        return attackHistory.get(0);
    }

    /**
     * Get attack history (most recent first)
     */
    public List<AttackRecord> getAttackHistory() {
        return new ArrayList<>(attackHistory);
    }

    /**
     * Get current in-progress attack (may not have hitsplat yet)
     */
    public AttackRecord getCurrentAttack() {
        return currentAttack;
    }

    /**
     * Clear all history
     */
    public void clearHistory() {
        attackHistory.clear();
        currentAttack = null;
    }

    /**
     * Classify attack type based on animation ID.
     * Animation IDs sourced from OSRS Wiki and community research.
     */
    private String classifyAttackType(int animationId) {
        // Check MAGIC first
        if (animationId == 1167 || // Trident
                animationId == 9493 || // Tumeken's shadow
                animationId == 8532 || // Sanguinesti staff
                animationId == 1162 || animationId == 1163 || // Ancient spells (barrage/blitz)
                animationId == 1978 || animationId == 1979 || // Ancient barrage variants
                isInRange(animationId, 710, 730) || // Standard spells
                animationId == 393 || // Fire/water/air/earth strike
                animationId == 724 || animationId == 727 || // Iban blast, god spells
                animationId == 811) { // Staff bash magic
            return "Magic";
        }

        // Check RANGED second
        if (animationId == 426 || // Bow (standard)
                animationId == 427 || // Crossbow (old)
                animationId == 428 || // Throwing weapons
                animationId == 7552 || // Crossbow (modern) / Rune crossbow
                animationId == 5061 || // Toxic blowpipe
                animationId == 9168 || // Zaryte crossbow
                animationId == 7617 || // Dragon crossbow / Rune crossbow
                animationId == 7618 || // Chinchompas
                animationId == 8194 || // Dragon crossbow
                animationId == 7555 || // Ballista
                animationId == 929 || // Magic shortbow (spec)
                animationId == 7521 || // Crossbow variant
                animationId == 1074 || // Magic comp bow
                animationId == 2075 || // Seercull
                animationId == 7067 || // Crystal bow
                animationId == 10914 || // Zaryte crossbow alt
                animationId == 9858 || // ACB spec
                animationId == 7549 || // Dark bow spec
                animationId == 4230) { // Bow/Crossbow animation
            return "Ranged";
        }

        // MELEE - check specific animations
        if (animationId == 9471 || // Osmumten's fang
                animationId == 7642 || // BGS spec
                animationId == 7045 || // BGS whack / Scythe
                animationId == 7054 || // Scythe
                animationId == 419 || // Keris slap
                animationId == 381 || // Keris poke
                animationId == 390 || // Voidwaker whack / Sword slash
                animationId == 386 || // Voidwaker poke / Sword
                animationId == 11275 || // Voidwaker spec
                animationId == 376 || // DDS poke
                animationId == 377 || // DDS slash
                animationId == 1062 || // DDS spec
                animationId == 1658 || // Whip
                animationId == 8145 || // Blade of saeldor
                animationId == 7514 || animationId == 7515 || // Sword slash/stab
                animationId == 1872 || // DDS spec (alt)
                animationId == 7516 || // Rapier
                animationId == 8288 || // Dragon warhammer
                animationId == 2323 || // Granite maul
                animationId == 7644 || // Voidwaker (alt)
                animationId == 1378 || // AGS spec
                isInRange(animationId, 391, 425) || // Basic melee range (excluding 426 = bow)
                isInRange(animationId, 1659, 1668)) { // Whip/abyssal variants
            return "Melee";
        }

        return "Unknown";
    }

    private boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Called each game tick to handle timeout for attacks without hitsplats
     */
    public void onGameTick() {
        int currentTick = client.getTickCount();

        // Clear stale pending hitsplat (only valid for same-tick matching)
        if (pendingHitsplat != null && pendingHitsplatTick != currentTick) {
            logger.debug("[Combat Analytics] Clearing stale pending hitsplat from tick {} (now tick {})",
                    pendingHitsplatTick, currentTick);
            pendingHitsplat = null;
            pendingHitsplatTarget = null;
            pendingHitsplatTick = -1;
        }

        if (currentAttack == null) {
            return;
        }

        int ticksSinceCast = currentTick - currentAttack.castTick;

        // If more than 10 ticks since cast and no hitsplat, finalize anyway
        if (ticksSinceCast > 10) {
            logger.debug("[Combat Analytics] Attack timed out after {} ticks", ticksSinceCast);
            finalizeAttack();
        }
    }
}
