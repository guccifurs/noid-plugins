package com.tonic.plugins.tickpreviewqueue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("tickpreviewqueue")
public interface TickPreviewQueueConfig extends Config
{
    @ConfigItem(
        name = "Enabled",
        keyName = "enabled",
        description = "Enable tick preview inventory click queueing",
        position = 0
    )
    default boolean enabled()
    {
        return false;
    }

    @Range(
        min = 10,
        max = 600
    )
    @ConfigItem(
        name = "Preview window (ms)",
        keyName = "previewWindowMs",
        description = "How many milliseconds before the next tick to start queuing inventory clicks",
        position = 1
    )
    default int previewWindowMs()
    {
        return 80;
    }

    @ConfigItem(
        name = "Ignore mismatch safety",
        keyName = "ignoreMismatchSafety",
        description = "If enabled, queued clicks will fire even if the slot's item changed",
        position = 2
    )
    default boolean ignoreMismatchSafety()
    {
        return false;
    }

    @ConfigItem(
        name = "Show visual preview",
        keyName = "showVisualPreview",
        description = "Show predicted inventory state during preview window",
        position = 3
    )
    default boolean showVisualPreview()
    {
        return true;
    }
}
