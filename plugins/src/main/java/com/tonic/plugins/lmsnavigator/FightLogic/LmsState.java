package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.GameTick;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified state management for LMS Navigator.
 * This is the single source of truth for all plugin state.
 */
public class LmsState {
    // === Core References ===
    private static Client client;

    // === Task Management ===
    public enum LmsTask {
        IDLE, // No active task
        INIT_PHASE, // Initial potion sipping and setup
        ROAM, // Searching for targets/items
        GO_TO_SAFE_ZONE, // Walking to announced safe zone
        ENGAGE_TARGET, // Active combat with target
        WAIT_IGNORE, // Waiting for ignore to expire
        UPGRADE_GEAR // Looting chest and upgrading gear
    }

    // === Task Priority System ===
    private static final Map<LmsTask, Integer> TASK_PRIORITIES = new HashMap<>();
    static {
        TASK_PRIORITIES.put(LmsTask.IDLE, 0);
        TASK_PRIORITIES.put(LmsTask.ROAM, 1);
        TASK_PRIORITIES.put(LmsTask.GO_TO_SAFE_ZONE, 2);
        TASK_PRIORITIES.put(LmsTask.WAIT_IGNORE, 3);
        TASK_PRIORITIES.put(LmsTask.ENGAGE_TARGET, 4);
        TASK_PRIORITIES.put(LmsTask.INIT_PHASE, 5);
        TASK_PRIORITIES.put(LmsTask.UPGRADE_GEAR, 6);
    }

    private static LmsTask currentTask = LmsTask.IDLE;
    private static LmsTask previousTask = LmsTask.IDLE;
    private static long taskStartTime = 0;

    // === Target Management (Unified) ===
    private static Player currentTarget = null;
    private static String targetName = "Unknown";
    private static boolean hasTarget = false;
    private static boolean isInCombat = false;
    private static boolean isAttacking = false;
    private static boolean isBeingAttacked = false;
    private static long lastTargetUpdate = 0;
    private static final long TARGET_TIMEOUT_MS = 15000;

    // Target acquisition state
    private static boolean lockedTarget = false;
    private static boolean targetAcquisitionInProgress = false;

    // === Ignore System ===
    private static final Map<String, Long> ignoredPlayers = new ConcurrentHashMap<>();
    private static final long IGNORE_DURATION_MS = 10000; // 10 seconds default

    // === Safe Zone Management ===
    private static WorldPoint safeZoneLocation = null;
    private static String safeZoneName = "Unknown";
    private static boolean finalSafeZoneAnnounced = false;
    private static boolean safeZoneBoxEnforced = false;
    private static boolean reachedSafeZone = false;
    private static final int SAFE_ZONE_HALF_BOX_SIZE = 6; // 12x12 area

    // === Combat State ===
    private static boolean playerFrozen = false;
    private static boolean targetFrozen = false;
    private static int playerFreezeTicks = 0;
    private static int targetFreezeTicks = 0;

    // === Navigation State ===
    private static boolean isNavigating = false;
    private static WorldPoint navigationDestination = null;

    // === Upgrade State ===
    private static boolean upgradeInProgress = false;
    private static WorldPoint targetChestLocation = null;
    private static long lastUpgradeAttempt = 0;

    // === Plugin State ===
    private static boolean inLmsInstance = false;
    private static boolean pluginEnabled = false;
    private static long matchStartTime = 0;

    // === Initialization ===
    public static void initialize(Client clientRef) {
        client = clientRef;
        reset();
        Logger.norm("[LmsState] Initialized with client reference");
    }

    public static void reset() {
        currentTask = LmsTask.IDLE;
        previousTask = LmsTask.IDLE;
        taskStartTime = 0;

        clearTarget();
        ignoredPlayers.clear();

        safeZoneLocation = null;
        safeZoneName = "Unknown";
        finalSafeZoneAnnounced = false;
        safeZoneBoxEnforced = false;
        reachedSafeZone = false;

        playerFrozen = false;
        targetFrozen = false;
        playerFreezeTicks = 0;
        targetFreezeTicks = 0;

        isNavigating = false;
        navigationDestination = null;

        upgradeInProgress = false;
        targetChestLocation = null;
        lastUpgradeAttempt = 0;

        inLmsInstance = false;
        matchStartTime = 0;

        Logger.norm("[LmsState] All state reset");
    }

