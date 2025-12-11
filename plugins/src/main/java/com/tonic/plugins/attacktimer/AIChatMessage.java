package com.tonic.plugins.attacktimer;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a chat message for AI conversation history.
 * Named AIChatMessage to avoid conflict with net.runelite.api.events.ChatMessage
 */
public class AIChatMessage {
    @SerializedName("timestamp")
    private String timestamp;
    
    @SerializedName("role")
    private String role;
    
    @SerializedName("content")
    private String content;
    
    public void setTimestamp(String timestamp) { 
        this.timestamp = timestamp; 
    }
    
    public void setRole(String role) { 
        this.role = role; 
    }
    
    public void setContent(String content) { 
        this.content = content; 
    }

    public String getTimestamp() {
        return this.timestamp;
    }
    
    public String getRole() {
        return this.role;
    }
    
    public String getContent() {
        return this.content;
    }

    public AIChatMessage() {}

    public AIChatMessage(String timestamp, String role, String content) {
        this.timestamp = timestamp;
        this.role = role;
        this.content = content;
    }
}