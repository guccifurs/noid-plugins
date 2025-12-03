package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import com.tonic.plugins.lmsnavigator.FightLogic.LmsState;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.queries.NpcQuery;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.CombatAPI;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class TargetManagement
{
    // Client reference for local player checks
    private static Client client;
    
    // Target tracking
    private static String currentTargetName = "Unknown";
    private static boolean hasTarget = false;
    private static long lastTargetUpdate = 0;
    private static final long TARGET_TIMEOUT_MS = 15000; // 15 seconds timeout;

    // ActorEx-based aggressive targeting (locked until death / override on attacker)
    private static ActorEx<?> currentTargetEx;
    private static boolean lockedTarget = false;

    // Target state flags
    private static boolean isInCombat = false;
    private static boolean isAttacking = false;
    private static boolean isBeingAttacked = false;
    
    /**
     * Set client reference for local player checks
     */
    public static void setClient(Client client)
    {
        TargetManagement.client = client;
        Logger.norm("[TargetManagement] Client reference set");
    }
    
    /**
     * Check if a player is the local player
     */
    private static boolean isLocalPlayer(Player player)
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return false;
        }
        return player == client.getLocalPlayer();
    }
    
    /**
     * Process player interaction changes to detect combat targets
     */
    public static void onInteractingChanged(InteractingChanged event)
    {
        Actor source = event.getSource();
        Actor target = event.getTarget();
        
        // Check if the local player is involved in the interaction
        if (source != null && source instanceof Player)
        {
            Player player = (Player) source;
            if (isLocalPlayer(player))
            {
                // Local player is interacting with someone
                if (target != null && target instanceof Player)
                {
                    Player targetPlayer = (Player) target;

                    // Do not re-target players that are currently ignored due to
                    // 'already fighting' or other ignore reasons.
                    if (LmsState.isIgnored(targetPlayer))
                    {
                        Logger.norm("[TargetManagement] Local player interacted with ignored player: " + targetPlayer.getName() + " - not setting target.");
                    }
                    else
                    {
                        setTarget(targetPlayer.getName(), true);
                        Logger.norm("[TargetManagement] Local player interacting with: " + targetPlayer.getName());
                    }
                }
                else
                {
                    // Local player stopped interacting - don't clear immediately, let timeout handle it
                    Logger.norm("[TargetManagement] Local player stopped interacting - keeping target with timeout");
                }
            }
        }
        
        // Check if local player is being targeted
        if (target != null && target instanceof Player)
        {
            Player player = (Player) target;
            if (isLocalPlayer(player))
            {
                // Someone is interacting with local player
                if (source != null && source instanceof Player)
                {
                    Player sourcePlayer = (Player) source;
                    setTarget(sourcePlayer.getName(), true);
                    Logger.norm("[TargetManagement] Being targeted by: " + sourcePlayer.getName());
                }
            }
        }
    }
    
    /**
     * Process game tick to check target timeout
     */
    public static void onGameTick(GameTick event)
    {
        if (client == null)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            clearTarget();
            return;
        }
        
        ActorEx<?> targetEx = updateTargetingEx();
        if (targetEx == null)
        {
            clearTarget();
            return;
        }

        applyExTarget(targetEx);

        // Do NOT clear just because interaction briefly stops.
        // We now rely on:
        //  - distance based clearing in LMSNavigatorPlugin (>= 18 tiles)
        //  - chat-based 'already fighting' handling (which ignores targets)
        //  - death / disappearance checks above
        // This avoids losing valid targets mid-fight.
    }
    
    /**
     * Set the current target
     */
    private static void setTarget(String targetName, boolean inCombat)
    {
        if (targetName == null || targetName.trim().isEmpty())
        {
            clearTarget();
            return;
        }
        
        String newTargetName = targetName.trim();
        
        // Only update if target actually changed
        if (!newTargetName.equals(currentTargetName))
        {
            Logger.norm("[TargetManagement] Target changed from '" + currentTargetName + "' to '" + newTargetName + "'");
            currentTargetName = newTargetName;
        }
        
        hasTarget = true;
        isInCombat = inCombat;
        lastTargetUpdate = System.currentTimeMillis();
        
        Logger.norm("[TargetManagement] Target set: " + currentTargetName + " (in combat: " + inCombat + ")");
    }

    public static void setTargetPlayer(Player player)
    {
        if (player == null)
        {
            clearTarget();
            return;
        }

        ActorEx<?> ex = ActorEx.fromActor(player);
        if (ex == null)
        {
            clearTarget();
            return;
        }

        // Force a locked target using the aggressive ActorEx-based system.
        lockedTarget = true;
        applyExTarget(ex);
    }
    
    /**
     * Clear the current target
     */
    public static void clearTarget()
    {
        if (hasTarget)
        {
            Logger.norm("[TargetManagement] Target cleared: " + currentTargetName);
        }
        
        currentTargetName = "Unknown";
        hasTarget = false;
        isInCombat = false;
        isAttacking = false;
        isBeingAttacked = false;
        lastTargetUpdate = 0;
        currentTargetEx = null;
        lockedTarget = false;
    }
    
    /**
     * Get current target name
     */
    public static String getCurrentTargetName()
    {
        return currentTargetName;
    }
    
    /**
     * Check if player has a target
     */
    public static boolean hasTarget()
    {
        return hasTarget;
    }
    
    /**
     * Check if player is in combat
     */
    public static boolean isInCombat()
    {
        return isInCombat;
    }
    
    /**
     * Check if player is attacking someone
     */
    public static boolean isAttacking()
    {
        return isAttacking;
    }
    
    /**
     * Check if player is being attacked
     */
    public static boolean isBeingAttacked()
    {
        return isBeingAttacked;
    }
    
    /**
     * Get target information as formatted string
     */
    public static String getTargetInfo()
    {
        if (!hasTarget)
        {
            return "No target";
        }
        
        return currentTargetName + (isInCombat ? " (In Combat)" : "");
    }
    
    /**
     * Reset all target data (for new games)
     */
    public static void reset()
    {
        Logger.norm("[TargetManagement] Resetting all target data");
        clearTarget();
    }
    
    /**
     * Force update target state based on current interactions
     */
    public static void updateTargetState()
    {
        // This can be called to manually refresh target state
        if (hasTarget && System.currentTimeMillis() - lastTargetUpdate > TARGET_TIMEOUT_MS)
        {
            clearTarget();
        }
    }

    // === Ignore handling (delegates to LmsState) ===

    /**
     * Ignore a target for a given number of seconds.
     */
    public static void ignoreTargetForSeconds(Player target, int seconds)
    {
        LmsState.ignoreTargetForSeconds(target, seconds);
        Logger.norm("[TargetManagement] Ignoring " + (target != null ? target.getName() : "null") + " for " + seconds + "s");
    }

    /**
     * Check if a target is currently ignored.
     */
    public static boolean isIgnored(Player target)
    {
        return LmsState.isIgnored(target);
    }

    /**
     * Clean up expired ignores (called each tick).
     */
    public static void cleanupExpiredIgnores()
    {
        LmsState.cleanupExpiredIgnores();
    }

    /**
     * Get the number of active ignores (for WaitIgnoreTask).
     */
    public static int getIgnoreCount()
    {
        return LmsState.getIgnoreSize();
    }

    // === Aggressive locked-target helpers (ActorEx-based) ===

    public static ActorEx<?> updateTargetingEx()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            currentTargetEx = null;
            lockedTarget = false;
            return null;
        }

        // If we died, clear target.
        if (client.getLocalPlayer().getHealthRatio() == 0)
        {
            currentTargetEx = null;
            lockedTarget = false;
            return null;
        }

        // If current target is dead, clear it.
        if (currentTargetEx != null && currentTargetEx.isDead())
        {
            currentTargetEx = null;
            lockedTarget = false;
        }

        // HIGH PRIORITY: attacker override – whoever is attacking us becomes target.
        ActorEx<?> attacker = getOurAttackerEx();
        if (attacker != null)
        {
            currentTargetEx = attacker;
            lockedTarget = true;
            return currentTargetEx;
        }

        // If we have a locked target, keep it (no switching) until death/clear.
        if (lockedTarget && currentTargetEx != null)
        {
            return currentTargetEx;
        }

        // No locked target – try to acquire a new reachable, non-ignored player target.
        ActorEx<?> newTarget = findAcquirablePlayerTargetEx();
        if (newTarget != null)
        {
            currentTargetEx = newTarget;
            lockedTarget = true;
            return currentTargetEx;
        }

        return null;
    }

    private static ActorEx<?> getOurAttackerEx()
    {
        PlayerEx localEx = PlayerEx.getLocal();
        if (localEx == null)
        {
            return null;
        }

        // Scan players directly: anyone whose interacting target is us and who is not idle.
        PlayerQuery pq = new PlayerQuery()
            .keepIf(p -> p != null && !p.equals(localEx))
            .keepIf(p -> p.getInteracting() != null && p.getInteracting().equals(localEx))
            .keepIf(p -> !p.isIdle() || p.healthBarVisible());

        return pq.nearest();
    }

    private static ActorEx<?> findAcquirablePlayerTargetEx()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return null;
        }

        // Only acquire targets in PvP areas (LMS).
        if (!CombatAPI.isInPvpArea())
        {
            return null;
        }

        PlayerEx localEx = PlayerEx.getLocal();
        if (localEx == null)
        {
            return null;
        }

        List<ActorEx<?>> candidates = new ArrayList<>();

        PlayerQuery pq = new PlayerQuery()
            .keepIf(p -> p != null && !p.equals(localEx))
            .keepIf(p -> p.canAttack() && !p.isDead())
            .keepIf(p -> !isInCombatWithSomeoneElse(p))
            .keepIf(p -> !LmsState.isIgnored(p.getPlayer()))
            .keepIf(TargetManagement::isReachableEx);

        for (PlayerEx p : pq.collect())
        {
            candidates.add(p);
        }

        ActorEx<?> best = null;
        double bestDist = Double.MAX_VALUE;
        for (ActorEx<?> cand : candidates)
        {
            double d = distanceToLocal(cand);
            if (d < bestDist)
            {
                bestDist = d;
                best = cand;
            }
        }
        return best;
    }

    private static boolean isInCombatWithSomeoneElse(ActorEx<?> target)
    {
        if (target == null)
        {
            return false;
        }

        ActorEx<?> interacting = target.getInteracting();
        if (interacting == null)
        {
            return false;
        }

        PlayerEx localEx = PlayerEx.getLocal();
        if (localEx == null)
        {
            return false;
        }

        // If the target is interacting with someone who is NOT us, they're in combat with someone else.
        return !interacting.equals(localEx);
    }

    private static boolean isReachableEx(ActorEx<?> target)
    {
        if (target == null)
        {
            return false;
        }

        WorldPoint targetPos = target.getWorldPoint();
        if (targetPos == null)
        {
            return false;
        }

        if (client != null && client.getLocalPlayer() != null)
        {
            WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
            if (localPos != null)
            {
                double dist = localPos.distanceTo(targetPos);
                if (dist > 20.0)
                {
                    return false;
                }
            }
        }

        try
        {
            return MovementAPI.canPathTo(targetPos);
        }
        catch (Exception e)
        {
            // Fallback: allow if very close.
            return distanceToLocal(target) <= 8.0;
        }
    }

    private static double distanceToLocal(ActorEx<?> target)
    {
        if (target == null || client == null || client.getLocalPlayer() == null)
        {
            return Double.MAX_VALUE;
        }

        WorldPoint localPos = client.getLocalPlayer().getWorldLocation();
        WorldPoint targetPos = target.getWorldPoint();
        if (localPos == null || targetPos == null)
        {
            return Double.MAX_VALUE;
        }
        return localPos.distanceTo(targetPos);
    }

    private static void applyExTarget(ActorEx<?> targetEx)
    {
        if (targetEx == null)
        {
            clearTarget();
            return;
        }

        currentTargetEx = targetEx;

        String name = targetEx.getName();
        if (name == null || name.trim().isEmpty())
        {
            name = "Unknown";
        }

        String oldName = currentTargetName;
        currentTargetName = name;
        hasTarget = true;
        isInCombat = true;
        lastTargetUpdate = System.currentTimeMillis();

        if (!currentTargetName.equals(oldName))
        {
            Logger.norm("[TargetManagement] Target changed from '" + oldName + "' to '" + currentTargetName + "'");
        }
    }
}
