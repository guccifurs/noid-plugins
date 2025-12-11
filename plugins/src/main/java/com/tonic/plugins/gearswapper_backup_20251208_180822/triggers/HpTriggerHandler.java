package com.tonic.plugins.gearswapper.triggers;

import com.tonic.api.game.CombatAPI;
import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Actor;
import net.runelite.api.events.GameTick;
import java.util.Collection;

/**
 * Handler for HP-based triggers
 */
public class HpTriggerHandler implements TriggerHandler
{
    private final TriggerEngine engine;
    
    // Cooldown tracking for each trigger
    private java.util.Map<String, Long> lastTriggerTimes = new java.util.HashMap<>();
    private java.util.Map<String, Integer> consecutiveTrueCounts = new java.util.HashMap<>();

    @Inject
    public HpTriggerHandler(TriggerEngine engine)
    {
        this.engine = engine;
    }

    @Override
    public Client getClient()
    {
        // Use the client provided by the trigger engine
        return engine != null ? engine.getClient() : null;
    }

    @Override
    public boolean canHandle(TriggerEvent event)
    {
        return event.getType() == TriggerEventType.GAME_TICK;
    }

    @Override
    public void handleEvent(TriggerEvent event, Collection<Trigger> triggers)
    {
        if (event.getType() == TriggerEventType.GAME_TICK)
        {
            if (!(event.getSourceEvent() instanceof GameTick))
            {
                return;
            }

            Client client = getClient();
            if (client == null || client.getLocalPlayer() == null)
            {
                return;
            }

            // Get current HP values using VitaLite API with error handling
            int currentPlayerHp = 0;
            int currentPlayerMaxHp = 0;
            
            try
            {
                currentPlayerHp = CombatAPI.getHealth();
                currentPlayerMaxHp = client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
            }
            catch (Exception e)
            {
                // API call failed, skip this tick
                System.out.println("[HP Trigger] Error getting HP: " + e.getMessage());
                return;
            }
            
            // Get target HP if available - use same percentage method as debug overlay
            Actor target = getCachedTarget();
            int targetHealthPercentage = -1;
            
            if (target != null)
            {
                try {
                    targetHealthPercentage = com.tonic.Static.invoke(() -> {
                        // Use the same percentage calculation as debug overlay
                        int healthRatio = target.getHealthRatio();
                        int healthScale = target.getHealthScale();
                        
                        if (healthRatio >= 0 && healthScale > 0)
                        {
                            return (healthRatio * 100) / healthScale;
                        }
                        return -1;
                    });
                } catch (Exception e) {
                    targetHealthPercentage = -1;
                }
            }

            // Add HP data to event for potential use by actions
            event.addData("playerHp", currentPlayerHp);
            event.addData("playerMaxHp", currentPlayerMaxHp);
            event.addData("targetHpPercentage", targetHealthPercentage);
            event.addData("target", target);
            event.addData("playerHpPercentage", currentPlayerMaxHp > 0 ? (double)currentPlayerHp / currentPlayerMaxHp * 100 : 0);
            
            if (target != null)
            {
                event.addData("targetHpPercentage", targetHealthPercentage);
                event.addData("target", target);
            }

            // Check all HP triggers
            for (Trigger trigger : triggers)
            {
                if (!trigger.isEnabled())
                {
                    continue;
                }

                if (!trigger.isReadyToFire())
                {
                    continue;
                }

                // Check if this trigger handles HP events
                if (trigger.getType() == TriggerType.HP)
                {
                    handleHpTrigger(trigger, event, target, targetHealthPercentage, currentPlayerHp, currentPlayerMaxHp);
                }
            }

            // Update last HP values - removed as fields are not used
            // lastPlayerHp = currentPlayerHp;
            // lastTargetHp = currentTargetHp;
        }
    }

