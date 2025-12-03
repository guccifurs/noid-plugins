package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.ui.overlay.OverlayManager;

public class GetMode
{
    // Game mode flags
    private static boolean isOneDefPure = false;
    private static boolean isMaxMed = false;
    private static boolean isZerker = false;
    private static String currentMode = "Unknown";
    
    // Overlay manager reference for refreshing overlay
    private static OverlayManager overlayManager;
    
    // Game mode constants
    public static final String ONE_DEF_PURE = "1 Def Pure";
    public static final String MAX_MED = "Max/Med";
    public static final String ZERKER = "Zerker";
    
    /**
     * Set overlay manager reference for refreshing overlay
     */
    public static void setOverlayManager(OverlayManager manager)
    {
        overlayManager = manager;
        Logger.norm("[GetMode] Overlay manager reference set");
    }
    
    /**
     * Process chat message to detect LMS game mode
     */
    public static void processChatMessage(ChatMessage event)
    {
        String message = event.getMessage();
        Logger.norm("[GetMode] processChatMessage called - message type: " + event.getType() + " - message: " + message);
        
        if (event.getType() == ChatMessageType.GAMEMESSAGE)
        {
            // Debug: Log the message we're checking
            Logger.norm("[GetMode] Checking message: " + message);
            
            // Check for LMS mode message (handle both Mode: and mode:)
            if (message.startsWith("Last Man Standing Mode:") || message.startsWith("Last Man Standing mode:"))
            {
                String mode = extractMode(message);
                setGameMode(mode);
                
                Logger.norm("[GetMode] Detected LMS game mode: " + mode);
            }
            else
            {
                // Debug: Show why it didn't match
                Logger.norm("[GetMode] Message doesn't start with 'Last Man Standing Mode:' - not a GAMEMESSAGE");
            }
        }
        else
        {
            Logger.norm("[GetMode] Not a GAMEMESSAGE, skipping - type: " + event.getType());
        }
    }
    
    /**
     * Extract game mode from chat message
     */
    private static String extractMode(String message)
    {
        // Message format: "Last Man Standing mode: <col=ef1020>Max/Med</col>"
        String[] parts = message.split(":");
        if (parts.length >= 2)
        {
            String modeWithColors = parts[1].trim();
            
            // Remove HTML color tags
            String cleanMode = modeWithColors.replaceAll("<col=[0-9a-fA-F]+>", "")
                                          .replaceAll("</col>", "")
                                          .trim();
            
            Logger.norm("[GetMode] Extracted mode: " + cleanMode + " (from: " + modeWithColors + ")");
            return cleanMode;
        }
        return "Unknown";
    }
    
    /**
     * Set game mode and update flags
     */
    private static void setGameMode(String mode)
    {
        // Reset all flags first
        resetFlags();
        
        // Set the appropriate flag based on mode
        switch (mode)
        {
            case ONE_DEF_PURE:
                isOneDefPure = true;
                currentMode = ONE_DEF_PURE;
                break;
            case MAX_MED:
                isMaxMed = true;
                currentMode = MAX_MED;
                break;
            case ZERKER:
                isZerker = true;
                currentMode = ZERKER;
                break;
            default:
                currentMode = "Unknown";
                break;
        }
        
        // Refresh overlay to show new game mode
        refreshOverlay();
    }
    
    /**
     * Refresh the overlay to show updated game mode
     */
    private static void refreshOverlay()
    {
        if (overlayManager != null)
        {
            Logger.norm("[GetMode] Refreshing overlay to show new game mode: " + currentMode);
            // RuneLite overlays automatically rerender on each game tick
            // The overlay will pick up the new game mode on next render cycle
            // No manual refresh needed - just log that we detected the change
        }
        else
        {
            Logger.warn("[GetMode] Cannot refresh overlay - overlay manager is null");
        }
    }
    
    /**
     * Reset all game mode flags
     */
    public static void resetFlags()
    {
        isOneDefPure = false;
        isMaxMed = false;
        isZerker = false;
        currentMode = "Unknown";
    }
    
    /**
     * Get current game mode
     */
    public static String getCurrentMode()
    {
        return currentMode;
    }
    
    /**
     * Check if current mode is 1 Def Pure
     */
    public static boolean isOneDefPure()
    {
        return isOneDefPure;
    }
    
    /**
     * Check if current mode is Max/Med
     */
    public static boolean isMaxMed()
    {
        return isMaxMed;
    }
    
    /**
     * Check if current mode is Zerker
     */
    public static boolean isZerker()
    {
        return isZerker;
    }
    
    /**
     * Check if any game mode is detected
     */
    public static boolean hasGameMode()
    {
        return isOneDefPure || isMaxMed || isZerker;
    }
}
