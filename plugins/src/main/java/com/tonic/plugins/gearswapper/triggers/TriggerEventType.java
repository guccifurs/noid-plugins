package com.tonic.plugins.gearswapper.triggers;

/**
 * Enumeration of trigger event types
 */
public enum TriggerEventType {
    // Animation triggers
    ANIMATION_CHANGED("Animation Changed", "When an actor's animation changes"),

    // Game tick trigger
    GAME_TICK("Game Tick", "On each game tick"),

    // Health triggers
    HP_CHANGED("HP Changed", "When HP changes"),

    // Experience triggers
    XP_DROPPED("XP Dropped", "When XP drops"),
    DAMAGE_DEALT("Damage Dealt", "When damage is dealt"),
    DAMAGE_RECEIVED("Damage Received", "When damage is received"),
    COMBAT_STATE_CHANGED("Combat State Changed", "When combat state changes"),
    LOCATION_CHANGED("Location Changed", "When location changes"),
    PLAYER_SPAWNED("Player Spawned", "When a player spawns"),
    GFX_CHANGED("GFX Changed", "When an actor's graphic (SpotAnim) changes"),
    CUSTOM("Custom", "Custom event type");

    private final String displayName;
    private final String description;

    TriggerEventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
