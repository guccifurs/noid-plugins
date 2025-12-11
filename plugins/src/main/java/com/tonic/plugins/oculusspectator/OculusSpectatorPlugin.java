package com.tonic.plugins.oculusspectator;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.game.CameraAPI;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
    name = "Oculus Spectator",
    description = "Use the Oculus Orb to spectate and follow other players",
    tags = {"oculus", "spectate", "follow", "camera", "orb"}
)
public class OculusSpectatorPlugin extends Plugin
{
    private static final String SPECTATE_OPTION = "Spectate";
    
    @Inject
    private Client client;

    @Inject
    private OculusSpectatorConfig config;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private OculusSpectatorOverlay overlay;

    private Player spectateTarget;
    private String spectateTargetName;

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[Oculus Spectator] Plugin started");
        keyManager.registerKeyListener(clearSpectateListener);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        Logger.norm("[Oculus Spectator] Plugin stopped");
        keyManager.unregisterKeyListener(clearSpectateListener);
        overlayManager.remove(overlay);
        stopSpectating();
    }

    @Provides
    OculusSpectatorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OculusSpectatorConfig.class);
    }

    private final HotkeyListener clearSpectateListener = new HotkeyListener(() -> config.clearSpectateKeybind())
    {
        @Override
        public void hotkeyPressed()
        {
            stopSpectating();
        }
    };

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        // Only add menu option if not spectating and it's a player
        if (spectateTarget != null)
        {
            return;
        }

        if (event.getType() != MenuAction.PLAYER_SECOND_OPTION.getId() 
            && event.getType() != MenuAction.PLAYER_THIRD_OPTION.getId()
            && event.getType() != MenuAction.PLAYER_FOURTH_OPTION.getId()
            && event.getType() != MenuAction.PLAYER_FIFTH_OPTION.getId())
        {
            return;
        }

        final MenuEntry menuEntry = client.createMenuEntry(-1)
            .setOption(SPECTATE_OPTION)
            .setTarget(event.getTarget())
            .setType(MenuAction.RUNELITE)
            .setIdentifier(event.getIdentifier());

        menuEntry.onClick(e -> startSpectating(event.getIdentifier()));
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (spectateTarget == null)
        {
            return;
        }

        // Check if target still exists
        if (!isTargetValid())
        {
            Logger.norm("[Oculus Spectator] Target lost, stopping spectate");
            stopSpectating();
            return;
        }

        // Ensure orb mode is enabled while spectating
        if (client.getOculusOrbState() == 0)
        {
            client.setOculusOrbState(1);
        }

        // Continuously steer the camera to look at the spectated target
        updateCameraToTarget();
    }

    private void startSpectating(int playerId)
    {
        try
        {
            if (client == null)
            {
                return;
            }

            Player target = null;
            java.util.List<Player> players = client.getPlayers();
            if (players == null)
            {
                return;
            }

            for (Player player : players)
            {
                if (player != null && player.getId() == playerId)
                {
                    target = player;
                    break;
                }
            }

            if (target == null)
            {
                Logger.norm("[Oculus Spectator] Player not found");
                return;
            }

            spectateTarget = target;
            spectateTargetName = target.getName();
            Logger.norm("[Oculus Spectator] Now spectating: " + target.getName());
        }
        catch (Exception e)
        {
            Logger.error("[Oculus Spectator] Error starting spectate: " + e.getMessage());
        }
    }

    private void stopSpectating()
    {
        if (spectateTarget != null)
        {
            Logger.norm("[Oculus Spectator] Stopped spectating");
            spectateTarget = null;
            spectateTargetName = null;
            
            // Disable oculus orb mode
            if (client != null)
            {
                client.setOculusOrbState(0);
            }
        }
    }

    private boolean isTargetValid()
    {
        if (spectateTargetName == null || spectateTargetName.isEmpty())
        {
            return false;
        }

        java.util.List<Player> players = client.getPlayers();
        if (players == null)
        {
            return false;
        }

        for (Player player : players)
        {
            if (player == null)
            {
                continue;
            }

            String name = player.getName();
            if (name != null && name.equalsIgnoreCase(spectateTargetName))
            {
                // Refresh spectateTarget reference to the current Player instance
                spectateTarget = player;
                return true;
            }
        }

        return false;
    }

    private void updateCameraToTarget()
    {
        if (client == null || spectateTarget == null)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return;
        }

        WorldPoint self = local.getWorldLocation();
        WorldPoint target = spectateTarget.getWorldLocation();
        if (self == null || target == null)
        {
            return;
        }

        int dx = target.getX() - self.getX();
        int dy = target.getY() - self.getY();
        if (dx == 0 && dy == 0)
        {
            return;
        }

        double angleDeg = Math.toDegrees(Math.atan2(dx, dy));
        if (angleDeg < 0)
        {
            angleDeg += 360.0;
        }

        CameraAPI.setYawTargetDegrees(angleDeg);
        CameraAPI.setPitchTargetDegrees(45.0);
    }

    public Player getSpectateTarget()
    {
        return spectateTarget;
    }
}
