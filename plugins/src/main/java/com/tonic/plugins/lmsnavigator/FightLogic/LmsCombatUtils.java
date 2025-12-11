package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.widgets.PrayerAPI;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import static com.tonic.api.widgets.PrayerAPI.*;

/**
 * Utility class for Last Man Standing specific combat operations.
 */
public class LmsCombatUtils {
    
    /**
     * Activates the appropriate protection prayer based on the opponent's combat style.
     * @param style The detected combat style of the opponent
     */
    public static void activateProtectionPrayer(CombatStyle style) {
        // Turn off all protection prayers first to prevent conflicts
        PROTECT_FROM_MELEE.turnOff();
        PROTECT_FROM_MISSILES.turnOff();
        PROTECT_FROM_MAGIC.turnOff();
        
        // Activate the appropriate prayer
        switch (style) {
            case MELEE:
                PROTECT_FROM_MELEE.turnOn();
                Logger.norm("[LMS Combat] Activated Protect from Melee");
                break;
            case RANGED:
                PROTECT_FROM_MISSILES.turnOn();
                Logger.norm("[LMS Combat] Activated Protect from Missiles");
                break;
            case MAGIC:
                PROTECT_FROM_MAGIC.turnOn();
                Logger.norm("[LMS Combat] Activated Protect from Magic");
                break;
        }
    }
    
    /**
     * Uses special attack if the player has enough energy and is in melee range.
     * @param target The target player
     * @return true if special attack was used, false otherwise
     */
    public static boolean useSpecialAttack(Player target) {
        if (target == null) return false;
        
        // Get client instance
        Client client = Static.getClient();
        if (client == null || client.getLocalPlayer() == null) return false;
        
        // Check if we have enough special attack energy
        if (CombatAPI.getSpecEnergy() < 50) {
            return false;
        }
        
        // Check if we're in melee range
        if (!isInMeleeRange(client, target)) {
            return false;
        }
        
        // Toggle special attack
        CombatAPI.toggleSpec();
        Logger.norm("[LMS Combat] Used special attack on " + target.getName());
        return true;
    }
    
    /**
     * Checks if the player is in melee range of the target.
     * @param client The game client
     * @param target The target player
     * @return true if in melee range, false otherwise
     */
    public static boolean isInMeleeRange(Client client, Player target) {
        if (client == null || client.getLocalPlayer() == null || target == null) {
            return false;
        }
        
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        WorldPoint targetPos = target.getWorldLocation();
        
        if (playerPos == null || targetPos == null) {
            return false;
        }
        
        return Math.abs(playerPos.getX() - targetPos.getX()) <= 1 && 
               Math.abs(playerPos.getY() - targetPos.getY()) <= 1;
    }
    
    /**
     * Gets the optimal combat prayer based on the current situation.
     * @return The optimal PrayerAPI enum value, or null if none applicable
     */
    public static PrayerAPI getOptimalCombatPrayer() {
        // Try to get the highest level prayer available
        PrayerAPI prayer = PrayerAPI.getMeleePrayer();
        if (prayer == null) {
            prayer = PrayerAPI.getMeleeAttackPrayer();
        }
        if (prayer == null) {
            prayer = PrayerAPI.getMeleeStrengthPrayer();
        }
        if (prayer == null) {
            prayer = PrayerAPI.getMeleeDefensePrayer();
        }
        
        return prayer;
    }
    
    /**
     * Activates the optimal combat prayer for the current situation.
     */
    public static void activateOptimalCombatPrayer() {
        PrayerAPI prayer = getOptimalCombatPrayer();
        if (prayer != null) {
            prayer.turnOn();
            Logger.norm("[LMS Combat] Activated combat prayer: " + prayer.name());
        }
    }
}
