package com.tonic.plugins.clicktest;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@PluginDescriptor(name = "Click Tester", description = "Test different click methods to see which show visible indicators", tags = {
        "click", "test", "debug" })
public class ClickTestPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ClickTestConfig config;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    private ClickTestPanel panel;
    private NavigationButton navigationButton;
    private MousePathOverlay pathOverlay;

    private final HotkeyListener captureHotkeyListener = new HotkeyListener(() -> config.captureHotkey()) {
        @Override
        public void hotkeyPressed() {
            if (panel != null) {
                panel.captureMousePositionFromHotkey();
            }
        }
    };

    @Override
    protected void startUp() throws Exception {
        // Create and register path overlay
        pathOverlay = new MousePathOverlay(client);
        overlayManager.add(pathOverlay);

        // Create panel and give it access to the overlay
        panel = new ClickTestPanel(client, config, pathOverlay);

        // Create a simple colored icon (no external resource needed)
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = icon.createGraphics();
        g.setColor(new java.awt.Color(76, 175, 80)); // Green
        g.fillOval(2, 2, 12, 12);
        g.setColor(java.awt.Color.WHITE);
        g.fillOval(5, 5, 6, 6);
        g.dispose();

        navigationButton = NavigationButton.builder()
                .tooltip("Click Tester")
                .icon(icon)
                .priority(100)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navigationButton);
        keyManager.registerKeyListener(captureHotkeyListener);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navigationButton);
        keyManager.unregisterKeyListener(captureHotkeyListener);

        if (pathOverlay != null) {
            overlayManager.remove(pathOverlay);
        }
    }

    @Provides
    ClickTestConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ClickTestConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Can use for timing if needed
    }
}
