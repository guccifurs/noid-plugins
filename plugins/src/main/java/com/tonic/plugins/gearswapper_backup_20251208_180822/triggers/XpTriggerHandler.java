package com.tonic.plugins.gearswapper.triggers;

import com.tonic.api.game.CombatAPI;
import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Actor;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.Skill;
import java.util.Collection;

/**
 * Handler for XP-based triggers
 */
public class XpTriggerHandler implements TriggerHandler
{
    private final TriggerEngine engine;
    
    // Cooldown tracking for each trigger
    private java.util.Map<String, Long> lastTriggerTimes = new java.util.HashMap<>();

    @Inject
    public XpTriggerHandler(TriggerEngine engine)
    {
        this.engine = engine;
    }

    @Override
    public boolean canHandle(TriggerEvent event)
    {
        return event != null && event.getType() == TriggerEventType.XP_DROPPED;
    }

    @Override
    public void handleEvent(TriggerEvent event, Collection<Trigger> triggers)
    {
        if (event == null || triggers == null || triggers.isEmpty())
        {
            return;
        }

        // Debug: Log event received
        System.out.println("[XP Trigger] Received event: " + event.getType() + ", source: " + event.getSourceEvent());

        // Check if the event is a FakeXpDrop
        Object eventData = event.getSourceEvent();
        if (eventData instanceof FakeXpDrop)
        {
            FakeXpDrop xpDrop = (FakeXpDrop) eventData;
            
            // Debug: Log XP drop details
            System.out.println("[XP Trigger] Processing FakeXpDrop: " + xpDrop.getXp() + " XP, skill: " + 
                (xpDrop.getSkill() != null ? xpDrop.getSkill().getName() : "Unknown"));
            
            // Process all XP triggers
            for (Trigger trigger : triggers)
            {
                // Check if this trigger handles XP events
                if (trigger.getType() == TriggerType.XP)
                {
                    System.out.println("[XP Trigger] Found XP trigger: " + trigger.getName());
                    handleXpTrigger(trigger, event, xpDrop);
                }
            }
        }
        else
        {
            System.out.println("[XP Trigger] Event is not FakeXpDrop: " + (eventData != null ? eventData.getClass().getSimpleName() : "null"));
        }
    }

    public Client getClient()
    {
        // Use the client provided by the trigger engine
        return engine != null ? engine.getClient() : null;
    }

    public void handleGameTick(GameTick event)
    {
        // XP triggers are handled by FakeXpDrop events, not game ticks
    }

    private void handleXpTrigger(Trigger trigger, TriggerEvent triggerEvent, FakeXpDrop event)
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
        
        int xpThreshold = (int) config.getXpThreshold(); // XP amount threshold
        String skillFilter = config.getSkillFilter(); // Skill to filter
        
        // Build debug message
        StringBuilder debugMsg = new StringBuilder();
        debugMsg.append("[TRIGGER CHECK] ").append(trigger.getName()).append(": ");
        
        boolean shouldFire = false;
        int xpAmount = 0;
        String skillName = "";

        // Get XP drop information
        if (event != null)
        {
            xpAmount = event.getXp();
            Skill skill = event.getSkill();
            skillName = skill != null ? skill.getName() : "Unknown";
            
            debugMsg.append("SKILL:").append(skillName).append(" ");
            debugMsg.append("XP:").append(xpAmount).append(">").append(xpThreshold).append(" ");
            
            // Check skill filter
            if (!"any".equalsIgnoreCase(skillFilter) && !skillName.equalsIgnoreCase(skillFilter))
            {
                debugMsg.append("✗ WRONG SKILL");
                shouldFire = false;
            }
            else
            {
                // Check XP threshold
                shouldFire = xpAmount >= xpThreshold;
                debugMsg.append(shouldFire ? "✓ READY!" : "✗ WAITING");
            }
        }
        else
        {
            debugMsg.append("✗ NO XP DATA");
            shouldFire = false;
        }

        // Check special attack requirement if enabled
        if (shouldFire && config.isRequireSpecialAttack())
        {
            try
            {
                int specEnergy = CombatAPI.getSpecEnergy();
                int requiredSpec = config.getSpecialAttackThreshold();
                
                debugMsg.append("SPEC:").append(specEnergy).append("%>").append(requiredSpec).append("% ");
                
                // Debug: Log detailed spec check
                System.out.println("[XP Trigger] Special attack check - Current: " + specEnergy + "%, Required: " + requiredSpec + "%");
                
                if (specEnergy < requiredSpec)
                {
                    shouldFire = false;
                    debugMsg.append("✗ INSUFFICIENT SPEC");
                    System.out.println("[XP Trigger] Special attack FAILED - insufficient energy");
                }
                else
                {
                    debugMsg.append("✓ SPEC OK");
                    System.out.println("[XP Trigger] Special attack PASSED");
                }
            }
            catch (Exception e)
            {
                debugMsg.append("✗ SPEC ERROR");
                System.out.println("[XP Trigger] Special attack ERROR: " + e.getMessage());
                shouldFire = false;
            }
        }
        else
        {
            // Debug: Log why spec check was skipped
            if (!config.isRequireSpecialAttack())
            {
                System.out.println("[XP Trigger] Special attack check SKIPPED - not required");
            }
            else if (!shouldFire)
            {
                System.out.println("[XP Trigger] Special attack check SKIPPED - trigger already failed");
            }
        }

        // Send debug message
        sendDebugMessage(debugMsg.toString());

        // Debug: Log final decision
        System.out.println("[XP Trigger] FINAL DECISION for '" + trigger.getName() + "': " + 
            (shouldFire ? "FIRE TRIGGER" : "DO NOT FIRE") + " - " + debugMsg.toString());

        if (shouldFire)
        {
            // Update cooldown timestamp immediately
            lastTriggerTimes.put(triggerId, currentTime);
            engine.fireTrigger(trigger, triggerEvent);
            System.out.println("[XP Trigger] TRIGGER FIRED: " + trigger.getName());
        }
        else
        {
            System.out.println("[XP Trigger] TRIGGER BLOCKED: " + trigger.getName());
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
                        // Fallback to console
                        System.out.println("[Gear Swapper Trigger] " + message);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("[Gear Swapper Trigger] " + message);
        }
    }

    public Actor getTarget()
    {
        Client client = getClient();
        if (client == null)
        {
            return null;
        }

        // For XP triggers, target is the current combat target
        return client.getLocalPlayer().getInteracting();
    }
}
