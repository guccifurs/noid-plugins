package com.tonic.plugins.oculusspectator;

import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;

public class OculusSpectatorOverlay extends Overlay
{
    private final OculusSpectatorPlugin plugin;
    private final OculusSpectatorConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public OculusSpectatorOverlay(OculusSpectatorPlugin plugin, OculusSpectatorConfig config)
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

        Player target = plugin.getSpectateTarget();
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
            .left("👁️ Spectating:")
            .right(targetName)
            .rightColor(Color.CYAN)
            .build());

        return panelComponent.render(graphics);
    }
}
