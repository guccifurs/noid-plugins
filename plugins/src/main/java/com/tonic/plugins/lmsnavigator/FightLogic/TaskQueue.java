package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState.LmsTask;
import com.tonic.plugins.lmsnavigator.FightLogic.tasks.*;
import net.runelite.api.Player;

/**
 * Enhanced task queue with priority-based preemption system.
 * Works with unified LmsState to ensure proper task execution.
 */
public class TaskQueue {
    /**
     * Execute the current task's tick method.
     * This is called every game tick.
     */
    public static void tick() {
        LmsState.LmsTask currentTask = LmsState.getCurrentTask();

        // Clean up expired ignores before any task execution
        LmsState.cleanupExpiredIgnores();

        // Check for target timeout
        if (LmsState.hasTarget() && LmsState.isTargetExpired()) {
            Logger.norm("[TaskQueue] Target expired, clearing target");
            LmsState.clearTarget();
        }

        try {
            switch (currentTask) {
                case IDLE:
                    // No active task - look for next action
                    handleIdleState();
                    break;

                case INIT_PHASE:
                    InitPhaseTask.tick();
                    break;

                case ROAM:
                    RoamTask.tick();
                    break;

                case GO_TO_SAFE_ZONE:
                    GoToSafeZoneTask.tick();
                    break;

                case ENGAGE_TARGET:
                    EngageTargetTask.tick();
                    break;

                case WAIT_IGNORE:
                    WaitIgnoreTask.tick();
                    break;

                case UPGRADE_GEAR:
                    UpgradeGearTask.tick();
                    break;

                default:
                    Logger.warn("[TaskQueue] Unknown task: " + currentTask + " -> IDLE");
                    preempt(LmsState.LmsTask.IDLE);
                    break;
            }
        } catch (Exception e) {
            Logger.error("[TaskQueue] Error executing task " + currentTask + ": " + e.getMessage());
            e.printStackTrace();
            // Fall back to IDLE on error
            preempt(LmsState.LmsTask.IDLE);
        }
    }

    /**
     * Handle idle state by looking for next appropriate action
     */
    private static void handleIdleState() {
        // Priority order: UPGRADE_GEAR > ENGAGE_TARGET > GO_TO_SAFE_ZONE > ROAM

        // Check for upgrade opportunity first
        if (UpgradeGearTask.shouldStartUpgrade()) {
            Logger.norm("[TaskQueue] Idle: Starting upgrade task");
            preempt(LmsState.LmsTask.UPGRADE_GEAR);
            return;
        }

        // Check for combat targets
        if (LmsState.hasTarget()) {
            Logger.norm("[TaskQueue] Idle: Has target, engaging");
            preempt(LmsState.LmsTask.ENGAGE_TARGET);
            return;
        }

        // Try to acquire a target
        Player target = findBestTarget();
        if (target != null) {
            Logger.norm("[TaskQueue] Idle: Found target " + target.getName() + ", engaging");
            LmsState.setTarget(target);
            preempt(LmsState.LmsTask.ENGAGE_TARGET);
            return;
        }

        // Check for safe zone navigation
        if (LmsState.isFinalSafeZoneAnnounced() && !LmsState.hasReachedSafeZone()) {
            Logger.norm("[TaskQueue] Idle: Going to safe zone");
            preempt(LmsState.LmsTask.GO_TO_SAFE_ZONE);
            return;
        }

        // Default to roaming
        Logger.norm("[TaskQueue] Idle: No specific task, roaming");
        preempt(LmsState.LmsTask.ROAM);
    }

    /**
     * Find the best available target based on current priorities
     */
    private static Player findBestTarget() {
        // Delegate to a unified target acquisition system
        // For now, use existing logic from RoamTask
        return RoamTask.findNearbyNonIgnoredPlayer();
    }

    /**
     * Preempt current task with a new one if allowed by priority system
     */
    public static void preempt(LmsState.LmsTask newTask) {
        if (newTask == null) {
            Logger.warn("[TaskQueue] Cannot preempt with null task");
            return;
        }

        LmsState.LmsTask currentTask = LmsState.getCurrentTask();

        // Check if new task can preempt current task
        if (newTask != LmsState.LmsTask.IDLE && !LmsState.canPreempt(newTask)) {
            Logger.norm("[TaskQueue] Preemption blocked: " + newTask + " cannot override " + currentTask);
            return;
        }

        Logger.norm("[TaskQueue] Preempting " + currentTask + " with " + newTask);
        LmsState.setTask(newTask);
    }

    /**
     * Force a task change (bypassing priority system)
     * Only used for special cases like task completion
     */
    public static void forceTask(LmsState.LmsTask newTask) {
        Logger.norm("[TaskQueue] Force setting task to " + newTask);
        LmsState.forceSetTask(newTask);
    }

    /**
     * Get current executing task
     */
    public static LmsState.LmsTask getCurrentTask() {
        return LmsState.getCurrentTask();
    }

    /**
     * Check if a specific task is currently running
     */
    public static boolean isTaskActive(LmsState.LmsTask task) {
        return LmsState.getCurrentTask() == task;
    }

    /**
     * Get time spent in current task
     */
    public static long getTimeInCurrentTask() {
        return LmsState.getTimeInCurrentTask();
    }

    /**
     * Check if current task is taking too long (potential stuck state)
     */
    public static boolean isCurrentTaskStuck(long timeoutMs) {
        return getTimeInCurrentTask() > timeoutMs;
    }
}
