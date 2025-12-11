package com.tonic.plugins.gearswapper.humanized;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.MouseMovementSequence;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGenerator;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import net.runelite.api.Client;
import net.runelite.api.Actor;
import net.runelite.api.widgets.Widget;
import java.awt.Shape;
import java.awt.Polygon;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

/**
 * Helper for humanized mouse movement before widget interactions.
 */
public class HumanizedMouseHelper {

    private static final Random random = new Random();
    private static volatile int currentX = 0;
    private static volatile int currentY = 0;

    public interface PathPointListener {
        void onPoint(int x, int y);
    }

    private static PathPointListener pathListener;

    public static void setPathListener(PathPointListener listener) {
        pathListener = listener;
    }

    public static void setCurrentPosition(int x, int y) {
        currentX = x;
        currentY = y;
    }

    public static Point getRandomPointInBounds(Rectangle bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
            return null;

        double sigma = 0.20; // Standard deviation relative to size (20%)

        // Gaussian distribution centered at 0.5 (center of bounds)
        double gX = 0.5 + (random.nextGaussian() * sigma);
        double gY = 0.5 + (random.nextGaussian() * sigma);

        // Clamping to [0.05, 0.95] to avoid extreme edges
        gX = Math.max(0.05, Math.min(0.95, gX));
        gY = Math.max(0.05, Math.min(0.95, gY));

        int x = bounds.x + (int) (bounds.width * gX);
        int y = bounds.y + (int) (bounds.height * gY);
        return new Point(x, y);
    }

    public static Point getRandomPointInShape(Shape shape) {
        if (shape == null)
            return null;
        Rectangle bounds = shape.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0)
            return null;

        // Rejection sampling - try 30 times to find a point inside
        for (int i = 0; i < 30; i++) {
            Point p = getRandomPointInBounds(bounds);
            if (p != null && shape.contains(p)) {
                return p;
            }
        }

        // Fallback to center if sampling fails (unlikely for reasonable shapes)
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    public static Rectangle getWidgetBounds(int interfaceId) {
        return Static.invoke(() -> {
            Widget widget = WidgetAPI.get(interfaceId);
            if (widget == null || widget.isHidden())
                return null;
            return widget.getBounds();
        });
    }

    public static Point getWidgetCenter(int interfaceId) {
        Rectangle bounds = getWidgetBounds(interfaceId);
        if (bounds == null)
            return null;
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    public static Point getActorClickPoint(Actor actor) {
        if (actor == null)
            return null;
        // Use Static.invoke to ensure thread safety if called from unpredictable
        // context
        // But getClickbox() should be fast.
        try {
            Shape clickbox = actor.getConvexHull();
            if (clickbox != null) {
                if (clickbox != null) {
                    return getRandomPointInShape(clickbox);
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return null;
    }

    public static Point getTileClickPoint(Client client, WorldPoint worldPoint) {
        if (client == null || worldPoint == null)
            return null;

        return Static.invoke(() -> {
            try {
                if (client.getLocalPlayer() == null)
                    return null;
                LocalPoint local = LocalPoint.fromWorld(client, worldPoint);
                if (local == null)
                    return null;

                Polygon poly = Perspective.getCanvasTilePoly(client, local);
                if (poly != null) {
                    if (poly != null) {
                        return getRandomPointInShape(poly);
                    }
                }
            } catch (Exception e) {
                // Fallback
            }
            return null;
        });
    }

    public static boolean moveToPosition(Client client, int targetX, int targetY, int maxTimeMs) {
        if (client == null)
            return false;
        Canvas canvas = client.getCanvas();
        if (canvas == null)
            return false;

        try {
            TrajectoryGenerator generator = TrajectoryService.createGenerator();
            MouseMovementSequence sequence = generator.generate(currentX, currentY, targetX, targetY);
            List<MouseDataPoint> points = sequence.getPoints();

            if (points.isEmpty()) {
                currentX = targetX;
                currentY = targetY;
                dispatchMouseMove(canvas, targetX, targetY);
                return true;
            }

            long totalDuration = 0;
            for (int i = 1; i < points.size(); i++) {
                totalDuration += points.get(i).getTimestampMillis() - points.get(i - 1).getTimestampMillis();
            }

            double timeScale = 1.0;
            if (totalDuration > 0 && totalDuration > maxTimeMs) {
                timeScale = (double) maxTimeMs / totalDuration;
            }

            for (int i = 0; i < points.size(); i++) {
                MouseDataPoint point = points.get(i);
                currentX = point.getX();
                currentY = point.getY();
                dispatchMouseMove(canvas, point.getX(), point.getY());

                if (pathListener != null)
                    pathListener.onPoint(point.getX(), point.getY());

                if (i > 0 && i < points.size() - 1) {
                    long delay = points.get(i).getTimestampMillis() - points.get(i - 1).getTimestampMillis();
                    int scaledDelay = (int) (delay * timeScale);
                    if (scaledDelay > 0 && scaledDelay < 200)
                        Thread.sleep(scaledDelay);
                }
            }

            currentX = targetX;
            currentY = targetY;
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Logger.warn("[Humanized] Movement error: " + e.getMessage());
            return false;
        }
    }

    public static boolean moveToWidget(Client client, int interfaceId, int maxTimeMs) {
        Rectangle bounds = getWidgetBounds(interfaceId);
        if (bounds == null)
            return false;
        Point target = getRandomPointInBounds(bounds);
        if (target == null)
            return false;
        return moveToPosition(client, target.x, target.y, maxTimeMs);
    }

    private static void dispatchMouseMove(Canvas canvas, int x, int y) {
        try {
            MouseEvent move = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0,
                    false);
            canvas.dispatchEvent(move);
        } catch (Exception e) {
        }
    }

    public static void dispatchClick(Canvas canvas, int x, int y) {
        try {
            long now = System.currentTimeMillis();
            canvas.dispatchEvent(
                    new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, now, 0, x, y, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(
                    new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, now + 50, 0, x, y, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(
                    new MouseEvent(canvas, MouseEvent.MOUSE_CLICKED, now + 50, 0, x, y, 1, false, MouseEvent.BUTTON1));
        } catch (Exception e) {
        }
    }
}
