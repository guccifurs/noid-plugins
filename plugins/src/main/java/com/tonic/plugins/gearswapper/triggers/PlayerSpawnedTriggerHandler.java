package com.tonic.plugins.gearswapper.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import com.tonic.Logger;
import com.tonic.api.game.GameAPI;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.PlayerSpawned;

import java.util.Collection;

/**
 * Handler for player spawned triggers
 */
public class PlayerSpawnedTriggerHandler implements TriggerHandler {

    private final TriggerEngine engine;

    public PlayerSpawnedTriggerHandler(TriggerEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean canHandle(TriggerEvent event) {
        return event.getType() == TriggerEventType.PLAYER_SPAWNED;
    }

    @Override
    public void handleEvent(TriggerEvent event, Collection<Trigger> triggers) {
        if (!(event.getSourceEvent() instanceof PlayerSpawned)) {
            return;
        }

        PlayerSpawned spawnEvent = (PlayerSpawned) event.getSourceEvent();
        Player spawnedPlayer = spawnEvent.getPlayer();

        if (spawnedPlayer == null || spawnedPlayer == getClient().getLocalPlayer()) {
            return;
        }

        // Add player data to event
        event.addData("spawnedPlayer", spawnedPlayer);

        Logger.norm("[Player Spawned] Processing spawn: " + spawnedPlayer.getName());

        // Check all triggers
        for (Trigger trigger : triggers) {
            if (!trigger.isEnabled() || !trigger.isReadyToFire()) {
                continue;
            }

            // Check if this trigger handles player spawned events
            if (trigger.getType() == TriggerType.PLAYER_SPAWNED) {
                handlePlayerSpawnedTrigger(trigger, event, spawnedPlayer);
            }
        }
    }

    private void handlePlayerSpawnedTrigger(Trigger trigger, TriggerEvent event, Player spawnedPlayer) {
        TriggerConfig config = trigger.getConfig();
        Client client = getClient();
        Player localPlayer = client != null ? client.getLocalPlayer() : null;

        if (localPlayer == null)
            return;

        // 1. Check Radius (if > 0)
        int radius = config.getPlayerSpawnedRadius();
        if (radius > 0) {
            int distance = localPlayer.getWorldLocation().distanceTo(spawnedPlayer.getWorldLocation());
            if (distance > radius) {
                return; // Outside radius
            }
        }

        // 2. Check "No Target Required"
        GearSwapperPlugin plugin = engine.getPlugin();
        if (config.isPlayerSpawnedNoTarget()) {
            if (plugin.getCurrentTarget() != null) {
                return; // We have a target, so skip
            }
        }

        // 3. Ignore Friends/Clan
        if (config.isPlayerSpawnedIgnoreFriends()) {
            // Note: isFriended logic might need adjustment based on exact RuneLite API
            // version
            // For now assuming client.isFriended(name, false) works or similar
            if (client.isFriended(spawnedPlayer.getName(), false) || spawnedPlayer.isClanMember()) {
                return; // Ignore friend/clan
            }
        }

        // 4. Attackable Only (Wilderness Level Check - Same as FindTarget)
        if (config.isPlayerSpawnedAttackableOnly()) {
            int wildyLevel = GameAPI.getWildyLevel();
            int myCombat = localPlayer.getCombatLevel();
            int targetCombat = spawnedPlayer.getCombatLevel();
            int diff = Math.abs(myCombat - targetCombat);

            // If in wilderness (wildyLevel > 0), check combat bracket
            if (wildyLevel > 0 && diff > wildyLevel) {
                return; // Out of attack range for this wilderness level
            }
            // If NOT in wilderness and wildyLevel == 0, assume PvP world or skip check
            // (User can use this on PvP worlds where level difference doesn't matter)
        }

        // 5. Set Target Action (if enabled)
        Logger.norm("[Player Spawned] SetTarget config: " + config.isPlayerSpawnedSetTarget());
        if (config.isPlayerSpawnedSetTarget()) {
            plugin.setCurrentTarget(spawnedPlayer);
            Logger.norm("[Player Spawned] ✅ Auto-targeted: " + spawnedPlayer.getName());
        }

        // All conditions met - fire the trigger
        Logger.norm(
                "[Player Spawned] 🎯 Firing trigger: " + trigger.getName() + " for player: " + spawnedPlayer.getName());
        engine.fireTrigger(trigger, event);
    }

    @Override
    public Client getClient() {
        return engine.getClient();
    }
}
