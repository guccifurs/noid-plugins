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
import net.runelite.api.coords.WorldPoint;

import java.util.Random;

public class WeUnfrozenTargetFrozen
{
    // Placeholder for WeUnfrozenTargetFrozen fight logic

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
        if (state != CombatState.WE_FROZEN_TARGET_UNFROZEN)
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

        // Auto-eat Shark when HP is low or when we've reached the -3 cooldown window.
        // For this state the -3 rule is explicitly required.
        if (maybeEatShark(60, playerCd))
        {
            return;
        }

        if (playerCd > 1)
        {
            equipTankWaiting();
            return;
        }

        int targetCd = AttackTimers.getTargetCooldown();
        int theirTicks = Math.max(0, targetCd);

        WorldPoint ourPos = local.getWorldLocation();
        WorldPoint targetPos = target.getWorldLocation();

        boolean inMeleeRange = false;
        if (ourPos != null && targetPos != null)
        {
            int dx = Math.abs(ourPos.getX() - targetPos.getX());
            int dy = Math.abs(ourPos.getY() - targetPos.getY());
            int chebyshevDistance = Math.max(dx, dy);
            boolean diagonal = (dx == 1 && dy == 1);
            inMeleeRange = (chebyshevDistance < 2) && !diagonal;
        }

        int roll = RANDOM.nextInt(100);

        if (inMeleeRange)
        {
            // In melee range: 55% melee, 30% magic, 15% range
            if (roll < 55)
            {
                attackWithMelee(target);
                return;
            }

            if (roll < 85)
            {
                // Magic: use full mage gear only if target's cooldown is more than 1 tick away.
                // Otherwise, stay in tank mage gear ("tank barrage").
                boolean useTankMage = theirTicks <= 1;
                attackWithMagic(useTankMage, target);
                return;
            }

            attackWithRange(target);
            return;
        }

        // Not in melee range: only magic / range
        int nonMeleeRoll = RANDOM.nextInt(100);

        // Preserve relative weighting of 30% magic vs 15% range (2:1 ratio)
        if (nonMeleeRoll < 67)
        {
            boolean useTankMage = theirTicks <= 1;
            attackWithMagic(useTankMage, target);
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
