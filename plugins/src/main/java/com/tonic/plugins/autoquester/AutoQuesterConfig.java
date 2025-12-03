package com.tonic.plugins.autoquester;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AutoQuesterConfig.GROUP)
public interface AutoQuesterConfig extends Config
{
    String GROUP = "autoquester";

    @ConfigItem(
        keyName = "enabled",
        name = "Enable",
        description = "Enable Auto Quester"
    )
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "questProfile",
        name = "Quest",
        description = "Built-in quest profile to run (None = use custom script only)."
    )
    default AutoQuesterQuest questProfile()
    {
        return AutoQuesterQuest.NONE;
    }

    @ConfigItem(
        keyName = "questScript",
        name = "Custom script",
        description = "Quest script lines (one step per line). Format: OPCODE|param1|param2... (used when Quest=None or as fallback)."
    )
    default String questScript()
    {
        return "";
    }

    @ConfigItem(
        keyName = "logSteps",
        name = "Log steps",
        description = "Log each quest step execution to the client log."
    )
    default boolean logSteps()
    {
        return true;
    }
}
