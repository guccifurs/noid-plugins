package com.tonic.plugins.lmsnavigator.FightLogic.tasks;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.FightLogic.TaskQueue;
import com.tonic.plugins.lmsnavigator.FightLogic.UpgradeManager;
import com.tonic.plugins.lmsnavigator.LMSNavigatorPlugin;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import com.tonic.plugins.lmsnavigator.KeyManagement;
import com.tonic.plugins.lmsnavigator.UnifiedTargetManager;
import com.tonic.api.game.MovementAPI;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Task to handle gear upgrades after getting a kill (bloody key in inventory).
 * 
 * Flow:
 * 1. Find nearest chest
 * 2. Move to chest
 * 3. Open chest (search)
 * 4. Process upgrades from loot
 * 5. Drop old/unwanted items
 * 6. Return to previous task
 * 
 * Can be interrupted if someone attacks us.
 */
public class UpgradeGearTask {
    private static WorldPoint targetChest = null;
    private static UpgradeState state = UpgradeState.FIND_CHEST;
    private static int ticksWaiting = 0;
    private static int stateTick = 0;
    private static int tickCount = 0;

    private enum UpgradeState {
        FIND_CHEST,
        MOVE_TO_CHEST,
        OPEN_CHEST,
        WAIT_FOR_LOOT,
        PROCESS_UPGRADES,
        CLEANUP,
        DONE
    }

    public static void tick() {
        Logger.norm("[UpgradeGearTask] tick() called, state=" + state);

        Client client = LMSNavigatorPlugin.getClient();
        if (client == null) {
            Logger.warn("[UpgradeGearTask] Client is null, aborting");
            finish();
            return;
        }

        // Abort upgrade if we get into combat; combat takes priority over upgrades.
        if (isPlayerAttackingSomeone()) {
            Logger.norm("[UpgradeGearTask] Detected combat, aborting upgrade task.");
            finish();
            return;
        }

        try {
            tickCount++;
            UpgradeState currentState = state;

            switch (currentState) {
                case FIND_CHEST:
                    handleFindChest();
                    break;
                case MOVE_TO_CHEST:
                    handleMoveToChest();
                    break;
                case OPEN_CHEST:
                    handleOpenChest();
                    break;
                case WAIT_FOR_LOOT:
                case PROCESS_UPGRADES:
                    handleProcessUpgrades();
                    break;
                case CLEANUP:
                    handleCleanup();
                    break;
                case DONE:
                    finish();
                    break;
            }
        } catch (Exception e) {
            Logger.error("[UpgradeGearTask] Error in state " + state + ": " + e.getMessage());
            e.printStackTrace();
            finish();
        }
    }

    private static void handleFindChest() {
        Logger.norm("[UpgradeGearTask] Looking for chest...");

        targetChest = UpgradeManager.findNearestChest();
        if (targetChest == null) {
            Logger.warn("[UpgradeGearTask] No chest found nearby. Skipping upgrade.");
            finish();
            return;
        }

        Logger.norm("[UpgradeGearTask] Found chest at: " + targetChest);

        // Update unified state
        LmsState.setUpgradeInProgress(true, targetChest);

        state = UpgradeState.MOVE_TO_CHEST;
        stateTick = 0;
    }

    private static void handleMoveToChest() {
        if (targetChest == null) {
            Logger.warn("[UpgradeGearTask] Target chest lost, returning to FIND_CHEST");
            state = UpgradeState.FIND_CHEST;
            return;
        }

        WorldPoint playerPos = getPlayerLocation();
        if (playerPos == null) {
            return;
        }

        double distance = playerPos.distanceTo(targetChest);
        Logger.norm("[UpgradeGearTask] Distance to chest: " + distance);

        if (distance <= 2) {
            Logger.norm("[UpgradeGearTask] Close enough to chest, opening");
            state = UpgradeState.OPEN_CHEST;
            stateTick = 0;
            return;
        }

        // Navigate to chest every few ticks to avoid spam
        if (stateTick % 5 == 0) {
            Logger.norm("[UpgradeGearTask] Walking to chest at " + targetChest);
            MovementAPI.walkToWorldPoint(targetChest.getX(), targetChest.getY());
        }

        stateTick++;
    }

