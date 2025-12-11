package com.tonic.plugins.humanequipper;

import com.tonic.data.wrappers.ActorEx;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;

public class HumanEquipperOverlay extends Overlay
{
    private final HumanEquipperPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public HumanEquipperOverlay(HumanEquipperPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        ActorEx<?> target = plugin.getCachedTarget();
        if (target == null)
        {
            return null;
        }

        panelComponent.getChildren().clear();

        String name = target.getName();
        if (name == null || name.isEmpty())
        {
            name = "Unknown";
        }

        panelComponent.getChildren().add(LineComponent.builder()
            .left("🎯 Target:")
            .right(name)
            .rightColor(Color.CYAN)
            .build());

        return panelComponent.render(graphics);
    }
}
