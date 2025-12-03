package com.tonic.plugins.lmsnavigator.FightLogic.tasks;

import com.tonic.Logger;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.data.AttackStyle;
import com.tonic.plugins.lmsnavigator.FightLogic.*;
import com.tonic.plugins.lmsnavigator.LMSNavigatorPlugin;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Engage current target: dispatch to existing fight logic.
 * If target dies/is cleared, finish and re-evaluate.
 */
public class EngageTargetTask
{
    public static void tick()
    {
        Player target = LmsState.getCurrentTarget();
        if (target == null)
        {
            Logger.norm("[EngageTargetTask] No current target; finishing engagement.");
            finish();
            return;
        }

        enforceSafeZoneDiscipline(target);

        // Dispatch to the correct fight state logic, just like the original plugin
        CombatState state = FightStateManager.getCurrentState();
        switch (state)
        {
            case BOTH_UNFROZEN:
                BothUnfrozen.onGameTick();
                break;
            case TARGET_FROZEN_WE_UNFROZEN:
                TargetFrozenWeUnfrozen.onGameTick();
                break;
            case WE_FROZEN_TARGET_UNFROZEN:
                WeUnfrozenTargetFrozen.onGameTick();
                break;
            case BOTH_FROZEN_MELEE:
                BothFrozenMelee.onGameTick();
                break;
            case BOTH_FROZEN:
                BothFrozen.onGameTick();
                break;
            default:
                Logger.warn("[EngageTargetTask] Unknown or no combat state: " + state);
                break;
        }
    }

    private static void finish()
    {
        Logger.norm("[EngageTargetTask] Combat finished; clearing target.");
        LmsState.setTarget(null);

        // Resume safe zone navigation if it was active, else roam
        if (LmsState.isFinalSafeZoneAnnounced())
        {
            TaskQueue.preempt(LmsState.LmsTask.GO_TO_SAFE_ZONE);
        }
        else
        {
            TaskQueue.preempt(LmsState.LmsTask.ROAM);
        }
    }

    private static void enforceSafeZoneDiscipline(Player target)
    {
        if (!LmsState.isSafeZoneBoxEnforced())
        {
            return;
        }

        Client client = LMSNavigatorPlugin.getClient();
        if (client == null)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        WorldPoint localPos = local.getWorldLocation();
        if (localPos != null && !LmsState.isInsideSafeZoneBox(localPos))
        {
            WorldPoint clamp = LmsState.clampToSafeZoneBox(localPos);
            Logger.norm("[EngageTargetTask] Staying inside safe zone box, moving to " + clamp);
            MovementAPI.walkToWorldPoint(clamp.getX(), clamp.getY());
        }

        boolean targetInside = target != null && LmsState.isInsideSafeZoneBox(target.getWorldLocation());
        if (!targetInside)
        {
            // Target is outside our safe zone box. We rely on existing combat automation
            // or user settings to choose an appropriate style (e.g. ranged/mage) rather
            // than forcibly switching to the fourth style, which is defensive for many
            // weapons.
            Logger.norm("[EngageTargetTask] Target outside safe zone box; keeping current attack style.");
        }
    }
}
