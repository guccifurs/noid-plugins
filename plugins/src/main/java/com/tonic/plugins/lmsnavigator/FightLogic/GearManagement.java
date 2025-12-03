package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import java.util.List;

/**
 * Centralized gear + prayer presets for LMS Navigator fight logic.
 *
 * All methods are designed to be called once per tick and will:
 *  - Equip all required gear pieces (using InventoryAPI + EquipmentAPI, with wildcard support)
 *  - Toggle on the specified offensive/defensive prayers
 *
 * This mirrors the behavior of AttackTimer's equipGearFromConfig / equipItemByName helpers,
 * but uses hard-coded presets for LMS game modes instead of user configs.
 */
public class GearManagement
{
    // === Public entrypoints ===
    // Each of these aims to equip the full loadout in a single tick.

    // ---- Max/Med ----
    public static void equipMaxMedRanged()
    {
        equipItems(
            "Rune crossbow",
            "Black d'hide body",
            "Rune platelegs",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.RIGOUR);
        log("Max/Med Ranged");
    }

    public static void equipMaxMedTank()
    {
        equipItems(
            "Ahrim's staff*",
            "Black d'hide body",
            "Rune platelegs",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.AUGURY);
        log("Max/Med Tank");
    }

    public static void equipMaxMedMagic()
    {
        equipItems(
            "Ahrim's staff*",
            "Mystic robe top",
            "Mystic robe bottom",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.AUGURY);
        log("Max/Med Magic");
    }

    public static void equipMaxMedMelee()
    {
        equipItems(
            "Black d'hide body",
            "Rune platelegs",
            "Abyssal whip",
            "Dragon defender"
        );
        setPrayers(PrayerAPI.PIETY);
        log("Max/Med Melee");
    }

    public static void equipMaxMedMageTank()
    {
        equipItems(
            "Ahrim's staff*",
            "Mystic robe top",
            "Rune platelegs",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.AUGURY);
        log("Max/Med MageTank");
    }

    public static void equipMaxMedSpec()
    {
        equipItems(
            "Black d'hide body",
            "Rune platelegs",
            "Dragon dagger*",
            "Dragon defender"
        );
        setPrayers(PrayerAPI.PIETY);
        log("Max/Med Spec");
    }

    // ---- Zeker ----
    public static void equipZekerRanged()
    {
        equipItems(
            "Rune crossbow",
            "Black d'hide body",
            "Rune platelegs",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.EAGLE_EYE);
        log("Zeker Ranged");
    }

    public static void equipZekerTank()
    {
        equipItems(
            "Ahrim's staff*",
            "Black d'hide body",
            "Rune platelegs",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.MYSTIC_MIGHT);
        log("Zeker Tank");
    }

    public static void equipZekerMagic()
    {
        equipItems(
            "Ahrim's staff*",
            "Mystic robe top",
            "Mystic robe bottom",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.MYSTIC_MIGHT);
        log("Zeker Magic");
    }

    public static void equipZekerMelee()
    {
        equipItems(
            "Black d'hide body",
            "Rune platelegs",
            "Abyssal whip",
            "Dragon defender"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.ULTIMATE_STRENGTH, PrayerAPI.INCREDIBLE_REFLEXES);
        log("Zeker Melee");
    }

    public static void equipZekerMageTank()
    {
        equipItems(
            "Ahrim's staff*",
            "Mystic robe top",
            "Rune platelegs",
            "Spirit shield"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.MYSTIC_MIGHT);
        log("Zeker MageTank");
    }

    public static void equipZekerSpec()
    {
        equipItems(
            "Black d'hide body",
            "Rune platelegs",
            "Dragon dagger*",
            "Rune defender"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.ULTIMATE_STRENGTH, PrayerAPI.INCREDIBLE_REFLEXES);
        log("Zeker Spec");
    }

    // ---- 1 def pure ----
    public static void equipOneDefRanged()
    {
        equipItems(
            "Rune crossbow",
            "Amulet of glory",
            "Black d'hide chaps"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.EAGLE_EYE);
        log("1 def Ranged");
    }

    public static void equipOneDefMage()
    {
        equipItems(
            "Ghostly robe",
            "Ancient staff",
            "Occult necklace"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.MYSTIC_MIGHT);
        log("1 def Mage");
    }

