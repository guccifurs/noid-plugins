package com.tonic.plugins.dropparty;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("dropparty")
public interface DropPartyConfig extends Config {
    // ==================== Tracking Settings ====================
    @ConfigSection(name = "Tracking", description = "Player tracking settings", position = 0)
    String trackingSection = "tracking";

    @ConfigItem(keyName = "playerName", name = "Player Name", description = "Name of the player to track (or right-click a player and select 'Track Path')", section = trackingSection, position = 0)
    default String playerName() {
        return "";
    }

    @Range(min = 10, max = 1000)
    @ConfigItem(keyName = "pathDurationTicks", name = "Path Duration (ticks)", description = "How long (in game ticks, ~600ms each) the path tiles remain visible. 100 ticks = 60 seconds", section = trackingSection, position = 1)
    default int pathDurationTicks() {
        return 100; // ~60 seconds
    }

    // ==================== Overlay Settings ====================
    @ConfigSection(name = "Overlay", description = "Visual overlay settings", position = 1)
    String overlaySection = "overlay";

    @ConfigItem(keyName = "overlayColor", name = "Overlay Color", description = "Color of the trail tiles", section = overlaySection, position = 0)
    default Color overlayColor() {
        return new Color(0, 150, 200);
    }

    @ConfigItem(keyName = "fontStyle", name = "Font Style", description = "Font style for timer text", section = overlaySection, position = 1)
    default FontStyle fontStyle() {
        return FontStyle.BOLD;
    }

    @Range(min = 10, max = 40)
    @ConfigItem(keyName = "textSize", name = "Text Size", description = "Size of timer text", section = overlaySection, position = 2)
    default int textSize() {
        return 18;
    }

    // ==================== Automation Settings ====================
    @ConfigSection(name = "Automation", description = "Auto-follow and loot settings", position = 2)
    String automationSection = "automation";

    @ConfigItem(keyName = "followHotkey", name = "Follow Hotkey", description = "Hotkey to toggle auto-follow on/off", section = automationSection, position = 0)
    default Keybind followHotkey() {
        return new Keybind(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK);
    }

    @ConfigItem(keyName = "autoFollow", name = "Auto Follow", description = "Automatically click tiles to follow the trail", section = automationSection, position = 1)
    default boolean autoFollow() {
        return false;
    }

    @Range(min = 1, max = 100)
    @ConfigItem(keyName = "clickAtTimerTicks", name = "Click at Timer (ticks)", description = "Click the tile when countdown reaches this many ticks remaining. 5 ticks = ~3 seconds", section = automationSection, position = 2)
    default int clickAtTimerTicks() {
        return 5; // ~3 seconds
    }

    @ConfigItem(keyName = "autoLoot", name = "Auto Loot", description = "Automatically pick up items on the trail (closest first)", section = automationSection, position = 3)
    default boolean autoLoot() {
        return false;
    }

    @ConfigItem(keyName = "minLootValue", name = "Min Loot Value (GP)", description = "Minimum GE value of items to loot (0 = loot all)", section = automationSection, position = 4)
    default int minLootValue() {
        return 10000;
    }

    @Range(min = 1, max = 15)
    @ConfigItem(keyName = "maxLootRange", name = "Max Loot Range (tiles)", description = "Maximum distance from player to loot items", section = automationSection, position = 5)
    default int maxLootRange() {
        return 6;
    }

    @Range(min = 0, max = 4)
    @ConfigItem(keyName = "lootActionIndex", name = "Loot Action Index", description = "Action index for looting (0-4). Try 2 for most items, 0 for some.", section = automationSection, position = 6)
    default int lootActionIndex() {
        return 2;
    }

    // ==================== Font Style Enum ====================
    @Getter
    @AllArgsConstructor
    enum FontStyle {
        BOLD("Bold", Font.BOLD),
        ITALIC("Italic", Font.ITALIC),
        PLAIN("Plain", Font.PLAIN);

        private final String name;
        private final int font;

        @Override
        public String toString() {
            return getName();
        }
    }
}
