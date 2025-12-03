package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.magic.spellbooks.Ancient;
import com.tonic.plugins.lmsnavigator.GetMode;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.Random;
import java.util.List;

public class BothUnfrozen
{

    private static Client client;
    private static final Random RANDOM = new Random();

    public static void setClient(Client c)
    {
        client = c;
    }

    /**
     * Tick handler for the BothUnfrozen state.
     *
     * Rules:
     * - Attack when our CD is 1 or lower.
     * - If our CD is higher than 1 we equip tank gear.
     * - Both unfrozen (general): 80% magic, 20% range.
     * - Both unfrozen and within combat range (<= 4 tiles):
     *   65% magic, 25% melee, 10% range.
     * - If our and their attack timers are synced, use tank mage loadout; otherwise normal mage.
     * - For magic casts: use Ice Barrage; if boosted magic < 94, use Ice Blitz instead.
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

        // Only act in the BOTH_UNFROZEN combat state
        CombatState state = FightStateManager.getCurrentState();
        if (state != CombatState.BOTH_UNFROZEN)
        {
            return;
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

        if (maybePotOffence())
        {
            return;
        }

        // Auto-eat Shark when HP is low or when we've reached the -3 cooldown window
        if (maybeEatShark(55, playerCd))
        {
            return;
        }

        // If our CD is higher than 1, equip tank gear and optionally move around target
        if (playerCd > 1)
        {
            equipTankWaiting();

            // 65% chance each tick to move up to 5 tiles around the target
            int rollHighCd = RANDOM.nextInt(100);
            if (rollHighCd < 65)
            {
                WorldPoint targetPos = target.getWorldLocation();
                if (targetPos != null)
                {
                    int maxOffset = 5;
                    int offsetX = RANDOM.nextInt(maxOffset * 2 + 1) - maxOffset; // [-5, 5]
                    int offsetY = RANDOM.nextInt(maxOffset * 2 + 1) - maxOffset; // [-5, 5]

                    int destX = targetPos.getX() + offsetX;
                    int destY = targetPos.getY() + offsetY;

                    MovementAPI.walkToWorldPoint(destX, destY);
                }
            }

            return;
        }

        // Attack when CD is 1 or lower
        int opponentCd = AttackTimers.getTargetCooldown();
        int ourTicks = Math.max(0, playerCd);
        int theirTicks = Math.max(0, opponentCd);
        boolean attacksSynced = (ourTicks == theirTicks);

        int distance = getDistance(local, target);
        boolean inCombatRange = distance >= 0 && distance <= 4;

        int roll = RANDOM.nextInt(100);

        if (inCombatRange)
        {
            // Both unfrozen in combat range (4 tiles or closer):
            // 65% magic, 25% melee, 10% ranged
            if (roll < 65)
            {
                attackWithMagic(attacksSynced, target);
            }
            else if (roll < 90)
            {
                attackWithMelee(target);
            }
            else
            {
                attackWithRange(target);
            }
        }
        else
        {
            // Both unfrozen (general): 80% magic, 20% range
            if (roll < 80)
            {
                attackWithMagic(attacksSynced, target);
            }
            else
            {
                attackWithRange(target);
            }
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

    private static int getDistance(Player a, Player b)
    {
        if (a == null || b == null)
        {
            return -1;
        }

        WorldPoint ap = a.getWorldLocation();
        WorldPoint bp = b.getWorldLocation();
        if (ap == null || bp == null)
        {
            return -1;
        }

        int dx = Math.abs(ap.getX() - bp.getX());
        int dy = Math.abs(ap.getY() - bp.getY());
        return Math.max(dx, dy); // Chebyshev distance
    }

    private static void attackWithMagic(boolean useTankMage, Player target)
    {
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

        // Cast Ice Barrage (or Ice Blitz if magic < 94) on the target
        castIceSpell(target);
    }

    private static void attackWithRange(Player target)
    {
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

    private static void attackWithMelee(Player target)
    {
        if (maybeUseSpecMelee(target))
        {
            return;
        }

        if (GetMode.isMaxMed())
        {
            GearManagement.equipMaxMedMelee();
        }
        else if (GetMode.isZerker())
        {
            GearManagement.equipZekerMelee();
        }
        else if (GetMode.isOneDefPure())
        {
            GearManagement.equipOneDefMelee();
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
        // 1 def pure has no dedicated tank loadout defined; leave gear as-is for now.
    }

    /**
     * Cast Ice Barrage on the target, falling back to Ice Blitz when boosted
     * magic level is less than 94. Mirrors the behavior from AttackTimer.
     */
    private static void castIceSpell(Player target)
    {
        if (target == null)
        {
            return;
        }

        int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);

