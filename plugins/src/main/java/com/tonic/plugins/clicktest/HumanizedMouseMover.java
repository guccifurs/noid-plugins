package com.tonic.plugins.clicktest;

import net.runelite.api.Client;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates humanized mouse movement paths and dispatches them to the canvas.
 * Creates smooth, curved paths with varying speeds like a real human would
 * move.
 * Tracks position via VirtualMousePosition to avoid issues with blocked input.
 */
public class HumanizedMouseMover {

    private final Client client;
    private final java.util.function.BiConsumer<String, String> logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    // Virtual position tracking - this is where the "simulated mouse" is
    private final VirtualMousePosition virtualPosition = new VirtualMousePosition();

    private volatile boolean moving = false;
    private volatile boolean cancelled = false;

    // Movement settings - tuned for realistic human movement
    private int minDelay = 8; // min ms between steps (fast parts)
    private int maxDelay = 25; // max ms between steps (slow parts)
    private double curvature = 0.25; // how much the path curves (0 = straight, 1 = very curved)
    private int baseSteps = 20; // base number of steps (adjusted by distance)

    // Special marker to identify our events (so blocker can let them through)
    public static final String SIMULATED_EVENT_MARKER = "SIMULATED_CLICK_TEST";

    public HumanizedMouseMover(Client client, java.util.function.BiConsumer<String, String> logger) {
        this.client = client;
        this.logger = logger;
    }

    public boolean isMoving() {
        return moving;
    }

    public void cancel() {
        cancelled = true;
    }

    public VirtualMousePosition getVirtualPosition() {
        return virtualPosition;
    }

