package com.tonic.plugins.gearswapper.triggers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

/**
 * Base trigger class that defines what a trigger is and how it behaves
 * Thread-safe and validated implementation
 */
public class Trigger
{
    // Core immutable fields
    private final String id;
    private volatile String name;
    private volatile TriggerType type;
    private final TriggerConfig config;
    
    // Thread-safe mutable state
    private final List<TriggerAction> actions = new ArrayList<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicLong lastFired = new AtomicLong(0);
    private final AtomicInteger fireCount = new AtomicInteger(0);
    private final long createdTime;
    private volatile TriggerStats stats;

    /**
     * Constructor with validation
     */
    public Trigger(String id, String name, TriggerType type)
    {
        // Validate ID
        if (id == null || id.trim().isEmpty())
        {
            this.id = "trigger_" + UUID.randomUUID().toString().substring(0, 8);
        }
        else
        {
            this.id = validateId(id);
        }
        
        // Validate name
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Unnamed Trigger";
        
        // Validate type
        this.type = type != null ? type : TriggerType.HP;
        
        this.config = new TriggerConfig();
        this.createdTime = System.currentTimeMillis();
        this.stats = new TriggerStats();
    }
    
    /**
     * Copy constructor for safe cloning
     */
    public Trigger(Trigger other)
    {
        if (other == null)
        {
            throw new IllegalArgumentException("Cannot copy null trigger");
        }
        
        this.id = other.id; // ID is immutable, keep same
        this.name = other.name;
        this.type = other.type;
        this.config = new TriggerConfig(); // Fresh config
        this.enabled.set(other.enabled.get());
        this.lastFired.set(other.lastFired.get());
        this.fireCount.set(other.fireCount.get());
        this.createdTime = other.createdTime;
        this.stats = new TriggerStats(other.stats);
        
        // Deep copy actions
        synchronized (other.actions)
        {
            for (TriggerAction action : other.actions)
            {
                // Note: This would require action cloning support
                // For now, we'll add references (actions should be immutable)
                this.actions.add(action);
            }
        }
    }
    
    /**
     * Validate trigger ID format
     */
    private String validateId(String id)
    {
        String trimmed = id.trim();
        
        // Remove invalid characters
        String validated = trimmed.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Ensure it starts with letter or underscore
        if (!validated.matches("^[a-zA-Z_].*"))
        {
            validated = "trigger_" + validated;
        }
        
        // Limit length
        if (validated.length() > 50)
        {
            validated = validated.substring(0, 50);
        }
        
        return validated;
    }

    // Thread-safe getters and setters with validation
    public String getId() { return id; } // ID is immutable
    
    public String getName() { return name; }
    
    public void setName(String name) 
    {
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Unnamed Trigger";
    }

    public TriggerType getType() { return type; }
    
    public void setType(TriggerType type) 
    {
        this.type = type != null ? type : TriggerType.HP;
    }

    public TriggerConfig getConfig() { return config; } // Config is immutable after construction
    
    /**
     * Update trigger configuration (for deserialization)
     */
    public void updateConfig(TriggerConfig newConfig)
    {
        if (newConfig != null && newConfig.isValid())
        {
            // Copy all config properties
            this.config.setCooldownMs(newConfig.getCooldownMs());
            this.config.setOnlyInCombat(newConfig.isOnlyInCombat());
            this.config.setOnlyWhenLoggedIn(newConfig.isOnlyWhenLoggedIn());
            this.config.setTestMode(newConfig.isTestMode());
            this.config.setDebugEnabled(newConfig.isDebugEnabled());
            
            // Animation-specific
            this.config.setAnimationId(newConfig.getAnimationId());
            this.config.setTargetFilterByValue(newConfig.getTargetFilterValue());
            
            // HP-specific
            this.config.setHpThreshold(newConfig.getHpThreshold());
            this.config.setHpIsPercentage(newConfig.isHpIsPercentage());
            this.config.setHpTargetType(newConfig.getHpTargetType());
            this.config.setHpThresholdType(newConfig.getHpThresholdType());
            this.config.setMinConsecutiveTicks(newConfig.getMinConsecutiveTicks());
            
            // Special attack
            this.config.setRequireSpecialAttack(newConfig.isRequireSpecialAttack());
            this.config.setSpecialAttackThreshold(newConfig.getSpecialAttackThreshold());
            
            // Distance
            this.config.setMaxDistance(newConfig.getMaxDistance());
            
            // XP-specific
            this.config.setXpThreshold(newConfig.getXpThreshold());
            this.config.setMaxXpThreshold(newConfig.getMaxXpThreshold());
            this.config.setSkillFilter(newConfig.getSkillFilter());
            
            // Damage-specific
            this.config.setDamageThreshold(newConfig.getDamageThreshold());
            this.config.setDamageType(newConfig.getDamageType());
            
            // Status-specific
            this.config.setStatusThreshold(newConfig.getStatusThreshold());
            
            // Location-specific
            this.config.setAreaName(newConfig.getAreaName());
            this.config.setX1(newConfig.getX1());
            this.config.setY1(newConfig.getY1());
            this.config.setX2(newConfig.getX2());
            this.config.setY2(newConfig.getY2());
        }
    }

