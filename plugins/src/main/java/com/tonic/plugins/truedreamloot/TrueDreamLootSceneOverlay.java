package com.tonic.plugins.truedreamloot;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;

public class TrueDreamLootSceneOverlay extends Overlay {
    private final Client client;
    private final TrueDreamLootPlugin plugin;
    private final TrueDreamLootConfig config;

    @Inject
    public TrueDreamLootSceneOverlay(Client client, TrueDreamLootPlugin plugin, TrueDreamLootConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showRadiusOverlay())
            return null;
        if (client == null)
            return null;

        WorldPoint center = plugin.getCenterTile();
        if (center == null) {
            // Render a warning or instruction
            return null;
        }

        int radius = config.maxRadius();
        net.runelite.api.WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
            return null;
        net.runelite.api.Scene scene = worldView.getScene();
        if (scene == null)
            return null;

        net.runelite.api.Tile[][][] tiles = scene.getTiles();
        int plane = worldView.getPlane();
        if (tiles == null || plane < 0 || plane >= tiles.length)
            return null;
        net.runelite.api.Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null)
            return null;

        graphics.setColor(new Color(0, 255, 0, 50));
        for (int x = 0; x < planeTiles.length; x++) {
            for (int y = 0; y < planeTiles[x].length; y++) {
                net.runelite.api.Tile tile = planeTiles[x][y];
                if (tile == null)
                    continue;

                WorldPoint wp = tile.getWorldLocation();
                if (wp == null)
                    continue;

                if (wp.distanceTo(center) <= radius) {
                    LocalPoint lp = tile.getLocalLocation();
                    if (lp == null)
                        continue;

                    Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                    if (poly != null) {
                        graphics.fill(poly);
                        graphics.setColor(new Color(0, 255, 0, 100)); // Border
                        graphics.draw(poly);
                        graphics.setColor(new Color(0, 255, 0, 50)); // Reset fill color
                    }
                }
            }
        }

        // We intentionally do not log here anymore to avoid spamming the console
        // every frame; this overlay is called very frequently by the client.
        return null;
    }
}
