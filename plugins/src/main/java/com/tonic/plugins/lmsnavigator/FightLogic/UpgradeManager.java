package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileItemQuery;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.plugins.lmsnavigator.KeyManagement;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Manages gear upgrades after killing a player and looting a chest.
 * Tracks which gear slots have been upgraded to avoid redundant upgrades.
 */
public class UpgradeManager {
    private static Client client;

    // Chest object ID (LMS reward/supply chest)
    private static final int LMS_CHEST_ID = 29063;

    // Bloody key item ID
    private static final int BLOODY_KEY_ID = 23490;

    // Upgrade tracking per slot
    public enum GearSlot {
        STAFF,
        ROBE_TOP,
        LEGS,
        CROSSBOW,
        MELEE_WEAPON,
        BODY,
        AMULET
    }

    // Track which slots are upgraded
    private static final Set<GearSlot> upgradedSlots = new HashSet<>();

    // State for upgrade task
    private static boolean chestOpened = false;
    private static int actionCooldown = 0;
    private static WorldPoint lastChestLocation = null;

    // Upgrade definitions: base item patterns -> upgrade item patterns (in priority
    // order)
    private static final Map<GearSlot, UpgradeDefinition> UPGRADE_DEFINITIONS = new LinkedHashMap<>();

    // Known supply-chest object IDs (Deadman/LMS variants)
    private static final Set<Integer> SUPPLY_CHEST_IDS = new HashSet<>(Arrays.asList(
            29069, // LMS Chest (discovered via debug logging)
            ObjectID.REWARD_CHEST,
            ObjectID.SUPPLY_CHEST,
            ObjectID.SUPPLY_CHEST_37383,
            ObjectID.DEADMAN_SUPPLY_CHEST,
            ObjectID.DEADMAN_SUPPLY_CHEST_33114,
            ObjectID.DEADMAN_SUPPLY_CHEST_33115,
            ObjectID.DEADMAN_SUPPLY_CHEST_33116,
            ObjectID.DEADMAN_SUPPLY_CHEST_33117,
            ObjectID.DEADMAN_SUPPLY_CHEST_33118,
            ObjectID.DEADMAN_SUPPLY_CHEST_33119,
            ObjectID.DEADMAN_SUPPLY_CHEST_33120,
            ObjectID.DEADMAN_SUPPLY_CHEST_33121,
            ObjectID.DEADMAN_SUPPLY_CHEST_33122,
            ObjectID.DEADMAN_SUPPLY_CHEST_33123,
            ObjectID.DEADMAN_SUPPLY_CHEST_33124,
            ObjectID.DEADMAN_SUPPLY_CHEST_33125));

    static {
        // Staff upgrades
        UPGRADE_DEFINITIONS.put(GearSlot.STAFF, new UpgradeDefinition(
                Arrays.asList("Ahrim's staff"),
                Arrays.asList("Staff of the dead", "Kodai wand", "Volatile nightmare staff", "Zuriel's staff")));

        // Robe top upgrades (support all Mystic robe top variants via wildcard)
        UPGRADE_DEFINITIONS.put(GearSlot.ROBE_TOP, new UpgradeDefinition(
                Arrays.asList("Mystic robe top*"),
                Arrays.asList("Ahrim's robetop", "Ancestral robe top")));

        // Legs upgrades
        UPGRADE_DEFINITIONS.put(GearSlot.LEGS, new UpgradeDefinition(
                Arrays.asList("Rune platelegs"),
                Arrays.asList("Torag's platelegs", "Dharok's platelegs", "Verac's plateskirt")));

        // Crossbow upgrades
        UPGRADE_DEFINITIONS.put(GearSlot.CROSSBOW, new UpgradeDefinition(
                Arrays.asList("Rune crossbow"),
                Arrays.asList("Zaryte crossbow")));

        // Melee weapon upgrades
        UPGRADE_DEFINITIONS.put(GearSlot.MELEE_WEAPON, new UpgradeDefinition(
                Arrays.asList("Abyssal whip"),
                Arrays.asList("Rapier", "Vesta longsword")));

        // Body upgrades
        UPGRADE_DEFINITIONS.put(GearSlot.BODY, new UpgradeDefinition(
                Arrays.asList("Black d'hide body"),
                Arrays.asList("Karil's leathertop")));

        // Amulet upgrades
        UPGRADE_DEFINITIONS.put(GearSlot.AMULET, new UpgradeDefinition(
                Arrays.asList("Amulet of glory"),
                Arrays.asList("Amulet of fury")));
    }

    public static void setClient(Client c) {
        client = c;
    }

