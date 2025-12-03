package com.tonic.plugins.lmsnavigator;

import com.tonic.api.widgets.InventoryAPI;
import net.runelite.api.gameval.ItemID;

public class KeyManagement
{
    private static final int BLOOD_KEY_ID = ItemID.BR_BLOODY_KEY; // Bloody key (LMS)
    
    /**
     * Check if the player currently has the Blood key in their inventory.
     */
    public static boolean hasBloodKey()
    {
        return InventoryAPI.contains(BLOOD_KEY_ID);
    }
}
