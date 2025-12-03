package com.tonic.plugins.lmsnavigator.FightLogic.tasks;

import com.tonic.Logger;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.LMSNavigatorPlugin;
import net.runelite.api.Client;

/**
 * Init phase: sip/restore logic at start of match.
 * Mirrors the original initPhase in LMSNavigatorPlugin.
 */
public class InitPhaseTask
{
    private static final String[] SARADOMIN_BREWS = {
        "Saradomin brew(4)", "Saradomin brew(3)", "Saradomin brew(2)", "Saradomin brew(1)", "Saradomin brew"
    };
    private static final String[] SUPER_RESTORES = {
        "Super restore(4)", "Super restore(3)", "Super restore(2)", "Super restore(1)", "Super restore"
    };
    private static final String[] SUPER_COMBATS = {
        "Super combat potion(4)", "Super combat potion(3)", "Super combat potion(2)", "Super combat potion(1)", "Super combat potion"
    };
    private static final String[] RANGING_POTIONS = {
        "Ranging potion(4)", "Ranging potion(3)", "Ranging potion(2)", "Ranging potion(1)", "Ranging potion"
    };

    private static int step = 0;
    private static int ticks = 0;

    public static void tick()
    {
        // If we no longer have init pots, finish early.
        if (!hasRequiredSupplies())
        {
            Logger.norm("[InitPhaseTask] Init pots not found; finishing init phase.");
            finish();
            return;
        }

        switch (step)
        {
            case 0:
                InventoryAPI.interact("Saradomin brew(4)", "Drink");
                step = 1;
                ticks = 0;
                break;
            case 1:
                ticks++;
                if (ticks >= 3)
                {
                    step = 2;
                    ticks = 0;
                }
                break;
            case 2:
                InventoryAPI.interact("Super restore(4)", "Drink");
                step = 3;
                ticks = 0;
                break;
            case 3:
                ticks++;
                if (ticks >= 3)
                {
                    step = 4;
                    ticks = 0;
                }
                break;
            case 4:
                InventoryAPI.interact("Super combat potion(4)", "Drink");
                step = 5;
                ticks = 0;
                break;
            case 5:
                ticks++;
                if (ticks >= 3)
                {
                    step = 6;
                    ticks = 0;
                }
                break;
            case 6:
                // Try multiple possible names for ranging potion
                InventoryAPI.interact("Ranging potion", "Drink");
                InventoryAPI.interact("Ranging potion(4)", "Drink");
                step = 7;
                ticks = 0;
                break;
            case 7:
                ticks++;
                if (ticks >= 3)
                {
                    finish();
                }
                break;
            default:
                finish();
                break;
        }

        // Walk a random tile each tick during init phase to avoid standing still
        randomWalk();
    }

    private static boolean hasRequiredSupplies()
    {
        Client client = LMSNavigatorPlugin.getClient();
        if (client == null)
        {
            return false;
        }

        switch (step)
        {
            case 0:
                return containsAny(SARADOMIN_BREWS);
            case 2:
                return containsAny(SUPER_RESTORES);
            case 4:
                return containsAny(SUPER_COMBATS);
            case 6:
                return containsAny(RANGING_POTIONS);
            default:
                return true;
        }
    }

    private static boolean containsAny(String... names)
    {
        for (String name : names)
        {
            if (InventoryAPI.contains(name))
            {
                return true;
            }
        }
        return false;
    }

    private static void randomWalk()
    {
        Client client = LMSNavigatorPlugin.getClient();
        if (client == null) return;
        var local = client.getLocalPlayer();
        if (local == null) return;
        var pos = local.getWorldLocation();
        if (pos == null) return;

        int distance = 50;
        double angle = Math.random() * 2 * Math.PI;
        int dx = (int) Math.round(Math.cos(angle) * distance);
        int dy = (int) Math.round(Math.sin(angle) * distance);

        int x = pos.getX() + dx;
        int y = pos.getY() + dy;

        MovementAPI.walkToWorldPoint(x, y);
    }

    private static void finish()
    {
        Logger.norm("[InitPhaseTask] Init phase finished.");
        LmsState.setTask(LmsState.LmsTask.ROAM);
        // Reset state in case this task is reused (unlikely)
        step = 0;
        ticks = 0;
    }
}