    public List<TriggerAction> getActions() 
    {
        synchronized (actions)
        {
            return new ArrayList<>(actions); // Return copy for thread safety
        }
    }

    public boolean isEnabled() { return enabled.get(); }
    
    public void setEnabled(boolean enabled) 
    {
        this.enabled.set(enabled);
    }

    public long getLastFired() { return lastFired.get(); }
    
    public void setLastFired(long lastFired) 
    {
        this.lastFired.set(Math.max(0, lastFired));
    }

    public int getFireCount() { return fireCount.get(); }
    
    public void incrementFireCount() 
    {
        fireCount.incrementAndGet();
    }
    
    public void setFireCount(int fireCount) 
    {
        this.fireCount.set(Math.max(0, fireCount));
    }

    public long getCreatedTime() { return createdTime; } // Immutable

    public TriggerStats getStats() { return stats; }
    
    public void setStats(TriggerStats stats) 
    {
        this.stats = stats != null ? stats : new TriggerStats();
    }

    public long getCooldownMs()
    {
        return config.getCooldownMs();
    }

    /**
     * Add an action to this trigger with validation
     */
    public boolean addAction(TriggerAction action)
    {
        if (action == null)
        {
            return false;
        }
        
        synchronized (actions)
        {
            // Check for duplicate actions (same type and description)
            for (TriggerAction existing : actions)
            {
                if (existing.getClass().equals(action.getClass()) && 
                    existing.getDescription().equals(action.getDescription()))
                {
                    return false; // Duplicate found
                }
            }
            
            return actions.add(action);
        }
    }

    /**
     * Remove an action from this trigger
     */
    public boolean removeAction(TriggerAction action)
    {
        if (action == null)
        {
            return false;
        }
        
        synchronized (actions)
        {
            return actions.remove(action);
        }
    }
    
    /**
     * Remove action by index
     */
    public boolean removeAction(int index)
    {
        if (index < 0 || index >= getActionCount())
        {
            return false;
        }
        
        synchronized (actions)
        {
            actions.remove(index);
            return true;
        }
    }

    /**
     * Clear all actions from this trigger
     */
    public void clearActions()
    {
        synchronized (actions)
        {
            actions.clear();
        }
    }
    
    /**
     * Get action count
     */
    public int getActionCount()
    {
        synchronized (actions)
        {
            return actions.size();
        }
    }
    
    /**
     * Get action by index
     */
    public TriggerAction getAction(int index)
    {
        if (index < 0 || index >= getActionCount())
        {
            return null;
        }
        
        synchronized (actions)
        {
            return actions.get(index);
        }
    }

    /**
     * Check if trigger is ready to fire (not on cooldown)
     */
    public boolean isReadyToFire()
    {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastFired.get()) >= config.getCooldownMs();
    }
    
    /**
     * Validate trigger consistency
     */
    public boolean isValid()
    {
        // Basic validation
        if (id == null || id.trim().isEmpty()) return false;
        if (name == null || name.trim().isEmpty()) return false;
        if (type == null) return false;
        if (config == null || !config.isValid()) return false;
        
        // Action validation
        synchronized (actions)
        {
            for (TriggerAction action : actions)
            {
                if (action == null) return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get validation error message
     */
    public String getValidationError()
    {
        if (id == null || id.trim().isEmpty())
            return "Trigger ID cannot be null or empty";
            
        if (name == null || name.trim().isEmpty())
            return "Trigger name cannot be null or empty";
            
        if (type == null)
            return "Trigger type cannot be null";
            
        if (config == null)
            return "Trigger config cannot be null";
            
        String configError = config.getValidationError();
        if (configError != null)
            return "Config error: " + configError;
            
        // Action validation
        synchronized (actions)
        {
            for (int i = 0; i < actions.size(); i++)
            {
                if (actions.get(i) == null)
                    return "Action at index " + i + " is null";
            }
        }
        
        return null; // No error
    }
    
    /**
     * Create a safe copy of this trigger
     */
    public Trigger copy()
    {
        return new Trigger(this);
    }

    @Override
    public String toString()
    {
        return String.format("Trigger{id='%s', name='%s', type=%s, enabled=%s, fireCount=%d}", 
                           id, name, type, enabled.get(), fireCount.get());
    }
}
