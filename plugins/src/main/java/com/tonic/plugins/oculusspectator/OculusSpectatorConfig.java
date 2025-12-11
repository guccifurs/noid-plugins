package com.tonic.plugins.oculusspectator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("oculusspectator")
public interface OculusSpectatorConfig extends Config
{
    @ConfigItem(
        keyName = "clearSpectateKeybind",
        name = "Clear Spectate Keybind",
        description = "Hotkey to stop spectating and return control",
        position = 0
    )
    default Keybind clearSpectateKeybind()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "followSpeed",
        name = "Follow Speed",
        description = "How fast the orb follows the target (1-100)",
        position = 1
    )
    @Range(min = 1, max = 100)
    default int followSpeed()
    {
        return 30;
    }

    @ConfigItem(
        keyName = "heightOffset",
        name = "Height Offset",
        description = "How high above the target to position the orb (0-500)",
        position = 2
    )
    @Range(min = 0, max = 500)
    default int heightOffset()
    {
        return 200;
    }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Spectate Indicator",
        description = "Display an overlay showing who you're spectating",
        position = 3
    )
    default boolean showOverlay()
    {
        return true;
    }
}
