package com.tonic.plugins.gearswapper.humanized;

import com.tonic.Logger;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.MouseMovementSequence;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGenerator;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import net.runelite.api.Client;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Executes a queue of humanized actions within a time constraint.
 */
public class HumanizedActionQueue {

    private final Client client;
    private final List<HumanizedAction> pendingActions = new CopyOnWriteArrayList<>();
    private final java.util.concurrent.atomic.AtomicBoolean executing = new java.util.concurrent.atomic.AtomicBoolean(
            false);
    private volatile int currentX = 0;
    private volatile int currentY = 0;

    public interface PathPointListener {
        void onPoint(int x, int y);
    }

    private PathPointListener pathListener;

    public HumanizedActionQueue(Client client) {
        this.client = client;
    }

    public void setPathListener(PathPointListener listener) {
        this.pathListener = listener;
    }

    public void queue(int targetX, int targetY, Runnable action, String description) {
        synchronized (pendingActions) {
            pendingActions.add(new HumanizedAction(targetX, targetY, action, description));
        }
    }

    public void queue(Point target, Runnable action, String description) {
        synchronized (pendingActions) {
            if (target != null) {
                pendingActions.add(new HumanizedAction(target, action, description));
            } else {
                pendingActions.add(new HumanizedAction(action, description));
            }
        }
    }

    public int size() {
        return pendingActions.size();
    }

    public void clear() {
        pendingActions.clear();
    }

    public boolean isExecuting() {
        return executing.get();
    }

    public void setCurrentPosition(int x, int y) {
        this.currentX = x;
        this.currentY = y;
    }

