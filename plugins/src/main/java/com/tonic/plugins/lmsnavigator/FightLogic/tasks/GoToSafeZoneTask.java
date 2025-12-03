package com.tonic.plugins.lmsnavigator.FightLogic.tasks;

import com.tonic.Logger;
import com.tonic.api.game.MovementAPI;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.FightLogic.TaskQueue;
import com.tonic.plugins.lmsnavigator.LMSNavigatorPlugin;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Walk to the announced final safe zone.
 * Re-issues navigation each tick until we arrive.
 */
public class GoToSafeZoneTask
{
    private static final int ARRIVAL_THRESHOLD = 15; // tiles considered "close enough" to safe zone

    public static void tick()
    {
        WorldPoint safeZone = LmsState.getSafeZone();
        if (safeZone == null)
        {
            Logger.warn("[GoToSafeZoneTask] No safe zone set; finishing task.");
            finish();
            return;
        }

        // Work in TEMPLATE coordinates for distance/box checks so we are
        // consistent with how LmsState stores the safe zone.
        WorldPoint currentTemplate = getCurrentTemplateLocation();
        if (currentTemplate == null)
        {
            Logger.warn("[GoToSafeZoneTask] Unable to get current location.");
            return;
        }

        double distance = currentTemplate.distanceTo(safeZone);
        Logger.norm("[GoToSafeZoneTask] Distance to safe zone: " + distance);

        // Start enforcing the safe zone box once we are reasonably close.
        boolean enforceBox = distance <= ARRIVAL_THRESHOLD;
        LmsState.setSafeZoneBoxEnforced(enforceBox);
        LmsState.setRoamDisabled(enforceBox);

        if (LmsState.isSafeZoneBoxEnforced())
        {
            if (LmsState.isInsideSafeZoneBox(currentTemplate))
            {
                // Already inside the box; mark safe zone as reached
                Logger.norm("[GoToSafeZoneTask] Inside safe zone box; finishing safe-zone navigation.");
                LmsState.setReachedSafeZone(true);
                finish();
                return;
            }

            // Close to the safe zone but not yet inside the box. Let the
            // template-based navigator handle the final approach instead of
            // issuing direct walk commands to a clamped tile.
            LMSNavigatorPlugin plugin = LMSNavigatorPlugin.getPlugin();
            if (plugin != null && !plugin.isCurrentlyNavigating())
            {
                Logger.norm("[GoToSafeZoneTask] Near safe zone; navigating to center: " + safeZone);
                plugin.navigateToTemplate(safeZone);
            }
            return;
        }

        // Only re-issue navigation if we are far (> ARRIVAL_THRESHOLD tiles) or not currently navigating
        if (distance > ARRIVAL_THRESHOLD || !isNavigating())
        {
            Logger.norm("[GoToSafeZoneTask] Navigating to safe zone: " + safeZone);
            LMSNavigatorPlugin plugin = LMSNavigatorPlugin.getPlugin();
            if (plugin != null)
            {
                plugin.navigateToTemplate(safeZone);
            }
        }
    }

    private static void finish()
    {
        // Once at safe zone, we can roam locally (or idle).
        TaskQueue.preempt(LmsState.LmsTask.ROAM);
    }

    private static WorldPoint getCurrentLocation()
    {
        Client client = LMSNavigatorPlugin.getClient();
        if (client == null) return null;
        Player local = client.getLocalPlayer();
        return local == null ? null : local.getWorldLocation();
    }

    private static WorldPoint getCurrentTemplateLocation()
    {
        LMSNavigatorPlugin plugin = LMSNavigatorPlugin.getPlugin();
        if (plugin == null)
        {
            return null;
        }
        return plugin.getPlayerTemplateLocation();
    }

    private static boolean isNavigating()
    {
        LMSNavigatorPlugin plugin = LMSNavigatorPlugin.getPlugin();
        return plugin != null && plugin.isCurrentlyNavigating();
    }
}
