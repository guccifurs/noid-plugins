package com.tonic.plugins.noid;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.plugins.noid.auth.NoidAuthService;
import com.tonic.plugins.noid.auth.NoidUser;
import com.tonic.plugins.noid.update.NoidUpdateChecker;
import com.tonic.plugins.noid.update.PluginManager;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@PluginDescriptor(name = "Noid", description = "Authentication & Auto-Update for Noid plugins", tags = { "auth", "noid",
        "update" })
public class NoidPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private NoidConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private NoidOverlay overlay;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ConfigManager configManager;

    @Inject
    private net.runelite.client.callback.ClientThread clientThread;

    private static final String CONFIG_GROUP = "noid";
    private static final String SESSION_KEY = "sessionToken";
    private static final String DISCORD_ID_KEY = "discordId";

    private boolean authenticated = false;

    private NoidUser currentUser = null;

    private NoidAuthService authService;
    private NoidUpdateChecker updateChecker;
    private PluginManager pluginManager;
    private NoidPanel panel;
    private NavigationButton navigationButton;
    private int heartbeatTicks = 0;
    private static final int HEARTBEAT_INTERVAL = 100; // ~60 seconds

    @Provides
    NoidConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NoidConfig.class);
    }

    @Override
    protected void startUp() {
        Logger.norm("[Noid] Plugin started - v1.0.0 with Discord OAuth");

        // FIRST: Check and download plugin updates SYNCHRONOUSLY before anything else
        // This ensures plugins are downloaded before RuneLite finishes loading
        pluginManager = new PluginManager();
        pluginManager.checkAndUpdateAll();

        overlayManager.add(overlay);

        authService = new NoidAuthService();
        updateChecker = new NoidUpdateChecker("guccifurs/noid-plugins");

        // Create and register the panel
        panel = new NoidPanel(this, authService);

        // Generate simple icon (Discord blue circle)
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = icon.createGraphics();
        g.setColor(new java.awt.Color(88, 101, 242)); // Discord blue
        g.fillOval(0, 0, 16, 16);
        g.dispose();

        navigationButton = NavigationButton.builder()
                .tooltip("Noid Authentication")
                .icon(icon)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navigationButton);

        // Try to restore previous session
        restoreSession();

        // Check for self updates
        checkForUpdates();

        // Check for plugin updates async and notify if available
        checkPluginUpdatesAsync();
    }

    /**
     * Check for plugin updates in background and show notification if available
     */
    private void checkPluginUpdatesAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait for client to fully load
                java.util.List<com.tonic.plugins.noid.update.UpdateInfo> updates = pluginManager.getAvailableUpdates();

                if (!updates.isEmpty()) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("[Noid] Updates available: ");
                    for (com.tonic.plugins.noid.update.UpdateInfo u : updates) {
                        msg.append(u.getPluginName()).append(" ").append(u.getLatestVersion()).append(" ");
                    }
                    msg.append("- Click Noid panel to update!");

                    // Show in-game notification (must be on client thread)
                    if (client != null && clientThread != null) {
                        clientThread.invoke(() -> {
                            client.addChatMessage(
                                    net.runelite.api.ChatMessageType.GAMEMESSAGE,
                                    "",
                                    msg.toString(),
                                    "");
                        });
                    }

                    Logger.norm(msg.toString());
                }
            } catch (Exception e) {
                Logger.error("[Noid] Async update check failed: " + e.getMessage());
            }
        }, "Noid-UpdateCheck").start();
    }

    /**
     * Try to restore previous login session
     */
    private void restoreSession() {
        String savedDiscordId = configManager.getConfiguration(CONFIG_GROUP, DISCORD_ID_KEY);

        if (savedDiscordId != null && !savedDiscordId.isEmpty()) {
            Logger.norm("[Noid] Restoring session for Discord ID: " + savedDiscordId);

            // Restore session using saved Discord ID
            NoidUser user = authService.restoreSession(savedDiscordId);
            if (user != null && user.hasActiveSubscription()) {
                this.currentUser = user;
                this.authenticated = true;
                Logger.norm("[Noid] ✅ Session restored: " + user.getDiscordName());

                if (panel != null) {
                    panel.updateUI();
                }

                // Download plugins for subscriber
                if (pluginManager != null) {
                    pluginManager.checkAndUpdateAll();
                }
            } else {
                Logger.norm("[Noid] Session expired or invalid, please login again");
            }
        }
    }

    @Override
    protected void shutDown() {
        Logger.norm("[Noid] Plugin stopped");
        overlayManager.remove(overlay);

        if (navigationButton != null) {
            clientToolbar.removeNavigation(navigationButton);
        }

        if (authService != null) {
            authService.logout();
        }

        authenticated = false;
        currentUser = null;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        heartbeatTicks++;

        // Send heartbeat periodically
        if (authenticated && heartbeatTicks >= HEARTBEAT_INTERVAL) {
            heartbeatTicks = 0;
            authService.sendHeartbeat("NoidPlugin");
        }
    }

    // Note: authenticate() method removed - OAuth via panel is now the only auth
    // method

    /**
     * Check for plugin updates from GitHub
     */
    private void checkForUpdates() {
        updateChecker.checkForUpdates().thenAccept(updateInfo -> {
            if (updateInfo != null && updateInfo.hasUpdate()) {
                Logger.norm("[Noid] Update available: " + updateInfo.getLatestVersion());

                Logger.norm("[Noid] Downloading update...");
                updateChecker.downloadUpdate(updateInfo);
            } else {
                Logger.norm("[Noid] Plugin is up to date");
            }
        }).exceptionally(throwable -> {
            Logger.error("[Noid] Update check failed: " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Generate hardware ID for this machine
     */
    private String generateHwid() {
        try {
            StringBuilder hwid = new StringBuilder();

            // Get system properties
            String os = System.getProperty("os.name", "");
            String osVersion = System.getProperty("os.version", "");
            String osArch = System.getProperty("os.arch", "");
            String userName = System.getProperty("user.name", "");
            String userHome = System.getProperty("user.home", "");

            // Combine properties
            hwid.append(os).append(osVersion).append(osArch);
            hwid.append(userName).append(userHome);

            // Add network info if available
            try {
                java.net.NetworkInterface ni = java.net.NetworkInterface.getNetworkInterfaces().nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        hwid.append(String.format("%02X", b));
                    }
                }
            } catch (Exception ignored) {
            }

            // Hash the result
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(hwid.toString().getBytes());

            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (Exception e) {
            // Fallback to random HWID (not ideal)
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * Check if the current user is authenticated with an active subscription.
     * Other plugins should call this to gate their functionality.
     */
    public boolean isAuthenticated() {
        return authenticated && currentUser != null;
    }

    /**
     * Get the current authenticated user, or null if not authenticated.
     */
    public NoidUser getCurrentUser() {
        return currentUser;
    }

    /**
     * Log plugin usage for analytics
     */
    public void logPluginUsage(String pluginName, String action) {
        if (authService != null && authenticated) {
            authService.logUsage(pluginName, action);
        }
    }

    /**
     * Set authenticated state (called by panel after OAuth)
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        if (panel != null) {
            panel.updateUI();
        }
    }

    /**
     * Set current user (called by panel after OAuth)
     */
    public void setCurrentUser(NoidUser user) {
        this.currentUser = user;
        if (user != null && user.hasActiveSubscription()) {
            this.authenticated = true;

            // Save Discord ID for session persistence
            if (user.getDiscordId() != null) {
                configManager.setConfiguration(CONFIG_GROUP, DISCORD_ID_KEY, user.getDiscordId());
                Logger.norm("[Noid] Session saved for next startup");
            }

            // Immediately download/update child plugins after successful OAuth
            if (pluginManager != null) {
                pluginManager.checkAndUpdateAll();
            }
        }
        if (panel != null) {
            panel.updateUI();
        }
    }

    /**
     * Get the auth service for OAuth flow
     */
    public NoidAuthService getAuthService() {
        return authService;
    }

    /**
     * Get the plugin manager for update checking
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
}
