package com.tonic.plugins.truedreamloot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("truedreamloot")
public interface TrueDreamLootConfig extends Config {
    @Range(min = 1, max = 50)
    @ConfigItem(keyName = "maxRadius", name = "Max Radius", description = "Max radius of where we allowed to loot items", position = 1)
    default int maxRadius() {
        return 10;
    }

    @ConfigItem(keyName = "showRadiusOverlay", name = "Show Radius Overlay", description = "Show the looting radius on the screen", position = 2)
    default boolean showRadiusOverlay() {
        return true;
    }

    @Range(min = 1, max = 300)
    @Units(Units.SECONDS)
    @ConfigItem(keyName = "returnToCenterTime", name = "Return to Center Time", description = "Time in seconds on when to return to our center tile", position = 3)
    default int returnToCenterTime() {
        return 10;
    }

    @ConfigItem(keyName = "bankItems", name = "Bank Items", description = "Bank items when there is less than 8 inventory spots left", position = 4)
    default boolean bankItems() {
        return true;
    }

    @ConfigItem(keyName = "minLootValue", name = "Item Value Threshold", description = "Check GE price only loot items higher than this value", position = 5)
    default int minLootValue() {
        return 1000;
    }
}
