package com.tonic.plugins.gearswapper.triggers;

/**
 * Enumeration of all supported action types
 */
public enum ActionType
{
    GEAR_SWAP("Gear Swap", "Switch to a specific gear set"),
    EXECUTE_COMMAND("Execute Command", "Run a custom command"),
    SHOW_NOTIFICATION("Show Notification", "Display a message to the user"),
    PLAY_SOUND("Play Sound", "Play a notification sound"),
    SEND_CHAT_MESSAGE("Send Chat Message", "Send a message to chat"),
    SIMULATE_KEYPRESS("Simulate Keypress", "Press a key or hotkey"),
    LOG_EVENT("Log Event", "Write event information to log"),
    MOVE("Move", "Move a specific number of tiles away from target"),
    CUSTOM("Custom", "Custom action with user-defined behavior");

    private final String displayName;
    private final String description;

    ActionType(String displayName, String description)
    {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
