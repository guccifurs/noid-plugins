package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState.LmsTask;
import com.tonic.plugins.lmsnavigator.FightLogic.tasks.*;

/**
 * Simple task queue/dispatcher for LMS behavior.
 * Only one task runs at a time; higher-priority tasks preempt.
 */
public class TaskQueue
{
    /**
     * Tick the current task. Called once per game tick.
     */
    public static void tick()
    {
        LmsState.LmsTask current = LmsState.getCurrentTask();
        Logger.norm("[TaskQueue] Ticking task: " + current);
        try
        {
            switch (current)
            {
                case IDLE:
                    // Nothing
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
                default:
                    Logger.warn("[TaskQueue] Unknown task: " + current + ". Switching to IDLE.");
                    LmsState.setTask(LmsState.LmsTask.IDLE);
                    break;
            }
        }
        catch (Exception e)
        {
            Logger.error("[TaskQueue] Exception in task " + current + ": " + e.getMessage());
            e.printStackTrace();
            LmsState.setTask(LmsState.LmsTask.IDLE);
        }
    }

    /**
     * Preempt to a new task. Cleans up any previous task if needed.
     */
    public static void preempt(LmsTask newTask)
    {
        Logger.norm("[TaskQueue] Preempting to task: " + newTask);
        LmsState.setTask(newTask);
        // If a task needs explicit init, we can call start() here.
        // For now, tasks are stateless and start on first tick.
    }
}
