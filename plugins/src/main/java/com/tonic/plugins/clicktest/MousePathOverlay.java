package com.tonic.plugins.clicktest;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Overlay that draws a fading trail showing the mouse movement path.
 * Creates a visual representation of simulated mouse movements.
 */
public class MousePathOverlay extends Overlay {

    private final Client client;
    private final List<PathPoint> pathPoints = new ArrayList<>();

    // Settings
    private Color trailColor = new Color(65, 150, 255); // Blue
    private int maxPoints = 100;
    private long fadeTimeMs = 2000; // Points fade over 2 seconds
    private boolean enabled = true;

    public MousePathOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    /**
     * Add a point to the trail
     */
    public void addPoint(int x, int y) {
        if (!enabled)
            return;

        synchronized (pathPoints) {
            pathPoints.add(new PathPoint(x, y, System.currentTimeMillis()));

            // Limit max points
            while (pathPoints.size() > maxPoints) {
                pathPoints.remove(0);
            }
        }
    }

    /**
     * Clear all points
     */
    public void clear() {
        synchronized (pathPoints) {
            pathPoints.clear();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled)
            clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setTrailColor(Color color) {
        this.trailColor = color;
    }

    public void setFadeTime(long ms) {
        this.fadeTimeMs = ms;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!enabled || pathPoints.isEmpty()) {
            return null;
        }

        long now = System.currentTimeMillis();

        synchronized (pathPoints) {
            // Remove expired points
            Iterator<PathPoint> iter = pathPoints.iterator();
            while (iter.hasNext()) {
                PathPoint p = iter.next();
                if (now - p.timestamp > fadeTimeMs) {
                    iter.remove();
                }
            }

            if (pathPoints.isEmpty()) {
                return null;
            }

            // Enable anti-aliasing for smooth lines
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            PathPoint prevPoint = null;

            for (int i = 0; i < pathPoints.size(); i++) {
                PathPoint point = pathPoints.get(i);

                // Calculate fade based on age
                long age = now - point.timestamp;
                float alpha = 1.0f - (float) age / fadeTimeMs;
                alpha = Math.max(0, Math.min(1, alpha));

                // Also fade based on position in trail (newer = brighter)
                float positionFade = (float) i / pathPoints.size();
                alpha *= (0.3f + positionFade * 0.7f);

                Color pointColor = new Color(
                        trailColor.getRed(),
                        trailColor.getGreen(),
                        trailColor.getBlue(),
                        (int) (alpha * 255));

                // Draw connecting line
                if (prevPoint != null) {
                    graphics.setColor(pointColor);
                    graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    graphics.drawLine(prevPoint.x, prevPoint.y, point.x, point.y);
                }

                // Draw point dot
                int dotSize = 4 + (int) (positionFade * 4); // Larger dots at end
                graphics.setColor(pointColor);
                graphics.fill(new Ellipse2D.Double(
                        point.x - dotSize / 2.0,
                        point.y - dotSize / 2.0,
                        dotSize,
                        dotSize));

                prevPoint = point;
            }

            // Draw final position marker (larger, brighter)
            if (!pathPoints.isEmpty()) {
                PathPoint last = pathPoints.get(pathPoints.size() - 1);
                graphics.setColor(new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue(), 255));
                graphics.setStroke(new BasicStroke(2.0f));
                int ringSize = 12;
                graphics.drawOval(last.x - ringSize / 2, last.y - ringSize / 2, ringSize, ringSize);
            }
        }

        return null;
    }

    /**
     * Data class for a point in the path
     */
    private static class PathPoint {
        final int x;
        final int y;
        final long timestamp;

        PathPoint(int x, int y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }
    }
}
