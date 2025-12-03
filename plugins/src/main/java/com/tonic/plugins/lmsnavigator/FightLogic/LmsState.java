package com.tonic.plugins.lmsnavigator.FightLogic;

import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Central LMS state and small task enum.
 * Used by TaskQueue and managers to coordinate roaming, safe-zone routing, and combat.
 */
public class LmsState
{
    public enum LmsTask
    {
        IDLE,
        INIT_PHASE,
        ROAM,
        GO_TO_SAFE_ZONE,
        ENGAGE_TARGET,
        WAIT_IGNORE
    }

    private static LmsTask currentTask = LmsTask.IDLE;
    private static Player currentTarget;
    private static final Map<Player, Long> ignoredUntil = new HashMap<>();
    private static WorldPoint announcedSafeZone;
    private static boolean finalSafeZoneAnnounced = false;
    private static boolean safeZoneBoxEnforced = false;
    private static final int SAFE_ZONE_HALF_BOX = 6; // 12x12 area centered on safe zone
    private static boolean roamDisabled = false;
    private static boolean reachedSafeZone = false;

    // === Task management ===
    public static void setTask(LmsTask task)
    {
        currentTask = task;
    }

    public static LmsTask getCurrentTask()
    {
        return currentTask;
    }

    // === Target management ===
    public static void setTarget(Player target)
    {
        currentTarget = target;
    }

    public static Player getCurrentTarget()
    {
        return currentTarget;
    }

    // === Ignore management ===
    public static void ignoreTargetForSeconds(Player target, int seconds)
    {
        if (target == null) return;
        ignoredUntil.put(target, System.currentTimeMillis() + seconds * 1000L);
    }

    public static boolean isIgnored(Player target)
    {
        if (target == null) return false;
        Long expiry = ignoredUntil.get(target);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry)
        {
            ignoredUntil.remove(target);
            return false;
        }
        return true;
    }

    public static void cleanupExpiredIgnores()
    {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Player, Long>> it = ignoredUntil.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<Player, Long> e = it.next();
            if (now > e.getValue())
            {
                it.remove();
            }
        }
    }

    public static int getIgnoreSize()
    {
        cleanupExpiredIgnores();
        return ignoredUntil.size();
    }

    // === Safe zone management ===
    public static void setSafeZone(WorldPoint safeZone)
    {
        announcedSafeZone = safeZone;
        if (safeZone == null)
        {
            safeZoneBoxEnforced = false;
            reachedSafeZone = false;
        }
    }

    public static WorldPoint getSafeZone()
    {
        return announcedSafeZone;
    }

    public static void setFinalSafeZoneAnnounced(boolean announced)
    {
        finalSafeZoneAnnounced = announced;
    }

    public static boolean isFinalSafeZoneAnnounced()
    {
        return finalSafeZoneAnnounced;
    }

    public static void setReachedSafeZone(boolean reached)
    {
        reachedSafeZone = reached;
    }

    public static boolean hasReachedSafeZone()
    {
        return reachedSafeZone;
    }

    // === Safe zone box enforcement ===
    public static void setSafeZoneBoxEnforced(boolean enforced)
    {
        safeZoneBoxEnforced = enforced && announcedSafeZone != null;
    }

    public static boolean isSafeZoneBoxEnforced()
    {
        return safeZoneBoxEnforced && announcedSafeZone != null;
    }

    public static boolean isInsideSafeZoneBox(WorldPoint point)
    {
        if (point == null || announcedSafeZone == null)
        {
            return true;
        }

        int minX = announcedSafeZone.getX() - SAFE_ZONE_HALF_BOX;
        int maxX = announcedSafeZone.getX() + SAFE_ZONE_HALF_BOX;
        int minY = announcedSafeZone.getY() - SAFE_ZONE_HALF_BOX;
        int maxY = announcedSafeZone.getY() + SAFE_ZONE_HALF_BOX;

        return point.getX() >= minX && point.getX() <= maxX
            && point.getY() >= minY && point.getY() <= maxY
            && point.getPlane() == announcedSafeZone.getPlane();
    }

    public static WorldPoint clampToSafeZoneBox(WorldPoint point)
    {
        if (point == null || announcedSafeZone == null)
        {
            return point;
        }

        int minX = announcedSafeZone.getX() - SAFE_ZONE_HALF_BOX;
        int maxX = announcedSafeZone.getX() + SAFE_ZONE_HALF_BOX;
        int minY = announcedSafeZone.getY() - SAFE_ZONE_HALF_BOX;
        int maxY = announcedSafeZone.getY() + SAFE_ZONE_HALF_BOX;

        int clampedX = Math.max(minX, Math.min(maxX, point.getX()));
        int clampedY = Math.max(minY, Math.min(maxY, point.getY()));

        return new WorldPoint(clampedX, clampedY, announcedSafeZone.getPlane());
    }

    public static int getSafeZoneHalfBoxSize()
    {
        return SAFE_ZONE_HALF_BOX;
    }

    public static boolean isRoamDisabled()
    {
        return roamDisabled;
    }

    public static void setRoamDisabled(boolean disabled)
    {
        roamDisabled = disabled;
    }
}
