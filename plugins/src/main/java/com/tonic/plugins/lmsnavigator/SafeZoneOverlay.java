package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

/**
 * Overlay to highlight the safe zone box (fog markers area) in cyan.
 */
public class SafeZoneOverlay extends Overlay
{
    private final Client client;
    private static final int FOG_MARKER_ID = 34905;

    public SafeZoneOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(1);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!LmsState.isSafeZoneBoxEnforced())
        {
            return null;
        }

        if (client == null)
        {
            return null;
        }

        try
        {
            WorldView worldView = client.getTopLevelWorldView();
            if (worldView == null)
            {
                return null;
            }

            Scene scene = worldView.getScene();
            if (scene == null)
            {
                return null;
            }

            int plane = worldView.getPlane();
            Tile[][][] tiles = scene.getTiles();
            if (tiles == null || plane < 0 || plane >= tiles.length)
            {
                return null;
            }

            Tile[][] planeTiles = tiles[plane];
            if (planeTiles == null)
            {
                return null;
            }

            graphics.setColor(new Color(0, 255, 255, 150)); // Cyan border
            graphics.setStroke(new BasicStroke(2));

            boolean anyFogMarker = false;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (int x = 0; x < planeTiles.length; x++)
            {
                Tile[] row = planeTiles[x];
                if (row == null)
                {
                    continue;
                }

                for (int y = 0; y < row.length; y++)
                {
                    Tile tile = row[y];
                    if (tile == null)
                    {
                        continue;
                    }

                    GameObject[] gameObjects = tile.getGameObjects();
                    if (gameObjects == null)
                    {
                        continue;
                    }

                    boolean hasFogMarker = false;
                    for (GameObject obj : gameObjects)
                    {
                        if (obj != null && obj.getId() == FOG_MARKER_ID)
                        {
                            hasFogMarker = true;
                            break;
                        }
                    }

                    if (!hasFogMarker)
                    {
                        continue;
                    }

                    anyFogMarker = true;

                    if (x < minX)
                    {
                        minX = x;
                    }
                    if (x > maxX)
                    {
                        maxX = x;
                    }
                    if (y < minY)
                    {
                        minY = y;
                    }
                    if (y > maxY)
                    {
                        maxY = y;
                    }
                }
            }

            if (!anyFogMarker)
            {
                Logger.norm("[SafeZoneOverlay] No fog markers (" + FOG_MARKER_ID + ") found on plane " + worldView.getPlane());
                return null;
            }

            if (minX > maxX || minY > maxY)
            {
                return null;
            }

            graphics.setColor(new Color(0, 255, 0, 60));

            for (int x = minX; x <= maxX && x < planeTiles.length; x++)
            {
                if (x < 0)
                {
                    continue;
                }

                Tile[] row = planeTiles[x];
                if (row == null)
                {
                    continue;
                }

                int rowMaxY = Math.min(maxY, row.length - 1);
                for (int y = minY; y <= rowMaxY; y++)
                {
                    if (y < 0)
                    {
                        continue;
                    }

                    Tile tile = row[y];
                    if (tile == null)
                    {
                        continue;
                    }

                    LocalPoint local = tile.getLocalLocation();
                    if (local == null)
                    {
                        continue;
                    }

                    Polygon poly = getTilePoly(client, local, plane);
                    if (poly != null)
                    {
                        graphics.fill(poly);
                    }
                }
            }

            graphics.setColor(new Color(0, 255, 255, 150));

            for (int x = 0; x < planeTiles.length; x++)
            {
                Tile[] row = planeTiles[x];
                if (row == null)
                {
                    continue;
                }

                for (int y = 0; y < row.length; y++)
                {
                    Tile tile = row[y];
                    if (tile == null)
                    {
                        continue;
                    }

                    GameObject[] gameObjects = tile.getGameObjects();
                    if (gameObjects == null)
                    {
                        continue;
                    }

                    boolean hasFogMarker = false;
                    for (GameObject obj : gameObjects)
                    {
                        if (obj != null && obj.getId() == FOG_MARKER_ID)
                        {
                            hasFogMarker = true;
                            break;
                        }
                    }

                    if (!hasFogMarker)
                    {
                        continue;
                    }

                    LocalPoint local = tile.getLocalLocation();
                    if (local == null)
                    {
                        continue;
                    }

                    Polygon poly = getTilePoly(client, local, plane);
                    if (poly != null)
                    {
                        graphics.draw(poly);
                    }
                }
            }
        }
        catch (Exception e)
        {
            Logger.error(e);
            Logger.error("[SafeZoneOverlay] Error during render: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    private Polygon getTilePoly(Client client, LocalPoint localPoint, int plane)
    {
        if (client == null || localPoint == null)
        {
            return null;
        }

        // Let RuneLite handle tile polygon computation and bounds.
        return Perspective.getCanvasTilePoly(client, localPoint);
    }
}