    // === Task Management with Priority System ===
    public static boolean canPreempt(LmsTask newTask) {
        if (newTask == null)
            return false;

        int currentPriority = TASK_PRIORITIES.getOrDefault(currentTask, 0);
        int newPriority = TASK_PRIORITIES.getOrDefault(newTask, 0);

        return newPriority > currentPriority;
    }

    public static void setTask(LmsTask newTask) {
        if (newTask == currentTask)
            return;

        // Only allow task changes if they have higher priority or we're finishing
        // current task
        if (newTask != LmsTask.IDLE && !canPreempt(newTask)) {
            Logger.norm("[LmsState] Task preemption blocked: " + newTask + " cannot override " + currentTask);
            return;
        }

        previousTask = currentTask;
        currentTask = newTask;
        taskStartTime = System.currentTimeMillis();

        Logger.norm("[LmsState] Task changed: " + previousTask + " -> " + currentTask + " (priority: "
                + TASK_PRIORITIES.get(newTask) + ")");

        // Cleanup previous task if needed
        onTaskChanged(previousTask, newTask);
    }

    /**
     * Force set task bypassing priority check.
     * Used when a task is finishing and needs to transition to a lower-priority
     * task.
     */
    public static void forceSetTask(LmsTask newTask) {
        if (newTask == currentTask)
            return;

        previousTask = currentTask;
        currentTask = newTask;
        taskStartTime = System.currentTimeMillis();

        Logger.norm("[LmsState] Task FORCE changed: " + previousTask + " -> " + currentTask);

        // Cleanup previous task if needed
        onTaskChanged(previousTask, newTask);
    }

    private static void onTaskChanged(LmsTask oldTask, LmsTask newTask) {
        // Clean up task-specific state
        if (oldTask == LmsTask.ENGAGE_TARGET && newTask != LmsTask.ENGAGE_TARGET) {
            // Don't clear target immediately, let timeout handle it
            Logger.norm("[LmsState] Leaving combat task, keeping target with timeout");
        }

        if (oldTask == LmsTask.UPGRADE_GEAR && newTask != LmsTask.UPGRADE_GEAR) {
            upgradeInProgress = false;
            targetChestLocation = null;
            Logger.norm("[LmsState] Upgrade task finished, cleaning up upgrade state");
        }
    }

    public static LmsTask getCurrentTask() {
        return currentTask;
    }

    public static LmsTask getPreviousTask() {
        return previousTask;
    }

    public static long getTaskStartTime() {
        return taskStartTime;
    }

    public static long getTimeInCurrentTask() {
        return System.currentTimeMillis() - taskStartTime;
    }

    // === Unified Target Management ===
    public static void setTarget(Player target) {
        if (target == null) {
            clearTarget();
            return;
        }

        currentTarget = target;
        targetName = target.getName() != null ? target.getName() : "Unknown";
        hasTarget = true;
        isInCombat = true;
        lastTargetUpdate = System.currentTimeMillis();
        lockedTarget = true;

        Logger.norm("[LmsState] Target set: " + targetName);
    }

    public static void clearTarget() {
        if (hasTarget) {
            Logger.norm("[LmsState] Target cleared: " + targetName);
        }

        currentTarget = null;
        targetName = "Unknown";
        hasTarget = false;
        isInCombat = false;
        isAttacking = false;
        isBeingAttacked = false;
        lastTargetUpdate = 0;
        lockedTarget = false;
        targetAcquisitionInProgress = false;
    }

    public static Player getCurrentTarget() {
        return currentTarget;
    }

    public static String getTargetName() {
        return targetName;
    }

    public static boolean hasTarget() {
        return hasTarget && !isTargetExpired();
    }

    public static boolean isInCombat() {
        return isInCombat && hasTarget();
    }

    public static boolean isAttacking() {
        return isAttacking && hasTarget();
    }

    public static boolean isBeingAttacked() {
        return isBeingAttacked && hasTarget();
    }

