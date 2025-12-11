package com.tonic.plugins.gearswapper.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import net.runelite.api.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to execute custom commands/prompts
 */
public class CustomAction extends TriggerAction
{
    private static final Logger logger = LoggerFactory.getLogger(CustomAction.class);
    
    private volatile String customCommand;

    public CustomAction(String customCommand)
    {
        super("custom_" + System.currentTimeMillis(), ActionType.EXECUTE_COMMAND, "Execute: " + customCommand);
        this.customCommand = customCommand != null ? customCommand : "";
    }
    
    /**
     * Copy constructor
     */
    private CustomAction(CustomAction other)
    {
        super(other);
        this.customCommand = other.customCommand;
    }

    @Override
    protected void executeAction(GearSwapperPlugin plugin, Client client, TriggerEvent event)
    {
        if (customCommand == null || customCommand.trim().isEmpty())
        {
            logger.warn("[Custom Action] Empty command, skipping execution");
            return;
        }
        
        logger.info("[Trigger Action] Executing custom command: {}", customCommand);
        
        // Parse and execute the custom command
        String command = customCommand.trim();
        
        if (command.startsWith("Prayer:"))
        {
            // Handle prayer activation using plugin method
            String prayerName = command.substring(7).trim();
            String[] prayers = {prayerName};
            plugin.applyPrayersForTrigger(prayers);
            logger.info("[Trigger Action] Activated prayer: {}", prayerName);
        }
        else if (command.startsWith("Spell:"))
        {
            // Handle spell casting using plugin method
            String spellName = command.substring(6).trim();
            String[] spells = {spellName};
            plugin.castSpellForTrigger(spells);
            logger.info("[Trigger Action] Cast spell: {}", spellName);
        }
        else if (command.startsWith("Attack"))
        {
            // Handle attack action using plugin method
            plugin.attackForTrigger();
            logger.info("[Trigger Action] Attacked current target");
        }
        else if (command.startsWith("Gear:"))
        {
            // Handle gear switching by type - ACTIVATE the matching loadout
            String gearType = command.substring(5).trim();
            for (int i = 0; i < 10; i++)
            {
                String loadoutName = plugin.getLoadoutNameForTrigger(i);
                if (loadoutName != null && loadoutName.toLowerCase().contains(gearType.toLowerCase()))
                {
                    logger.info("[Trigger Action] Switching to gear set: {} (matches type: {})", loadoutName, gearType);
                    try
                    {
                        plugin.activateGearSetByName(loadoutName);
                        logger.info("[Trigger Action] Successfully activated gear set: {}", loadoutName);
                        return; // Stop after first match
                    }
                    catch (Exception e)
                    {
                        logger.error("[Trigger Action] Failed to activate gear set '{}': {}", loadoutName, e.getMessage(), e);
                        // Continue searching
                    }
                }
            }
            // If no loadout matched by name
            logger.warn("[Trigger Action] No loadout found matching type: {}", gearType);
        }
        else
        {
            // Generic command execution (no weapon/item matching)
            logger.info("[Trigger Action] Executing generic command: {}", command);
        }
    }
    
    // Getters and setters with validation
    public String getCustomCommand() { return customCommand; }
    
    public void setCustomCommand(String customCommand)
    {
        this.customCommand = customCommand != null ? customCommand : "";
        setDescription("Execute: " + this.customCommand);
    }
    
    @Override
    public boolean isValid()
    {
        if (!super.isValid()) return false;
        return customCommand != null && !customCommand.trim().isEmpty();
    }
    
    @Override
    public String getValidationError()
    {
        String baseError = super.getValidationError();
        if (baseError != null) return baseError;
        
        if (customCommand == null || customCommand.trim().isEmpty())
            return "Custom command cannot be null or empty";
            
        return null; // No error
    }
    
    @Override
    public TriggerAction copy()
    {
        return new CustomAction(this);
    }
}
