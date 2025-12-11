package com.tonic.plugins.autorooftops;

import net.runelite.api.coords.WorldPoint;

public enum RooftopCourse
{
    GNOME_STRONGHOLD(1),
    DRAYNOR_VILLAGE(10),
    AL_KHARID(20),
    VARROCK(30),
    CANIFIS(40),
    SEERS_VILLAGE(60),
    POLLNIVNEACH(70),
    RELLEKKA(80),
    ARDOUGNE(90);

    private final int requiredLevel;

    RooftopCourse(int requiredLevel)
    {
        this.requiredLevel = requiredLevel;
    }

    public int getRequiredLevel()
    {
        return requiredLevel;
    }

    public static RooftopCourse getBestCourse(int agilityLevel)
    {
        for (RooftopCourse course : values())
        {
            if (agilityLevel >= course.requiredLevel)
            {
                // Continue to find the highest available
            }
        }
        // Return the last one that matches
        for (int i = values().length - 1; i >= 0; i--)
        {
            if (agilityLevel >= values()[i].requiredLevel)
            {
                return values()[i];
            }
        }
        return GNOME_STRONGHOLD; // Default to lowest
    }
}
