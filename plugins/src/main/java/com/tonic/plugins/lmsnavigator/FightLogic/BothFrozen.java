package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.magic.spellbooks.Ancient;
import com.tonic.plugins.lmsnavigator.GetMode;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;

import java.util.Random;
import java.util.List;
import com.tonic.data.wrappers.ItemEx;

public class BothFrozen
{

    private static Client client;
    private static final Random RANDOM = new Random();
    private static int brewSipsThisFrozen = 0;
    private static boolean wasInStateLastTick = false;

    public static void setClient(Client c)
    {
        client = c;
    }

    /**
     * Tick handler for the BothFrozen state (non-melee distance).
     *
     * Rules:
     * - Only runs when CombatState is BOTH_FROZEN.
     * - Attack when our CD is 1 or lower.
     * - 65% magic (blood barrage / blood blitz), 35% ranged.
     * - If our and their attack timers are synced, use tank mage loadout; otherwise normal mage.
     */
    public static void onGameTick()
    {
        if (client == null)
        {
            return;
        }

        if (!GetMode.hasGameMode())
        {
            return; // We don't know which loadouts to use yet
        }

        // Only act in the BOTH_FROZEN combat state (non-melee distance)
        CombatState state = FightStateManager.getCurrentState();
        if (state != CombatState.BOTH_FROZEN)
        {
            wasInStateLastTick = false;
            return;
        }

        if (!wasInStateLastTick)
        {
            brewSipsThisFrozen = 0;
            wasInStateLastTick = true;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        Player target = resolveTarget();
        if (target == null)
        {
            return;
        }

        int playerCd = AttackTimers.getPlayerCooldown();

        if (maybeHandleBrewsAndRestores())
        {
            return;
        }

        // Auto-eat Shark when HP is low or when we've reached the -3 cooldown window
        if (maybeEatShark(75, playerCd))
        {
            return;
        }

        // If our CD is higher than 1, equip tank gear and wait (no movement possible while frozen)
        if (playerCd > 1)
        {
            equipTankWaiting();
            return;
        }

        // Attack when CD is 1 or lower
        int opponentCd = AttackTimers.getTargetCooldown();
        int ourTicks = Math.max(0, playerCd);
        int theirTicks = Math.max(0, opponentCd);
        boolean attacksSynced = (ourTicks == theirTicks);

        int roll = RANDOM.nextInt(100);

        // 65% magic, 35% range
        if (roll < 65)
        {
            attackWithBloodMagic(attacksSynced, target);
        }
        else
        {
            attackWithRange(target);
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

    private static void attackWithBloodMagic(boolean useTankMage, Player target)
    {
        if (target == null)
        {
            return;
        }

        // Equip appropriate mage gear for current mode
        if (GetMode.isMaxMed())
        {
            if (useTankMage)
            {
                GearManagement.equipMaxMedMageTank();
            }
            else
            {
                GearManagement.equipMaxMedMagic();
            }
        }
        else if (GetMode.isZerker())
        {
            if (useTankMage)
            {
                GearManagement.equipZekerMageTank();
            }
            else
            {
                GearManagement.equipZekerMagic();
            }
        }
        else if (GetMode.isOneDefPure())
        {
            // 1 def pure has only one mage setup
            GearManagement.equipOneDefMage();
        }

        castBloodSpell(target);
    }

    private static void attackWithRange(Player target)
    {
        if (target == null)
        {
            return;
        }

        if (GetMode.isMaxMed())
        {
            GearManagement.equipMaxMedRanged();
        }
        else if (GetMode.isZerker())
        {
            GearManagement.equipZekerRanged();
        }
        else if (GetMode.isOneDefPure())
        {
            GearManagement.equipOneDefRanged();
        }

        attackTarget(target);
    }

    private static void attackTarget(Player target)
    {
        if (target == null)
        {
            return;
        }

        TClient tClient = Static.getClient();
        if (tClient == null)
        {
            return;
        }

        int targetId = target.getId();
        Static.invoke(() -> tClient.getPacketWriter().playerActionPacket(1, targetId, false));
    }

    private static boolean maybeEatShark(int hpThreshold, int playerCd)
    {
        int currentHp = AttackTimers.getPlayerHpFromOrb();

        boolean shouldEat = false;

        if (currentHp <= 84 && (playerCd <= -3 || currentHp <= hpThreshold))
        {
            shouldEat = true;
        }

        if (!shouldEat)
        {
            return false;
        }

        if (!InventoryAPI.contains("Shark"))
        {
            return false;
        }

        InventoryAPI.interact("Shark", "eat");
        AttackTimers.onSharkEaten();
        return true;
    }

    private static boolean maybeHandleBrewsAndRestores()
    {
        if (brewSipsThisFrozen < 2)
        {
            if (sipSaradominBrew())
            {
                brewSipsThisFrozen++;
                return true;
            }
        }

        int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);
        if (boostedMagic < 99)
        {
            if (drinkSanfewSerum() || drinkSuperRestore())
            {
                return true;
            }
        }

        return false;
    }

    private static boolean sipSaradominBrew()
    {
        List<ItemEx> inventoryItems = InventoryAPI.getItems();
        for (ItemEx item : inventoryItems)
        {
            if (item == null || item.getName() == null)
            {
                continue;
            }

            String itemName = item.getName();
            if (!itemName.toLowerCase().startsWith("saradomin brew"))
            {
                continue;
            }

            String[] actions = item.getActions();
            if (actions == null)
            {
                continue;
            }

            for (String action : actions)
            {
                if (action != null && action.equalsIgnoreCase("Drink"))
                {
                    InventoryAPI.interact(item, "Drink");
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean drinkSanfewSerum()
    {
        List<ItemEx> inventoryItems = InventoryAPI.getItems();
        for (ItemEx item : inventoryItems)
        {
            if (item == null || item.getName() == null)
            {
                continue;
            }

            String itemName = item.getName();
            if (!itemName.toLowerCase().startsWith("sanfew serum"))
            {
                continue;
            }

            String[] actions = item.getActions();
            if (actions == null)
            {
                continue;
            }

            for (String action : actions)
            {
                if (action != null && action.equalsIgnoreCase("Drink"))
                {
                    InventoryAPI.interact(item, "Drink");
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean drinkSuperRestore()
    {
        List<ItemEx> inventoryItems = InventoryAPI.getItems();
        for (ItemEx item : inventoryItems)
        {
            if (item == null || item.getName() == null)
            {
                continue;
            }

            String itemName = item.getName();
            if (!itemName.toLowerCase().startsWith("super restore"))
            {
                continue;
            }

            String[] actions = item.getActions();
            if (actions == null)
            {
                continue;
            }

            for (String action : actions)
            {
                if (action != null && action.equalsIgnoreCase("Drink"))
                {
                    InventoryAPI.interact(item, "Drink");
                    return true;
                }
            }
        }
        return false;
    }

    private static void equipTankWaiting()
    {
        if (GetMode.isMaxMed())
        {
            GearManagement.equipMaxMedTank();
        }
        else if (GetMode.isZerker())
        {
            GearManagement.equipZekerTank();
        }
        // 1 def pure: keep current gear
    }

    /**
     * Cast Blood Barrage or Blood Blitz on the target, based on boosted magic
     * level. Mirrors the blood spell selection from AttackTimer.
     */
    private static void castBloodSpell(Player target)
    {
        if (target == null)
        {
            return;
        }

        int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);

        // If magic level is < 82, cannot cast blood spells - do nothing and let caller's
        // next tick potentially fall back to range via its roll.
        if (boostedMagic < 82)
        {
            return;
        }

        // If magic level is >= 94, cast Blood Barrage
        if (boostedMagic >= 94)
        {
            if (!Ancient.BLOOD_BARRAGE.canCast())
            {
                return;
            }

            int spellWidgetId = Ancient.BLOOD_BARRAGE.getWidget();
            Static.invoke(() -> WidgetAPI.onPlayer(spellWidgetId, -1, -1, target.getId(), false));
            return;
        }

        // Magic level is >= 82 and < 94 - cast Blood Blitz
        if (!Ancient.BLOOD_BLITZ.canCast())
        {
            return;
        }

        int spellWidgetId = Ancient.BLOOD_BLITZ.getWidget();
        Static.invoke(() -> WidgetAPI.onPlayer(spellWidgetId, -1, -1, target.getId(), false));
    }
}
