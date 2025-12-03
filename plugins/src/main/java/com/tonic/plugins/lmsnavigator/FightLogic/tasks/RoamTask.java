package com.tonic.plugins.lmsnavigator.FightLogic.tasks;

import com.tonic.Logger;
import com.tonic.api.game.MovementAPI;
import com.tonic.plugins.lmsnavigator.FightLogic.GearManagement;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.plugins.lmsnavigator.FightLogic.TaskQueue;
import com.tonic.plugins.lmsnavigator.GetMode;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import com.tonic.plugins.lmsnavigator.LMSNavigatorPlugin;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.Random;

/**
 * Roam when no safe zone is announced.
 * Random walk within a radius; look for non-ignored, non-in-combat players to engage.
 */
public class RoamTask
{
    private static final Random RANDOM = new Random();
    private static final int ROAM_RADIUS = 60;
    private static final int FOG_MARKER_ID = 34905;
    private static WorldPoint currentRoamTarget;
    private static long lastRoamCommand;
    private static long lastNoPlayerLog;
    private static int safeZoneIdleTicksUntilMove;

    public static void tick()
    {
        // If we know any safe zone and roaming is not disabled yet, stop roaming and go there
        if ((LmsState.getSafeZone() != null || LmsState.isFinalSafeZoneAnnounced()) && !LmsState.isRoamDisabled())
        {
            Logger.norm("[RoamTask] Safe zone available; switching to GO_TO_SAFE_ZONE.");
            TaskQueue.preempt(LmsState.LmsTask.GO_TO_SAFE_ZONE);
            return;
        }

        // If roaming is disabled (e.g., when boxed), we are in the final safe zone.
        // Still try to engage nearby players before idling inside the box.
        if (LmsState.isRoamDisabled())
        {
            Player nearby = findNearbyNonIgnoredPlayer();
            if (nearby != null)
            {
                Logger.norm("[RoamTask] Safe-zone found nearby player: " + nearby.getName() + ". Engaging.");
                LMSNavigatorPlugin.engagePlayerFromRoam(nearby);
                return;
            }

            handleSafeZoneIdle();
            return;
        }

        // Look for a nearby non-ignored player every tick
        Player nearby = findNearbyNonIgnoredPlayer();
        if (nearby != null)
        {
            Logger.norm("[RoamTask] Found nearby player: " + nearby.getName() + ". Engaging.");
            LMSNavigatorPlugin.engagePlayerFromRoam(nearby);
            return;
        }
        else
        {
            long now = System.currentTimeMillis();
            if (now - lastNoPlayerLog > 3000)
            {
                lastNoPlayerLog = now;
                Logger.norm("[RoamTask] No nearby non-ignored players found, continuing roam.");
            }
        }

        // Otherwise, random walk.
        randomWalk();
    }

