package com.tonic.plugins.lmsnavigator.FightLogic;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.magic.spellbooks.Ancient;
import com.tonic.plugins.lmsnavigator.GetMode;
import com.tonic.plugins.lmsnavigator.TargetManagement;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.Random;

public class TargetFrozenWeUnfrozen
{
    // Placeholder for TargetFrozenWeUnfrozen fight logic

    private static Client client;
    private static final Random RANDOM = new Random();

    public static void setClient(Client c)
    {
        client = c;
    }

    public static void onGameTick()
    {
        if (client == null)
        {
            return;
        }

        if (!GetMode.hasGameMode())
        {
            return;
        }

        CombatState state = FightStateManager.getCurrentState();
        if (state != CombatState.TARGET_FROZEN_WE_UNFROZEN)
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

        // Auto-eat Shark when HP is low or when we've reached the -3 cooldown window
        if (maybeEatShark(84, playerCd))
        {
            return;
        }

        // If our CD is higher than 1, equip tank gear and stand under target while we wait
        if (playerCd > 1)
        {
            equipTankWaiting();
            walkUnderTarget(target);
            return;
        }

        // From here on, CD is 1 or lower: we always attack in this state
        int opponentCd = AttackTimers.getTargetCooldown();
        int ourTicks = Math.max(0, playerCd);
        int theirTicks = Math.max(0, opponentCd);

        // Approximate "free hit" window based on AttackTimer's logic:
        // treat it as a free hit if opponent's timer is at least 2 ticks
        // ahead of ours.
        boolean hasFreeHit = (theirTicks - ourTicks) >= 2;

        // If we have a free hit, we are allowed to mage in this state
        if (hasFreeHit)
        {
            attackWithMagic(false, target);
            return;
        }

        // No free hit: 55% melee, 45% bolts (range)
        int roll = RANDOM.nextInt(100);
        if (roll < 55)
        {
            attackWithMelee(target);
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
        return Math.max(dx, dy);
    }

    private static void attackWithMagic(boolean useTankMage, Player target)
    {
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
            GearManagement.equipOneDefMage();
        }

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
        if (BothUnfrozen.maybeUseSpecMelee(target))
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
        // 1 def pure: keep current gear
    }

    private static void castIceSpell(Player target)
    {
        if (target == null)
        {
            return;
        }

        int boostedMagic = SkillAPI.getBoostedLevel(Skill.MAGIC);

        if (boostedMagic < 94)
        {
            if (Ancient.ICE_BLITZ.canCast())
            {
                int spellWidgetId = Ancient.ICE_BLITZ.getWidget();
                Static.invoke(() -> WidgetAPI.onPlayer(spellWidgetId, -1, -1, target.getId(), false));
                return;
            }
        }

        if (!Ancient.ICE_BARRAGE.canCast())
        {
            return;
        }

        int spellWidgetId = Ancient.ICE_BARRAGE.getWidget();
        Static.invoke(() -> WidgetAPI.onPlayer(spellWidgetId, -1, -1, target.getId(), false));
    }

    private static void walkUnderTarget(Player target)
    {
        if (target == null)
        {
            return;
        }

        WorldPoint targetLocation = target.getWorldLocation();
        if (targetLocation == null)
        {
            return;
        }

        MovementAPI.walkToWorldPoint(targetLocation.getX(), targetLocation.getY());
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
}
