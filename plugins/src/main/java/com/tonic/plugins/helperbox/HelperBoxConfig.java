package com.tonic.plugins.helperbox;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(HelperBoxConfig.GROUP)
public interface HelperBoxConfig extends Config
{
    String GROUP = "helperbox";

    @ConfigItem(
        keyName = "enabled",
        name = "Enable HelperBox",
        description = "Enable the HelperBox agility trainer"
    )
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "course",
        name = "Agility course",
        description = "Which rooftop course to run"
    )
    default HelperBoxCourse course()
    {
        return HelperBoxCourse.DRAYNOR;
    }

    @ConfigItem(
        keyName = "autoWalkToCourse",
        name = "Auto-walk to course",
        description = "Automatically walk to the selected rooftop start from anywhere"
    )
    default boolean autoWalkToCourse()
    {
        return true;
    }

    @ConfigItem(
        keyName = "useStamina",
        name = "Use stamina potions",
        description = "Drink stamina potions when run energy is low"
    )
    default boolean useStamina()
    {
        return true;
    }

    @ConfigItem(
        keyName = "staminaThreshold",
        name = "Stamina threshold",
        description = "Run energy % below which to drink stamina",
        position = 5
    )
    default int staminaThreshold()
    {
        return 35;
    }

    @ConfigItem(
        keyName = "stopAtLevel",
        name = "Stop at agility level",
        description = "Stop HelperBox when this agility level is reached (0 = ignore)",
        position = 6
    )
    default int stopAtLevel()
    {
        return 20;
    }

    @ConfigItem(
        keyName = "logSteps",
        name = "Log steps",
        description = "Log HelperBox agility steps to the console"
    )
    default boolean logSteps()
    {
        return true;
    }

    @ConfigItem(
        keyName = "antiStuck",
        name = "Anti-stuck recovery",
        description = "Try to detect and recover if stuck on the course"
    )
    default boolean antiStuck()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableWitchsHouse",
        name = "Enable Witch's House quest",
        description = "Run Witch's House quest helper instead of agility"
    )
    default boolean enableWitchsHouse()
    {
        return false;
    }
}
