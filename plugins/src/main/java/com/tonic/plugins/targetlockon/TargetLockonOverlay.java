package com.tonic.plugins.targetlockon;

import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;

public class TargetLockonOverlay extends Overlay
{
    private final TargetLockonPlugin plugin;
    private final TargetLockonConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public TargetLockonOverlay(TargetLockonPlugin plugin, TargetLockonConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay())
        {
            return null;
        }

        Player target = plugin.getLockedTarget();
        if (target == null)
        {
            return null;
        }

        panelComponent.getChildren().clear();
        
        String targetName = target.getName();
        if (targetName == null)
        {
            targetName = "Unknown";
        }

        panelComponent.getChildren().add(LineComponent.builder()
            .left("🎯 Locked:")
            .right(targetName)
            .rightColor(Color.YELLOW)
            .build());

        return panelComponent.render(graphics);
    }
}
