package com.tonic.plugins.clicktest;

import net.runelite.api.Client;

import java.awt.*;
import java.awt.event.*;

/**
 * Intercepts and blocks real mouse events on the game canvas.
 * When enabled, real mouse movement won't update the client's internal
 * position.
 * Blocked clicks are redirected to HumanizedMouseMover for smooth movement.
 */
public class MouseInputBlocker {

    private final Client client;
    private boolean blocking = false;
    private boolean humanizeEnabled = true; // When true, blocked clicks trigger humanized movement
    private AWTEventListener awtListener;
    private HumanizedMouseMover mover;

    // Callback for logging
    private final java.util.function.BiConsumer<String, String> logger;

    public MouseInputBlocker(Client client, java.util.function.BiConsumer<String, String> logger) {
        this.client = client;
        this.logger = logger;
        this.mover = new HumanizedMouseMover(client, logger);
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean isHumanizeEnabled() {
        return humanizeEnabled;
    }

    public void setHumanizeEnabled(boolean enabled) {
        this.humanizeEnabled = enabled;
        if (logger != null) {
            logger.accept("SYSTEM", "Humanized clicks " + (enabled ? "ENABLED" : "DISABLED"));
        }
    }

    public HumanizedMouseMover getMover() {
        return mover;
    }

    public void startBlocking() {
        if (blocking)
            return;

        blocking = true;

        // Install AWT event listener to intercept mouse events
        awtListener = event -> {
            if (!blocking)
                return;
            if (!(event instanceof MouseEvent))
                return;

            MouseEvent me = (MouseEvent) event;

            // Check if this is one of our simulated events - let it through!
            Object source = me.getSource();
            if (source instanceof HumanizedMouseMover.SimulatedEventSource) {
                // This is our simulated event - don't block it, let it through
                // Reset the source to the original canvas so it works properly
                HumanizedMouseMover.SimulatedEventSource simSource = (HumanizedMouseMover.SimulatedEventSource) source;
                me.setSource(simSource.originalSource);
                return; // Don't consume - let it pass
            }

            // Only block events on the game canvas
            if (client == null || client.getCanvas() == null)
                return;
            if (me.getSource() != client.getCanvas())
                return;

            // Block all mouse motion events (these update position)
            if (me.getID() == MouseEvent.MOUSE_MOVED ||
                    me.getID() == MouseEvent.MOUSE_DRAGGED ||
                    me.getID() == MouseEvent.MOUSE_ENTERED ||
                    me.getID() == MouseEvent.MOUSE_EXITED) {

                me.consume();
                // Don't log every motion - too spammy
            }

            // Handle clicks - either block or redirect to humanized mover
            if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                me.consume();

                if (humanizeEnabled && !mover.isMoving()) {
                    // Redirect to humanized mover
                    int button = me.getButton();
                    if (logger != null) {
                        logger.accept("REDIRECT", "Click at (" + me.getX() + ", " + me.getY() + ") → Humanized move");
                    }
                    mover.moveAndClick(me.getX(), me.getY(), button);
                } else if (logger != null) {
                    logger.accept("BLOCKED", "Click at (" + me.getX() + ", " + me.getY() + ")");
                }
            }

            // Also consume release and click to complete blocking
            if (me.getID() == MouseEvent.MOUSE_RELEASED ||
                    me.getID() == MouseEvent.MOUSE_CLICKED) {
                me.consume();
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        if (logger != null) {
            logger.accept("SYSTEM", "Mouse blocking ENABLED" + (humanizeEnabled ? " + Humanized clicks" : ""));
        }
    }

    public void stopBlocking() {
        if (!blocking)
            return;

        blocking = false;

        if (awtListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
            awtListener = null;
        }

        if (logger != null) {
            logger.accept("SYSTEM", "Mouse blocking DISABLED");
        }
    }

    public void toggle() {
        if (blocking) {
            stopBlocking();
        } else {
            startBlocking();
        }
    }

    public void shutdown() {
        stopBlocking();
        if (mover != null) {
            mover.shutdown();
        }
    }
}