    private void handleHpTrigger(Trigger trigger, TriggerEvent event, 
                                Actor target, int targetHpPercentage, 
                                int playerHp, int playerMaxHp)
    {
        TriggerConfig config = trigger.getConfig();
        
        // Check cooldown first
        long currentTime = System.currentTimeMillis();
        String triggerId = trigger.getId();
        Long lastTriggerTime = lastTriggerTimes.get(triggerId);
        
        // Check if trigger is on cooldown (accounting for 1-tick delay)
        if (lastTriggerTime != null && (currentTime - lastTriggerTime) < (config.getCooldownMs() + 600)) {
            return; // Still on cooldown (cooldown + 1 tick)
        }
        
        int threshold = (int) config.getHpThreshold(); // Whole numbers only
        TriggerConfig.HpTargetType targetType = config.getHpTargetType();
        TriggerConfig.HpThresholdType thresholdType = config.getHpThresholdType();

        // Build debug message
        StringBuilder debugMsg = new StringBuilder();
        debugMsg.append("[TRIGGER CHECK] ").append(trigger.getName()).append(": ");
        
        boolean shouldFire = false;
        int currentHpPercentage = 0;

        // Check if target is in combat
        Client client = getClient();
        boolean targetInCombat = target != null && client != null && client.getLocalPlayer().getInteracting() != null;
        debugMsg.append("COMBAT:").append(targetInCombat ? "✓" : "✗").append(" ");
        
        // Check distance
        int distance = config.getMaxDistance();
        boolean inRange = true;
        if (distance > 0 && target != null && client != null && client.getLocalPlayer() != null) {
            int playerX = client.getLocalPlayer().getWorldLocation().getX();
            int playerY = client.getLocalPlayer().getWorldLocation().getY();
            int targetX = target.getWorldLocation().getX();
            int targetY = target.getWorldLocation().getY();
            int actualDistance = Math.max(Math.abs(playerX - targetX), Math.abs(playerY - targetY));
            inRange = actualDistance <= distance;
            debugMsg.append("DIST:").append(actualDistance).append("/").append(distance).append(inRange ? "✓" : "✗").append(" ");
        }

        // Determine which HP to check based on target type
        if (targetType == TriggerConfig.HpTargetType.TARGET)
        {
            String targetFilter = config.getTargetFilterValue();
            debugMsg.append("TARGET:").append(targetFilter.toUpperCase()).append(" ");
            
            // Treat both 'current' (new) and legacy 'target' as "current target" filter
            if ("current".equals(targetFilter) || "target".equals(targetFilter))
            {
                if (target != null && targetHpPercentage >= 0)
                {
                    currentHpPercentage = targetHpPercentage;
                    debugMsg.append("HP:").append(currentHpPercentage).append("% ");
                }
                else
                {
                    debugMsg.append("HP:✗ ");
                    sendDebugMessage(debugMsg.toString());
                    return;
                }
            }
            else if ("any".equals(targetFilter))
            {
                if (target != null && targetHpPercentage >= 0)
                {
                    currentHpPercentage = targetHpPercentage;
                    debugMsg.append("HP:").append(currentHpPercentage).append("% ");
                }
                else
                {
                    currentHpPercentage = playerMaxHp > 0 ? (playerHp * 100) / playerMaxHp : 0;
                    debugMsg.append("HP:").append(currentHpPercentage).append("%(PLAYER) ");
                }
            }
        }
        else if (targetType == TriggerConfig.HpTargetType.PLAYER)
        {
            currentHpPercentage = playerMaxHp > 0 ? (playerHp * 100) / playerMaxHp : 0;
            debugMsg.append("HP:").append(currentHpPercentage).append("%(PLAYER) ");
        }

        // Check threshold condition
        debugMsg.append("COND:").append(thresholdType.name()).append(" ").append(threshold).append("% ");
        
        if (thresholdType == TriggerConfig.HpThresholdType.ABOVE)
        {
            shouldFire = currentHpPercentage > threshold;
            debugMsg.append(shouldFire ? "✓ READY!" : "✗ WAITING");
        }
        else if (thresholdType == TriggerConfig.HpThresholdType.BELOW)
        {
            shouldFire = currentHpPercentage < threshold;
            debugMsg.append(shouldFire ? "✓ READY!" : "✗ WAITING");
        }

        // Apply minConsecutiveTicks smoothing
        int minTicks = config.getMinConsecutiveTicks();
        if (minTicks > 1)
        {
            int count = consecutiveTrueCounts.getOrDefault(triggerId, 0);
            if (shouldFire)
            {
                count++;
            }
            else
            {
                count = 0;
            }
            consecutiveTrueCounts.put(triggerId, count);

            if (shouldFire && count < minTicks)
            {
                // Condition not yet held long enough
                shouldFire = false;
                debugMsg.append(" CONSEC:").append(count).append("/").append(minTicks).append("✗");
            }
            else if (shouldFire)
            {
                debugMsg.append(" CONSEC:").append(count).append("/").append(minTicks).append("✓");
            }
        }

        // Check special attack requirement if enabled
        if (shouldFire && config.isRequireSpecialAttack())
        {
            try
            {
                // Import CombatAPI to check special attack
                int specEnergy = com.tonic.api.game.CombatAPI.getSpecEnergy();
                int requiredSpec = config.getSpecialAttackThreshold();
                
                debugMsg.append("SPEC:").append(specEnergy).append("%>").append(requiredSpec).append("% ");
                
                if (specEnergy < requiredSpec)
                {
                    shouldFire = false;
                    debugMsg.append("✗ INSUFFICIENT SPEC");
                }
                else
                {
                    debugMsg.append("✓ SPEC OK");
                }
            }
            catch (Exception e)
            {
                debugMsg.append("✗ SPEC ERROR");
                shouldFire = false;
            }
        }
        
        // Check distance if configured
        if (shouldFire && !inRange)
        {
            shouldFire = false;
            debugMsg.append("✗ OUT OF RANGE");
        }

        // Send debug message if enabled for this trigger
        if (config.isDebugEnabled())
        {
            sendDebugMessage(debugMsg.toString());
        }

        if (shouldFire)
        {
            // Update cooldown timestamp immediately
            lastTriggerTimes.put(triggerId, currentTime);
            engine.fireTrigger(trigger, event);
        }
    }

