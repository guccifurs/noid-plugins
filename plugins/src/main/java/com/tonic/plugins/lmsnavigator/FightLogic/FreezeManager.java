package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;

import java.util.Arrays;

/**
 * Freeze tracking logic ported from GearSwapper.
 * Tracks freeze / immunity ticks for the local player and their current target
 * based on freeze GFX (Ice Barrage, Ice Blitz, Entangle, etc.).
 */
public class FreezeManager
{
    private static Client client;

    private static final int FREEZE_DURATION_TICKS = 33;      // Same as GearSwapper
    private static final int FREEZE_IMMUNITY_TICKS = 4;       // Same as GearSwapper
    private static final long TARGET_CACHE_DURATION_MS = 30_000L; // 30s cached target

    // GFX IDs used in GearSwapper for freeze detection
    private static final int[] FREEZE_GFX_IDS = {369, 360, 361, 363, 358, 359, 143, 144};

    private enum FreezeState
    {
        FROZEN,
        IMMUNITY,
        NONE
    }

    private static class FreezeTimer
    {
        FreezeState state;
        int ticksRemaining;

        FreezeTimer()
        {
            this.state = FreezeState.FROZEN;
            this.ticksRemaining = FREEZE_DURATION_TICKS;
        }

        void tick()
        {
            if (ticksRemaining > 0)
            {
                ticksRemaining--;

                if (state == FreezeState.FROZEN && ticksRemaining == 0)
                {
                    state = FreezeState.IMMUNITY;
                    ticksRemaining = FREEZE_IMMUNITY_TICKS;
                }
                else if (state == FreezeState.IMMUNITY && ticksRemaining == 0)
                {
                    state = FreezeState.NONE;
                }
            }
        }

        boolean canBeFrozen()
        {
            return state == FreezeState.NONE;
        }

        boolean isActive()
        {
            return state != FreezeState.NONE;
        }

        void reset()
        {
            state = FreezeState.NONE;
            ticksRemaining = 0;
        }
    }

    private static Player currentTarget;
    private static Player cachedTarget;
    private static long cachedTargetTime = 0L;
    private static int targetDistance = -1;

    private static FreezeTimer playerFreezeTimer;
    private static FreezeTimer targetFreezeTimer;
    private static boolean playerFreezeNewThisTick;
    private static boolean targetFreezeNewThisTick;
    private static WorldPoint playerLastPosition;
    private static WorldPoint targetLastPosition;

    public static void setClient(Client c)
    {
        client = c;
        Logger.norm("[FreezeManager] Client reference set");
    }

    /**
     * Tick handler – mirrors GearSwapper freeze tick logic (movement cancels freeze,
     * ticks decrement, immunity applied after thaw).
     */
    public static void onGameTick(GameTick event)
    {
        if (client == null)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        checkFreezeStatus(local);

        if (playerFreezeTimer != null)
        {
            if (!playerFreezeNewThisTick)
            {
                playerFreezeTimer.tick();
            }
            else
            {
                playerFreezeNewThisTick = false;
            }

            if (!playerFreezeTimer.isActive())
            {
                playerFreezeTimer = null;
            }
        }

        if (targetFreezeTimer != null)
        {
            if (!targetFreezeNewThisTick)
            {
                targetFreezeTimer.tick();
            }
            else
            {
                targetFreezeNewThisTick = false;
            }

            if (!targetFreezeTimer.isActive())
            {
                targetFreezeTimer = null;
            }
        }

        // Maintain a simple current target using the same approach as GearSwapper
        if (client != null && client.getLocalPlayer() != null)
        {
            Actor interacting = client.getLocalPlayer().getInteracting();
            if (interacting instanceof Player)
            {
                // Live target: update current and cached target
                currentTarget = (Player) interacting;
                cachedTarget = currentTarget;
                cachedTargetTime = System.currentTimeMillis();

                WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
                WorldPoint targetPos = currentTarget.getWorldLocation();
                if (localPos != null && targetPos != null)
                {
                    targetDistance = localPos.distanceTo(targetPos);
                }
            }
            else if (cachedTarget != null)
            {
                // Use cached target only if it is still fresh
                long now = System.currentTimeMillis();
                if (cachedTargetTime > 0 && now - cachedTargetTime < TARGET_CACHE_DURATION_MS)
                {
                    currentTarget = cachedTarget;

                    WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
                    WorldPoint targetPos = currentTarget.getWorldLocation();
                    if (localPos != null && targetPos != null)
                    {
                        targetDistance = localPos.distanceTo(targetPos);
                    }
                }
                else
                {
                    // Cached target expired
                    cachedTarget = null;
                    currentTarget = null;
                    targetDistance = -1;
                }
            }
            else
            {
                currentTarget = null;
                targetDistance = -1;
            }
        }
    }

