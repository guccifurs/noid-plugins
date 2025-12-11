package com.tonic.plugins.gearswapper.triggers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a trigger event that occurs in the game
 * Thread-safe and validated implementation
 */
public class TriggerEvent
{
    // Core immutable fields
    private final TriggerEventType type;
    private final Object sourceEvent;
    private final long timestamp;
    
    // Thread-safe data storage
    private final Map<String, Object> data;
    
    // Event metadata
    private final String eventId;
    private final long creationTime;

    /**
     * Constructor with validation
     */
    public TriggerEvent(TriggerEventType type, Object sourceEvent)
    {
        // Validate type
        this.type = type != null ? type : TriggerEventType.CUSTOM;
        
        // Validate source event
        this.sourceEvent = sourceEvent; // Can be null for synthetic events
        
        // Set immutable timestamps
        this.timestamp = System.currentTimeMillis();
        this.creationTime = this.timestamp;
        
        // Thread-safe data storage
        this.data = new ConcurrentHashMap<>();
        
        // Generate unique event ID
        this.eventId = generateEventId();
    }
    
    /**
     * Copy constructor for safe cloning
     */
    private TriggerEvent(TriggerEvent other)
    {
        if (other == null)
        {
            throw new IllegalArgumentException("Cannot copy null event");
        }
        
        this.type = other.type;
        this.sourceEvent = other.sourceEvent;
        this.timestamp = other.timestamp;
        this.creationTime = other.creationTime;
        this.eventId = other.eventId; // Keep same ID for copy
        
        // Deep copy data map
        this.data = new ConcurrentHashMap<>(other.data);
    }
    
    /**
     * Generate unique event ID
     */
    private String generateEventId()
    {
        return "event_" + type.name() + "_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(System.identityHashCode(this));
    }

    // Immutable getters
    public TriggerEventType getType() { return type; }
    
    public Object getSourceEvent() { return sourceEvent; }
    
    public long getTimestamp() { return timestamp; }
    
    public long getCreationTime() { return creationTime; }
    
    public String getEventId() { return eventId; }
    
    /**
     * Get immutable copy of data map
     */
    public Map<String, Object> getData() 
    {
        return Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * Add data to this event with validation
     */
    public void addData(String key, Object value)
    {
        if (key == null || key.trim().isEmpty())
        {
            throw new IllegalArgumentException("Event data key cannot be null or empty");
        }
        
        // Validate key format
        String validatedKey = validateDataKey(key);
        
        // Store value (can be null, represents removal)
        if (value == null)
        {
            data.remove(validatedKey);
        }
        else
        {
            data.put(validatedKey, value);
        }
    }
    
    /**
     * Validate data key format
     */
    private String validateDataKey(String key)
    {
        String trimmed = key.trim();
        
        // Remove invalid characters
        String validated = trimmed.replaceAll("[^a-zA-Z0-9_.-]", "_");
        
        // Limit length
        if (validated.length() > 100)
        {
            validated = validated.substring(0, 100);
        }
        
        return validated;
    }

    /**
     * Get data from this event
     */
    public Object getData(String key)
    {
        if (key == null || key.trim().isEmpty())
        {
            return null;
        }
        
        return data.get(key);
    }

    /**
     * Get data as a specific type with safe casting
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type)
    {
        Object value = getData(key);
        if (value != null && type.isInstance(value))
        {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Get data as a specific type with default value
     */
    public <T> T getData(String key, Class<T> type, T defaultValue)
    {
        T value = getData(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get data as String
     */
    public String getDataAsString(String key)
    {
        Object value = getData(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Get data as String with default
     */
    public String getDataAsString(String key, String defaultValue)
    {
        String value = getDataAsString(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get data as Integer
     */
    public Integer getDataAsInteger(String key)
    {
        Object value = getData(key);
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Integer.parseInt((String) value);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Get data as Integer with default
     */
    public int getDataAsInteger(String key, int defaultValue)
    {
        Integer value = getDataAsInteger(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get data as Boolean
     */
    public Boolean getDataAsBoolean(String key)
    {
        Object value = getData(key);
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        if (value instanceof String)
        {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    /**
     * Get data as Boolean with default
     */
    public boolean getDataAsBoolean(String key, boolean defaultValue)
    {
        Boolean value = getDataAsBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Check if event contains specific data key
     */
    public boolean hasData(String key)
    {
        return key != null && data.containsKey(key);
    }
    
    /**
     * Get all data keys
     */
    public java.util.Set<String> getDataKeys()
    {
        return Collections.unmodifiableSet(data.keySet());
    }
    
    /**
     * Get data size
     */
    public int getDataSize()
    {
        return data.size();
    }
    
    /**
     * Clear all data
     */
    public void clearData()
    {
        data.clear();
    }
    
    /**
     * Validate event consistency
     */
    public boolean isValid()
    {
        return type != null && eventId != null && !eventId.trim().isEmpty();
    }
    
    /**
     * Get validation error message
     */
    public String getValidationError()
    {
        if (type == null)
            return "Event type cannot be null";
            
        if (eventId == null || eventId.trim().isEmpty())
            return "Event ID cannot be null or empty";
            
        return null; // No error
    }
    
    /**
     * Create a safe copy of this event
     */
    public TriggerEvent copy()
    {
        return new TriggerEvent(this);
    }
    
    /**
     * Get event age in milliseconds
     */
    public long getAge()
    {
        return System.currentTimeMillis() - timestamp;
    }

    @Override
    public String toString()
    {
        return String.format("TriggerEvent{id='%s', type=%s, timestamp=%d, age=%dms, data=%d entries}", 
                           eventId, type, timestamp, getAge(), data.size());
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TriggerEvent that = (TriggerEvent) obj;
        return eventId.equals(that.eventId);
    }
    
    @Override
    public int hashCode()
    {
        return eventId.hashCode();
    }
}
