package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.CombatAPI;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Unified target acquisition and management system.
 * Replaces the fragmented target management across multiple classes.
 * Handles HP bar independent targeting and combat state detection.
 */
public class UnifiedTargetManager {
    private static Client client;

    // Target acquisition configuration
    private static final double MAX_TARGET_DISTANCE = 20.0;
    private static final double MAX_COMBAT_DISTANCE = 18.0;
    private static final int TARGET_SEARCH_TICKS = 5; // Search every 5 ticks
    private static int searchTickCounter = 0;

    /**
     * Initialize the target manager
     */
    public static void initialize(Client clientRef) {
        client = clientRef;
        Logger.norm("[UnifiedTargetManager] Initialized with client reference");
    }

    /**
     * Main tick method - called every game tick
     */
    public static void onGameTick(GameTick event) {
        if (client == null || client.getLocalPlayer() == null) {
            LmsState.clearTarget();
            return;
        }

        // Update combat state based on interactions
        updateCombatState();

        // Check for target timeout
        if (LmsState.hasTarget() && LmsState.isTargetExpired()) {
            Logger.norm("[UnifiedTargetManager] Target expired, clearing");
            LmsState.clearTarget();
        }

        // Periodic target acquisition (only when we don't have a target)
        searchTickCounter++;
        if (!LmsState.hasTarget() && searchTickCounter >= TARGET_SEARCH_TICKS) {
            searchTickCounter = 0;
            tryAcquireTarget();
        }

        // Update existing target if we have one
        if (LmsState.hasTarget()) {
            updateExistingTarget();
        }
    }

    /**
     * Process interaction changes to detect combat
     */
    public static void onInteractingChanged(InteractingChanged event) {
        if (client == null || client.getLocalPlayer() == null)
            return;

        Actor source = event.getSource();
        Actor target = event.getTarget();
        Player local = client.getLocalPlayer();

        // Check if local player is involved
        if (source != null && source instanceof Player && source == local) {
            // Local player is interacting with someone
            if (target != null && target instanceof Player) {
                Player targetPlayer = (Player) target;

                // Don't target ignored players
                if (LmsState.isIgnored(targetPlayer)) {
                    Logger.norm("[UnifiedTargetManager] Ignoring interaction with ignored player: "
                            + targetPlayer.getName());
                    return;
                }

                // Set target if we're attacking someone
                LmsState.setTarget(targetPlayer);
                Logger.norm("[UnifiedTargetManager] Local player attacking: " + targetPlayer.getName());
            }
        }

        // Check if local player is being targeted
        if (target != null && target instanceof Player && target == local) {
            if (source != null && source instanceof Player) {
                Player attacker = (Player) source;

                // Don't target ignored players even if they attack us
                if (LmsState.isIgnored(attacker)) {
                    Logger.norm("[UnifiedTargetManager] Ignoring attack from ignored player: " + attacker.getName());
                    return;
                }

                // Set attacker as target (defensive targeting)
                LmsState.setTarget(attacker);
                Logger.norm("[UnifiedTargetManager] Being attacked by: " + attacker.getName());
            }
        }
    }

    /**
     * Update combat state based on current interactions
     */
    private static void updateCombatState() {
        if (client == null || client.getLocalPlayer() == null)
            return;

        Player local = client.getLocalPlayer();
        boolean isAttacking = false;
        boolean isBeingAttacked = false;

        // Check if we're attacking someone
        if (local.getInteracting() != null && local.getInteracting() instanceof Player) {
            isAttacking = true;
        }

        // Check if someone is attacking us
        for (Player player : client.getPlayers()) {
            if (player != null && player.getInteracting() == local) {
                isBeingAttacked = true;
                break;
            }
        }

        LmsState.setCombatState(isAttacking, isBeingAttacked);
    }

    /**
     * Try to acquire a new target (public method for external calls)
     */
    public static void tryAcquireTarget() {
        // Don't acquire targets if we're busy with high-priority tasks
        if (LmsState.getCurrentTask() == LmsState.LmsTask.UPGRADE_GEAR) {
            return; // Don't interrupt upgrades
        }

        // First check if we're being attacked (highest priority)
        Player attacker = findCurrentAttacker();
        if (attacker != null) {
            LmsState.setTarget(attacker);
            Logger.norm("[UnifiedTargetManager] Acquired attacker target: " + attacker.getName());
            return;
        }

        // Then look for nearby targets
        Player target = findBestNearbyTarget();
        if (target != null) {
            LmsState.setTarget(target);
            Logger.norm("[UnifiedTargetManager] Acquired nearby target: " + target.getName());
        }
    }

    /**
     * Update existing target (check if still valid)
     */
    private static void updateExistingTarget() {
        Player currentTarget = LmsState.getCurrentTarget();
        if (currentTarget == null)
            return;

        // Basic validity checks - don't use full isValidTarget() as it's too strict
        // during countdown/early game (PvP area not yet active, etc)

        // Only clear if target is definitely invalid:
        // 1. Target is null/gone
        // 2. Target is dead
        // 3. Target is way too far (> 40 tiles)
        // 4. We're not in LMS instance at all

        if (!LmsState.isInLmsInstance()) {
            Logger.norm("[UnifiedTargetManager] Not in LMS instance, clearing target");
            LmsState.clearTarget();
            return;
        }

        if (currentTarget.getHealthRatio() == 0) {
            Logger.norm("[UnifiedTargetManager] Target dead, clearing: " + currentTarget.getName());
            LmsState.clearTarget();
            return;
        }

        WorldPoint targetPos = currentTarget.getWorldLocation();
        WorldPoint localPos = client != null && client.getLocalPlayer() != null
                ? client.getLocalPlayer().getWorldLocation()
                : null;

        if (targetPos != null && localPos != null) {
            double distance = localPos.distanceTo(targetPos);
            if (distance > 40.0) {
                Logger.norm("[UnifiedTargetManager] Target too far (" + distance + " tiles), clearing");
                LmsState.clearTarget();
                return;
            }
        }

        // Target is still valid - update timestamp if in combat
        if (LmsState.isInCombat()) {
            // Refresh target timeout during combat
            LmsState.setTarget(currentTarget); // This updates the timestamp
        }
    }