    private static void handleOpenChest() {
        if (targetChest == null) {
            state = UpgradeState.FIND_CHEST;
            return;
        }

        // If we no longer have the bloody key, the chest was already looted - skip to
        // cleanup
        if (!UpgradeManager.hasBloodyKey()) {
            Logger.norm("[UpgradeGearTask] No bloody key - chest already looted, finishing");
            finish();
            return;
        }

        WorldPoint playerPos = getPlayerLocation();
        if (playerPos == null || playerPos.distanceTo(targetChest) > 3) {
            Logger.warn("[UpgradeGearTask] Too far from chest, moving back");
            state = UpgradeState.MOVE_TO_CHEST;
            return;
        }

        // Try to open chest at the target location
        UpgradeManager.openChest(targetChest);

        if (UpgradeManager.isChestOpened()) {
            Logger.norm("[UpgradeGearTask] Chest opened, processing upgrades");
            state = UpgradeState.PROCESS_UPGRADES;
            stateTick = 0;
        } else {
            stateTick++;
            if (stateTick > 10) {
                Logger.warn("[UpgradeGearTask] Failed to open chest after 10 ticks, finishing");
                finish();
            }
        }
    }

    private static void handleProcessUpgrades() {
        boolean upgradeDone = UpgradeManager.processUpgradeTick();

        if (upgradeDone) {
            Logger.norm("[UpgradeGearTask] Upgrades processed, starting cleanup");
            state = UpgradeState.CLEANUP;
            stateTick = 0;
        } else {
            stateTick++;
            // Give more time for upgrades - they can take a while
            if (stateTick > 60) { // 1 minute
                Logger.warn("[UpgradeGearTask] Upgrade processing timeout, moving to cleanup");
                state = UpgradeState.CLEANUP;
                stateTick = 0;
            }
        }
    }

    private static void handleCleanup() {
        Logger.norm("[UpgradeGearTask] Performing cleanup...");

        // Perform one-time cleanup
        boolean cleanupDone = UpgradeManager.cleanupChestLoot();

        if (cleanupDone) {
            Logger.norm("[UpgradeGearTask] Cleanup completed, finishing upgrade");
            finish();
        } else {
            stateTick++;
            if (stateTick > 20) {
                Logger.warn("[UpgradeGearTask] Cleanup timeout, finishing anyway");
                finish();
            }
        }
    }

    private static void finish() {
        Logger.norm("[UpgradeGearTask] Upgrade task finished. Resuming normal behavior.");

        // Clean up unified state
        LmsState.setUpgradeInProgress(false, null);

        reset();

        // Return to appropriate task based on priorities - use forceTask to bypass
        // priority
        if (LmsState.isFinalSafeZoneAnnounced() && !LmsState.hasReachedSafeZone()) {
            TaskQueue.forceTask(LmsState.LmsTask.GO_TO_SAFE_ZONE);
        } else if (UnifiedTargetManager.hasTarget()) {
            TaskQueue.forceTask(LmsState.LmsTask.ENGAGE_TARGET);
        } else {
            TaskQueue.forceTask(LmsState.LmsTask.ROAM);
        }
    }

    private static void reset() {
        state = UpgradeState.FIND_CHEST;
        targetChest = null;
        ticksWaiting = 0;
        stateTick = 0;
        tickCount = 0;
        UpgradeManager.setChestOpened(false);
    }

    public static WorldPoint getTargetChestLocation() {
        return targetChest;
    }

    public static boolean shouldStartUpgrade() {
        // Don't start if already upgrading
        if (LmsState.getCurrentTask() == LmsState.LmsTask.UPGRADE_GEAR) {
            return false;
        }

        // Don't start if in combat
        if (LmsState.isInCombat()) {
            return false;
        }

        // Check if we have bloody key (via shared UpgradeManager helper)
        if (!UpgradeManager.hasBloodyKey()) {
            return false;
        }

        return true;
    }

    private static WorldPoint getPlayerLocation() {
        Client client = LMSNavigatorPlugin.getClient();
        if (client == null) {
            return null;
        }
        Player local = client.getLocalPlayer();
        if (local == null) {
            return null;
        }
        return local.getWorldLocation();
    }

    private static boolean isPlayerAttackingSomeone() {
        Client client = LMSNavigatorPlugin.getClient();
        if (client == null) {
            return false;
        }
        Player local = client.getLocalPlayer();
        if (local == null) {
            return false;
        }

        for (Player p : client.getPlayers()) {
            if (p == null || p == local) {
                continue;
            }
            if (p.getInteracting() == local) {
                return true;
            }
        }
        return false;
    }
}
