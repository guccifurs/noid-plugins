package com.tonic.plugins.dropparty;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropPartyOverlay extends Overlay {
    private static final int OUTLINE_ALPHA = 255;
    private static final int FILL_ALPHA = 50;

    // Green color for tracked player tile
    private static final Color TRACKED_PLAYER_TILE_COLOR = new Color(0, 255, 0);

    private final Client client;
    private final DropPartyPlugin plugin;

    @Inject
    public DropPartyOverlay(Client client, DropPartyPlugin plugin) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Show tracking status
        renderTrackingStatus(graphics);

        // Render green tile under tracked player
        renderTrackedPlayerTile(graphics);

        // Render trail tiles
        renderTrailTiles(graphics);

        return null;
    }

    private void renderTrackingStatus(Graphics2D graphics) {
        String playerName = plugin.getActivePlayerName();
        if (playerName == null || playerName.isEmpty()) {
            return;
        }

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        graphics.setFont(font);

        String statusText = "Tracking: " + playerName;
        String followText = "Follow: " + (plugin.isFollowActive() ? "ON" : "OFF");

        int x = 10;
        int y = 30;

        FontMetrics fm = graphics.getFontMetrics();

        int tileCount = plugin.getTimedTiles().size();
        String tileText = "Tiles: " + tileCount;

        int looted = plugin.getLootedItemCount();
        String lootText = "Looted: " + looted;

        int width = Math.max(fm.stringWidth(statusText),
                Math.max(fm.stringWidth(tileText),
                        Math.max(fm.stringWidth(lootText), fm.stringWidth(followText))))
                + 10;
        int lineHeight = fm.getHeight();
        int height = lineHeight * 4 + 8;

        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRect(x - 5, y - fm.getAscent() - 2, width, height);

        // Draw status
        graphics.setColor(TRACKED_PLAYER_TILE_COLOR);
        graphics.drawString(statusText, x, y);

        // Show follow status
        graphics.setColor(plugin.isFollowActive() ? Color.GREEN : Color.GRAY);
        graphics.drawString(followText, x, y + 15);

        // Show tile count
        graphics.setColor(Color.WHITE);
        graphics.drawString(tileText, x, y + 30);

        // Show total looted count
        graphics.drawString(lootText, x, y + 45);
    }

    /**
     * Render a green tile under the tracked player for easy visibility.
     */
    private void renderTrackedPlayerTile(Graphics2D graphics) {
        Player trackedPlayer = plugin.getTrackedPlayer();
        if (trackedPlayer == null) {
            return;
        }

        LocalPoint localPoint = trackedPlayer.getLocalLocation();
        if (localPoint == null) {
            return;
        }

        Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
        if (tilePoly == null) {
            return;
        }

        // Green tile with semi-transparent fill
        graphics.setColor(new Color(0, 255, 0, 80));
        graphics.fill(tilePoly);
        graphics.setColor(new Color(0, 255, 0, 200));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(tilePoly);
    }

    private void renderTrailTiles(Graphics2D graphics) {
        List<DropPartyPlugin.TimedTile> tiles = plugin.getTimedTiles();
        if (tiles == null || tiles.isEmpty()) {
            return;
        }

        int currentTick = plugin.getCurrentTick();
        Color singleTimerColor = plugin.getOverlayColor();
        if (singleTimerColor == null) {
            singleTimerColor = new Color(0, 150, 200); // Blue for single timer
        }
        Color multiTimerColor = new Color(255, 165, 0); // Orange for multiple timers

        int fontSize = plugin.getTextSize();
        int fontStyle = plugin.getFontStyle();

        // Count active timers per location and track the best tile to display
        Map<WorldPoint, Integer> timerCounts = new HashMap<>();
        Map<WorldPoint, DropPartyPlugin.TimedTile> bestTiles = new HashMap<>();

        for (DropPartyPlugin.TimedTile tile : tiles) {
            if (tile == null || tile.location == null || tile.isExpired(currentTick)) {
                continue;
            }

            // Count this timer
            timerCounts.merge(tile.location, 1, Integer::sum);

            // Track the best tile (lowest remaining time, non-grace preferred)
            DropPartyPlugin.TimedTile existing = bestTiles.get(tile.location);
            if (existing == null) {
                bestTiles.put(tile.location, tile);
            } else {
                boolean existingGrace = existing.isGracePeriod(currentTick);
                boolean currentGrace = tile.isGracePeriod(currentTick);

                if (existingGrace && !currentGrace) {
                    // Prefer non-grace tiles
                    bestTiles.put(tile.location, tile);
                } else if (!existingGrace && !currentGrace) {
                    // Both active - prefer lower remaining time
                    if (tile.getRemainingTicks(currentTick) < existing.getRemainingTicks(currentTick)) {
                        bestTiles.put(tile.location, tile);
                    }
                }
            }
        }

        for (Map.Entry<WorldPoint, DropPartyPlugin.TimedTile> entry : bestTiles.entrySet()) {
            WorldPoint location = entry.getKey();
            DropPartyPlugin.TimedTile timedTile = entry.getValue();

            if (timedTile == null)
                continue;

            LocalPoint localPoint = LocalPoint.fromWorld(client, location);
            if (localPoint == null) {
                continue;
            }

            Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, localPoint, 1);
            if (tilePoly == null) {
                continue;
            }

            // Determine color based on timer count
            int timerCount = timerCounts.getOrDefault(location, 1);
            Color tileColor = (timerCount >= 2) ? multiTimerColor : singleTimerColor;

            float alphaMultiplier;
            int remaining = timedTile.getRemainingTicks(currentTick);

            if (timedTile.isGracePeriod(currentTick)) {
                alphaMultiplier = 1.0f;
            } else {
                alphaMultiplier = (float) remaining / timedTile.durationTicks;
            }

            int adjustedOutlineAlpha = Math.max(50, (int) (OUTLINE_ALPHA * alphaMultiplier));
            int adjustedFillAlpha = Math.max(10, (int) (FILL_ALPHA * alphaMultiplier));

            Color outlineColor = new Color(
                    tileColor.getRed(),
                    tileColor.getGreen(),
                    tileColor.getBlue(),
                    adjustedOutlineAlpha);
            Color fillColor = new Color(
                    tileColor.getRed(),
                    tileColor.getGreen(),
                    tileColor.getBlue(),
                    adjustedFillAlpha);

            graphics.setColor(fillColor);
            graphics.fill(tilePoly);

            graphics.setColor(outlineColor);
            graphics.setStroke(new BasicStroke(2));
            graphics.draw(tilePoly);

            Point textLocation = centerPoint(tilePoly.getBounds());
            if (textLocation != null) {
                Font font = new Font(Font.SANS_SERIF, fontStyle, fontSize);
                graphics.setFont(font);

                // Show ticks remaining, with count indicator if multiple timers
                String timerText;
                if (timerCount >= 2) {
                    timerText = remaining > 0 ? remaining + "t (" + timerCount + ")" : "!";
                } else {
                    timerText = remaining > 0 ? remaining + "t" : "!";
                }

                graphics.setColor(Color.BLACK);
                graphics.drawString(timerText, textLocation.getX() + 1, textLocation.getY() + 1);
                graphics.setColor(Color.WHITE);
                graphics.drawString(timerText, textLocation.getX(), textLocation.getY());
            }
        }
    }

    private Point centerPoint(Rectangle rect) {
        int x = (int) (rect.getX() + rect.getWidth() / 2);
        int y = (int) (rect.getY() + rect.getHeight() / 2);
        return new Point(x, y);
    }
}
