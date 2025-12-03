package com.tonic.plugins.gearswapper.triggers;

/**
 * Enumeration of all supported trigger types organized by categories
 */
public enum TriggerType
{
    // HP Category
    HP("HP", "Health-based triggers"),
    
    // Animation Category
    ANIMATION("Animation", "Animation-based triggers"),
    
    // XP Category
    XP("XP", "Experience-based triggers");

    private final String displayName;
    private final String description;

    TriggerType(String displayName, String description)
    {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
