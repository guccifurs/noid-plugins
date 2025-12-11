package com.tonic.plugins.autorooftops;

import net.runelite.client.config.*;

@ConfigGroup(AutoRooftopsConfig.GROUP)
public interface AutoRooftopsConfig extends Config
{
    String GROUP = "autorooftops";

    @ConfigItem(
        keyName = "enabled",
        name = "Enable",
        description = "Enable Auto Rooftops"
    )
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "course",
        name = "Course",
        description = "Rooftop course to run"
    )
    default RooftopCourse course()
    {
        return RooftopCourse.GNOME_STRONGHOLD;
    }

    @ConfigItem(
        keyName = "useStaminaPotions",
        name = "Use stamina potions",
        description = "Automatically drink stamina potions when run energy is low"
    )
    default boolean useStaminaPotions()
    {
        return true;
    }

    @ConfigItem(
        keyName = "staminaThreshold",
        name = "Stamina threshold",
        description = "Run energy percentage below which to drink stamina potions"
    )
    default int staminaThreshold()
    {
        return 30;
    }

    @ConfigItem(
        keyName = "pickupMarks",
        name = "Pickup marks",
        description = "Automatically pickup Marks of Grace"
    )
    default boolean pickupMarks()
    {
        return true;
    }

    @ConfigItem(
        keyName = "antiBan",
        name = "Anti-ban",
        description = "Enable anti-ban measures (random delays)"
    )
    default boolean antiBan()
    {
        return true;
    }

    @ConfigItem(
        keyName = "logSteps",
        name = "Log steps",
        description = "Log each course step execution to the client log."
    )
    default boolean logSteps()
    {
        return true;
    }

    @ConfigItem(
        keyName = "stopAtLevel",
        name = "Stop at level",
        description = "Stop training when reaching this agility level (0 to disable)"
    )
    default int stopAtLevel()
    {
        return 0;
    }
}
