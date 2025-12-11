package com.tonic.plugins.autorooftops;

import net.runelite.api.coords.WorldPoint;

public class CourseStage
{
    private final WorldPoint location;
    private final String action;
    private final int waitTicks;
    private final boolean isMarkPickup;
    private final int failureXp; // XP lost on failure
    private final WorldPoint fallbackLocation; // Where to walk if stuck

    public CourseStage(WorldPoint location, String action, int waitTicks, boolean isMarkPickup, int failureXp, WorldPoint fallbackLocation)
    {
        this.location = location;
        this.action = action;
        this.waitTicks = waitTicks;
        this.isMarkPickup = isMarkPickup;
        this.failureXp = failureXp;
        this.fallbackLocation = fallbackLocation;
    }

    public WorldPoint getLocation() { return location; }
    public String getAction() { return action; }
    public int getWaitTicks() { return waitTicks; }
    public boolean isMarkPickup() { return isMarkPickup; }
    public int getFailureXp() { return failureXp; }
    public WorldPoint getFallbackLocation() { return fallbackLocation; }
}
