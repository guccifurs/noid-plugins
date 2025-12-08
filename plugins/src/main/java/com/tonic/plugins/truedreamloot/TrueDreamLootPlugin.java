package com.tonic.plugins.truedreamloot;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@PluginDescriptor(name = "TrueDream Loot", description = "Automated radius-based looting with banking", tags = { "loot",
        "banking", "radius" })
public class TrueDreamLootPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private TrueDreamLootConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TrueDreamLootOverlay overlay;

    @Inject
    private TrueDreamLootSceneOverlay sceneOverlay;

    private WorldPoint centerTile;
    private Instant startTime;
    private Instant lastActionTime;
    private int itemsLooted = 0;
    private long totalValueLooted = 0;
    private int timesBanked = 0;

    private enum State {
        IDLE,
        LOOTING,
        BANKING,
        RETURNING
    }

    private State currentState = State.IDLE;

    @Provides
    TrueDreamLootConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TrueDreamLootConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        overlayManager.add(sceneOverlay);

        startTime = Instant.now();
        lastActionTime = Instant.now();

        // Auto-center on the local player so the overlay and looter work out of the box
        if (client != null && client.getLocalPlayer() != null) {
            centerTile = client.getLocalPlayer().getWorldLocation();
            Logger.norm("TrueDream Loot: Auto center set to player position: " + centerTile);
        }

        Logger.norm("TrueDream Loot Plugin started v1.9 - center loot + banker-first banking + instant-tick looting");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayManager.remove(sceneOverlay);
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        client.createMenuEntry(-1)
                .setOption("Set Center")
                .setTarget("")
                .setType(MenuAction.RUNELITE);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("Set Center")) {
            if (client.getSelectedSceneTile() != null) {
                centerTile = client.getSelectedSceneTile().getWorldLocation();
            } else {
                centerTile = client.getLocalPlayer().getWorldLocation();
            }
            Logger.norm("TrueDream Loot Center set to: " + centerTile);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (centerTile == null)
            return;

        handleState();
    }

    private void handleState() {
        // Check banking trigger
        if (config.bankItems() && InventoryAPI.getEmptySlots() < 8 && currentState != State.BANKING) {
            currentState = State.BANKING;
        }

        switch (currentState) {
            case IDLE:
                handleIdle();
                break;
            case LOOTING:
                handleLooting();
                break;
            case BANKING:
                handleBanking();
                break;
            case RETURNING:
                handleReturning();
                break;
        }
    }

    private void lootItem(TileItemEx loot) {
        Logger.norm("[TrueDream Loot] Looting: " + loot.getName() + " at " + loot.getWorldPoint());
        // Use the same ground item action index (2) that works in DropParty for
        // taking items, so we rely on the same packet behavior here.
        TileItemAPI.interact(loot, 2);
        lastActionTime = Instant.now();
        itemsLooted++;
        totalValueLooted += loot.getGePrice() * loot.getQuantity();
    }

    private void handleIdle() {
        // Check for loot
        TileItemEx loot = findBestLootableItem();
        if (loot != null) {
            currentState = State.LOOTING;
            lootItem(loot);
            return;
        }

        // Check return to center
        if (Duration.between(lastActionTime, Instant.now()).getSeconds() > config.returnToCenterTime()) {
            currentState = State.RETURNING;
        }
    }

    private void handleLooting() {
        TileItemEx loot = findBestLootableItem();
        if (loot != null) {
            lootItem(loot);
        } else {
            currentState = State.IDLE;
        }
    }

    private void handleBanking() {
        if (BankAPI.isOpen()) {
            BankAPI.depositAll();
            timesBanked++;
            currentState = State.RETURNING;
            return;
        }

        // Prefer Bankers (NPCs) so this works reliably at the Grand Exchange, then
        // fall back to any nearby bank booth.
        NpcEx banker = NpcAPI.search().withName("Banker").nearest();
        if (banker != null) {
            NpcAPI.interact(banker, "Bank");
            return;
        }

        TileObjectEx booth = TileObjectAPI.search().withName("Bank booth").nearest();
        if (booth != null) {
            TileObjectAPI.interact(booth, "Bank");
        }
    }

    private void handleReturning() {
        if (client.getLocalPlayer().getWorldLocation().distanceTo(centerTile) <= 1) {
            currentState = State.IDLE;
            lastActionTime = Instant.now();
            return;
        }

        MovementAPI.walkToWorldPoint(centerTile);
    }

    private TileItemEx findBestLootableItem() {
        int radius = config.maxRadius();
        int minValue = config.minLootValue();
        if (centerTile == null) {
            return null;
        }

        // Search around the configured center tile instead of the current
        // player position, so the behavior matches the radius overlay.
        com.tonic.queries.TileItemQuery query = TileItemAPI.search().within(centerTile, radius);
        if (minValue > 0) {
            query = query.greaterThanGePrice(minValue);
        }

        List<TileItemEx> items = query.collect();
        if (items == null || items.isEmpty()) {
            return null;
        }

        TileItemEx best = null;
        long bestValue = 0;

        for (TileItemEx item : items) {
            if (item == null || item.getWorldPoint() == null)
                continue;

            long value = item.getGePrice() * item.getQuantity();
            if (value > bestValue) {
                bestValue = value;
                best = item;
            }
        }

        if (best != null) {
            Logger.norm("[TrueDream Loot] Best loot: " + best.getName() + " value: " + bestValue
                    + " at " + best.getWorldPoint());
        }

        return best;
    }

    public WorldPoint getCenterTile() {
        return centerTile;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public int getItemsLooted() {
        return itemsLooted;
    }

    public long getTotalValueLooted() {
        return totalValueLooted;
    }

    public int getTimesBanked() {
        return timesBanked;
    }

    public State getCurrentState() {
        return currentState;
    }

    /**
     * Human-readable status text for the overlay without exposing internal enum details.
     */
    public String getStatusText() {
        if (centerTile == null) {
            return "Set Center";
        }

        // Use the enum name; this is safe here and avoids the overlay calling name()
        // on a private enum type directly (which caused the access error).
        return currentState != null ? currentState.name() : "Unknown";
    }
}