        // If magic level is less than 94, try to cast Ice Blitz instead
        if (boostedMagic < 94)
        {
            if (Ancient.ICE_BLITZ.canCast())
            {
                int spellWidgetId = Ancient.ICE_BLITZ.getWidget();
                Static.invoke(() -> WidgetAPI.onPlayer(spellWidgetId, -1, -1, target.getId(), false));
                return;
            }
            // If we can't cast blitz, fall through and try barrage (in case runes/level allow it)
        }

        // Magic level is 94 or higher (or blitz failed) - cast Ice Barrage if possible
        if (!Ancient.ICE_BARRAGE.canCast())
        {
            return;
        }

        int spellWidgetId = Ancient.ICE_BARRAGE.getWidget();
        Static.invoke(() -> WidgetAPI.onPlayer(spellWidgetId, -1, -1, target.getId(), false));
    }

    private static boolean maybeEatShark(int hpThreshold, int playerCd)
    {
        int currentHp = AttackTimers.getPlayerHpFromOrb();

        boolean shouldEat = false;

        // Global rule: when we hit -3 CD, eat a shark (if available)
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

    private static boolean maybePotOffence()
    {
        int strength = SkillAPI.getBoostedLevel(Skill.STRENGTH);
        int ranged = SkillAPI.getBoostedLevel(Skill.RANGED);

        if (strength != 99 || ranged != 99)
        {
            return false;
        }

        boolean acted = false;

        if (drinkSuperCombat())
        {
            acted = true;
        }

        if (drinkRangingPotion())
        {
            acted = true;
        }

        return acted;
    }

    static boolean maybeUseSpecMelee(Player target)
    {
        if (target == null)
        {
            return false;
        }

        int specEnergy = CombatAPI.getSpecEnergy();
        if (specEnergy < 25)
        {
            return false;
        }

        int roll = RANDOM.nextInt(100);
        if (roll >= 65)
        {
            return false;
        }

        if (GetMode.isMaxMed())
        {
            GearManagement.equipMaxMedSpec();
        }
        else if (GetMode.isZerker())
        {
            GearManagement.equipZekerSpec();
        }
        else if (GetMode.isOneDefPure())
        {
            GearManagement.equipOneDefSpec();
        }

        if (!CombatAPI.isSpecEnabled())
        {
            CombatAPI.toggleSpec();
        }

        attackTarget(target);
        return true;
    }

    private static boolean drinkSuperCombat()
    {
        List<ItemEx> inventoryItems = InventoryAPI.getItems();
        for (ItemEx item : inventoryItems)
        {
            if (item == null || item.getName() == null)
            {
                continue;
            }

            String itemName = item.getName();
            if (!itemName.toLowerCase().startsWith("super combat"))
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

    private static boolean drinkRangingPotion()
    {
        List<ItemEx> inventoryItems = InventoryAPI.getItems();
        for (ItemEx item : inventoryItems)
        {
            if (item == null || item.getName() == null)
            {
                continue;
            }

            String itemName = item.getName();
            if (!itemName.toLowerCase().startsWith("ranging potion"))
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
}
