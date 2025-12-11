package com.tonic.plugins.targetlockon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("targetlockon")
public interface TargetLockonConfig extends Config
{
    @ConfigItem(
        keyName = "clearLockKeybind",
        name = "Clear Lock Keybind",
        description = "Hotkey to clear the camera lock",
        position = 0
    )
    default Keybind clearLockKeybind()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "smoothSpeed",
        name = "Smoothing Speed",
        description = "How fast the camera follows (1 = slowest, 100 = instant)",
        position = 1
    )
    default int smoothSpeed()
    {
        return 15;
    }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Lock Indicator",
        description = "Display an overlay showing the locked target's name",
        position = 2
    )
    default boolean showOverlay()
    {
        return true;
    }
}