    /**
     * Reset upgrade state for a new game.
     */
    public static void reset() {
        upgradedSlots.clear();
        chestOpened = false;
        actionCooldown = 0;
        lastChestLocation = null;
        Logger.norm("[UpgradeManager] Reset upgrade state for new game");
    }

    /**
     * Check if we have a bloody key in inventory (indicates a kill).
     * Delegate to shared LMS key helper so detection matches overlays.
     */
    public static boolean hasBloodyKey() {
        return KeyManagement.hasBloodKey();
    }

    /**
     * Check if a slot has been upgraded already.
     */
    public static boolean isSlotUpgraded(GearSlot slot) {
        return upgradedSlots.contains(slot);
    }

    /**
     * Mark a slot as upgraded.
     */
    public static void markSlotUpgraded(GearSlot slot) {
        upgradedSlots.add(slot);
        Logger.norm("[UpgradeManager] Marked " + slot + " as upgraded");
    }

    /**
     * Get the upgraded item pattern for a slot, or the base pattern if not
     * upgraded.
     */
    public static String getActiveItemPattern(GearSlot slot) {
        UpgradeDefinition def = UPGRADE_DEFINITIONS.get(slot);
        if (def == null) {
            return null;
        }

        if (isSlotUpgraded(slot)) {
            // Return the first upgrade pattern that we have in inventory/equipped
            for (String upgrade : def.upgrades) {
                if (hasItemMatchingPattern(upgrade)) {
                    return upgrade;
                }
            }
        }

        // Return first base pattern we have
        for (String base : def.baseItems) {
            if (hasItemMatchingPattern(base)) {
                return base;
            }
        }

        return def.baseItems.isEmpty() ? null : def.baseItems.get(0);
    }

    /**
     * Check if we have an item matching a pattern in inventory or equipment.
     */
    private static boolean hasItemMatchingPattern(String pattern) {
        // Check inventory
        boolean inInventory = false;
        List<ItemEx> items = InventoryAPI.getItems();
        for (ItemEx item : items) {
            if (matchesPattern(item, pattern)) {
                inInventory = true;
                break;
            }
        }

        if (inInventory)
            return true;

        // Check equipment
        return EquipmentAPI.isEquipped(item -> matchesPattern(item, pattern));
    }

    /**
     * Find the nearest chest within visible range.
     */
    public static WorldPoint findNearestChest() {
        if (client == null)
            return null;

        Player local = client.getLocalPlayer();
        if (local == null)
            return null;

        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null)
            return null;

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
            return null;

        Scene scene = worldView.getScene();
        if (scene == null)
            return null;

