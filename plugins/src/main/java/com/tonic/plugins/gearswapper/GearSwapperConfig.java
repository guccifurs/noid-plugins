package com.tonic.plugins.gearswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GearSwapperConfig.GROUP)
public interface GearSwapperConfig extends Config
{
    String GROUP = "gearswapper";

    @ConfigItem(
        keyName = "showFreezeOverlay",
        name = "Show Freeze Overlay",
        description = "Show freeze timers on screen",
        position = 0
    )
    default boolean showFreezeOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showDebugOverlay",
        name = "Show Debug Overlay",
        description = "Show trigger system debug overlay for testing",
        position = 1
    )
    default boolean showDebugOverlay()
    {
        return false;
    }

    @ConfigItem(
        keyName = "enableMouseCircleTest",
        name = "Mouse Circle Test",
        description = "Move the OS mouse in a circle for 10 seconds (test only)",
        position = 2
    )
    default boolean enableMouseCircleTest()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showAnimationIdOverlay",
        name = "Show Animation ID Overlay",
        description = "Show last 5 animation IDs for you and your target",
        position = 3
    )
    default boolean showAnimationIdOverlay()
    {
        return false;
    }

    // ==== Left Click Cast ====

    @ConfigItem(
        keyName = "leftClickCastEnabled",
        name = "Left Click Cast",
        description = "When enabled, replace left-click Attack/Fight on players with casting a configured spell while wielding matching weapons.",
        position = 10
    )
    default boolean leftClickCastEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "leftClickCastWeapons",
        name = "LCC Weapons",
        description = "Weapon patterns for Left Click Cast (one per line, supports * wildcard). Example: Staff*\nKodai",
        position = 11
    )
    default String leftClickCastWeapons()
    {
        return "";
    }

    @ConfigItem(
        keyName = "leftClickCastSpell",
        name = "LCC Spell",
        description = "Spell to cast on left-click target (e.g. Ice Barrage). Uses ancient spellbook names.",
        position = 12
    )
    default String leftClickCastSpell()
    {
        return "";
    }
}
