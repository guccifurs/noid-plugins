package com.tonic.plugins.truedreamloot;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

public class TrueDreamLootOverlay extends Overlay {
        private final TrueDreamLootPlugin plugin;
        private final PanelComponent panelComponent = new PanelComponent();

        @Inject
        public TrueDreamLootOverlay(TrueDreamLootPlugin plugin) {
                this.plugin = plugin;
                setPosition(OverlayPosition.TOP_LEFT);
        }

        @Override
        public Dimension render(Graphics2D graphics) {
                panelComponent.getChildren().clear();

                panelComponent.getChildren().add(TitleComponent.builder()
                                .text("TrueDream Loot")
                                .color(Color.GREEN)
                                .build());

                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Status:")
                                .right(plugin.getStatusText())
                                .rightColor(plugin.getCenterTile() == null ? Color.RED : Color.GREEN)
                                .build());

                Duration runtime = Duration.between(plugin.getStartTime(), Instant.now());
                String timeStr = String.format("%02d:%02d:%02d",
                                runtime.toHours(),
                                runtime.toMinutesPart(),
                                runtime.toSecondsPart());

                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Time Running:")
                                .right(timeStr)
                                .build());

                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Items Looted:")
                                .right(Integer.toString(plugin.getItemsLooted()))
                                .build());

                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Total Value:")
                                .right(Long.toString(plugin.getTotalValueLooted()))
                                .build());

                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Times Banked:")
                                .right(Integer.toString(plugin.getTimesBanked()))
                                .build());

                return panelComponent.render(graphics);
        }
}
