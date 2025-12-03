package com.tonic.plugins.gearswapper.triggers;

/**
 * Statistics for a trigger
 */
public class TriggerStats
{
    private long totalFired;
    private long lastFired;
    private long averageFireInterval;
    private long minFireInterval;
    private long maxFireInterval;
    private boolean hasFired;

    public TriggerStats()
    {
        this.totalFired = 0;
        this.lastFired = 0;
        this.averageFireInterval = 0;
        this.minFireInterval = Long.MAX_VALUE;
        this.maxFireInterval = 0;
        this.hasFired = false;
    }
    
    /**
     * Copy constructor
     */
    public TriggerStats(TriggerStats other)
    {
        if (other == null)
        {
            this.totalFired = 0;
            this.lastFired = 0;
            this.averageFireInterval = 0;
            this.minFireInterval = Long.MAX_VALUE;
            this.maxFireInterval = 0;
            this.hasFired = false;
        }
        else
        {
            this.totalFired = other.totalFired;
            this.lastFired = other.lastFired;
            this.averageFireInterval = other.averageFireInterval;
            this.minFireInterval = other.minFireInterval;
            this.maxFireInterval = other.maxFireInterval;
            this.hasFired = other.hasFired;
        }
    }

    // Getters and setters
    public long getTotalFired() { return totalFired; }
    public void setTotalFired(long totalFired) { this.totalFired = totalFired; }

    public long getLastFired() { return lastFired; }
    public void setLastFired(long lastFired) { this.lastFired = lastFired; }

    public long getAverageFireInterval() { return averageFireInterval; }
    public void setAverageFireInterval(long averageFireInterval) { this.averageFireInterval = averageFireInterval; }

    public long getMinFireInterval() { return minFireInterval; }
    public void setMinFireInterval(long minFireInterval) { this.minFireInterval = minFireInterval; }

    public long getMaxFireInterval() { return maxFireInterval; }
    public void setMaxFireInterval(long maxFireInterval) { this.maxFireInterval = maxFireInterval; }

    public boolean hasFired() { return hasFired; }
    public void setHasFired(boolean hasFired) { this.hasFired = hasFired; }

    /**
     * Update statistics when trigger fires
     */
    public void onTriggerFired(long currentTime)
    {
        if (hasFired)
        {
            long interval = currentTime - lastFired;
            
            // Update average
            if (totalFired > 0)
            {
                averageFireInterval = ((averageFireInterval * totalFired) + interval) / (totalFired + 1);
            }
            else
            {
                averageFireInterval = interval;
            }
            
            // Update min/max
            minFireInterval = Math.min(minFireInterval, interval);
            maxFireInterval = Math.max(maxFireInterval, interval);
        }
        
        totalFired++;
        lastFired = currentTime;
        hasFired = true;
    }

    @Override
    public String toString()
    {
        return String.format("TriggerStats{totalFired=%d, lastFired=%d, avgInterval=%dms}", 
                           totalFired, lastFired, averageFireInterval);
    }
}