    public static boolean isTargetExpired() {
        return hasTarget && (System.currentTimeMillis() - lastTargetUpdate > TARGET_TIMEOUT_MS);
    }

    public static boolean isLockedTarget() {
        return lockedTarget;
    }

    public static void setCombatState(boolean attacking, boolean beingAttacked) {
        isAttacking = attacking;
        isBeingAttacked = beingAttacked;
        isInCombat = attacking || beingAttacked;
    }

    // === Ignore System (Improved) ===
    public static void ignoreTargetForSeconds(Player target, int seconds) {
        if (target == null || target.getName() == null)
            return;

        long expiry = System.currentTimeMillis() + (seconds * 1000L);
        ignoredPlayers.put(target.getName(), expiry);

        Logger.norm("[LmsState] Ignoring " + target.getName() + " for " + seconds + "s");
    }

    public static boolean isIgnored(Player target) {
        if (target == null || target.getName() == null)
            return false;

        Long expiry = ignoredPlayers.get(target.getName());
        if (expiry == null)
            return false;

        if (System.currentTimeMillis() > expiry) {
            ignoredPlayers.remove(target.getName());
            return false;
        }

        return true;
    }

    public static void cleanupExpiredIgnores() {
        long now = System.currentTimeMillis();
        ignoredPlayers.entrySet().removeIf(entry -> now > entry.getValue());
    }

    public static int getIgnoreSize() {
        cleanupExpiredIgnores();
        return ignoredPlayers.size();
    }

    // === Safe Zone Management ===
    public static void setSafeZone(WorldPoint safeZone) {
        safeZoneLocation = safeZone;
        if (safeZone == null) {
            safeZoneBoxEnforced = false;
            reachedSafeZone = false;
            Logger.norm("[LmsState] Safe zone cleared");
        } else {
            Logger.norm("[LmsState] Safe zone set: " + safeZone);
        }
    }

    public static void setSafeZoneName(String name) {
        safeZoneName = name != null ? name : "Unknown";
    }

    public static WorldPoint getSafeZone() {
        return safeZoneLocation;
    }

    public static String getSafeZoneName() {
        return safeZoneName;
    }

    public static void setFinalSafeZoneAnnounced(boolean announced) {
        finalSafeZoneAnnounced = announced;
        Logger.norm("[LmsState] Final safe zone announced: " + announced);
    }

    public static boolean isFinalSafeZoneAnnounced() {
        return finalSafeZoneAnnounced;
    }

    public static void setReachedSafeZone(boolean reached) {
        reachedSafeZone = reached;
        Logger.norm("[LmsState] Reached safe zone: " + reached);
    }

    public static boolean hasReachedSafeZone() {
        return reachedSafeZone;
    }

    public static void setSafeZoneBoxEnforced(boolean enforced) {
        safeZoneBoxEnforced = enforced && safeZoneLocation != null;
        Logger.norm("[LmsState] Safe zone box enforced: " + safeZoneBoxEnforced);
    }

    public static boolean isSafeZoneBoxEnforced() {
        return safeZoneBoxEnforced;
    }

    public static boolean isInsideSafeZoneBox(WorldPoint point) {
        if (point == null || safeZoneLocation == null || !safeZoneBoxEnforced) {
            return true;
        }

        int minX = safeZoneLocation.getX() - SAFE_ZONE_HALF_BOX_SIZE;
        int maxX = safeZoneLocation.getX() + SAFE_ZONE_HALF_BOX_SIZE;
        int minY = safeZoneLocation.getY() - SAFE_ZONE_HALF_BOX_SIZE;
        int maxY = safeZoneLocation.getY() + SAFE_ZONE_HALF_BOX_SIZE;

        return point.getX() >= minX && point.getX() <= maxX
                && point.getY() >= minY && point.getY() <= maxY
                && point.getPlane() == safeZoneLocation.getPlane();
    }