    private static void handleSafeZoneIdle()
    {
        // Only idle-walk when we actually have an enforced safe zone box
        if (!LmsState.isSafeZoneBoxEnforced())
        {
            return;
        }

        // Do not issue idle walks while we have or are resolving a combat target
        if (LmsState.getCurrentTarget() != null || TargetManagement.hasTarget())
        {
            return;
        }

        LMSNavigatorPlugin plugin = LMSNavigatorPlugin.getPlugin();
        if (plugin == null)
        {
            return;
        }

        // Make sure we are actually inside the box in template space
        WorldPoint currentTemplate = plugin.getPlayerTemplateLocation();
        if (currentTemplate == null || !LmsState.isInsideSafeZoneBox(currentTemplate))
        {
            return;
        }

        // Ensure we are in tank gear while idling in the safe zone
        equipSafeZoneTankGear();

        if (safeZoneIdleTicksUntilMove > 0)
        {
            safeZoneIdleTicksUntilMove--;
            return;
        }

        Client client = LMSNavigatorPlugin.getClient();
        if (client == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        Scene scene = worldView.getScene();
        if (scene == null)
        {
            return;
        }

        int plane = worldView.getPlane();
        Tile[][][] tiles = scene.getTiles();
        if (tiles == null || plane < 0 || plane >= tiles.length)
        {
            return;
        }

        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null)
        {
            return;
        }

        boolean anyFogMarker = false;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int x = 0; x < planeTiles.length; x++)
        {
            Tile[] row = planeTiles[x];
            if (row == null)
            {
                continue;
            }

            for (int y = 0; y < row.length; y++)
            {
                Tile tile = row[y];
                if (tile == null)
                {
                    continue;
                }

                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects == null)
                {
                    continue;
                }

                boolean hasFogMarker = false;
                for (GameObject obj : gameObjects)
                {
                    if (obj != null && obj.getId() == FOG_MARKER_ID)
                    {
                        hasFogMarker = true;
                        break;
                    }
                }

                if (!hasFogMarker)
                {
                    continue;
                }

                anyFogMarker = true;

                if (x < minX)
                {
                    minX = x;
                }
                if (x > maxX)
                {
                    maxX = x;
                }
                if (y < minY)
                {
                    minY = y;
                }
                if (y > maxY)
                {
                    maxY = y;
                }
            }
        }

        if (!anyFogMarker || minX > maxX || minY > maxY)
        {
            return;
        }

        for (int attempt = 0; attempt < 10; attempt++)
        {
            int rangeX = maxX - minX + 1;
            int rangeY = maxY - minY + 1;
            if (rangeX <= 0 || rangeY <= 0)
            {
                return;
            }

            int rx = minX + RANDOM.nextInt(rangeX);
            int ry = minY + RANDOM.nextInt(rangeY);

            if (rx < 0 || rx >= planeTiles.length)
            {
                continue;
            }

            Tile[] row = planeTiles[rx];
            if (row == null || ry < 0 || ry >= row.length)
            {
                continue;
            }

            Tile tile = row[ry];
            if (tile == null)
            {
                continue;
            }

            WorldPoint randomInstance = tile.getWorldLocation();
            if (randomInstance == null)
            {
                continue;
            }

            Logger.norm("[RoamTask] Safe-zone idle move to " + randomInstance);
            MovementAPI.walkToWorldPoint(randomInstance);

            // Schedule the next idle move between 2 and 8 ticks
            safeZoneIdleTicksUntilMove = 2 + RANDOM.nextInt(7);
            return;
        }
    }

    private static void equipSafeZoneTankGear()
    {
        if (GetMode.isMaxMed())
        {
            GearManagement.equipMaxMedTank();
        }
        else if (GetMode.isZerker())
        {
            GearManagement.equipZekerTank();
        }
        else if (GetMode.isOneDefPure())
        {
            // 1 def pure has no dedicated tank loadout defined; leave gear as-is.
        }
    }

    public static Player findNearbyNonIgnoredPlayer()
    {
        // Simple proximity check: iterate players and find the closest non-ignored player.
        Client client = getClient();
        if (client == null) return null;
        Player local = client.getLocalPlayer();
        if (local == null) return null;
        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null) return null;

        Player bestNoBar = null;
        int bestNoBarDist = Integer.MAX_VALUE;
        Player bestAny = null;
        int bestAnyDist = Integer.MAX_VALUE;

        for (Player p : client.getPlayers())
        {
            if (p == null || p == local) continue;
            if (TargetManagement.isIgnored(p)) continue;
            // Do NOT filter by HP bar; attack nearby players even if they are in combat
            WorldPoint pos = p.getWorldLocation();
            if (pos == null || pos.getPlane() != localPos.getPlane()) continue;
            int dist = localPos.distanceTo(pos);
            if (dist > 12) continue; // consider within 12 tiles

            int ratio = p.getHealthRatio();
            boolean hasHpBar = ratio >= 0; // -1 means no bar shown

            if (!hasHpBar)
            {
                if (dist < bestNoBarDist)
                {
                    bestNoBarDist = dist;
                    bestNoBar = p;
                }
            }
            else
            {
                if (dist < bestAnyDist)
                {
                    bestAnyDist = dist;
                    bestAny = p;
                }
            }
        }

        return bestNoBar != null ? bestNoBar : bestAny;
    }

    private static void randomWalk()
    {
        WorldPoint current = getCurrentLocation();
        if (current == null) return;

        if (currentRoamTarget != null)
        {
            int remaining = current.distanceTo(currentRoamTarget);
            if (remaining <= 2)
            {
                currentRoamTarget = null;
            }
        }

        long now = System.currentTimeMillis();
        if (currentRoamTarget != null && now - lastRoamCommand < 3500)
        {
            // Let existing walk continue
            return;
        }

        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        int distance = 10 + RANDOM.nextInt(ROAM_RADIUS - 10);
        int dx = (int) Math.round(Math.cos(angle) * distance);
        int dy = (int) Math.round(Math.sin(angle) * distance);

        currentRoamTarget = new WorldPoint(
            current.getX() + dx,
            current.getY() + dy,
            current.getPlane()
        );

        lastRoamCommand = now;
        Logger.norm("[RoamTask] Roaming to " + currentRoamTarget);
        MovementAPI.walkToWorldPoint(currentRoamTarget.getX(), currentRoamTarget.getY());
    }

    private static WorldPoint getCurrentLocation()
    {
        Client client = getClient();
        if (client == null) return null;
        Player local = client.getLocalPlayer();
        return local == null ? null : local.getWorldLocation();
    }

    private static Client getClient()
    {
        return LMSNavigatorPlugin.getClient();
    }
}
