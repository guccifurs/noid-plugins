package com.tonic.plugins.gearswapper.triggers;

/**
 * Statistics for a trigger action
 */
public class ActionStats
{
    private final String id;
    private final ActionType type;
    private final String description;
    private final boolean enabled;
    private final long createdTime;
    private long lastExecuted;
    private int executionCount;
    private long totalExecutionTime;
    private long minExecutionTime;
    private long maxExecutionTime;
    private boolean hasExecuted;

    public ActionStats(String id, ActionType type, String description, boolean enabled, long createdTime)
    {
        this.id = id;
        this.type = type;
        this.description = description;
        this.enabled = enabled;
        this.createdTime = createdTime;
        this.lastExecuted = 0;
        this.executionCount = 0;
        this.totalExecutionTime = 0;
        this.minExecutionTime = Long.MAX_VALUE;
        this.maxExecutionTime = 0;
        this.hasExecuted = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public ActionType getType() { return type; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public long getCreatedTime() { return createdTime; }

    public long getLastExecuted() { return lastExecuted; }
    public void setLastExecuted(long lastExecuted) { this.lastExecuted = lastExecuted; }

    public int getExecutionCount() { return executionCount; }
    public void setExecutionCount(int executionCount) { this.executionCount = executionCount; }

    public long getTotalExecutionTime() { return totalExecutionTime; }
    public void setTotalExecutionTime(long totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }

    public long getMinExecutionTime() { return minExecutionTime; }
    public void setMinExecutionTime(long minExecutionTime) { this.minExecutionTime = minExecutionTime; }

    public long getMaxExecutionTime() { return maxExecutionTime; }
    public void setMaxExecutionTime(long maxExecutionTime) { this.maxExecutionTime = maxExecutionTime; }

    public boolean isHasExecuted() { return hasExecuted; }
    public void setHasExecuted(boolean hasExecuted) { this.hasExecuted = hasExecuted; }

    /**
     * Get average execution time
     */
    public long getAverageExecutionTime()
    {
        return executionCount > 0 ? totalExecutionTime / executionCount : 0;
    }

    @Override
    public String toString()
    {
        return String.format("ActionStats{id='%s', type=%s, executions=%d, avgTime=%dms}", 
                           id, type, executionCount, getAverageExecutionTime());
    }
}
