package com.tonic.plugins.dropparty;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.TileItemEx;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@PluginDescriptor(name = "Drop Party", description = "Track drop party paths and auto-loot items (speed optimized)", tags = {
        "drop",
        "party", "loot", "trail", "marker" })
public class DropPartyPlugin extends Plugin {

    private static final String TRACK_OPTION = "<col=00ff00>Track Path</col>";
    private static final String STOP_TRACK_OPTION = "<col=ff0000>Stop Tracking</col>";

    @Inject
    private Client client;

    @Inject
    private DropPartyConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DropPartyOverlay overlay;

    @Inject
    private KeyManager keyManager;

    @Getter(AccessLevel.PACKAGE)
    private final List<TimedTile> timedTiles = new ArrayList<>();

    @Getter(AccessLevel.PACKAGE)
    private String cachedPlayerName = null;

    @Getter(AccessLevel.PACKAGE)
    private Color overlayColor;

    @Getter(AccessLevel.PACKAGE)
    private int fontStyle;

    @Getter(AccessLevel.PACKAGE)
    private int textSize;

    @Getter(AccessLevel.PACKAGE)
    private Player trackedPlayer;

    private WorldPoint lastTrackedPosition;

    @Getter(AccessLevel.PACKAGE)
    private int lootedItemCount = 0;

    // Current game tick counter for tick-based timing
    private int currentTick = 0;

    // Current target item we're trying to loot - stick with it until gone
    private WorldPoint currentTargetItemLocation = null;
    private int currentTargetItemId = -1;

    // Follow state - toggled by hotkey
    @Getter(AccessLevel.PACKAGE)
    private boolean followActive = false;

    // Hotkey listener for follow toggle
    private final HotkeyListener followHotkeyListener = new HotkeyListener(() -> config.followHotkey()) {
        @Override
        public void hotkeyPressed() {
            followActive = !followActive;
            Logger.norm("[Drop Party] Follow " + (followActive ? "ACTIVATED" : "DEACTIVATED") + " via hotkey");
        }
    };

    /**
     * Tick-based TimedTile for zero-delay path tracking.
     */
    public static class TimedTile {
        public final WorldPoint location;
        public final int createdAtTick;
        public final int durationTicks;
        private static final int GRACE_PERIOD_TICKS = 5; // ~3 seconds grace
        public boolean visited = false;

        public TimedTile(WorldPoint location, int durationTicks, int currentTick) {
            this.location = location;
            this.createdAtTick = currentTick;
            this.durationTicks = durationTicks;
        }

        public int getRemainingTicks(int currentTick) {
            int elapsed = currentTick - createdAtTick;
            return Math.max(-GRACE_PERIOD_TICKS, durationTicks - elapsed);
        }

        public boolean isExpired(int currentTick) {
            return (currentTick - createdAtTick) > (durationTicks + GRACE_PERIOD_TICKS);
        }

        public boolean isGracePeriod(int currentTick) {
            return (currentTick - createdAtTick) > durationTicks;
        }
    }

