package com.tonic.plugins.gearswapper.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import net.runelite.api.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to swap to a specific gear set
 */
public class GearSwapAction extends TriggerAction
{
    private static final Logger logger = LoggerFactory.getLogger(GearSwapAction.class);
    
    private volatile String gearSetName;

    public GearSwapAction(String gearSetName)
    {
        super("gear_swap_" + gearSetName, ActionType.GEAR_SWAP, "Switch to gear set: " + gearSetName);
        this.gearSetName = gearSetName != null ? gearSetName : "Unknown";
    }
    
    /**
     * Copy constructor
     */
    private GearSwapAction(GearSwapAction other)
    {
        super(other);
        this.gearSetName = other.gearSetName;
    }

    @Override
    protected void executeAction(GearSwapperPlugin plugin, Client client, TriggerEvent event)
    {
        if (gearSetName == null || gearSetName.trim().isEmpty())
        {
            logger.warn("[Gear Swap Action] Empty gear set name, skipping execution");
            return;
        }
        
        logger.info("[Gear Swap Action] Switching to gear set: {}", gearSetName);
        
        // Find the loadout index for detailed information
        for (int i = 0; i < 50; i++)
        {
            String loadoutName = plugin.getLoadoutNameForTrigger(i);
            if (gearSetName.equals(loadoutName))
            {
                // Get detailed loadout information for logging
                String spell = plugin.getLoadoutSpellForTrigger(i);
                String prayer = plugin.getLoadoutPrayerForTrigger(i);
                String attackStyle = plugin.getLoadoutAttackForTrigger(i);
                String[] items = plugin.getLoadoutItemsForTrigger(i);
                
                logger.info("[Gear Swap Action] Activating gear set {} (Spell: {}, Prayer: {}, Attack: {}, Items: {})", 
                           gearSetName, spell, prayer, attackStyle, items != null ? items.length : 0);
                
                // ACTUAL GEAR SWITCHING - Use the plugin's built-in method
                try
                {
                    plugin.activateGearSetByName(gearSetName);
                    logger.info("[Gear Swap Action] Successfully activated gear set: {}", gearSetName);
                }
                catch (Exception e)
                {
                    logger.error("[Gear Swap Action] Failed to activate gear set '{}': {}", gearSetName, e.getMessage(), e);
                }
                
                break;
            }
        }
        
        // If no matching loadout was found by index, try by name directly
        if (plugin.getLoadoutNameForTrigger(0) == null || !gearSetName.equals(plugin.getLoadoutNameForTrigger(0)))
        {
            try
            {
                plugin.activateGearSetByName(gearSetName);
                logger.info("[Gear Swap Action] Activated gear set by name lookup: {}", gearSetName);
            }
            catch (Exception e)
            {
                logger.warn("[Gear Swap Action] Could not find gear set '{}': {}", gearSetName, e.getMessage());
            }
        }
    }
    
    // Getters and setters with validation
    public String getGearSetName() { return gearSetName; }
    
    public void setGearSetName(String gearSetName)
    {
        this.gearSetName = gearSetName != null ? gearSetName : "Unknown";
        setDescription("Switch to gear set: " + this.gearSetName);
    }
    
    @Override
    public boolean isValid()
    {
        if (!super.isValid()) return false;
        return gearSetName != null && !gearSetName.trim().isEmpty();
    }
    
    @Override
    public String getValidationError()
    {
        String baseError = super.getValidationError();
        if (baseError != null) return baseError;
        
        if (gearSetName == null || gearSetName.trim().isEmpty())
            return "Gear set name cannot be null or empty";
            
        return null; // No error
    }
    
    @Override
    public TriggerAction copy()
    {
        return new GearSwapAction(this);
    }
}
