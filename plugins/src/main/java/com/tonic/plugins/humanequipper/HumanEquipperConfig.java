package com.tonic.plugins.humanequipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(HumanEquipperConfig.GROUP)
public interface HumanEquipperConfig extends Config
{
    String GROUP = "humanequipper";

    @ConfigItem(
        keyName = "itemNames",
        name = "Item names (slot 1)",
        description = "Item names to equip for slot 1, one per line.",
        position = 0
    )
    default String itemNames()
    {
        return "";
    }

    @ConfigItem(
        keyName = "itemNames2",
        name = "Item names (slot 2)",
        description = "Item names to equip for slot 2, one per line.",
        position = 0
    )
    default String itemNames2()
    {
        return "";
    }

    @ConfigItem(
        keyName = "itemNames3",
        name = "Item names (slot 3)",
        description = "Item names to equip for slot 3, one per line.",
        position = 0
    )
    default String itemNames3()
    {
        return "";
    }

    @ConfigItem(
        keyName = "itemNames4",
        name = "Item names (slot 4)",
        description = "Item names to equip for slot 4, one per line.",
        position = 0
    )
    default String itemNames4()
    {
        return "";
    }

    @ConfigItem(
        keyName = "itemNames5",
        name = "Item names (slot 5)",
        description = "Item names to equip for slot 5, one per line.",
        position = 0
    )
    default String itemNames5()
    {
        return "";
    }

    @Range(min = 1, max = 2400)
    @ConfigItem(
        keyName = "totalTimeMillis",
        name = "Total time (ms)",
        description = "Approximate total time from first to last script action, in milliseconds (e.g., 600).",
        position = 1
    )
    default int totalTimeMillis()
    {
        return 600;
    }

    @ConfigItem(
        keyName = "equipHotkey",
        name = "Equip hotkey (slot 1)",
        description = "Hotkey to run the equip sequence for slot 1.",
        position = 2
    )
    default Keybind equipHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "equipHotkey2",
        name = "Equip hotkey (slot 2)",
        description = "Hotkey to run the equip sequence for slot 2.",
        position = 2
    )
    default Keybind equipHotkey2()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "equipHotkey3",
        name = "Equip hotkey (slot 3)",
        description = "Hotkey to run the equip sequence for slot 3.",
        position = 2
    )
    default Keybind equipHotkey3()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "equipHotkey4",
        name = "Equip hotkey (slot 4)",
        description = "Hotkey to run the equip sequence for slot 4.",
        position = 2
    )
    default Keybind equipHotkey4()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "equipHotkey5",
        name = "Equip hotkey (slot 5)",
        description = "Hotkey to run the equip sequence for slot 5.",
        position = 2
    )
    default Keybind equipHotkey5()
    {
        return Keybind.NOT_SET;
    }

    @Range(min = 5, max = 30)
    @ConfigItem(
        keyName = "smoothingIntervalMs",
        name = "Smoothing interval (ms)",
        description = "Target interval between OS mouse samples (lower = smoother, more CPU).",
        position = 3
    )
    default int smoothingIntervalMs()
    {
        return 10;
    }

    @Range(min = 50, max = 200)
    @ConfigItem(
        keyName = "speedPercent",
        name = "Speed percent",
        description = "Base movement speed (100 = normal, 200 = 2x faster, 50 = half speed).",
        position = 4
    )
    default int speedPercent()
    {
        return 100;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "distanceSpeedScalingPercent",
        name = "Distance speed scaling",
        description = "0 = off, 100 = strong: longer distances move faster than short ones.",
        position = 5
    )
    default int distanceSpeedScalingPercent()
    {
        return 50;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
        keyName = "randomTargetOffsetRadius",
        name = "Random click radius (px)",
        description = "Maximum random offset from item center in pixels for the click target.",
        position = 6
    )
    default int randomTargetOffsetRadius()
    {
        return 2;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "pathNoiseStrength",
        name = "Path noise strength",
        description = "0 = none, 100 = maximum subtle jitter in the path for extra variability.",
        position = 7
    )
    default int pathNoiseStrength()
    {
        return 20;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "curveStrengthPercent",
        name = "Curve strength",
        description = "0 = straight lines, 100 = strong curved arcs between points.",
        position = 8
    )
    default int curveStrengthPercent()
    {
        return 30;
    }

    @Range(min = 0, max = 120)
    @ConfigItem(
        keyName = "clickHoldMillis",
        name = "Click hold (ms)",
        description = "How long to hold the mouse button down when clicking items.",
        position = 9
    )
    default int clickHoldMillis()
    {
        return 40;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "perItemDelayRandomness",
        name = "Per-item delay randomness (%)",
        description = "0 = uniform spacing, 100 = very random gaps between items (overall total is still approximate).",
        position = 10
    )
    default int perItemDelayRandomness()
    {
        return 20;
    }

    @ConfigItem(
        keyName = "inventoryTabKey",
        name = "Inventory tab key",
        description = "Your in-game hotkey to open the inventory tab (e.g., F1 or Esc).",
        position = 11
    )
    default Keybind inventoryTabKey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "prayerTabKey",
        name = "Prayer tab key",
        description = "Your in-game hotkey to open the prayer tab (e.g., F2).",
        position = 12
    )
    default Keybind prayerTabKey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "magicTabKey",
        name = "Magic tab key",
        description = "Your in-game hotkey to open the magic spellbook tab (e.g., F3).",
        position = 13
    )
    default Keybind magicTabKey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "specTabKey",
        name = "Spec tab key",
        description = "Your in-game hotkey to open the special attack bar (if bound).",
        position = 14
    )
    default Keybind specTabKey()
    {
        return Keybind.NOT_SET;
    }
}
