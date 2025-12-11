package com.tonic.plugins.tickpreviewqueue;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.game.GameAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;
import java.util.*;
import java.util.List;

@PluginDescriptor(
    name = "Tick Preview Queue",
    description = "Queues inventory clicks in the last X ms of a tick and fires them on the next tick",
    tags = {"tick", "inventory", "queue"}
)
public class TickPreviewQueuePlugin extends Plugin
{
    private static final int TICK_LENGTH_MS = 600;

    @Inject
    private Client client;

    @Inject
    private TickPreviewQueueConfig config;

    @Inject
    private OverlayManager overlayManager;

    private TickPreviewOverlay overlay;

    private long lastTickStartMs = 0L;

    private final List<QueuedClick> queuedClicks = new ArrayList<>();

    // V2a: Prediction state
    private final Map<Integer, Integer> predictedInventory = new HashMap<>(); // slot → itemId
    private final Map<EquipmentSlot, Integer> predictedEquipment = new EnumMap<>(EquipmentSlot.class);

    private static class QueuedClick
    {
        private final int id; // MenuEntry identifier
        private final int opcode;
        private final int param0;
        private final int param1;
        private final int itemId;

        private QueuedClick(int id, int opcode, int param0, int param1, int itemId)
        {
            this.id = id;
            this.opcode = opcode;
            this.param0 = param0;
            this.param1 = param1;
            this.itemId = itemId;
        }
    }

    @Provides
    TickPreviewQueueConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TickPreviewQueueConfig.class);
    }

    @Override
    protected void startUp()
    {
        queuedClicks.clear();
        predictedInventory.clear();
        predictedEquipment.clear();
        lastTickStartMs = 0L;
        overlay = new TickPreviewOverlay();
        overlayManager.add(overlay);
        Logger.norm("Tick Preview Queue v2.0"); // Bumped version for V2a
    }

    @Override
    protected void shutDown()
    {
        queuedClicks.clear();
        predictedInventory.clear();
        predictedEquipment.clear();
        lastTickStartMs = 0L;
        if (overlay != null)
        {
            overlayManager.remove(overlay);
            overlay = null;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        lastTickStartMs = System.currentTimeMillis();

        if (!config.enabled())
        {
            queuedClicks.clear();
            predictedInventory.clear();
            predictedEquipment.clear();
            return;
        }

        if (queuedClicks.isEmpty())
        {
            return;
        }

        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            queuedClicks.clear();
            predictedInventory.clear();
            predictedEquipment.clear();
            return;
        }

        boolean ignoreSafety = config.ignoreMismatchSafety();

        List<QueuedClick> toProcess = new ArrayList<>(queuedClicks);
        queuedClicks.clear();
        predictedInventory.clear(); // Reset prediction after tick
        predictedEquipment.clear();

        for (QueuedClick click : toProcess)
        {
            if (!ignoreSafety && !isMatchingInventoryItem(click))
            {
                continue;
            }

            GameAPI.invokeMenuAction(
                click.id,
                click.opcode,
                click.param0,
                click.param1,
                click.itemId
            );
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!config.enabled())
        {
            return;
        }

        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (!isWithinPreviewWindow())
        {
            return;
        }

        if (event == null)
        {
            return;
        }

        int itemId = event.getItemId();
        if (itemId <= 0)
        {
            return;
        }

        int widgetId = event.getParam1();
        if (widgetId != InterfaceID.Inventory.ITEMS)
        {
            return;
        }

        MenuAction action = event.getMenuAction();
        if (action == null)
        {
            return;
        }

        // V2a: If this is an equip action, predict its effect
        if (isEquipAction(event))
        {
            predictEquipChange(event);
        }

        QueuedClick click = new QueuedClick(
            event.getId(),
            action.getId(),
            event.getParam0(),
            widgetId,
            itemId
        );

        queuedClicks.add(click);
        event.consume();
    }

    // V2a: Prediction helpers
    private boolean isEquipAction(MenuOptionClicked event)
    {
        String option = event.getMenuOption().toLowerCase();
        return option.contains("wear") || option.contains("wield") || option.contains("equip");
    }

    private void predictEquipChange(MenuOptionClicked event)
    {
        int slot = event.getParam0();
        int itemId = event.getItemId();
        
        // Simplified prediction: remove from inventory, add to equipment
        predictedInventory.put(slot, -1); // Mark slot as empty
        
        // For demo: assume it goes to WEAPON slot (real impl would map item→slot)
        predictedEquipment.put(EquipmentSlot.WEAPON, itemId); 
    }

    private boolean isWithinPreviewWindow()
    {
        if (lastTickStartMs == 0L)
        {
            return false;
        }

        int previewWindowMs = config.previewWindowMs();
        if (previewWindowMs <= 0)
        {
            return false;
        }

        if (previewWindowMs > TICK_LENGTH_MS)
        {
            previewWindowMs = TICK_LENGTH_MS;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastTickStartMs;
        long windowStart = TICK_LENGTH_MS - previewWindowMs;

        return elapsed >= windowStart && elapsed < TICK_LENGTH_MS;
    }

    private boolean isMatchingInventoryItem(QueuedClick click)
    {
        try
        {
            int slot = click.param0;
            ItemEx item = InventoryAPI.search().fromSlot(slot).first();
            if (item == null)
            {
                return false;
            }

            return item.getId() == click.itemId;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // V2a: Overlay to render predicted state during preview window
    private class TickPreviewOverlay extends Overlay
    {
        TickPreviewOverlay()
        {
            setPosition(OverlayPosition.DYNAMIC);
        }

        @Override
        public Dimension render(Graphics2D graphics)
        {
            if (!config.enabled() || !isWithinPreviewWindow())
            {
                return null;
            }

            if (!config.showVisualPreview())
            {
                return null;
            }

            Widget inventory = WidgetAPI.get(InterfaceID.Inventory.ITEMS);
            if (inventory != null && inventory.getChildren() != null && !predictedInventory.isEmpty())
            {
                Widget[] children = inventory.getChildren();

                graphics.setStroke(new BasicStroke(2f));

                for (Map.Entry<Integer, Integer> entry : predictedInventory.entrySet())
                {
                    int slot = entry.getKey();
                    if (slot < 0 || slot >= children.length)
                    {
                        continue;
                    }

                    Widget child = children[slot];
                    if (child == null)
                    {
                        continue;
                    }

                    Rectangle bounds = child.getBounds();
                    if (bounds == null)
                    {
                        continue;
                    }

                    graphics.setColor(new Color(0, 255, 0, 40));
                    graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

                    graphics.setColor(Color.GREEN);
                    graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                }
            }

            // Simplified: For demo, just log that we'd render here
            // Real implementation would draw predicted items
            return null;
        }
    }
}
