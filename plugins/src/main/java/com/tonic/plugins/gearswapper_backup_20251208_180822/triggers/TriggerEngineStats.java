package com.tonic.plugins.gearswapper.triggers;

/**
 * Statistics for the trigger engine
 */
public class TriggerEngineStats
{
    private final boolean enabled;
    private final int activeTriggerCount;
    private final long totalEventsProcessed;
    private final long totalTriggersFired;
    private final long uptime;
    private final boolean processing;

    public TriggerEngineStats(boolean enabled, int activeTriggerCount, long totalEventsProcessed, 
                            long totalTriggersFired, long uptime, boolean processing)
    {
        this.enabled = enabled;
        this.activeTriggerCount = activeTriggerCount;
        this.totalEventsProcessed = totalEventsProcessed;
        this.totalTriggersFired = totalTriggersFired;
        this.uptime = uptime;
        this.processing = processing;
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public int getActiveTriggerCount() { return activeTriggerCount; }
    public long getTotalEventsProcessed() { return totalEventsProcessed; }
    public long getTotalTriggersFired() { return totalTriggersFired; }
    public long getUptime() { return uptime; }
    public boolean isProcessing() { return processing; }

    /**
     * Get triggers fired per minute
     */
    public double getTriggersPerMinute()
    {
        if (uptime <= 0)
        {
            return 0;
        }
        return (totalTriggersFired * 60000.0) / uptime;
    }

    /**
     * Get events processed per minute
     */
    public double getEventsPerMinute()
    {
        if (uptime <= 0)
        {
            return 0;
        }
        return (totalEventsProcessed * 60000.0) / uptime;
    }

    @Override
    public String toString()
    {
        return String.format("TriggerEngineStats{enabled=%s, activeTriggers=%d, events=%d, triggers=%d, uptime=%ds, triggers/min=%.1f}", 
                           enabled, activeTriggerCount, totalEventsProcessed, totalTriggersFired, 
                           uptime / 1000, getTriggersPerMinute());
    }
}
