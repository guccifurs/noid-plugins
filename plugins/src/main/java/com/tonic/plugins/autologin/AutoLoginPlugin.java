package com.tonic.plugins.autologin;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.List;

@PluginDescriptor(
        name = "Auto Login",
        description = "Automatically logs in with configured username and password when on the login screen.",
        tags = {"login", "auto"}
)
public class AutoLoginPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private AutoLoginConfig config;

    @Provides
    AutoLoginConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoLoginConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[AutoLogin] Auto Login v1.1 - Auto Eat enabled");

        // If the plugin is enabled while already on the login screen, try immediately
        if (client != null && client.getGameState() == GameState.LOGIN_SCREEN)
        {
            Logger.norm("[AutoLogin] Detected login screen on startup, attempting login");
            attemptLogin();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Auto Eat: only run when enabled and logged in
        if (!config.autoEat())
        {
            return;
        }

        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        int threshold = config.autoEatHpThreshold();
        if (threshold <= 0)
        {
            return;
        }

        int currentHp = CombatAPI.getHealth();
        if (currentHp <= 0 || currentHp > threshold)
        {
            return;
        }

        eatAnyFood();
    }

    @Override
    protected void shutDown() throws Exception
    {
        Logger.norm("[AutoLogin] Auto Login plugin stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            attemptLogin();
        }
    }

    private void attemptLogin()
    {
        final String username = config.username();
        final String password = config.password();

        if (username == null || username.isEmpty() || password == null || password.isEmpty())
        {
            Logger.norm("[AutoLogin] Username or password not set in config, skipping auto login");
            return;
        }

        clientThread.invoke(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                Logger.norm("[AutoLogin] Skipping auto login: game state is " + client.getGameState());
                return;
            }

            try
            {
                client.setUsername(username);
                client.setPassword(password);
                client.setGameState(GameState.LOGGING_IN);
                Logger.norm("[AutoLogin] Attempting auto login with configured credentials");
            }
            catch (Exception e)
            {
                Logger.norm("[AutoLogin] Failed to auto login: " + e.getMessage());
            }
        });
    }

    private void eatAnyFood()
    {
        try
        {
            List<ItemEx> items = InventoryAPI.getItems();
            for (ItemEx item : items)
            {
                if (item == null)
                {
                    continue;
                }

                if (item.hasAction("Eat"))
                {
                    String name = item.getName();
                    InventoryAPI.interact(item, "Eat");
                    Logger.norm("[AutoLogin] Auto Eat: ate " + name);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Logger.norm("[AutoLogin] Auto Eat error: " + e.getMessage());
        }
    }
}
