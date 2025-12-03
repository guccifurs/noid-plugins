package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Computes the current combat state based on FreezeManager and distance to target.
 */
public class FightStateManager
{
    private static Client client;

    public static void setClient(Client c)
    {
        client = c;
        Logger.norm("[FightStateManager] Client reference set");
    }

    public static CombatState getCurrentState()
    {
        if (client == null)
        {
            return CombatState.NO_TARGET;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return CombatState.NO_TARGET;
        }

        Player target = FreezeManager.getCurrentTarget();

        // If FreezeManager has no current target, fall back to TargetManagement's target name.
        // This ensures combat state updates even when we are only being attacked and
        // haven't attacked back yet.
        if (target == null && TargetManagement.hasTarget())
        {
            String targetName = TargetManagement.getCurrentTargetName();
            if (targetName != null && !targetName.trim().isEmpty())
            {
                for (Player p : client.getPlayers())
                {
                    if (p == null)
                    {
                        continue;
                    }

                    String name = p.getName();
                    if (name != null && name.equals(targetName))
                    {
                        target = p;
                        break;
                    }
                }
            }
        }

        if (target == null)
        {
            return CombatState.NO_TARGET;
        }

        int playerFreeze = FreezeManager.getPlayerFreezeTicks();
        int targetFreeze = FreezeManager.getTargetFreezeTicks();
        boolean weFrozen = playerFreeze > 0;
        boolean targetFrozen = targetFreeze > 0;

        boolean inMeleeRange = false;

        WorldPoint ourPos = local.getWorldLocation();
        WorldPoint targetPos = target.getWorldLocation();
        if (ourPos != null && targetPos != null)
        {
            int dx = Math.abs(ourPos.getX() - targetPos.getX());
            int dy = Math.abs(ourPos.getY() - targetPos.getY());

            boolean diagonal = (dx == 1 && dy == 1);
            int chebyshevDistance = Math.max(dx, dy);

            // BothFrozenMelee means in melee range: distance < 2 and NOT diagonal
            inMeleeRange = (chebyshevDistance < 2) && !diagonal;
        }

        if (weFrozen && targetFrozen)
        {
            if (inMeleeRange)
            {
                return CombatState.BOTH_FROZEN_MELEE;
            }
            return CombatState.BOTH_FROZEN;
        }
        else if (!weFrozen && !targetFrozen)
        {
            return CombatState.BOTH_UNFROZEN;
        }
        else if (!weFrozen && targetFrozen)
        {
            return CombatState.TARGET_FROZEN_WE_UNFROZEN;
        }
        else if (weFrozen && !targetFrozen)
        {
            return CombatState.WE_FROZEN_TARGET_UNFROZEN;
        }

        return CombatState.NO_TARGET;
    }
}