    public static void equipOneDefMelee()
    {
        equipItems(
            "Dragon scimitar",
            "Black d'hide chaps",
            "Amulet of glory"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.ULTIMATE_STRENGTH, PrayerAPI.INCREDIBLE_REFLEXES);
        log("1 def Melee");
    }

    public static void equipOneDefSpec()
    {
        equipItems(
            "Dragon dagger*",
            "Black d'hide chaps",
            "Amulet of glory",
            "Dragon defender"
        );
        setPrayers(PrayerAPI.STEEL_SKIN, PrayerAPI.ULTIMATE_STRENGTH, PrayerAPI.INCREDIBLE_REFLEXES);
        log("1 def Spec");
    }

    // === Internal helpers ===

    private static void equipItems(String... patterns)
    {
        if (patterns == null)
        {
            return;
        }

        for (String pattern : patterns)
        {
            equipItemByPattern(pattern);
        }
    }

    private static void setPrayers(PrayerAPI... prayers)
    {
        if (prayers == null)
        {
            return;
        }

        Static.invoke(() -> {
            Client client = Static.getClient();

            for (PrayerAPI prayer : prayers)
            {
                if (prayer == null)
                {
                    continue;
                }

                boolean active = prayer.isActive();

                if (!active)
                {
                    try
                    {
                        // First try normal PrayerAPI.turnOn behaviour
                        prayer.turnOn();
                        active = prayer.isActive();

                        // If it didn't activate and we're in an LMS-style environment (boosted 99, real below requirement),
                        // bypass the level gating and click the prayer widget directly.
                        if (!active && client != null)
                        {
                            int boostedLevel = client.getBoostedSkillLevel(Skill.PRAYER);
                            int realLevel = client.getRealSkillLevel(Skill.PRAYER);

                            if (boostedLevel == 99 && realLevel < prayer.getLevel() && boostedLevel > 0)
                            {
                                WidgetAPI.interact(1, prayer.getInterfaceId(), -1, -1);
                                active = prayer.isActive();
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Logger.error("[GearManagement] Error turning on prayer " + prayer + ": " + e.getMessage());
                    }
                }

                if (active)
                {
                    Logger.norm("[GearManagement] Enabled prayer: " + prayer.name());
                }
            }
        });
    }

    /**
     * Equip an item from inventory by name or wildcard pattern (e.g. "Ahrim's staff*").
     */
    private static boolean equipItemByPattern(String pattern)
    {
        if (pattern == null || pattern.trim().isEmpty())
        {
            return false;
        }

        List<ItemEx> inventoryItems = InventoryAPI.getItems();
        boolean matchedAny = false;
        for (ItemEx item : inventoryItems)
        {
            if (item == null)
            {
                continue;
            }

            String name = item.getName();
            if (name == null || !matchesGearPattern(name, pattern))
            {
                continue;
            }

            matchedAny = true;

            // Skip if already equipped
            boolean alreadyEquipped = EquipmentAPI.isEquipped(i -> {
                if (i == null || i.getName() == null)
                {
                    return false;
                }
                return matchesGearPattern(i.getName(), pattern);
            });

            if (alreadyEquipped)
            {
                return true;
            }

            String[] actions = item.getActions();
            for (String actionName : new String[]{"Wear", "Equip", "Wield"})
            {
                if (actions == null)
                {
                    continue;
                }

                for (String action : actions)
                {
                    if (action != null && action.equalsIgnoreCase(actionName))
                    {
                        InventoryAPI.interact(item, actionName);
                        return true;
                    }
                }
            }
        }
        if (!matchedAny)
        {
            Logger.norm("[GearManagement] No inventory item matched pattern: " + pattern);
        }
        return false;
    }

    /**
     * Simple wildcard matcher used by AttackTimer ("*" suffix means "starts with").
     */
    private static boolean matchesGearPattern(String actualName, String pattern)
    {
        if (actualName == null || pattern == null)
        {
            return false;
        }

        String a = actualName.toLowerCase();
        String p = pattern.toLowerCase();

        if (p.endsWith("*"))
        {
            String prefix = p.substring(0, p.length() - 1);
            return a.startsWith(prefix);
        }

        return a.equals(p);
    }

    private static void log(String presetName)
    {
        Logger.norm("[GearManagement] Equipped preset: " + presetName);
    }
}
