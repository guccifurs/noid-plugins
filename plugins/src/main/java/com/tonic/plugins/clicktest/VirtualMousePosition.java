package com.tonic.plugins.clicktest;

import java.awt.*;
import java.util.function.Consumer;

/**
 * Tracks a virtual mouse position that is controlled by simulated events.
 * This position is independent of the actual OS mouse and client tracking.
 */
public class VirtualMousePosition {

    private volatile int x = 0;
    private volatile int y = 0;
    private volatile long lastUpdateTime = 0;

    // Optional listener for position changes
    private Consumer<Point> positionListener;

    public VirtualMousePosition() {
    }

    public VirtualMousePosition(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public synchronized void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.lastUpdateTime = System.currentTimeMillis();

        if (positionListener != null) {
            positionListener.accept(new Point(x, y));
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public synchronized Point getPosition() {
        return new Point(x, y);
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setPositionListener(Consumer<Point> listener) {
        this.positionListener = listener;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
