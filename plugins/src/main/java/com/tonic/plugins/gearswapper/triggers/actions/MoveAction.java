package com.tonic.plugins.gearswapper.triggers.actions;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import com.tonic.plugins.gearswapper.triggers.ActionType;
import com.tonic.plugins.gearswapper.triggers.TriggerAction;
import com.tonic.plugins.gearswapper.triggers.TriggerEvent;
import net.runelite.api.Client;

/**
 * Action to move a specific number of tiles away from the current target
 */
public class MoveAction extends TriggerAction
{
    private final int tilesToMove;
    
    public MoveAction(int tilesToMove)
    {
        super("move_" + tilesToMove + "_tiles", ActionType.MOVE, "Move " + tilesToMove + " tiles away from target");
        this.tilesToMove = tilesToMove;
    }
    
    /**
     * Copy constructor
     */
    protected MoveAction(MoveAction other)
    {
        super(other);
        this.tilesToMove = other.tilesToMove;
    }
    
    @Override
    protected void executeAction(GearSwapperPlugin plugin, Client client, TriggerEvent event)
    {
        System.out.println("[Move Action] === EXECUTION STARTED ===");
        System.out.println("[Move Action] Tiles to move: " + tilesToMove);

        if (plugin == null)
        {
            System.out.println("[Move Action] ERROR: Plugin is null");
            return;
        }

        try
        {
            // Delegate movement logic to the main plugin implementation
            plugin.executeMoveCommand(tilesToMove);
            System.out.println("[Move Action] Delegated to GearSwapperPlugin.executeMoveCommand");
            System.out.println("[Move Action] === EXECUTION COMPLETED ===");
        }
        catch (Exception e)
        {
            System.out.println("[Move Action] ERROR: Exception during execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public TriggerAction copy()
    {
        return new MoveAction(this);
    }
    
    public int getTilesToMove()
    {
        return tilesToMove;
    }
    
    @Override
    public String toString()
    {
        return getDescription();
    }
}
