package com.tonic.plugins.copilotsmiley;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;

@Singleton
public class CopilotSmileyOverlay extends Overlay {

    private static final String FLIPPING_WIDGET_HIGHLIGHT_CLASS = "com.flippingcopilot.ui.WidgetHighlightOverlay";
    private static final String SMILEY_RESOURCE_PATH = "/com/tonic/plugins/copilotsmiley/smiley.png";

    private final Client client;
    private final OverlayManager overlayManager;

    private BufferedImage smileyImage;
    private boolean imageLoadAttempted;

    @Inject
    public CopilotSmileyOverlay(Client client, OverlayManager overlayManager) {
        this.client = client;
        this.overlayManager = overlayManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client == null || overlayManager == null) {
            return null;
        }

        if (smileyImage == null) {
            loadImageOnce();
            if (smileyImage == null) {
                return null;
            }
        }

        Collection<Overlay> overlays = getAllOverlays();
        if (overlays == null || overlays.isEmpty()) {
            return null;
        }

        // Only show when the GE inventory widget is present (GE open)
        if (client.getWidget(467, 0) == null) {
            return null;
        }

        for (Overlay overlay : overlays) {
            if (overlay == this) {
                continue;
            }

            if (!overlay.getClass().getName().equals(FLIPPING_WIDGET_HIGHLIGHT_CLASS)) {
                continue;
            }

            Widget widget = extractWidget(overlay);
            if (widget == null) {
                continue;
            }

            Rectangle widgetBounds = widget.getBounds();
            if (widgetBounds == null) {
                continue;
            }

            // Filter to relevant widgets:
            // - Group 467 = GE inventory
            // - Group 149 = Normal inventory
            // - Group 465 = GE main interface (slots, buy/collect buttons)
            // - Group 162 = Chatbox / dialogue area (buy recommendations)
            int groupId = widget.getId() >>> 16;
            if (groupId != 467 && groupId != 149 && groupId != 465 && groupId != 162) {
                continue;
            }

            Rectangle relative = extractRelativeBounds(overlay);

            int x = widgetBounds.x;
            int y = widgetBounds.y;
            int width = widgetBounds.width;
            int height = widgetBounds.height;

            if (relative != null) {
                x += relative.x;
                y += relative.y;
                width = relative.width;
                height = relative.height;
            }

            // For chatbox overlays (group 162), tile smileys instead of stretching since
            // they're wide
            if (groupId == 162 && width > height * 2) {
                // Calculate tile size based on height to maintain aspect ratio
                int tileSize = height;
                int tilesNeeded = (int) Math.ceil((double) width / tileSize);

                for (int i = 0; i < tilesNeeded; i++) {
                    int tileX = x + (i * tileSize);
                    int tileWidth = Math.min(tileSize, x + width - tileX);
                    graphics.drawImage(smileyImage, tileX, y, tileWidth, height, null);
                }
            } else {
                // All other overlays: draw single smiley scaled to fit
                graphics.drawImage(smileyImage, x, y, width, height, null);
            }
        }

        return null;
    }

    private void loadImageOnce() {
        if (imageLoadAttempted) {
            return;
        }
        imageLoadAttempted = true;

        try (InputStream in = CopilotSmileyOverlay.class.getResourceAsStream(SMILEY_RESOURCE_PATH)) {
            if (in != null) {
                smileyImage = ImageIO.read(in);
            }
        } catch (IOException ignored) {
            // If loading fails, we simply don't draw the smiley.
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Overlay> getAllOverlays() {
        try {
            Field field = overlayManager.getClass().getDeclaredField("overlays");
            field.setAccessible(true);
            Object value = field.get(overlayManager);
            if (value instanceof Collection<?>) {
                return (Collection<Overlay>) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Widget extractWidget(Overlay overlay) {
        try {
            Field field = overlay.getClass().getDeclaredField("widget");
            field.setAccessible(true);
            Object value = field.get(overlay);
            if (value instanceof Widget) {
                return (Widget) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Rectangle extractRelativeBounds(Overlay overlay) {
        try {
            Field field = overlay.getClass().getDeclaredField("relativeBounds");
            field.setAccessible(true);
            Object value = field.get(overlay);
            if (value instanceof Rectangle) {
                return (Rectangle) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
