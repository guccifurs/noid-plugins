package com.tonic.plugins.gearswapper.triggers;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.events.AnimationChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Handler for animation-based triggers
 */
public class AnimationTriggerHandler implements TriggerHandler
{
    private static final Logger logger = LoggerFactory.getLogger(AnimationTriggerHandler.class);
    
    private final TriggerEngine engine;

    public AnimationTriggerHandler(TriggerEngine engine)
    {
        this.engine = engine;
    }

    @Override
    public boolean canHandle(TriggerEvent event)
    {
        return event.getType() == TriggerEventType.ANIMATION_CHANGED;
    }

    @Override
    public void handleEvent(TriggerEvent event, Collection<Trigger> triggers)
    {
        if (!(event.getSourceEvent() instanceof AnimationChanged))
        {
            return;
        }

        AnimationChanged animationEvent = (AnimationChanged) event.getSourceEvent();
        Actor actor = animationEvent.getActor();
        
        if (actor == null)
        {
            return;
        }

        int animationId = actor.getAnimation();
        if (animationId <= 0)
        {
            return;
        }

        // Add animation data to event
        event.addData("animationId", animationId);
        event.addData("actor", actor);
        event.addData("isPlayer", actor == getClient().getLocalPlayer());
        event.addData("isNpc", actor != getClient().getLocalPlayer());

        logger.debug("[Animation Handler] Processing animation {} for actor: {}", animationId, 
                    actor != getClient().getLocalPlayer() ? "NPC" : "Player");

        // Check all animation triggers
        for (Trigger trigger : triggers)
        {
            if (!trigger.isEnabled() || !trigger.isReadyToFire())
            {
                continue;
            }

            // Check if this trigger handles animation events
            if (trigger.getType() == TriggerType.ANIMATION)
            {
                handleAnimationTrigger(trigger, event, animationId, actor);
            }
        }
    }

    private void handleAnimationTrigger(Trigger trigger, TriggerEvent event, int animationId, Actor actor)
    {
        TriggerConfig config = trigger.getConfig();
        
        // Check if this is the right animation
        if (config.getAnimationId() != animationId)
        {
            return;
        }

        // Check target filter
        // For now, all animation triggers work the same way
        // TODO: Add target/player filtering when needed
        
        TriggerConfig.TargetFilter targetFilter = config.getTargetFilter();
        Client client = getClient();
        Actor localPlayer = client != null ? client.getLocalPlayer() : null;

        // Check if we should only trigger on current target
        if (targetFilter == TriggerConfig.TargetFilter.CURRENT)
        {
            if (localPlayer != null)
            {
                Actor currentTarget = localPlayer.getInteracting();
                if (currentTarget != actor)
                {
                    return; // Not our current target, skip
                }
            }
        }
        else if (targetFilter == TriggerConfig.TargetFilter.LOCAL)
        {
            if (localPlayer == null || actor != localPlayer)
            {
                return;
            }
        }

        // Check combat condition
        if (config.isOnlyInCombat() && !isInCombat())
        {
            return;
        }

        // All conditions met - fire the trigger
        logger.info("[Animation Handler] Animation {} matches trigger: {}", animationId, trigger.getName());
        engine.fireTrigger(trigger, event);
    }

    private boolean isInCombat()
    {
        // TODO: Implement proper combat detection
        // For now, we'll use a simple check
        return getClient().getLocalPlayer() != null && 
               getClient().getLocalPlayer().getInteracting() != null;
    }

    @Override
    public Client getClient()
    {
        return engine.getClient();
    }
}