    /**
     * Move from current virtual position to target, then click
     */
    public void moveAndClick(int targetX, int targetY, int button) {
        if (moving) {
            log("WARN", "Already moving, ignoring request");
            return;
        }

        Canvas canvas = client.getCanvas();
        if (canvas == null)
            return;

        // Use virtual position as start (not client position which may be -1,-1)
        final int startX = virtualPosition.getX();
        final int startY = virtualPosition.getY();

        executor.submit(() -> {
            moving = true;
            cancelled = false;

            try {
                double distance = Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2));

                // Use VitaLite TrajectoryGenerator for recorded human paths
                com.tonic.services.mouserecorder.trajectory.TrajectoryGenerator generator = com.tonic.services.mouserecorder.trajectory.TrajectoryService
                        .createGenerator();

                int trajectoryCount = com.tonic.services.mouserecorder.trajectory.TrajectoryService
                        .getDatabase().getTrajectoryCount();

                com.tonic.services.mouserecorder.MouseMovementSequence sequence = generator.generate(startX, startY,
                        targetX, targetY);

                java.util.List<com.tonic.services.mouserecorder.MouseDataPoint> points = sequence.getPoints();

                log("MOVE", "Trajectory: (" + startX + "," + startY + ") → (" + targetX + "," + targetY +
                        ") dist=" + (int) distance + " pts=" + points.size() + " db=" + trajectoryCount);

                // Move along trajectory path
                for (int i = 0; i < points.size() && !cancelled; i++) {
                    com.tonic.services.mouserecorder.MouseDataPoint point = points.get(i);

                    // Update virtual position
                    virtualPosition.setPosition(point.getX(), point.getY());

                    // Dispatch move event
                    dispatchMouseMove(point.getX(), point.getY());

                    // Use timing from recorded trajectory
                    if (i > 0) {
                        long delay = point.getTimestampMillis() - points.get(i - 1).getTimestampMillis();
                        if (delay > 0 && delay < 500) {
                            Thread.sleep(delay);
                        }
                    }
                }

                if (!cancelled) {
                    // Final position
                    virtualPosition.setPosition(targetX, targetY);
                    dispatchMouseMove(targetX, targetY);
                    Thread.sleep(20 + random.nextInt(30));

                    // Click!
                    dispatchClick(targetX, targetY, button);
                    log("CLICK", "Human click at (" + targetX + ", " + targetY + ")");
                }

            } catch (InterruptedException e) {
                log("WARN", "Movement interrupted");
            } finally {
                moving = false;
            }
        });
    }

    /**
     * Generate a curved, humanized path between two points
     */
    private List<Point> generateHumanizedPath(int x1, int y1, int x2, int y2) {
        List<Point> path = new ArrayList<>();

        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        // More steps for longer distances
        int numSteps = Math.max(8, Math.min(40, (int) (distance / 10)));

        // Calculate control point for bezier curve (adds curvature)
        double perpX = -(y2 - y1);
        double perpY = (x2 - x1);
        double len = Math.sqrt(perpX * perpX + perpY * perpY);
        if (len > 0) {
            perpX /= len;
            perpY /= len;
        }

        // Random offset perpendicular to the line
        double offset = (random.nextDouble() - 0.5) * 2 * distance * curvature;
        double ctrlX = (x1 + x2) / 2.0 + perpX * offset;
        double ctrlY = (y1 + y2) / 2.0 + perpY * offset;

        for (int i = 1; i <= numSteps; i++) {
            double t = (double) i / numSteps;

            // Quadratic bezier interpolation
            double u = 1 - t;
            double x = u * u * x1 + 2 * u * t * ctrlX + t * t * x2;
            double y = u * u * y1 + 2 * u * t * ctrlY + t * t * y2;

            // Add small random jitter (more at middle of path)
            double jitterFactor = 1.0 - 2.0 * Math.abs(t - 0.5);
            x += (random.nextDouble() - 0.5) * 2 * jitterFactor;
            y += (random.nextDouble() - 0.5) * 2 * jitterFactor;

            path.add(new Point((int) x, (int) y));
        }

        return path;
    }

    /**
     * Calculate delay at a given progress point
     * Uses ease-in-out: faster in middle, slower at start/end
     * (acceleration/deceleration)
     */
    private int calculateDelay(double progress, double distance) {
        // Ease-in-out curve: slow → fast → slow
        // cos curve gives smooth acceleration and deceleration
        double eased = (1 - Math.cos(progress * Math.PI)) / 2;

        // Inverted: we want short delays in the middle (fast), long at ends (slow)
        double speedFactor = 1 - Math.sin(progress * Math.PI); // 1 at ends, 0 in middle

        // Scale by distance - longer distances should have slightly faster middle speed
        double distanceScale = Math.min(1.5, distance / 200.0);

        int delay = minDelay + (int) ((maxDelay - minDelay) * speedFactor * distanceScale);

        // Add small randomness
        delay += random.nextInt(6) - 3;

        return Math.max(5, delay);
    }

    private void dispatchMouseMove(int x, int y) {
        Canvas canvas = client.getCanvas();
        if (canvas == null)
            return;

        long now = System.currentTimeMillis();
        MouseEvent move = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
                now, 0, x, y, 0, false);

        // Mark this event as simulated so blocker doesn't consume it
        move.setSource(new SimulatedEventSource(canvas, SIMULATED_EVENT_MARKER));

        canvas.dispatchEvent(move);
    }

    private void dispatchClick(int x, int y, int button) {
        Canvas canvas = client.getCanvas();
        if (canvas == null)
            return;

        long now = System.currentTimeMillis();

        // Press
        MouseEvent press = new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED,
                now, 0, x, y, 1, false, button);
        press.setSource(new SimulatedEventSource(canvas, SIMULATED_EVENT_MARKER));
        canvas.dispatchEvent(press);

        // Small delay
        try {
            Thread.sleep(30 + random.nextInt(40));
        } catch (InterruptedException ignored) {
        }

        // Release
        MouseEvent release = new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED,
                now + 40, 0, x, y, 1, false, button);
        release.setSource(new SimulatedEventSource(canvas, SIMULATED_EVENT_MARKER));
        canvas.dispatchEvent(release);

        // Click
        MouseEvent click = new MouseEvent(canvas, MouseEvent.MOUSE_CLICKED,
                now + 40, 0, x, y, 1, false, button);
        click.setSource(new SimulatedEventSource(canvas, SIMULATED_EVENT_MARKER));
        canvas.dispatchEvent(click);
    }

    private void log(String type, String message) {
        if (logger != null) {
            logger.accept(type, message);
        }
    }

    // Setters for tuning
    public void setMinDelay(int ms) {
        this.minDelay = ms;
    }

    public void setMaxDelay(int ms) {
        this.maxDelay = ms;
    }

    public void setCurvature(double c) {
        this.curvature = c;
    }

    public void setBaseSteps(int s) {
        this.baseSteps = s;
    }

    public void shutdown() {
        cancelled = true;
        executor.shutdownNow();
    }

    /**
     * Wrapper class to mark events as simulated
     */
    public static class SimulatedEventSource {
        public final Object originalSource;
        public final String marker;

        public SimulatedEventSource(Object source, String marker) {
            this.originalSource = source;
            this.marker = marker;
        }
    }
}
