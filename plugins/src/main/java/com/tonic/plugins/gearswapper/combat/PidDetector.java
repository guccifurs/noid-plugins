package com.tonic.plugins.gearswapper.combat;

/**
 * Detects if player has PID based on attack timing and distance.
 * 
 * PID (Player ID) determines attack order in OSRS.
 * If you have PID, your attacks hit faster.
 * 
 * Formulas based on: https://oldschool.runescape.wiki/w/Hit_delay
 * 
 * PID affects delays:
 * - If player has PID (processed first), no extra delay
 * - If player does NOT have PID (processed second), +1 tick to ALL delays
 */
public class PidDetector {

    public enum PidStatus {
        ON_PID, // Player has PID
        OFF_PID, // Player does not have PID
        UNKNOWN // Cannot determine (e.g., timing doesn't match expected values)
    }

    /**
     * Analyze an attack to determine PID status.
     * 
     * @param attackType         "Melee", "Ranged", or "Magic"
     * @param animationId        The animation ID of the attack
     * @param distance           Chebyshev distance to target at cast time
     * @param ticksFromCastToHit Ticks from cast animation to hitsplat
     * @return PID status based on timing rules
     */
    public static PidStatus analyzeAttack(String attackType, int animationId, int distance, int ticksFromCastToHit) {
        if (attackType == null || distance < 0 || ticksFromCastToHit < 0) {
            return PidStatus.UNKNOWN;
        }

        int expectedOnPid;
        int expectedOffPid;

        if ("Melee".equals(attackType)) {
            // Melee: Base delay is 0. With PID: 0 ticks. Without PID: 1 tick
            expectedOnPid = 0;
            expectedOffPid = 1;
        } else if ("Magic".equals(attackType)) {
            // Magic Standard: Formula: 1 + floor((1 + Distance) / 3)
            expectedOnPid = getMagicDelay(distance);
            expectedOffPid = expectedOnPid + 1;
        } else if ("Ranged".equals(attackType)) {
            // Determine formula based on animation ID
            if (isThrownOrBlowpipe(animationId)) {
                // Thrown/Blowpipe: 1 + floor(Distance / 6)
                expectedOnPid = getThrownDelay(distance);
            } else if (animationId == 7555) {
                // Ballista: dist<=4 ? 2 : 3
                expectedOnPid = (distance <= 4) ? 2 : 3;
            } else {
                // Standard Bow/Crossbow: 1 + floor((3 + Distance) / 6)
                // This covers 4230, 426, 7552, etc.
                expectedOnPid = getRangedDelay(distance);
            }

            expectedOffPid = expectedOnPid + 1;

            // Special case for close-range Ranged (0-1 tiles) which can match Melee timing
            // or match the formula (often 1 tick).
            // If ticks=0 or 1 at close range, assume On PID.
            if (distance <= 1 && (ticksFromCastToHit == 0 || ticksFromCastToHit == 1)) {
                return PidStatus.ON_PID;
            }
        } else {
            return PidStatus.UNKNOWN;
        }

        if (ticksFromCastToHit == expectedOnPid) {
            return PidStatus.ON_PID;
        } else if (ticksFromCastToHit == expectedOffPid) {
            return PidStatus.OFF_PID;
        }

        return PidStatus.UNKNOWN;
    }

    private static boolean isThrownOrBlowpipe(int anim) {
        return anim == 5061 || // Blowpipe
                anim == 428 || // Thrown
                anim == 7618; // Chinchompas
    }

    /**
     * Get expected delay for magic attacks (with PID).
     * Formula: 1 + floor((1 + Distance) / 3)
     */
    public static int getMagicDelay(int distance) {
        return 1 + (int) Math.floor((1.0 + distance) / 3.0);
    }

    /**
     * Get expected delay for ranged attacks - standard bows/crossbows (with PID).
     * Formula: 1 + floor((3 + Distance) / 6)
     */
    public static int getRangedDelay(int distance) {
        return 1 + (int) Math.floor((3.0 + distance) / 6.0);
    }

    /**
     * Get expected delay for thrown weapons/blowpipe (with PID).
     * Formula: 1 + floor(Distance / 6)
     */
    public static int getThrownDelay(int distance) {
        return 1 + (int) Math.floor(distance / 6.0);
    }

    /**
     * Get expected ticks for ON PID at given distance.
     * Returns magic formula as default for ranged/magic.
     */
    public static int getExpectedTicksOnPid(int distance) {
        return getMagicDelay(distance);
    }

    /**
     * Get expected ticks for OFF PID at given distance.
     * Returns magic formula + 1 as default for ranged/magic.
     */
    public static int getExpectedTicksOffPid(int distance) {
        return getMagicDelay(distance) + 1;
    }
}
