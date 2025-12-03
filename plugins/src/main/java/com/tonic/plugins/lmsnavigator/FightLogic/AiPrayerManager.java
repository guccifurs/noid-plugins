package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.plugins.lmsnavigator.LMSNavigatorPlugin;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;

import java.util.Random;

/**
 * Lightweight AI prayer manager for LMS Navigator.
 * Uses AttackTimers + FreezeManager + distance to choose overheads.
 */
public class AiPrayerManager
{
    private static final Random RANDOM = new Random();
    private static Client client;

    public static void setClient(Client c)
    {
        client = c;
        Logger.norm("[AiPrayerManager] Client reference set");
    }

    public static void onGameTick()
    {
        if (client == null)
        {
            return;
        }

        LMSNavigatorPlugin plugin = LMSNavigatorPlugin.getPlugin();
        if (plugin == null || !plugin.isInInstance())
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        Player target = resolveTarget();

        // If no combat target, just ensure a range/mage overhead is on while in instance.
        if (target == null)
        {
            ensureBaseOverheadIdle();
            return;
        }

        int targetCd = AttackTimers.getTargetCooldown();

        // Only switch 1 tick before they can attack, or occasionally while ready (0 or less).
        boolean shouldRoll = false;
        if (targetCd == 1)
        {
            shouldRoll = true;
        }
        else if (targetCd <= 0)
        {
            // 35% chance each tick to choose a new prayer when they are ready but not attacking.
            if (RANDOM.nextInt(100) < 35)
            {
                shouldRoll = true;
            }
        }

        if (!shouldRoll)
        {
            return;
        }

        WorldPoint ourPos = local.getWorldLocation();
        WorldPoint targetPos = target.getWorldLocation();
        if (ourPos == null || targetPos == null)
        {
            return;
        }

        int dx = Math.abs(ourPos.getX() - targetPos.getX());
        int dy = Math.abs(ourPos.getY() - targetPos.getY());
        int chebyshevDistance = Math.max(dx, dy);
        boolean diagonal = (dx == 1 && dy == 1);

        boolean weFrozen = FreezeManager.getPlayerFreezeTicks() > 0;
        boolean targetFrozen = FreezeManager.getTargetFreezeTicks() > 0;

        int meleeWeight;
        int rangeWeight;
        int mageWeight;

        // Base percentages from user spec.
        if (weFrozen && targetFrozen)
        {
            // Both frozen
            boolean closeNonDiagonal = (chebyshevDistance <= 1) && !diagonal;
            if (closeNonDiagonal)
            {
                // Both frozen, target closer than 2 tiles and not diagonal: 33/33/33
                meleeWeight = 33;
                rangeWeight = 33;
                mageWeight = 33;
            }
            else
            {
                // Both frozen, target further than 1 tile: 0 melee, 50 range, 50 mage
                meleeWeight = 0;
                rangeWeight = 50;
                mageWeight = 50;
            }
        }
        else if (!weFrozen && !targetFrozen)
        {
            // Both unfrozen
            if (chebyshevDistance < 4)
            {
                // Both unfrozen, target closer than 4 tiles: 33/33/33
                meleeWeight = 33;
                rangeWeight = 33;
                mageWeight = 33;
            }
            else
            {
                // Both unfrozen, target further than 4 tiles: 0 melee, 50 range, 50 mage
                meleeWeight = 0;
                rangeWeight = 50;
                mageWeight = 50;
            }
        }
        else if (!weFrozen && targetFrozen)
        {
            // Target frozen, we unfrozen: 33/33/33 regardless of distance
            meleeWeight = 33;
            rangeWeight = 33;
            mageWeight = 33;
        }
        else // weFrozen && !targetFrozen
        {
            // We frozen, target unfrozen
            if (chebyshevDistance <= 3)
            {
                // We frozen, target unfrozen, target closer than 4 (<=3): 33/33/33
                meleeWeight = 33;
                rangeWeight = 33;
                mageWeight = 33;
            }
            else
            {
                // We frozen, target unfrozen, target further than 3 tiles: 0 melee, 50 range, 50 mage
                meleeWeight = 0;
                rangeWeight = 50;
                mageWeight = 50;
            }
        }

        int[] weights = new int[]{meleeWeight, rangeWeight, mageWeight};

        // Apply weapon-based boost (+10% rebalanced from other styles).
        String weaponName = getOpponentWeaponName(target);
        if (weaponName != null && !weaponName.isEmpty())
        {
            String lower = weaponName.toLowerCase();

            if (containsAny(lower, "sword", "dagger", "scimitar", "abyssal"))
            {
                boostStyle(weights, 0); // melee
            }

            if (containsAny(lower, "bow", "dart", "knife"))
            {
                boostStyle(weights, 1); // range
            }

            if (containsAny(lower, "kodai", "staff"))
            {
                boostStyle(weights, 2); // mage
            }
        }

        int total = weights[0] + weights[1] + weights[2];
        if (total <= 0)
        {
            return;
        }

        int roll = RANDOM.nextInt(total);
        PrayerAPI prayer;

        if (roll < weights[0])
        {
            prayer = PrayerAPI.PROTECT_FROM_MELEE;
        }
        else if (roll < weights[0] + weights[1])
        {
            prayer = PrayerAPI.PROTECT_FROM_MISSILES;
        }
        else
        {
            prayer = PrayerAPI.PROTECT_FROM_MAGIC;
        }

        if (prayer != null && !prayer.isActive())
        {
            prayer.turnOn();
        }
    }

