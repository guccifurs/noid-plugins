package com.tonic.plugins.gearswapper.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import net.runelite.api.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for trigger actions with validation and thread safety
 */
public abstract class TriggerAction
{
    private static final Logger logger = LoggerFactory.getLogger(TriggerAction.class);
    
    // Core immutable fields
    private final String id;
    private volatile ActionType type;
    private volatile String description;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final long createdTime;

    /**
     * Constructor with validation
     */
    public TriggerAction(String id, ActionType type, String description)
    {
        // Validate and generate ID
        if (id == null || id.trim().isEmpty())
        {
            this.id = generateActionId();
        }
        else
        {
            this.id = validateActionId(id);
        }
        
        // Validate type
        this.type = type != null ? type : ActionType.CUSTOM;
        
        // Validate description
        this.description = description != null && !description.trim().isEmpty() ? 
                          description.trim() : "Unnamed Action";
        
        this.createdTime = System.currentTimeMillis();
    }
    
    /**
     * Copy constructor for safe cloning
     */
    protected TriggerAction(TriggerAction other)
    {
        if (other == null)
        {
            throw new IllegalArgumentException("Cannot copy null action");
        }
        
        this.id = other.id; // ID is immutable
        this.type = other.type;
        this.description = other.description;
        this.enabled.set(other.enabled.get());
        this.createdTime = other.createdTime;
    }
    
    /**
     * Generate unique action ID
     */
    private String generateActionId()
    {
        return "action_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Validate action ID format
     */
    private String validateActionId(String id)
    {
        String trimmed = id.trim();
        
        // Remove invalid characters
        String validated = trimmed.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Ensure it starts with letter or underscore
        if (!validated.matches("^[a-zA-Z_].*"))
        {
            validated = "action_" + validated;
        }
        
        // Limit length
        if (validated.length() > 50)
        {
            validated = validated.substring(0, 50);
        }
        
        return validated;
    }

    /**
     * Execute this action with comprehensive error handling
     */
    public final void execute(GearSwapperPlugin plugin, Client client, TriggerEvent event)
    {
        // Pre-execution validation
        if (!isEnabled())
        {
            logger.debug("[Trigger Action] Skipping disabled action: {}", getDescription());
            return;
        }
        
        if (!isValid())
        {
            logger.error("[Trigger Action] Cannot execute invalid action: {}", getDescription());
            return;
        }
        
        if (plugin == null || client == null || event == null)
        {
            logger.error("[Trigger Action] Missing required parameters for action: {}", getDescription());
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try
        {
            logger.debug("[Trigger Action] Executing action: {} ({})", getDescription(), getType());
            
            // Call the concrete implementation
            executeAction(plugin, client, event);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("[Trigger Action] Completed action: {} in {}ms", getDescription(), duration);
            
        }
        catch (Exception e)
        {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[Trigger Action] Error executing action {} after {}ms: {}", 
                        getDescription(), duration, e.getMessage(), e);
            
            // Handle execution error
            handleExecutionError(e, plugin, client, event);
        }
    }
    
    /**
     * Concrete implementation of the action
     */
    protected abstract void executeAction(GearSwapperPlugin plugin, Client client, TriggerEvent event);
    
    /**
     * Handle execution errors (can be overridden by subclasses)
     */
    protected void handleExecutionError(Exception e, GearSwapperPlugin plugin, Client client, TriggerEvent event)
    {
        // Default implementation just logs the error
        // Subclasses can override for custom error handling
    }

    // Thread-safe getters and setters with validation
    public String getId() { return id; } // ID is immutable
    
    public ActionType getType() { return type; }
    
    public void setType(ActionType type) 
    {
        this.type = type != null ? type : ActionType.CUSTOM;
    }

    public String getDescription() { return description; }
    
    public void setDescription(String description) 
    {
        this.description = description != null && !description.trim().isEmpty() ? 
                          description.trim() : "Unnamed Action";
    }

    public boolean isEnabled() { return enabled.get(); }
    
    public void setEnabled(boolean enabled) 
    {
        this.enabled.set(enabled);
    }
    
    public long getCreatedTime() { return createdTime; } // Immutable
    
    /**
     * Validate action consistency
     */
    public boolean isValid()
    {
        if (id == null || id.trim().isEmpty()) return false;
        if (type == null) return false;
        if (description == null || description.trim().isEmpty()) return false;
        
        return true;
    }
    
    /**
     * Get validation error message
     */
    public String getValidationError()
    {
        if (id == null || id.trim().isEmpty())
            return "Action ID cannot be null or empty";
            
        if (type == null)
            return "Action type cannot be null";
            
        if (description == null || description.trim().isEmpty())
            return "Action description cannot be null or empty";
            
        return null; // No error
    }
    
    /**
     * Create a safe copy of this action
     */
    public abstract TriggerAction copy();
    
    /**
     * Get action statistics (can be overridden by subclasses)
     */
    public ActionStats getStats()
    {
        return new ActionStats(id, type, description, enabled.get(), createdTime);
    }

    @Override
    public String toString()
    {
        return String.format("TriggerAction{id='%s', type=%s, description='%s', enabled=%s}", 
                           id, type, description, enabled.get());
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TriggerAction that = (TriggerAction) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}