    /**
     * Find player currently attacking us
     */
    private static Player findCurrentAttacker() {
        if (client == null || client.getLocalPlayer() == null)
            return null;

        Player local = client.getLocalPlayer();

        for (Player player : client.getPlayers()) {
            if (player == null || player == local)
                continue;

            // Check if this player is attacking us
            if (player.getInteracting() == local) {
                // Ensure the player appears alive (has health)
                if (player.getHealthRatio() > 0) {
                    // Check if not ignored
                    if (!LmsState.isIgnored(player)) {
                        return player;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find the best nearby target for acquisition
     */
    private static Player findBestNearbyTarget() {
        if (client == null || client.getLocalPlayer() == null)
            return null;

        Player local = client.getLocalPlayer();
        WorldPoint localPos = local.getWorldLocation();

        if (localPos == null)
            return null;

        List<Player> candidates = new ArrayList<>();

        // Find all valid targets
        for (Player player : client.getPlayers()) {
            if (player == null || player == local)
                continue;

            if (isValidTarget(player)) {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty())
            return null;

        // Sort by distance (closest first)
        candidates.sort((a, b) -> {
            WorldPoint posA = a.getWorldLocation();
            WorldPoint posB = b.getWorldLocation();

            if (posA == null)
                return 1;
            if (posB == null)
                return -1;

            double distA = localPos.distanceTo(posA);
            double distB = localPos.distanceTo(posB);

            return Double.compare(distA, distB);
        });

        return candidates.get(0);
    }

    /**
     * Check if a player is a valid target
     */
    private static boolean isValidTarget(Player player) {
        if (player == null)
            return false;

        // Must be in LMS instance
        if (!LmsState.isInLmsInstance())
            return false;

        // Must be in PvP area
        if (!CombatAPI.isInPvpArea())
            return false;

        // Must not be ignored
        if (LmsState.isIgnored(player))
            return false;

        // Must not be dead
        if (player.getHealthRatio() == 0)
            return false;

        // Must be reachable
        WorldPoint targetPos = player.getWorldLocation();
        if (targetPos == null)
            return false;

        WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
        if (localPos == null)
            return false;

        double distance = localPos.distanceTo(targetPos);
        if (distance > MAX_TARGET_DISTANCE)
            return false;

        // Check if we can path to them
        try {
            if (!MovementAPI.canPathTo(targetPos)) {
                // Allow if very close (might be in combat)
                if (distance > 8.0)
                    return false;
            }
        } catch (Exception e) {
            // Fallback to distance check
            if (distance > 8.0)
                return false;
        }

        // Check if player is already fighting someone else
        if (isInCombatWithSomeoneElse(player))
            return false;

        return true;
    }

    /**
     * Check if a player is already fighting someone else
     */
    private static boolean isInCombatWithSomeoneElse(Player player) {
        if (player == null || client == null || client.getLocalPlayer() == null)
            return false;

        Player local = client.getLocalPlayer();
        Actor interacting = player.getInteracting();

        if (interacting == null)
            return false;

        // If interacting with someone who is NOT us, they're in combat with someone
        // else
        return interacting != local;
    }

    /**
     * Force clear current target
     */
    public static void clearTarget() {
        LmsState.clearTarget();
    }

    /**
     * Manually set a target (bypasses validation)
     */
    public static void setTarget(Player player) {
        LmsState.setTarget(player);
    }

    /**
     * Get current target
     */
    public static Player getCurrentTarget() {
        return LmsState.getCurrentTarget();
    }

    /**
     * Check if we have a valid target
     */
    public static boolean hasTarget() {
        return LmsState.hasTarget();
    }

    /**
     * Check if we should attack targets without HP bars
     * Implements the user requirement to attack players even without visible HP
     * bars
     */
    public static boolean shouldAttackWithoutHpBar(Player player) {
        if (player == null)
            return false;

        // In LMS, we should attack valid targets even if HP bar isn't showing
        // This handles edge cases where HP bars are delayed or hidden

        // Must be a valid target by other criteria
        if (!isValidTarget(player))
            return false;

        // Additional check: only attack if we're in a combat-appropriate location
        if (LmsState.isSafeZoneBoxEnforced() && LmsState.isInsideSafeZoneBox(player.getWorldLocation())) {
            // Don't attack players inside the safe zone box
            return false;
        }

        return true;
    }

    /**
     * Get target information for debugging
     */
    public static String getTargetInfo() {
        if (!LmsState.hasTarget())
            return "No target";

        Player target = LmsState.getCurrentTarget();
        if (target == null)
            return "No target";

        String name = target.getName() != null ? target.getName() : "Unknown";
        WorldPoint pos = target.getWorldLocation();
        String position = pos != null ? "(" + pos.getX() + "," + pos.getY() + ")" : "(unknown)";

        return String.format("%s %s [Combat: %s, Attacking: %s, BeingAttacked: %s]",
                name, position,
                LmsState.isInCombat(),
                LmsState.isAttacking(),
                LmsState.isBeingAttacked());
    }
}