    private void sendDebugMessage(String message)
    {
        try {
            // Try to send to game chat
            com.tonic.Static.invoke(() -> {
                net.runelite.api.Client client = getClient();
                if (client != null) {
                    // Try game message first
                    try {
                        client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, 
                                            "[Gear Swapper]", message, "");
                    } catch (Exception e) {
                        // Fallback to console message
                        client.addChatMessage(net.runelite.api.ChatMessageType.CONSOLE, 
                                            "[Gear Swapper]", message, "");
                    }
                }
                return null;
            });
        } catch (Exception e) {
            // Always fallback to console with clear prefix
            System.out.println("[GEAR SWAPPER] " + message);
        }
    }

    private Actor getTarget()
    {
        Client client = getClient();
        if (client == null)
        {
            return null;
        }

        // Try to get the current target from the plugin
        // This is a simplified approach - in a real implementation, 
        // you'd get the target from the plugin's target tracking system
        Actor target = client.getLocalPlayer().getInteracting();
        return target;
    }

    // Target caching for HP handler - same duration as debug overlay
    private Actor cachedTarget = null;
    private long lastTargetTime = 0;
    private static final long TARGET_CACHE_DURATION = 30000; // Keep target for 30 seconds (same as debug overlay)

    private Actor getCachedTarget()
    {
        Actor currentTarget = getTarget();
        
        if (currentTarget != null)
        {
            // Update cached target
            cachedTarget = currentTarget;
            lastTargetTime = System.currentTimeMillis();
        }
        else
        {
            // Check if cached target is still valid
            long currentTime = System.currentTimeMillis();
            if (cachedTarget != null && (currentTime - lastTargetTime) < TARGET_CACHE_DURATION)
            {
                // Return cached target if within cache duration
                return cachedTarget;
            }
            else
            {
                // Clear cached target if expired
                cachedTarget = null;
                lastTargetTime = 0;
            }
        }
        
        return currentTarget;
    }
}
