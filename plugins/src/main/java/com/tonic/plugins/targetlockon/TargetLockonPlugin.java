package com.tonic.plugins.targetlockon;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
    name = "Target Lockon",
    description = "Lock camera onto a target player with right-click option",
    tags = {"camera", "lock", "target", "pvp", "tracking"}
)
public class TargetLockonPlugin extends Plugin
{
    private static final String LOCK_OPTION = "Lock Camera";
    
    @Inject
    private Client client;

    @Inject
    private TargetLockonConfig config;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TargetLockonOverlay overlay;

    @Inject
    private ScheduledExecutorService executor;

    private Player lockedTarget;
    private int targetYaw = -1;
    private int targetPitch = -1;
    private ScheduledFuture<?> cameraUpdateTask;

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[Target Lockon] Plugin started");
        keyManager.registerKeyListener(clearLockListener);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        Logger.norm("[Target Lockon] Plugin stopped");
        keyManager.unregisterKeyListener(clearLockListener);
        overlayManager.remove(overlay);
        clearLock();
    }

    @Provides
    TargetLockonConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TargetLockonConfig.class);
    }

    private final HotkeyListener clearLockListener = new HotkeyListener(() -> config.clearLockKeybind())
    {
        @Override
        public void hotkeyPressed()
        {
            clearLock();
        }
    };

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        // Only add menu option if no target is locked and it's a player
        if (lockedTarget != null)
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
            .setOption(LOCK_OPTION)
            .setTarget(event.getTarget())
            .setType(MenuAction.RUNELITE)
            .setIdentifier(event.getIdentifier());

        menuEntry.onClick(e -> lockOntoPlayer(event.getIdentifier()));
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (lockedTarget == null)
        {
            return;
        }

        // Check if target still exists
        if (!isTargetValid())
        {
            Logger.norm("[Target Lockon] Target lost, clearing lock");
            clearLock();
            return;
        }

        // Calculate target camera angles based on target position
        WorldPoint targetPos = lockedTarget.getWorldLocation();
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        
        if (targetPos != null && playerPos != null)
        {
            int dx = targetPos.getX() - playerPos.getX();
            int dy = targetPos.getY() - playerPos.getY();

            // Calculate angle using OSRS coordinate system
            double angleRadians = Math.atan2(dy, dx);
            double angleDegrees = Math.toDegrees(angleRadians);
            
            // Convert Math coordinates to OSRS camera coordinates
            // Math: 0°=east, 90°=north, ±180°=west, -90°=south
            // OSRS: 0=south, 90°→1024=north, 180°→512=west, 270°→1536=east
            double osrsAngle = 90 - angleDegrees;
            targetYaw = ((int) Math.round(osrsAngle * (2048.0 / 360.0))) & 2047;
            
            // Keep current pitch or use default
            targetPitch = 256; // Medium pitch
            
            // Start smooth camera updates if not already running
            if (cameraUpdateTask == null || cameraUpdateTask.isDone())
            {
                startSmoothCameraUpdate();
            }
        }
    }

    private void startSmoothCameraUpdate()
    {
        // Update camera position smoothly every 16ms (~60 FPS)
        cameraUpdateTask = executor.scheduleAtFixedRate(() -> {
            if (lockedTarget == null || client.getGameState() != net.runelite.api.GameState.LOGGED_IN)
            {
                if (cameraUpdateTask != null)
                {
                    cameraUpdateTask.cancel(false);
                }
                return;
            }

            int currentYaw = CameraAPI.getYaw();
            int currentPitch = CameraAPI.getPitch();

            // Calculate yaw difference (handle wrapping)
            int yawDiff = targetYaw - currentYaw;
            if (yawDiff > 1024) yawDiff -= 2048;
            if (yawDiff < -1024) yawDiff += 2048;

            int pitchDiff = targetPitch - currentPitch;

            // Calculate smooth step size based on config speed (1-100)
            float smoothFactor = config.smoothSpeed() / 100.0f;
            int yawStep = (int) (yawDiff * smoothFactor);
            int pitchStep = (int) (pitchDiff * smoothFactor);

            // Apply smoothed camera movement (negate yawStep to flip direction)
            if (Math.abs(yawDiff) > 2 || Math.abs(pitchDiff) > 2)
            {
                int newYaw = (currentYaw - yawStep) & 2047;
                int newPitch = currentPitch + pitchStep;
                
                CameraAPI.setYawTarget(newYaw);
                CameraAPI.setPitchTarget(newPitch);
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private void lockOntoPlayer(int playerId)
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
                Logger.norm("[Target Lockon] Player not found");
                return;
            }

            lockedTarget = target;
            Logger.norm("[Target Lockon] Locked onto: " + target.getName());
        }
        catch (Exception e)
        {
            Logger.error("[Target Lockon] Error locking onto player: " + e.getMessage());
        }
    }

    private void clearLock()
    {
        if (lockedTarget != null)
        {
            Logger.norm("[Target Lockon] Lock cleared");
            lockedTarget = null;
            targetYaw = -1;
            targetPitch = -1;
            
            if (cameraUpdateTask != null && !cameraUpdateTask.isDone())
            {
                cameraUpdateTask.cancel(false);
                cameraUpdateTask = null;
            }
        }
    }

    private boolean isTargetValid()
    {
        if (lockedTarget == null)
        {
            return false;
        }

        // Check if target still exists in the game
        java.util.List<Player> players = client.getPlayers();
        if (players == null)
        {
            return false;
        }

        for (Player player : players)
        {
            if (player != null && player.equals(lockedTarget))
            {
                return true;
            }
        }

        return false;
    }

    public Player getLockedTarget()
    {
        return lockedTarget;
    }
}