    public static WorldPoint clampToSafeZoneBox(WorldPoint point) {
        if (point == null || safeZoneLocation == null || !safeZoneBoxEnforced) {
            return point;
        }

        int minX = safeZoneLocation.getX() - SAFE_ZONE_HALF_BOX_SIZE;
        int maxX = safeZoneLocation.getX() + SAFE_ZONE_HALF_BOX_SIZE;
        int minY = safeZoneLocation.getY() - SAFE_ZONE_HALF_BOX_SIZE;
        int maxY = safeZoneLocation.getY() + SAFE_ZONE_HALF_BOX_SIZE;

        int clampedX = Math.max(minX, Math.min(maxX, point.getX()));
        int clampedY = Math.max(minY, Math.min(maxY, point.getY()));

        return new WorldPoint(clampedX, clampedY, safeZoneLocation.getPlane());
    }

    public static int getSafeZoneHalfBoxSize() {
        return SAFE_ZONE_HALF_BOX_SIZE;
    }

    // === Combat State Management ===
    public static void setFreezeState(boolean playerFrozen, boolean targetFrozen, int playerTicks, int targetTicks) {
        LmsState.playerFrozen = playerFrozen;
        LmsState.targetFrozen = targetFrozen;
        LmsState.playerFreezeTicks = playerTicks;
        LmsState.targetFreezeTicks = targetTicks;
    }

    public static boolean isPlayerFrozen() {
        return playerFrozen;
    }

    public static boolean isTargetFrozen() {
        return targetFrozen;
    }

    public static int getPlayerFreezeTicks() {
        return playerFreezeTicks;
    }

    public static int getTargetFreezeTicks() {
        return targetFreezeTicks;
    }

    // === Navigation State ===
    public static void setNavigating(boolean navigating, WorldPoint destination) {
        isNavigating = navigating;
        navigationDestination = navigating ? destination : null;

        if (navigating && destination != null) {
            Logger.norm("[LmsState] Navigation started to: " + destination);
        } else if (!navigating) {
            Logger.norm("[LmsState] Navigation stopped");
        }
    }

    public static boolean isNavigating() {
        return isNavigating;
    }

    public static WorldPoint getNavigationDestination() {
        return navigationDestination;
    }

    // === Upgrade State ===
    public static void setUpgradeInProgress(boolean inProgress, WorldPoint chestLocation) {
        upgradeInProgress = inProgress;
        targetChestLocation = inProgress ? chestLocation : null;
        lastUpgradeAttempt = System.currentTimeMillis();

        Logger.norm("[LmsState] Upgrade in progress: " + inProgress
                + (chestLocation != null ? " at " + chestLocation : ""));
    }

    public static boolean isUpgradeInProgress() {
        return upgradeInProgress;
    }

    public static WorldPoint getTargetChestLocation() {
        return targetChestLocation;
    }

    public static long getLastUpgradeAttempt() {
        return lastUpgradeAttempt;
    }

    // === Plugin State ===
    public static void setInLmsInstance(boolean inInstance) {
        inLmsInstance = inInstance;
        if (inInstance) {
            matchStartTime = System.currentTimeMillis();
            Logger.norm("[LmsState] Entered LMS instance");
        } else {
            Logger.norm("[LmsState] Left LMS instance");
        }
    }

    public static boolean isInLmsInstance() {
        return inLmsInstance;
    }

    public static void setPluginEnabled(boolean enabled) {
        pluginEnabled = enabled;
        Logger.norm("[LmsState] Plugin enabled: " + enabled);
    }

    public static boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public static long getMatchStartTime() {
        return matchStartTime;
    }

    public static long getTimeInMatch() {
        return matchStartTime > 0 ? System.currentTimeMillis() - matchStartTime : 0;
    }

    // === Utility Methods ===
    public static Client getClient() {
        return client;
    }

    public static boolean isInSafeZone() {
        if (client == null || client.getLocalPlayer() == null) {
            return false;
        }

        return isInsideSafeZoneBox(client.getLocalPlayer().getWorldLocation());
    }

    public static String getStateSummary() {
        return String.format("Task: %s, Target: %s, SafeZone: %s, Ignored: %d, Navigation: %s, Upgrade: %s",
                currentTask,
                hasTarget ? targetName : "None",
                safeZoneName,
                getIgnoreSize(),
                isNavigating ? "Yes" : "No",
                upgradeInProgress ? "Yes" : "No");
    }
}
