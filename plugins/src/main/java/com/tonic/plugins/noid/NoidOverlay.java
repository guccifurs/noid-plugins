package com.tonic.plugins.noid;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

/**
 * Empty overlay - NoidPlugin uses only UI panel
 */
public class NoidOverlay extends Overlay {

    @Inject
    public NoidOverlay(Client client, NoidPlugin plugin) {
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Overlay disabled - UI panel only
        return null;
    }
}
