package com.tonic.plugins.bankseller;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;

public class BankSellerOverlay extends Overlay {

    private final BankSellerPlugin plugin;
    private final BankSellerConfig config;
    private final PanelComponent panel = new PanelComponent();

    @Inject
    public BankSellerOverlay(BankSellerPlugin plugin, BankSellerConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled() && plugin.getState() == BankSellerPlugin.BankSellerState.IDLE) {
            return null;
        }

        panel.getChildren().clear();

        panel.getChildren().add(TitleComponent.builder()
                .text("Bank Seller")
                .color(new Color(0, 255, 0))
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right(plugin.getStatusMessage())
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("State:")
                .right(plugin.getState().name())
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Items Sold:")
                .right(String.valueOf(plugin.getItemsSold()))
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Gold Earned:")
                .right(NumberFormat.getInstance().format(plugin.getGoldEarned()) + " gp")
                .build());

        return panel.render(graphics);
    }
}