    private static Player resolveTarget()
    {
        Player target = FreezeManager.getCurrentTarget();

        if (target == null && TargetManagement.hasTarget() && client != null)
        {
            String targetName = TargetManagement.getCurrentTargetName();
            if (targetName != null && !targetName.trim().isEmpty())
            {
                for (Player p : client.getPlayers())
                {
                    if (p == null)
                    {
                        continue;
                    }

                    String name = p.getName();
                    if (name != null && name.equals(targetName))
                    {
                        target = p;
                        break;
                    }
                }
            }
        }

        return target;
    }

    private static void ensureBaseOverheadIdle()
    {
        // Only enforce when no protection is active.
        boolean melee = PrayerAPI.PROTECT_FROM_MELEE.isActive();
        boolean range = PrayerAPI.PROTECT_FROM_MISSILES.isActive();
        boolean mage = PrayerAPI.PROTECT_FROM_MAGIC.isActive();

        if (melee || range || mage)
        {
            return;
        }

        // Prefer range/mage when idle; never force melee here.
        if (RANDOM.nextBoolean())
        {
            PrayerAPI.PROTECT_FROM_MISSILES.turnOn();
        }
        else
        {
            PrayerAPI.PROTECT_FROM_MAGIC.turnOn();
        }
    }

    private static boolean containsAny(String lowerName, String... patterns)
    {
        if (lowerName == null)
        {
            return false;
        }
        for (String p : patterns)
        {
            if (p != null && !p.isEmpty() && lowerName.contains(p.toLowerCase()))
            {
                return true;
            }
        }
        return false;
    }

    private static void boostStyle(int[] weights, int index)
    {
        if (weights == null || index < 0 || index >= weights.length)
        {
            return;
        }

        int boost = 10;
        int taken = 0;

        int other1 = (index + 1) % 3;
        int other2 = (index + 2) % 3;

        // Take up to half from first other style
        int available1 = Math.max(0, weights[other1]);
        int delta1 = Math.min(boost / 2, available1);
        weights[other1] -= delta1;
        taken += delta1;

        // Take remaining from second other style
        if (taken < boost)
        {
            int available2 = Math.max(0, weights[other2]);
            int delta2 = Math.min(boost - taken, available2);
            weights[other2] -= delta2;
            taken += delta2;
        }

        // Apply what we actually managed to steal.
        weights[index] += taken;
    }

    private static String getOpponentWeaponName(Player target)
    {
        if (client == null || target == null)
        {
            return null;
        }

        try
        {
            PlayerComposition comp = target.getPlayerComposition();
            if (comp == null)
            {
                return null;
            }

            int weaponId = comp.getEquipmentId(KitType.WEAPON);
            if (weaponId <= 0)
            {
                return null;
            }

            return Static.invoke(() -> {
                if (client == null)
                {
                    return null;
                }
                return client.getItemDefinition(weaponId).getName();
            });
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
