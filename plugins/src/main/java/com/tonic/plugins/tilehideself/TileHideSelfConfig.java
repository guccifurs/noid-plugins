package com.tonic.plugins.tilehideself;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tilehideself")
public interface TileHideSelfConfig extends Config
{
    @ConfigItem(
        name = "Enabled",
        keyName = "enabled",
        description = "Hide your own character when another player is standing on the same tile",
        position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        name = "Hide 2D overlay",
        keyName = "hide2d",
        description = "Also hide your 2D overlay (HP bar, prayers, etc.) when overlapped",
        position = 1
    )
    default boolean hide2d()
    {
        return false;
    }

    @ConfigItem(
        name = "Hide delay (ticks)",
        keyName = "hideDelayTicks",
        description = "How many game ticks to wait while overlapped before hiding. 0 = instant, 1 = 1 tick delay, etc.",
        position = 2
    )
    default int hideDelayTicks()
    {
        return 0;
    }
}
