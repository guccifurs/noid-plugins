package com.tonic.plugins.gearswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(GearSwapperConfig.GROUP)
public interface GearSwapperConfig extends Config {
    String GROUP = "gearswapper";

    // ========== SECTIONS ==========

    @ConfigSection(name = "Main", description = "Core settings", position = 0)
    String mainSection = "mainSection";

    @ConfigSection(name = "Overlays", description = "Visual overlay settings", position = 1)
    String overlaysSection = "overlaysSection";

    @ConfigSection(name = "Timers", description = "Timer settings", position = 2)
    String timersSection = "timersSection";

    @ConfigSection(name = "Prayer", description = "Prayer settings", position = 3)
    String prayerSection = "prayerSection";

    @ConfigSection(name = "Debug", description = "Debug and development tools", position = 4, closedByDefault = true)
    String debugSection = "debugSection";

    // ========== MAIN SECTION ==========

    @ConfigItem(keyName = "consumeHotkeyInput", name = "Block Hotkey Typing", description = "Prevent loadout hotkeys from typing characters in chat", position = 0, section = mainSection)
    default boolean consumeHotkeyInput() {
        return true;
    }

    @ConfigItem(keyName = "clearTargetHotkey", name = "Clear Target Hotkey", description = "Press this key to clear the current and cached Gear Swapper target.", position = 1, section = mainSection)
    default Keybind clearTargetHotkey() {
        return Keybind.NOT_SET;
    }

    // ========== OVERLAYS SECTION ==========

    @ConfigItem(keyName = "showFreezeOverlay", name = "Freeze Overlay", description = "Show freeze timers on screen", position = 0, section = overlaysSection)
    default boolean showFreezeOverlay() {
        return true;
    }

    @ConfigItem(keyName = "showTargetOverlay", name = "Target Overlay", description = "Show an overlay highlighting the current target on screen", position = 1, section = overlaysSection)
    default boolean showTargetOverlay() {
        return true;
    }

    @Range(min = -50, max = 100)
    @ConfigItem(keyName = "targetTextOffset", name = "Target Text Offset", description = "Vertical offset in pixels for target name text (positive moves it up)", position = 2, section = overlaysSection)
    default int targetTextOffset() {
        return 0;
    }

    @ConfigItem(keyName = "showCombatAnalytics", name = "Combat Analytics", description = "Show combat analytics overlay with damage stats", position = 3, section = overlaysSection)
    default boolean showCombatAnalytics() {
        return false;
    }

    @ConfigItem(keyName = "pidOverlayEnabled", name = "PID Overlay", description = "Show PID (Player ID) overlay", position = 4, section = overlaysSection)
    default boolean pidOverlayEnabled() {
        return false;
    }

    @ConfigItem(keyName = "showAttackCooldown", name = "Attack Cooldown", description = "Show attack cooldown timer overlay", position = 5, section = overlaysSection)
    default boolean showAttackCooldown() {
        return false;
    }

    // ========== TIMERS SECTION ==========
    // (empty for now - add timer configs here)

    // ========== PRAYER SECTION ==========
    // (empty for now - add prayer configs here)

    // ========== DEBUG SECTION ==========

    @ConfigItem(keyName = "showDebugOverlay", name = "Debug Overlay", description = "Show trigger system debug overlay for testing", position = 0, section = debugSection)
    default boolean showDebugOverlay() {
        return false;
    }

    @ConfigItem(keyName = "showAnimationOverhead", name = "Animation Overhead", description = "Display current animation ID above your character (updates every tick)", position = 1, section = debugSection)
    default boolean showAnimationOverhead() {
        return false;
    }

    @ConfigItem(keyName = "showAnimationIdOverlay", name = "Animation ID Overlay", description = "Show last 5 animation IDs for you and your target", position = 2, section = debugSection)
    default boolean showAnimationIdOverlay() {
        return false;
    }

    @ConfigItem(keyName = "copyCoordinates", name = "Copy Coordinates", description = "Enable right-click 'Copy Coordinates' on tiles to copy X:Y:Z format", position = 3, section = debugSection)
    default boolean copyCoordinates() {
        return false;
    }

    // ========== INVENTORY UTILS SECTION ==========

    @ConfigSection(name = "Inventory Utils", description = "Quick action preset buttons overlay", position = 5, closedByDefault = true)
    String inventoryUtilsSection = "inventoryUtilsSection";

    @ConfigItem(keyName = "inventoryUtilsEnabled", name = "Enable Overlay", description = "Show clickable preset buttons on screen", position = 0, section = inventoryUtilsSection)
    default boolean inventoryUtilsEnabled() {
        return false;
    }

    @ConfigItem(keyName = "inventoryUtilsOrientation", name = "Orientation", description = "Layout direction for preset buttons", position = 1, section = inventoryUtilsSection)
    default InventoryUtilsOrientation inventoryUtilsOrientation() {
        return InventoryUtilsOrientation.VERTICAL;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(keyName = "inventoryUtilsPresetCount", name = "Preset Count", description = "Number of preset buttons to display (1-10)", position = 2, section = inventoryUtilsSection)
    default int inventoryUtilsPresetCount() {
        return 3;
    }

    @Range(min = 50, max = 200)
    @ConfigItem(keyName = "inventoryUtilsSize", name = "Button Size %", description = "Size of preset buttons (50-200%)", position = 3, section = inventoryUtilsSection)
    default int inventoryUtilsSize() {
        return 100;
    }

    // ========== HUMANIZED MOUSE SECTION ==========

    @ConfigSection(name = "Humanized Mouse", description = "Human-like mouse movement settings", position = 6, closedByDefault = true)
    String humanizedSection = "humanizedSection";

    @ConfigItem(keyName = "enableHumanizedMouse", name = "Enable Humanized Mouse", description = "Use human-like mouse movement for gear swaps", position = 0, section = humanizedSection)
    default boolean enableHumanizedMouse() {
        return false;
    }

    @ConfigItem(keyName = "humanizedReturnMouse", name = "Return Mouse to Start", description = "Return mouse to original position after swap", position = 1, section = humanizedSection)
    default boolean humanizedReturnMouse() {
        return true;
    }

    @ConfigItem(keyName = "showMouseTrail", name = "Show Mouse Trail", description = "Render a blue trail showing the simulated mouse path", position = 2, section = humanizedSection)
    default boolean showMouseTrail() {
        return true;
    }

    @Range(min = 50, max = 500)
    @ConfigItem(keyName = "humanizedMaxTimeMs", name = "Max Time (ms)", description = "Maximum time for humanized movement (50-500ms)", position = 3, section = humanizedSection)
    default int humanizedMaxTimeMs() {
        return 300;
    }

    @Range(min = 0, max = 150)
    @ConfigItem(keyName = "humanizedPingBuffer", name = "Ping Buffer (ms)", description = "Safety buffer before tick ends (lower = faster but riskier). 0 for aggressive, 50 recommended, 100 for high ping.", position = 4, section = humanizedSection)
    default int humanizedPingBuffer() {
        return 50;
    }

    @ConfigItem(keyName = "showTickTimeline", name = "Show Tick Timeline", description = "Display a scrolling timeline showing clicks relative to game ticks", position = 5, section = humanizedSection)
    default boolean showTickTimeline() {
        return false;
    }
}
