package com.tonic.plugins.tilehideself;

import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.List;

@PluginDescriptor(
    name = "Tile Hide Self",
    description = "Hides your own 2D character when another player is standing on the same tile.",
    tags = {"pvp", "nh", "visibility"}
)
public class TileHideSelfPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private TileHideSelfConfig config;

    @Inject
    private Hooks hooks;

    private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

    private boolean overlapping;
    private int overlappingTicks;

    @Provides
    TileHideSelfConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TileHideSelfConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        hooks.registerRenderableDrawListener(drawListener);
    }

    @Override
    protected void shutDown() throws Exception
    {
        hooks.unregisterRenderableDrawListener(drawListener);
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (!config.enabled() || client == null)
        {
            overlapping = false;
            overlappingTicks = 0;
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            overlapping = false;
            overlappingTicks = 0;
            return;
        }

        WorldPoint localPoint = local.getWorldLocation();
        if (localPoint == null)
        {
            overlapping = false;
            overlappingTicks = 0;
            return;
        }

        boolean foundOverlap = false;
        List<Player> players = client.getPlayers();
        for (Player p : players)
        {
            if (p == null || p == local)
            {
                continue;
            }

            WorldPoint otherPoint = p.getWorldLocation();
            if (otherPoint != null && otherPoint.equals(localPoint))
            {
                foundOverlap = true;
                break;
            }
        }

        if (foundOverlap)
        {
            if (overlapping)
            {
                overlappingTicks++;
            }
            else
            {
                overlapping = true;
                overlappingTicks = 1;
            }
        }
        else
        {
            overlapping = false;
            overlappingTicks = 0;
        }
    }

    private boolean shouldDraw(Renderable renderable, boolean drawingUi)
    {
        if (!config.enabled())
        {
            return true;
        }

        if (client == null || !client.isClientThread())
        {
            return true;
        }

        if (!(renderable instanceof Player))
        {
            return true;
        }

        Player local = client.getLocalPlayer();
        if (local == null || renderable != local)
        {
            return true;
        }
        // If we are not currently overlapping (or config disabled in tick), always draw
        if (!overlapping)
        {
            return true;
        }

        int delay = config.hideDelayTicks();
        if (delay < 0)
        {
            delay = 0;
        }

        int requiredTicks = delay + 1; // 0 -> first tick, 1 -> after 1 full tick, etc.
        if (overlappingTicks < requiredTicks)
        {
            return true;
        }

        // At this point, overlap has lasted long enough to hide.
        if (drawingUi)
        {
            // 2D overlay: only hide if explicitly configured
            return !config.hide2d();
        }

        // 3D model: always hide when active
        return false;
    }
}
