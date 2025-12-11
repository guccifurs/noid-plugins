package com.tonic.plugins.lmsnavigator.FightLogic.tasks;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.FightLogic.TaskQueue;
import com.tonic.plugins.lmsnavigator.FightLogic.tasks.RoamTask;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import net.runelite.api.Player;

/**
 * Wait while a target is ignored (e.g., after “already fighting” message).
 * Does nothing until the ignore expires, then re-evaluates.
 */
public class WaitIgnoreTask
{
    public static void tick()
    {
        // If all ignores expired, resume appropriate task.
        if (TargetManagement.getIgnoreCount() == 0)
        {
            Logger.norm("[WaitIgnoreTask] All ignores expired; resuming.");
            if (LmsState.isFinalSafeZoneAnnounced())
            {
                TaskQueue.preempt(LmsState.LmsTask.GO_TO_SAFE_ZONE);
            }
            else
            {
                TaskQueue.preempt(LmsState.LmsTask.ROAM);
            }
            return;
        }

        // If a final safe zone is announced while ignoring, immediately go to it
        if (LmsState.isFinalSafeZoneAnnounced())
        {
            Logger.norm("[WaitIgnoreTask] Final safe zone announced; going to safe zone while ignoring.");
            TaskQueue.preempt(LmsState.LmsTask.GO_TO_SAFE_ZONE);
            return;
        }

        // If a NEW combat target appears while we are in WAIT_IGNORE, immediately switch
        // back to combat instead of continuing to wait out the old ignore.
        if (TargetManagement.hasTarget())
        {
            Logger.norm("[WaitIgnoreTask] New combat target detected while waiting; switching to ENGAGE_TARGET.");
            TaskQueue.preempt(LmsState.LmsTask.ENGAGE_TARGET);
            return;
        }

        if (LmsState.isSafeZoneBoxEnforced()
            && LmsState.hasReachedSafeZone()
            && LmsState.getCurrentTarget() == null
            && !TargetManagement.hasTarget())
        {
            Player nearby = RoamTask.findNearbyNonIgnoredPlayer();
            if (nearby == null)
            {
                Logger.norm("[WaitIgnoreTask] All nearby players ignored in safe zone; switching to ROAM idle.");
                TaskQueue.preempt(LmsState.LmsTask.ROAM);
                return;
            }
        }

        // Try to acquire a new target if possible
        TargetManagement.updateTargetingEx();
        if (!TargetManagement.hasTarget())
        {
            Logger.norm("[WaitIgnoreTask] No acquirable targets found; roaming to search for new players.");
            TaskQueue.preempt(LmsState.LmsTask.ROAM);
            return;
        }

        // Optional: log remaining ignore count
        Logger.norm("[WaitIgnoreTask] Waiting; ignored players: " + TargetManagement.getIgnoreCount());
    }
}
