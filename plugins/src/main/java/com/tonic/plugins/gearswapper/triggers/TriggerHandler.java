package com.tonic.plugins.gearswapper.triggers;

import net.runelite.api.Client;
import java.util.Collection;

/**
 * Base interface for all trigger handlers
 */
public interface TriggerHandler
{
    /**
     * Check if this handler can handle the given event type
     */
    boolean canHandle(TriggerEvent event);

    /**
     * Handle a trigger event
     */
    void handleEvent(TriggerEvent event, Collection<Trigger> triggers);

    /**
     * Get the client (for handlers that need access to game state)
     */
    default Client getClient()
    {
        return null; // Handlers should get client from engine
    }
}