    @Provides
    DropPartyConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(DropPartyConfig.class);
    }

    @Override
    protected void startUp() {
        Logger.norm("[Drop Party] v3.0 - Speed optimized with tick-based timing");
        updateConfig();
        overlayManager.add(overlay);
        keyManager.registerKeyListener(followHotkeyListener);

        String configName = config.playerName();
        if (configName != null && !configName.isEmpty()) {
            cachedPlayerName = configName;
            Logger.norm("[Drop Party] Tracking from config: " + cachedPlayerName);
        }
    }

    @Override
    protected void shutDown() {
        Logger.norm("[Drop Party] Plugin stopped");
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(followHotkeyListener);
        clearAll();
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        MenuEntry[] entries = event.getMenuEntries();
        // Single set to track all names we've already added a Track option for
        Set<String> processedNames = new HashSet<>();

        for (MenuEntry entry : entries) {
            // Handle player options (right-clicking players in game)
            if (isPlayerOption(entry)) {
                String rawTarget = entry.getTarget();
                String targetName = Text.removeTags(rawTarget).replace('\u00A0', ' ').trim();

                int levelIdx = targetName.indexOf("(level-");
                if (levelIdx != -1) {
                    targetName = targetName.substring(0, levelIdx).trim();
                }

                if (targetName == null || targetName.isEmpty()) {
                    continue;
                }

                // Check if we already added a Track option for this name
                String standardizedName = Text.standardize(targetName);
                if (processedNames.contains(standardizedName)) {
                    continue;
                }
                processedNames.add(standardizedName);

                boolean isTrackingThis = cachedPlayerName != null
                        && Text.standardize(cachedPlayerName).equalsIgnoreCase(standardizedName);

                String optionText = isTrackingThis ? STOP_TRACK_OPTION : TRACK_OPTION;
                final String capturedName = targetName;

                client.createMenuEntry(-1)
                        .setOption(optionText)
                        .setTarget(rawTarget)
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> {
                            if (isTrackingThis)
                                stopTracking();
                            else
                                startTracking(capturedName);
                        });
            }

            // Handle chat message options (right-clicking names in chat)
            if (isChatOption(entry)) {
                String rawTarget = entry.getTarget();
                if (rawTarget == null || rawTarget.isEmpty()) {
                    continue;
                }

                String chatName = Text.removeTags(rawTarget).replace('\u00A0', ' ').trim();
                if (chatName.isEmpty()) {
                    continue;
                }

                // Check if we already added a Track option for this name
                String standardizedName = Text.standardize(chatName);
                if (processedNames.contains(standardizedName)) {
                    continue;
                }
                processedNames.add(standardizedName);

                boolean isTrackingThis = cachedPlayerName != null
                        && Text.standardize(cachedPlayerName).equalsIgnoreCase(standardizedName);

                String optionText = isTrackingThis ? STOP_TRACK_OPTION : TRACK_OPTION;
                final String capturedName = chatName;

                client.createMenuEntry(-1)
                        .setOption(optionText)
                        .setTarget(rawTarget)
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> {
                            if (isTrackingThis)
                                stopTracking();
                            else
                                startTracking(capturedName);
                        });
            }
        }
    }

    private boolean isPlayerOption(MenuEntry entry) {
        MenuAction type = entry.getType();
        return type == MenuAction.PLAYER_FIRST_OPTION
                || type == MenuAction.PLAYER_SECOND_OPTION
                || type == MenuAction.PLAYER_THIRD_OPTION
                || type == MenuAction.PLAYER_FOURTH_OPTION
                || type == MenuAction.PLAYER_FIFTH_OPTION
                || type == MenuAction.PLAYER_SIXTH_OPTION
                || type == MenuAction.PLAYER_SEVENTH_OPTION
                || type == MenuAction.PLAYER_EIGHTH_OPTION;
    }

    /**
     * Check if this is a chat-related menu option (for right-clicking names in
     * chat).
     */
    private boolean isChatOption(MenuEntry entry) {
        String option = entry.getOption();
        if (option == null)
            return false;

        // These are the options that appear when right-clicking a player name in chat
        return option.equals("Add friend")
                || option.equals("Message")
                || option.equals("Add ignore")
                || option.equals("Report");
    }

    private void startTracking(String name) {
        name = Text.removeTags(name).replace('\u00A0', ' ').trim();

        cachedPlayerName = name;
        timedTiles.clear();
        lastTrackedPosition = null;
        trackedPlayer = null;
        lootedItemCount = 0;
        followActive = false;
        currentTargetItemLocation = null;
        currentTargetItemId = -1;
        Logger.norm("[Drop Party] === STARTED TRACKING: [" + name + "] ===");
    }

    private void stopTracking() {
        Logger.norm("[Drop Party] === STOPPED TRACKING: [" + cachedPlayerName + "] ===");
        cachedPlayerName = null;
        timedTiles.clear();
        lastTrackedPosition = null;
        trackedPlayer = null;
        followActive = false;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        currentTick++;
        removeExpiredTiles();

        String nameToTrack = getActivePlayerName();

        // Try to find tracked player (may be null if they left)
        if (nameToTrack != null && !nameToTrack.isEmpty()) {
            trackedPlayer = findPlayerByName(nameToTrack);

            // Track player position if visible
            if (trackedPlayer != null) {
                trackPlayerPosition();
            }
        }

        // ALWAYS run automation if we have tiles or follow is active
        // This allows looting/following even when tracked player is gone
        if ((config.autoFollow() || followActive) || config.autoLoot()) {
            handleAutomation();
        }
    }

    String getActivePlayerName() {
        if (cachedPlayerName != null && !cachedPlayerName.isEmpty()) {
            return cachedPlayerName;
        }
        return config.playerName();
    }

    private void removeExpiredTiles() {
        Iterator<TimedTile> iterator = timedTiles.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired(currentTick)) {
                iterator.remove();
            }
        }
    }

    /**
     * Zero-delay automation handler.
     * Priority: 1) Loot current target (or find closest), 2) Follow path
     * Locks onto closest item and keeps clicking until it's gone.
     */
    private void handleAutomation() {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
            return;

        WorldPoint localPos = localPlayer.getWorldLocation();
        if (localPos == null)
            return;

        // PRIORITY 1: Loot items (with target locking)
        if (config.autoLoot()) {
            // First, check if our current target is still on the ground
            if (currentTargetItemLocation != null) {
                TileItemEx currentTarget = findItemAtLocation(currentTargetItemLocation, currentTargetItemId);
                if (currentTarget != null) {
                    // Target still exists - keep clicking it
                    lootItemImmediate(currentTarget);
                    return;
                } else {
                    // Target is GONE - clear it and look for next
                    Logger.norm("[Drop Party] Target item gone - looking for next");
                    currentTargetItemLocation = null;
                    currentTargetItemId = -1;
                }
            }

            // Find new closest item to target
            TileItemEx item = findClosestLootableItem();
            if (item != null) {
                // Lock onto this item
                currentTargetItemLocation = item.getWorldPoint();
                currentTargetItemId = item.getId();
                lootItemImmediate(item);
                return; // Loot takes priority
            }
        }

        // PRIORITY 2: Follow path IMMEDIATELY (no tick delays)
        if (config.autoFollow() || followActive) {
            if (!timedTiles.isEmpty()) {
                TimedTile tile = findTileToClick(localPos);
                if (tile != null) {
                    int remaining = tile.getRemainingTicks(currentTick);
                    if (remaining <= config.clickAtTimerTicks()) {
                        clickTile(tile.location);
                    }
                }
            }
        }
    }

    /**
     * Check if a specific item still exists at a location.
     */
    private TileItemEx findItemAtLocation(WorldPoint location, int itemId) {
        if (location == null)
            return null;

        List<TileItemEx> items = TileItemAPI.search().atLocation(location).collect();
        if (items == null)
            return null;

        for (TileItemEx item : items) {
            if (item != null && item.getId() == itemId) {
                return item;
            }
        }
        return null;
    }

    /**
     * Find the CLOSEST lootable item for maximum speed.
     * Sorted by distance, not value.
     */
    private TileItemEx findClosestLootableItem() {
        int minValue = config.minLootValue();
        int maxRange = config.maxLootRange();

        List<TileItemEx> items;
        if (minValue > 0) {
            items = TileItemAPI.search()
                    .within(maxRange)
                    .greaterThanGePrice(minValue)
                    .collect();
        } else {
            items = TileItemAPI.search()
                    .within(maxRange)
                    .collect();
        }

        if (items == null || items.isEmpty()) {
            return null;
        }

        // Find closest item
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
            return null;

        WorldPoint playerPos = localPlayer.getWorldLocation();
        if (playerPos == null)
            return null;

        TileItemEx closest = null;
        int closestDist = Integer.MAX_VALUE;

        for (TileItemEx item : items) {
            if (item == null || item.getId() == -1)
                continue;

            WorldPoint itemPos = item.getWorldPoint();
            if (itemPos == null)
                continue;

            int dist = playerPos.distanceTo(itemPos);
            if (dist < closestDist) {
                closestDist = dist;
                closest = item;
            }
        }

        return closest;
    }

    private TimedTile findTileToClick(WorldPoint currentPos) {
        if (timedTiles.isEmpty() || currentPos == null)
            return null;

        TimedTile oldest = null;
        int lowestTimer = Integer.MAX_VALUE;

        for (TimedTile tile : timedTiles) {
            if (tile.location == null || tile.location.equals(currentPos)) {
                tile.visited = true;
                continue;
            }

            if (tile.visited)
                continue;
            if (tile.isGracePeriod(currentTick))
                continue;

            int remaining = tile.getRemainingTicks(currentTick);
            if (remaining < lowestTimer) {
                lowestTimer = remaining;
                oldest = tile;
            }
        }
        return oldest;
    }

    /**
     * Immediate loot - no state tracking, just click.
     * Speed is critical - click every tick if needed.
     */
    private void lootItemImmediate(TileItemEx item) {
        if (item == null || item.getId() == -1)
            return;

        Logger.norm("[Drop Party] Looting: " + item.getName() + " (closest)");

        try {
            int actionIndex = config.lootActionIndex();
            TileItemAPI.interact(item, actionIndex);
            lootedItemCount++;
        } catch (Exception e) {
            Logger.norm("[Drop Party] Loot failed: " + e.getMessage());
        }
    }

    private void clickTile(WorldPoint tile) {
        if (tile == null)
            return;
        Logger.norm("[Drop Party] Following: " + tile);
        MovementAPI.walkToWorldPoint(tile.getX(), tile.getY());
    }

    private Player findPlayerByName(String name) {
        List<Player> players = client.getPlayers();
        if (players == null)
            return null;

        String standardizedSearch = Text.standardize(name);

        for (Player player : players) {
            if (player == null || player.getName() == null)
                continue;

            if (Text.standardize(player.getName()).equalsIgnoreCase(standardizedSearch)) {
                return player;
            }
        }
        return null;
    }

    private void trackPlayerPosition() {
        if (trackedPlayer == null)
            return;

        WorldPoint currentPos = trackedPlayer.getWorldLocation();
        if (currentPos == null)
            return;

        if (lastTrackedPosition == null || !lastTrackedPosition.equals(currentPos)) {
            timedTiles.add(new TimedTile(currentPos, config.pathDurationTicks(), currentTick));
            Logger.norm("[Drop Party] Tile added: " + currentPos + " (total: " + timedTiles.size() + ")");
            lastTrackedPosition = currentPos;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("dropparty"))
            return;

        if (event.getKey().equals("playerName")) {
            String newName = config.playerName();
            if (newName != null && !newName.isEmpty()) {
                startTracking(newName);
            }
        }
        updateConfig();
    }

    private void updateConfig() {
        this.overlayColor = config.overlayColor();
        this.fontStyle = config.fontStyle().getFont();
        this.textSize = config.textSize();
    }

    private void clearAll() {
        timedTiles.clear();
        trackedPlayer = null;
        cachedPlayerName = null;
        lastTrackedPosition = null;
        lootedItemCount = 0;
        followActive = false;
    }

    boolean isTracking() {
        String name = getActivePlayerName();
        return name != null && !name.isEmpty();
    }

    /**
     * Get current tick for overlay rendering.
     */
    int getCurrentTick() {
        return currentTick;
    }
}
