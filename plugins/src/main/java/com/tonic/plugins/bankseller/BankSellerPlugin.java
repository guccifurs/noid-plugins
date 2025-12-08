package com.tonic.plugins.bankseller;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.api.widgets.GrandExchangeAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(name = "<html><font color='#00FF00'>[NP]</font> Bank Seller</html>", description = "Withdraws tradeable items from bank and sells at GE", tags = {
        "bank", "ge", "seller", "gold" })
public class BankSellerPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private BankSellerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BankSellerOverlay overlay;

    @Inject
    private ItemManager itemManager;

    @Getter
    private BankSellerState state = BankSellerState.IDLE;

    @Getter
    private int itemsSold = 0;

    @Getter
    private long goldEarned = 0;

    @Getter
    private String statusMessage = "Idle";

    private Set<String> excludedItems = new HashSet<>();
    private int tickDelay = 0;

    @Provides
    BankSellerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BankSellerConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        parseExcludedItems();
        Logger.norm("[Bank Seller] Plugin started");
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        state = BankSellerState.IDLE;
        Logger.norm("[Bank Seller] Plugin stopped");
    }

    private void parseExcludedItems() {
        excludedItems.clear();
        String excluded = config.excludedItems();
        if (excluded != null && !excluded.isEmpty()) {
            Arrays.stream(excluded.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .forEach(excludedItems::add);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.enabled()) {
            if (state != BankSellerState.IDLE) {
                state = BankSellerState.IDLE;
                statusMessage = "Disabled";
            }
            return;
        }

        // Tick delay for pacing
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        try {
            switch (state) {
                case IDLE:
                    state = BankSellerState.CHECKING_OFFERS;
                    statusMessage = "Checking offers...";
                    break;

                case CHECKING_OFFERS:
                    handleCheckingOffers();
                    break;

                case OPEN_BANK:
                    handleOpenBank();
                    break;

                case WITHDRAW_ITEMS:
                    handleWithdrawItems();
                    break;

                case OPEN_GE:
                    handleOpenGE();
                    break;

                case SELL_ITEMS:
                    handleSellItems();
                    break;

                case COLLECT_GOLD:
                    handleCollectGold();
                    break;

                case DONE:
                    statusMessage = "Done! No more items to sell.";
                    break;
            }
        } catch (Exception e) {
            Logger.error("[Bank Seller] Error: " + e.getMessage());
            statusMessage = "Error: " + e.getMessage();
        }
    }

    private void handleCheckingOffers() {
        // Check if we have completed sell offers
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        boolean hasCompletedSell = false;

        for (GrandExchangeOffer offer : offers) {
            if (offer != null && offer.getState() == GrandExchangeOfferState.SOLD) {
                hasCompletedSell = true;
                break;
            }
        }

        if (hasCompletedSell) {
            state = BankSellerState.COLLECT_GOLD;
            return;
        }

        // Check if we have items in inventory to sell
        List<ItemEx> tradeableItems = getTradeableInventoryItems();
        if (!tradeableItems.isEmpty()) {
            state = BankSellerState.OPEN_GE;
            return;
        }

        // Otherwise go to bank
        state = BankSellerState.OPEN_BANK;
        statusMessage = "Opening bank...";
    }

    private void handleOpenBank() {
        if (BankAPI.isOpen()) {
            state = BankSellerState.WITHDRAW_ITEMS;
            return;
        }

        // Find and interact with banker
        NpcEx banker = new NpcQuery()
                .withNameContains("Banker")
                .withAction("Bank")
                .first();

        if (banker != null) {
            banker.interact("Bank");
            statusMessage = "Opening bank...";
            tickDelay = config.tickDelay();
        } else {
            statusMessage = "Cannot find banker!";
        }
    }

    private void handleWithdrawItems() {
        if (!BankAPI.isOpen()) {
            state = BankSellerState.OPEN_BANK;
            return;
        }

        // Set note mode first
        if (!BankAPI.isWithdrawNote()) {
            BankAPI.setWithdrawMode(true);
            tickDelay = config.tickDelay();
            return;
        }

        // If inventory is full, go sell
        if (InventoryAPI.isFull()) {
            state = BankSellerState.OPEN_GE;
            statusMessage = "Inventory full, going to GE...";
            return;
        }

        // Find tradeable items in bank and withdraw as many as possible
        List<ItemEx> bankItems = BankAPI.search().collect();
        int withdrawnThisTick = 0;
        int maxWithdrawPerTick = config.tickDelay() == 0 ? 28 : 1; // Fast mode = withdraw all at once

        for (ItemEx item : bankItems) {
            if (InventoryAPI.isFull() || withdrawnThisTick >= maxWithdrawPerTick) {
                break;
            }

            if (item == null)
                continue;
            String name = item.getName();
            if (name == null)
                continue;

            // Skip excluded items
            if (excludedItems.contains(name.toLowerCase()))
                continue;

            // Check if tradeable (basic check - not bonds, untradeable, etc.)
            int price = itemManager.getItemPrice(item.getId());
            if (price < config.minPrice())
                continue;

            // Skip coins
            if (item.getId() == 995)
                continue;

            BankAPI.withdraw(item.getId(), -1, true);
            statusMessage = "Withdrawing " + item.getName();
            withdrawnThisTick++;
        }

        if (withdrawnThisTick > 0) {
            tickDelay = config.tickDelay();
            // Check if we should continue withdrawing or go sell
            if (InventoryAPI.isFull() || config.tickDelay() == 0) {
                state = BankSellerState.OPEN_GE;
            }
            // else stay in WITHDRAW_ITEMS to get more items next tick
        } else {
            // No more items to withdraw
            List<ItemEx> invItems = getTradeableInventoryItems();
            if (!invItems.isEmpty()) {
                state = BankSellerState.OPEN_GE;
                statusMessage = "Going to sell...";
            } else {
                state = BankSellerState.DONE;
                statusMessage = "No more tradeable items in bank";
            }
        }
    }

    private void handleOpenGE() {
        if (BankAPI.isOpen()) {
            // Close bank first
            client.runScript(29);
            tickDelay = config.tickDelay();
            return;
        }

        if (GrandExchangeAPI.isOpen()) {
            state = BankSellerState.SELL_ITEMS;
            return;
        }

        // Find GE clerk
        NpcEx clerk = new NpcQuery()
                .withNameContains("Grand Exchange Clerk")
                .withAction("Exchange")
                .first();

        if (clerk != null) {
            clerk.interact("Exchange");
            statusMessage = "Opening GE...";
            tickDelay = config.tickDelay();
        } else {
            statusMessage = "Cannot find GE clerk!";
        }
    }

    private void handleSellItems() {
        if (!GrandExchangeAPI.isOpen()) {
            state = BankSellerState.OPEN_GE;
            return;
        }

        // Get free slot
        int freeSlot = GrandExchangeAPI.freeSlot();
        if (freeSlot == -1) {
            // No free slots, wait for offers to complete
            statusMessage = "Waiting for GE slot...";
            tickDelay = config.tickDelay();
            return;
        }

        // Get tradeable item from inventory
        List<ItemEx> items = getTradeableInventoryItems();
        if (items.isEmpty()) {
            state = BankSellerState.CHECKING_OFFERS;
            return;
        }

        ItemEx item = items.get(0);
        int price = itemManager.getItemPrice(item.getId());
        int sellPrice = (price * config.pricePercentage()) / 100;
        if (sellPrice < 1)
            sellPrice = 1;

        GrandExchangeAPI.startSellOffer(item.getId(), item.getQuantity(), sellPrice);
        itemsSold++;
        goldEarned += (long) sellPrice * item.getQuantity();
        statusMessage = "Selling " + item.getName() + " x" + item.getQuantity();
        tickDelay = config.tickDelay();
    }

    private void handleCollectGold() {
        if (!GrandExchangeAPI.isOpen()) {
            // Open GE first
            state = BankSellerState.OPEN_GE;
            return;
        }

        GrandExchangeAPI.collectAll();
        statusMessage = "Collecting gold...";
        tickDelay = config.tickDelay();
        state = BankSellerState.CHECKING_OFFERS;
    }

    private List<ItemEx> getTradeableInventoryItems() {
        return InventoryAPI.search().collect().stream()
                .filter(item -> {
                    if (item == null)
                        return false;
                    String name = item.getName();
                    if (name == null)
                        return false;
                    if (item.getId() == 995)
                        return false; // Skip coins
                    if (excludedItems.contains(name.toLowerCase()))
                        return false;
                    int price = itemManager.getItemPrice(item.getId());
                    return price >= config.minPrice();
                })
                .collect(Collectors.toList());
    }

    public enum BankSellerState {
        IDLE,
        CHECKING_OFFERS,
        OPEN_BANK,
        WITHDRAW_ITEMS,
        OPEN_GE,
        SELL_ITEMS,
        COLLECT_GOLD,
        DONE
    }
}
