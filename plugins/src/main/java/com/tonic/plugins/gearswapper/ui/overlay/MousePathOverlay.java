package com.tonic.plugins.gearswapper.ui.overlay;

import com.tonic.plugins.gearswapper.GearSwapperConfig;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MousePathOverlay extends Overlay {

    private final GearSwapperConfig config;

    private static class TimePoint {
        final Point point;
        final long time;

        TimePoint(Point point, long time) {
            this.point = point;
            this.time = time;
        }
    }

    private final ConcurrentLinkedDeque<TimePoint> points = new ConcurrentLinkedDeque<>();
    private static final long TRAIL_DURATION_MS = 1000; // Trail lasts 1 second

    @Inject
    public MousePathOverlay(Client client, GearSwapperConfig config) {
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    public void addPoint(Point point) {
        points.add(new TimePoint(point, System.currentTimeMillis()));
    }

    public void clear() {
        points.clear();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableHumanizedMouse() || !config.showMouseTrail()) {
            if (!points.isEmpty())
                clear();
            return null;
        }

        long now = System.currentTimeMillis();

        // Prune old points
        Iterator<TimePoint> it = points.iterator();
        while (it.hasNext()) {
            TimePoint tp = it.next();
            if (now - tp.time > TRAIL_DURATION_MS) {
                it.remove();
            }
        }

        if (points.isEmpty())
            return null;

        graphics.setStroke(new BasicStroke(2));

        // Re-iterate for drawing
        Iterator<TimePoint> drawIt = points.iterator();
        Point prev = null;

        while (drawIt.hasNext()) {
            TimePoint tp = drawIt.next();
            Point current = tp.point;

            if (prev != null) {
                float age = (now - tp.time);
                float alpha = 1.0f - (age / TRAIL_DURATION_MS);
                if (alpha < 0)
                    alpha = 0;
                if (alpha > 1)
                    alpha = 1;

                graphics.setColor(new Color(0, 191, 255, (int) (255 * alpha)));
                graphics.drawLine(prev.x, prev.y, current.x, current.y);
            }
            prev = current;
        }

        // Draw cursor at last point
        if (prev != null) {
            graphics.setColor(new Color(0, 255, 255));
            graphics.fillOval(prev.x - 3, prev.y - 3, 6, 6);
        }

        return null;
    }
}
