package com.tonic.plugins.gearswapper.humanized;

import java.awt.Point;

/**
 * Represents a single action to be executed with humanized mouse movement.
 */
public class HumanizedAction {

    private final Point targetPosition;
    private final Runnable action;
    private final String description;

    public HumanizedAction(int targetX, int targetY, Runnable action, String description) {
        this.targetPosition = new Point(targetX, targetY);
        this.action = action;
        this.description = description;
    }

    public HumanizedAction(Point target, Runnable action, String description) {
        this.targetPosition = target;
        this.action = action;
        this.description = description;
    }

    public HumanizedAction(Runnable action, String description) {
        this.targetPosition = null;
        this.action = action;
        this.description = description;
    }

    public Point getTargetPosition() {
        return targetPosition;
    }

    public boolean hasTargetPosition() {
        return targetPosition != null;
    }

    public Runnable getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public void execute() {
        if (action != null)
            action.run();
    }

    @Override
    public String toString() {
        if (targetPosition != null) {
            return String.format("HumanizedAction[%s @ (%d,%d)]", description, targetPosition.x, targetPosition.y);
        }
        return String.format("HumanizedAction[%s]", description);
    }
}