        int plane = worldView.getPlane();
        Tile[][][] tiles = scene.getTiles();
        if (tiles == null || plane < 0 || plane >= tiles.length)
            return null;

        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null)
            return null;

        WorldPoint bestChest = null;
        int bestDist = Integer.MAX_VALUE;
        int totalGameObjects = 0;
        int potentialChests = 0;

        for (int x = 0; x < planeTiles.length; x++) {
            Tile[] row = planeTiles[x];
            if (row == null)
                continue;

            for (int y = 0; y < row.length; y++) {
                Tile tile = row[y];
                if (tile == null)
                    continue;

                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects == null)
                    continue;

                for (GameObject obj : gameObjects) {
                    if (obj == null)
                        continue;

                    totalGameObjects++;

                    // Debug: Log all objects with IDs in a reasonable range
                    int objId = obj.getId();
                    if (objId > 0 && objId < 50000) {
                        // Check explicitly for IDs
                        if (SUPPLY_CHEST_IDS.contains(objId)) {
                            potentialChests++;
                            Logger.norm("[UpgradeManager] Found chest by ID: " + objId);
                        }
                    }

                    if (isSupplyChest(obj)) {
                        WorldPoint chestPos = tile.getWorldLocation();
                        if (chestPos == null)
                            continue;

                        int dist = localPos.distanceTo(chestPos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestChest = chestPos;
                        }
                    }
                }
            }
        }

        Logger.norm("[UpgradeManager] Scanned " + totalGameObjects + " game objects, found " + potentialChests
                + " potential chests by ID");

        if (bestChest == null) {
            Logger.norm("[UpgradeManager] No chest found. Trying fallback by name...");
            // Try fallback: look for any object named "Chest" with Search action
            bestChest = findChestByNameFallback(planeTiles, localPos, plane);
        }

        return bestChest;
    }

    /**
     * Fallback chest finder that looks for objects named "Chest" with "Search"
     * action.
     */
    private static WorldPoint findChestByNameFallback(Tile[][] planeTiles, WorldPoint localPos, int plane) {
        WorldPoint bestChest = null;
        int bestDist = Integer.MAX_VALUE;

        for (int x = 0; x < planeTiles.length; x++) {
            Tile[] row = planeTiles[x];
            if (row == null)
                continue;

            for (int y = 0; y < row.length; y++) {
                Tile tile = row[y];
                if (tile == null)
                    continue;

                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects == null)
                    continue;

                for (GameObject obj : gameObjects) {
                    if (obj == null)
                        continue;

                    try {
                        TileObjectEx ex = TileObjectEx.of(obj);
                        if (ex == null)
                            continue;

                        String name = ex.getName();
                        if (name != null && name.toLowerCase().contains("chest")) {
                            boolean hasSearch = ex.hasAction("Search");
                            Logger.norm("[UpgradeManager] Found object named '" + name + "' hasSearch=" + hasSearch
                                    + " id=" + obj.getId());

                            if (hasSearch) {
                                WorldPoint chestPos = tile.getWorldLocation();
                                if (chestPos == null)
                                    continue;

                                int dist = localPos.distanceTo(chestPos);
                                if (dist < bestDist) {
                                    bestDist = dist;
                                    bestChest = chestPos;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore errors from individual objects
                    }
                }
            }
        }

        return bestChest;
    }

    /**
     * Get distance to nearest chest.
     */
    public static int getDistanceToChest(WorldPoint chestPos) {
        if (client == null || chestPos == null)
            return Integer.MAX_VALUE;

        Player local = client.getLocalPlayer();
        if (local == null)
            return Integer.MAX_VALUE;

        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null)
            return Integer.MAX_VALUE;

        return localPos.distanceTo(chestPos);
    }

    /**
     * Move toward the chest.
     */
    public static void moveToChest(WorldPoint chestPos) {
        if (chestPos == null)
            return;
        MovementAPI.walkToWorldPoint(chestPos.getX(), chestPos.getY());
        Logger.norm("[UpgradeManager] Moving to chest at " + chestPos);
    }

    /**
     * Click the chest to open it.
     */
    public static void openChest(WorldPoint chestPos) {
        if (client == null || chestPos == null)
            return;

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
            return;

        Scene scene = worldView.getScene();
        if (scene == null)
            return;

        int plane = worldView.getPlane();
        Tile[][][] tiles = scene.getTiles();
        if (tiles == null || plane < 0 || plane >= tiles.length)
            return;

        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null)
            return;

        // Find the chest at this position
        for (int x = 0; x < planeTiles.length; x++) {
            Tile[] row = planeTiles[x];
            if (row == null)
                continue;

            for (int y = 0; y < row.length; y++) {
                Tile tile = row[y];
                if (tile == null)
                    continue;

                WorldPoint tilePos = tile.getWorldLocation();
                if (tilePos == null || !tilePos.equals(chestPos))
                    continue;

                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects == null)
                    continue;

                for (GameObject obj : gameObjects) {
                    if (isSupplyChest(obj)) {
                        // Click the chest
                        TileObjectEx chestEx = new TileObjectEx(obj);
                        chestEx.interact("Search");
                        chestOpened = true;
                        lastChestLocation = chestPos;
                        Logger.norm("[UpgradeManager] Opened chest at " + chestPos);
                        return;
                    }
                }
            }
        }
    }

    private static boolean isSupplyChest(GameObject obj) {
        if (obj == null)
            return false;

        // Fast path: explicit supply-chest IDs
        if (SUPPLY_CHEST_IDS.contains(obj.getId()))
            return true;

        // Use TileObjectEx to safely resolve name and actions on the client thread
        TileObjectEx ex = TileObjectEx.of(obj);
        if (ex == null)
            return false;

        String name = ex.getName();
        if (name == null)
            return false;

        String lower = name.toLowerCase();

        // LMS chests are typically just named "Chest"; require a Search action to avoid
        // matching random scenery chests.
        return lower.contains("chest") && ex.hasAction("search");
    }

    /**
     * Check if there's an upgrade item on the ground nearby (within 2 tiles) and
     * pick it up.
     * Also picks up essential items if we need them.
     */

    /**
     * Process upgrades - called each tick during UPGRADE_GEAR task.
     * Returns true if we should continue upgrading, false if done.
     */
    public static boolean processUpgradeTick() {
        if (actionCooldown > 0) {
            actionCooldown--;
            return true;
        }

        // First, check ground for any upgrade items and pick them up
        if (pickupUpgradeFromGround()) {
            actionCooldown = 1;
            return true;
        }

        // Check if we have any upgrades in inventory to process
        for (Map.Entry<GearSlot, UpgradeDefinition> entry : UPGRADE_DEFINITIONS.entrySet()) {
            GearSlot slot = entry.getKey();
            if (isSlotUpgraded(slot))
                continue;

            UpgradeDefinition def = entry.getValue();

            // Check if we have an upgrade item in inventory
            for (String upgradePattern : def.upgrades) {
                ItemEx upgradeItem = findInventoryItem(upgradePattern);
                if (upgradeItem != null) {
                    // We found an upgrade! Now handle the old item
                    if (dropOldItemForSlot(slot, def)) {
                        actionCooldown = 1;
                        return true;
                    }

                    // Old item handled, equip the upgrade or mark as upgraded
                    markSlotUpgraded(slot);
                    Logger.norm("[UpgradeManager] Upgraded " + slot + " to " + upgradePattern);
                    actionCooldown = 1;
                    return true;
                }
            }
        }
        // All done upgrading (no upgrades or drops this tick)
        return false;
    }

    /**
     * Post-upgrade cleanup: Called after PROCESS_UPGRADES completes.
     *
     * Behaviour:
     * - First, try to pick up any nearby upgrade items from the ground.
     * - Then, run a conservative dropUnwantedItems pass using isWantedItem
     * (which keeps food, pots, key items, and all base/upgrade gear).
     *
     * Returns true while there is more work to do, or false when cleanup is done.
     */
    public static boolean cleanupChestLoot() {
        // Try to pick up any nearby upgrades on the ground.
        if (pickupUpgradeFromGround()) {
            actionCooldown = 1;
            return true;
        }

        // Perform a single conservative inventory cleanup.
        if (dropUnwantedItems()) {
            actionCooldown = 1;
            return true;
        }

        return false;
    }

    /**
     * Try to pick up an upgrade item from the ground within range.
     */
    public static boolean pickupUpgradeFromGround() {
        if (client == null)
            return false;

        Player local = client.getLocalPlayer();
        if (local == null)
            return false;

        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null)
            return false;

        // Check ground items for upgrades
        TileItemQuery query = new TileItemQuery();
        List<TileItemEx> groundItems = query.collect();

        for (TileItemEx groundItem : groundItems) {
            if (groundItem == null)
                continue;

            WorldPoint itemPos = groundItem.getWorldPoint();
            if (itemPos == null)
                continue;

            int dist = localPos.distanceTo(itemPos);
            if (dist > 3)
                continue; // Only pick up very close items

            String itemName = groundItem.getName();
            if (itemName == null)
                continue;

            // Check if this is an upgrade we want
            for (Map.Entry<GearSlot, UpgradeDefinition> entry : UPGRADE_DEFINITIONS.entrySet()) {
                GearSlot slot = entry.getKey();
                if (isSlotUpgraded(slot))
                    continue;

                UpgradeDefinition def = entry.getValue();
                for (String upgradePattern : def.upgrades) {
                    if (matchesPatternString(itemName, upgradePattern)) {
                        groundItem.interact("Take");
                        Logger.norm("[UpgradeManager] Picking up upgrade: " + itemName);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Try to pick up upgrades while roaming (within 8 tiles).
     */
    public static boolean pickupUpgradeWhileRoaming() {
        if (client == null)
            return false;

        Player local = client.getLocalPlayer();
        if (local == null)
            return false;

        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null)
            return false;

        TileItemQuery query = new TileItemQuery();
        List<TileItemEx> groundItems = query.collect();

        for (TileItemEx groundItem : groundItems) {
            if (groundItem == null)
                continue;

            WorldPoint itemPos = groundItem.getWorldPoint();
            if (itemPos == null)
                continue;

            int dist = localPos.distanceTo(itemPos);
            if (dist > 8)
                continue;

            String itemName = groundItem.getName();
            if (itemName == null)
                continue;

            // Check if this is an upgrade we want and haven't upgraded yet
            for (Map.Entry<GearSlot, UpgradeDefinition> entry : UPGRADE_DEFINITIONS.entrySet()) {
                GearSlot slot = entry.getKey();
                if (isSlotUpgraded(slot))
                    continue;

                UpgradeDefinition def = entry.getValue();
                for (String upgradePattern : def.upgrades) {
                    if (matchesPatternString(itemName, upgradePattern)) {
                        groundItem.interact("Take");
                        Logger.norm("[UpgradeManager] Roam pickup upgrade: " + itemName);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Drop the old item for a slot before upgrading.
     * Returns true if an action was taken.
     */
    private static boolean dropOldItemForSlot(GearSlot slot, UpgradeDefinition def) {
        // Check if old item is equipped
        for (String basePattern : def.baseItems) {
            if (EquipmentAPI.isEquipped(item -> matchesPattern(item, basePattern))) {
                // Find an upgrade in inventory and equip it to force old to inventory
                for (String upgradePattern : def.upgrades) {
                    ItemEx upgradeItem = findInventoryItem(upgradePattern);
                    if (upgradeItem != null) {
                        InventoryAPI.interact(upgradeItem, "Wield", "Wear", "Equip");
                        Logger.norm("[UpgradeManager] Equipping " + upgradePattern + " to swap out " + basePattern);
                        return true;
                    }
                }
            }

            // Check if old item is in inventory
            ItemEx oldItem = findInventoryItem(basePattern);
            if (oldItem != null) {
                InventoryAPI.interact(oldItem, "Drop");
                Logger.norm("[UpgradeManager] Dropping old item: " + basePattern);
                return true;
            }
        }

        return false;
    }

    /**
     * Drop items that are not in our upgrade/keep list.
     */
    private static boolean dropUnwantedItems() {
        List<ItemEx> items = InventoryAPI.getItems();
        for (ItemEx item : items) {
            if (item == null)
                continue;

            String name = item.getName();
            if (name == null)
                continue;

            // Check if this item is wanted
            if (!isWantedItem(name)) {
                InventoryAPI.interact(item, "Drop");
                Logger.norm("[UpgradeManager] Dropping unwanted item: " + name);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if an item is in our wanted list (base items, upgrades, or essential
     * items).
     */
    private static boolean isWantedItem(String itemName) {
        if (itemName == null)
            return false;

        String lower = itemName.toLowerCase();

        // Essential items we always keep
        if (lower.contains("shark"))
            return true;
        if (lower.contains("karambwan"))
            return true;
        if (lower.contains("super combat"))
            return true;
        if (lower.contains("ranging potion"))
            return true;
        if (lower.contains("saradomin brew"))
            return true;
        if (lower.contains("super restore"))
            return true;
        if (lower.contains("anglerfish"))
            return true;
        if (lower.contains("rune pouch"))
            return true;
        if (lower.contains("dragon bolts"))
            return true;
        if (lower.contains("dragon dagger"))
            return true;
        if (lower.contains("dragon defender"))
            return true;
        if (lower.contains("spirit shield"))
            return true;
        if (lower.contains("mage's book"))
            return true;

        // Always keep mage legs: mystic robe bottoms and similar variants
        if (lower.contains("mystic robe bottom"))
            return true;

        // Never drop the LMS bloody key; it's required for upgrades
        if (lower.contains("bloody key"))
            return true;

        // Check if it's a base or upgrade item
        for (UpgradeDefinition def : UPGRADE_DEFINITIONS.values()) {
            for (String base : def.baseItems) {
                if (matchesPatternString(itemName, base))
                    return true;
            }
            for (String upgrade : def.upgrades) {
                if (matchesPatternString(itemName, upgrade))
                    return true;
            }
        }

        return false;
    }

    private static ItemEx findInventoryItem(String pattern) {
        List<ItemEx> items = InventoryAPI.getItems();
        for (ItemEx item : items) {
            if (matchesPattern(item, pattern)) {
                return item;
            }
        }
        return null;
    }

    private static boolean matchesPattern(ItemEx item, String pattern) {
        if (item == null || pattern == null)
            return false;
        String name = item.getName();
        return matchesPatternString(name, pattern);
    }

    private static boolean matchesPatternString(String name, String pattern) {
        if (name == null || pattern == null)
            return false;

        String n = name.toLowerCase();
        String p = pattern.toLowerCase();

        if (p.endsWith("*")) {
            return n.startsWith(p.substring(0, p.length() - 1));
        }

        return n.contains(p);
    }

    public static boolean isChestOpened() {
        return chestOpened;
    }

    public static void setChestOpened(boolean opened) {
        chestOpened = opened;
    }

    public static void setActionCooldown(int ticks) {
        actionCooldown = ticks;
    }

    /**
     * Get all upgrade patterns for checking.
     */
    public static List<String> getAllUpgradePatterns() {
        List<String> patterns = new ArrayList<>();
        for (UpgradeDefinition def : UPGRADE_DEFINITIONS.values()) {
            patterns.addAll(def.upgrades);
        }
        return patterns;
    }

    /**
     * Inner class to hold upgrade definitions.
     */
    private static class UpgradeDefinition {
        final List<String> baseItems;
        final List<String> upgrades;

        UpgradeDefinition(List<String> baseItems, List<String> upgrades) {
            this.baseItems = baseItems;
            this.upgrades = upgrades;
        }
    }
}