    public void executeAll(int availableMs, boolean returnToMouse, int realMouseX, int realMouseY) {
        if (pendingActions.isEmpty())
            return;

        // Ensure only one execution thread runs at a time
        if (!executing.compareAndSet(false, true)) {
            Logger.norm(
                    "[HumanizedQueue] [DEBUG] WE ARE BUSY IGNORING REQUEST (tasks should be picked up by active runner)");
            return;
        }

        // CRITICAL: Calculate absolute deadline at START to guarantee 1-tick completion
        final long startTime = System.currentTimeMillis();
        final long deadline = startTime + availableMs;

        Logger.norm("[HumanizedQueue] [DEBUG] Starting execution loop. Items in queue: " + pendingActions.size()
                + ", deadline in " + availableMs + "ms");

        try {
            // Keep processing until queue is empty (handles items added during execution)
            while (true) {
                List<HumanizedAction> actions = null;
                synchronized (pendingActions) {
                    if (!pendingActions.isEmpty()) {
                        actions = new ArrayList<>(pendingActions);
                        pendingActions.clear();
                    }
                }

                if (actions != null) {
                    Logger.norm("[HumanizedQueue] [DEBUG] Drained batch of " + actions.size() + " actions.");
                    TrajectoryGenerator generator = TrajectoryService.createGenerator();

                    for (int i = 0; i < actions.size(); i++) {
                        HumanizedAction action = actions.get(i);

                        // Calculate remaining time and time per remaining action
                        long now = System.currentTimeMillis();
                        long remainingMs = deadline - now;
                        int remainingActions = actions.size() - i;
                        int msPerAction = remainingActions > 0 ? (int) (remainingMs / remainingActions) : 0;

                        // If running critically low on time (<50ms remaining), execute all remaining
                        // actions instantly
                        if (remainingMs < 50) {
                            Logger.norm("[HumanizedQueue] [DEBUG] CRITICAL: Only " + remainingMs
                                    + "ms left! Executing remaining " + remainingActions + " actions instantly.");
                            for (int j = i; j < actions.size(); j++) {
                                actions.get(j).execute();
                            }
                            break; // Exit action loop
                        }

                        // Move mouse if we have enough time
                        if (action.hasTargetPosition() && msPerAction >= 30) {
                            Point target = action.getTargetPosition();
                            moveToPosition(generator, target.x, target.y, msPerAction);
                        } else if (action.hasTargetPosition()) {
                            // Not enough time for trajectory, teleport instead
                            Point target = action.getTargetPosition();
                            currentX = target.x;
                            currentY = target.y;
                            dispatchMouseMove(client.getCanvas(), target.x, target.y);
                            if (pathListener != null)
                                pathListener.onPoint(target.x, target.y);
                        }

                        action.execute();

                        // Tiny delay between actions only if we have time
                        if (i < actions.size() - 1 && msPerAction > 50) {
                            Thread.sleep(Math.min(10, msPerAction / 10));
                        }
                    }
                } else {
                    // Queue is empty.
                    // Perform return to mouse if requested
                    if (returnToMouse) {
                        try {
                            Logger.norm("[HumanizedQueue] [DEBUG] Queue empty, returning mouse.");
                            // Fetch CURRENT mouse position to allow user to influence end position
                            Point mousePos = java.awt.MouseInfo.getPointerInfo().getLocation();
                            java.awt.Point canvasLoc = client.getCanvas().getLocationOnScreen();
                            int destX = mousePos.x - canvasLoc.x;
                            int destY = mousePos.y - canvasLoc.y;

                            moveToPosition(TrajectoryService.createGenerator(), destX, destY, 100);
                        } catch (Exception e) {
                            // Fallback to original start position if error
                            if (realMouseX >= 0 && realMouseY >= 0) {
                                moveToPosition(TrajectoryService.createGenerator(), realMouseX, realMouseY, 100);
                            }
                        }
                    }

                    // CRITICAL: Check queue one last time under lock before exiting
                    // If items appeared during returnToMouse, we must continue!
                    synchronized (pendingActions) {
                        if (pendingActions.isEmpty()) {
                            Logger.norm("[HumanizedQueue] [DEBUG] Execution finished, queue empty.");
                            executing.set(false);
                            return;
                        } else {
                            Logger.norm("[HumanizedQueue] [DEBUG] New items found after return, continuing loop.");
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Logger.error("[HumanizedQueue] Error: " + e.getMessage());
        } finally {
            executing.set(false);
            Logger.norm("[HumanizedQueue] [DEBUG] Execution thread exiting.");
        }
    }

    private void moveToPosition(TrajectoryGenerator generator, int targetX, int targetY, int maxTimeMs)
            throws InterruptedException {
        Canvas canvas = client.getCanvas();
        if (canvas == null)
            return;

        // If timing is very tight (< 30ms), skip trajectory and teleport
        if (maxTimeMs < 30) {
            currentX = targetX;
            currentY = targetY;
            dispatchMouseMove(canvas, targetX, targetY);
            if (pathListener != null)
                pathListener.onPoint(targetX, targetY);
            return;
        }

        MouseMovementSequence sequence = generator.generate(currentX, currentY, targetX, targetY);
        List<MouseDataPoint> points = sequence.getPoints();

        if (points.isEmpty()) {
            currentX = targetX;
            currentY = targetY;
            dispatchMouseMove(canvas, targetX, targetY);
            return;
        }

        long totalDuration = 0;
        for (int i = 1; i < points.size(); i++) {
            totalDuration += points.get(i).getTimestampMillis() - points.get(i - 1).getTimestampMillis();
        }

        // AGGRESSIVE SCALING: Always scale to fit budget, even if trajectory is fast
        // This ensures we use all available time efficiently
        double timeScale = totalDuration > 0 ? (double) maxTimeMs / totalDuration : 1.0;
        // Cap scaling to prevent super slow movements (max 2x slower)
        if (timeScale > 2.0)
            timeScale = 2.0;
        // Cap minimum to prevent impossibly fast movements
        if (timeScale < 0.1)
            timeScale = 0.1;

        for (int i = 0; i < points.size(); i++) {
            MouseDataPoint point = points.get(i);
            currentX = point.getX();
            currentY = point.getY();
            dispatchMouseMove(canvas, point.getX(), point.getY());

            if (pathListener != null)
                pathListener.onPoint(point.getX(), point.getY());

            if (i > 0) {
                long delay = points.get(i).getTimestampMillis() - points.get(i - 1).getTimestampMillis();
                int scaledDelay = (int) (delay * timeScale);
                // Minimum 1ms, max 200ms per step to prevent freezing
                if (scaledDelay > 0 && scaledDelay < 200)
                    Thread.sleep(scaledDelay);
                else if (scaledDelay >= 200)
                    Thread.sleep(200);
            }
        }
        currentX = targetX;
        currentY = targetY;
    }

    private void dispatchMouseMove(Canvas canvas, int x, int y) {
        try {
            MouseEvent move = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0,
                    false);
            canvas.dispatchEvent(move);
        } catch (Exception e) {
        }
    }
}
