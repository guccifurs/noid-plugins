package com.tonic.plugins.gearswapper.triggers;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.events.GraphicChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Handler for GFX/Graphic-based triggers (SpotAnims)
 */
public class GfxTriggerHandler implements TriggerHandler {
    private static final Logger logger = LoggerFactory.getLogger(GfxTriggerHandler.class);

    private final TriggerEngine engine;

    public GfxTriggerHandler(TriggerEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean canHandle(TriggerEvent event) {
        return event.getType() == TriggerEventType.GFX_CHANGED;
    }

    @Override
    public void handleEvent(TriggerEvent event, Collection<Trigger> triggers) {
        if (!(event.getSourceEvent() instanceof GraphicChanged)) {
            return;
        }

        GraphicChanged graphicEvent = (GraphicChanged) event.getSourceEvent();
        Actor actor = graphicEvent.getActor();

        if (actor == null) {
            return;
        }

        int gfxId = actor.getGraphic();
        if (gfxId <= 0) {
            return;
        }

        // Add GFX data to event
        event.addData("gfxId", gfxId);
        event.addData("actor", actor);
        event.addData("isPlayer", actor == getClient().getLocalPlayer());
        event.addData("isNpc", actor != getClient().getLocalPlayer());

        logger.debug("[GFX Handler] Processing GFX {} for actor: {}", gfxId,
                actor != getClient().getLocalPlayer() ? "NPC/Target" : "LocalPlayer");

        // Check all GFX triggers
        for (Trigger trigger : triggers) {
            if (!trigger.isEnabled() || !trigger.isReadyToFire()) {
                continue;
            }

            // Check if this trigger handles GFX events
            if (trigger.getType() == TriggerType.GFX) {
                handleGfxTrigger(trigger, event, gfxId, actor);
            }
        }
    }

    private void handleGfxTrigger(Trigger trigger, TriggerEvent event, int gfxId, Actor actor) {
        TriggerConfig config = trigger.getConfig();

        // Check if this is the right GFX
        if (config.getGfxId() != gfxId) {
            return;
        }

        // Check target filter
        TriggerConfig.TargetFilter targetFilter = config.getTargetFilter();
        Client client = getClient();
        Actor localPlayer = client != null ? client.getLocalPlayer() : null;

        // Check if we should only trigger on current target
        if (targetFilter == TriggerConfig.TargetFilter.CURRENT) {
            if (localPlayer != null) {
                Actor currentTarget = localPlayer.getInteracting();
                if (currentTarget != actor) {
                    return; // Not our current target, skip
                }
            }
        } else if (targetFilter == TriggerConfig.TargetFilter.LOCAL) {
            if (localPlayer == null || actor != localPlayer) {
                return; // Not local player
            }
        }

        // Check combat condition
        if (config.isOnlyInCombat() && !isInCombat()) {
            return;
        }

        // All conditions met - fire the trigger
        logger.info("[GFX Handler] GFX {} matches trigger: {}", gfxId, trigger.getName());
        engine.fireTrigger(trigger, event);
    }

    private boolean isInCombat() {
        return getClient().getLocalPlayer() != null &&
                getClient().getLocalPlayer().getInteracting() != null;
    }

    @Override
    public Client getClient() {
        return engine.getClient();
    }
}