    /**
     * GraphicChanged handler – detects freeze GFX on the player or current target
     * (Ice Barrage, Ice Blitz, Entangle, etc.).
     */
    public static void onGraphicChanged(GraphicChanged event)
    {
    }

    private static boolean isFreezeGraphic(int graphicId)
    {
        for (int id : FREEZE_GFX_IDS)
        {
            if (id == graphicId)
            {
                return true;
            }
        }
        return false;
    }

    private static void checkFreezeStatus(Player localPlayer)
    {
        if (localPlayer == null)
        {
            playerLastPosition = null;
            targetLastPosition = null;
            return;
        }

        int playerGraphic = localPlayer.getGraphic();
        WorldPoint playerPosition = localPlayer.getWorldLocation();
        WorldPoint previousPlayerPosition = playerLastPosition;

        boolean playerFreezeDetectedThisTick = false;
        if (isFreezeGraphic(playerGraphic))
        {
            if (playerFreezeTimer == null || playerFreezeTimer.canBeFrozen())
            {
                playerFreezeTimer = new FreezeTimer();
                playerFreezeNewThisTick = true;
                playerFreezeDetectedThisTick = true;
                playerLastPosition = playerPosition;
            }
        }

        if (playerFreezeTimer != null && playerFreezeTimer.state == FreezeState.FROZEN && !playerFreezeDetectedThisTick)
        {
            if (previousPlayerPosition != null && playerPosition != null && !previousPlayerPosition.equals(playerPosition))
            {
                playerFreezeTimer.reset();
                playerFreezeTimer = null;
                playerLastPosition = playerPosition;
                return;
            }
        }

        if (!playerFreezeDetectedThisTick)
        {
            playerLastPosition = playerPosition;
        }

        Actor ourTarget = localPlayer.getInteracting();
        Player opponent = null;

        if (ourTarget instanceof Player)
        {
            opponent = (Player) ourTarget;
        }
        else
        {
            if (targetFreezeTimer == null)
            {
                targetLastPosition = null;
            }
        }

        if (opponent == null)
        {
            return;
        }

        int opponentGraphic = opponent.getGraphic();
        WorldPoint opponentPosition = opponent.getWorldLocation();
        WorldPoint previousOpponentPosition = targetLastPosition;

        boolean opponentFreezeDetectedThisTick = false;
        if (isFreezeGraphic(opponentGraphic))
        {
            if (targetFreezeTimer == null || targetFreezeTimer.canBeFrozen())
            {
                targetFreezeTimer = new FreezeTimer();
                targetFreezeNewThisTick = true;
                opponentFreezeDetectedThisTick = true;
                targetLastPosition = opponentPosition;
            }
        }

        if (targetFreezeTimer != null && targetFreezeTimer.state == FreezeState.FROZEN && !opponentFreezeDetectedThisTick)
        {
            if (previousOpponentPosition != null && opponentPosition != null && !previousOpponentPosition.equals(opponentPosition))
            {
                targetFreezeTimer.reset();
                targetFreezeTimer = null;
                targetLastPosition = opponentPosition;
                return;
            }
        }

        if (!opponentFreezeDetectedThisTick)
        {
            targetLastPosition = opponentPosition;
        }
    }

    // === Public getters (for future fight logic / overlay use) ===

    public static int getPlayerFreezeTicks()
    {
        if (playerFreezeTimer != null && playerFreezeTimer.state == FreezeState.FROZEN)
        {
            return playerFreezeTimer.ticksRemaining;
        }
        return 0;
    }

    public static int getTargetFreezeTicks()
    {
        if (targetFreezeTimer != null && targetFreezeTimer.state == FreezeState.FROZEN)
        {
            return targetFreezeTimer.ticksRemaining;
        }
        return 0;
    }

    public static int getPlayerFreezeImmunityTicks()
    {
        if (playerFreezeTimer != null && playerFreezeTimer.state == FreezeState.IMMUNITY)
        {
            return playerFreezeTimer.ticksRemaining;
        }
        return 0;
    }

    public static int getTargetFreezeImmunityTicks()
    {
        if (targetFreezeTimer != null && targetFreezeTimer.state == FreezeState.IMMUNITY)
        {
            return targetFreezeTimer.ticksRemaining;
        }
        return 0;
    }

    public static Player getCurrentTarget()
    {
        return currentTarget;
    }

    public static int getTargetDistance()
    {
        return targetDistance;
    }
}
