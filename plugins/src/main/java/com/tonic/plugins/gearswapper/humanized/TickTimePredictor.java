package com.tonic.plugins.gearswapper.humanized;

/**
 * Predicts tick timing for humanized mouse movement scheduling.
 * Tracks game ticks and calculates available time for actions.
 */
public class TickTimePredictor {

    public static final int TICK_DURATION_MS = 600;

    private volatile long lastTickTimestamp = 0;
    private volatile int currentTick = 0;
    private volatile int pingThresholdMs = 100;

    public void onGameTick(int tickCount) {
        this.lastTickTimestamp = System.currentTimeMillis();
        this.currentTick = tickCount;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public long getMsSinceLastTick() {
        if (lastTickTimestamp == 0)
            return 0;
        return System.currentTimeMillis() - lastTickTimestamp;
    }

    public int getMsUntilNextTick() {
        long elapsed = getMsSinceLastTick();
        return Math.max(0, TICK_DURATION_MS - (int) elapsed);
    }

    public int getAvailableMs(int maxExecutionTime) {
        int untilNext = getMsUntilNextTick();
        int safeTime = untilNext - pingThresholdMs;
        return Math.max(0, Math.min(safeTime, maxExecutionTime));
    }

    public boolean hasTimeForActions(int actionCount, int minMsPerAction) {
        return getAvailableMs(550) >= (actionCount * minMsPerAction);
    }

    public int calculateDelayPerAction(int actionCount, int maxExecutionTime) {
        if (actionCount <= 0)
            return 0;
        int available = getAvailableMs(maxExecutionTime);
        return Math.max(10, available / actionCount);
    }

    public void setPingThreshold(int ms) {
        this.pingThresholdMs = Math.max(0, ms);
    }

    public int getPingThreshold() {
        return pingThresholdMs;
    }

    public String getDebugInfo() {
        return String.format("Tick %d | Elapsed: %dms | Until next: %dms | Available: %dms",
                currentTick, getMsSinceLastTick(), getMsUntilNextTick(), getAvailableMs(450));
    }
}
