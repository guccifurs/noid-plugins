package com.tonic.plugins.lmsnavigator;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class LMSOverlay extends Overlay
{
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public LMSOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(150, 140));

        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("LMS Status")
            .build());

        // Get current game mode
        String currentGameMode = getGameMode();

        // Add status information
        addStatusLine("Game mode:", currentGameMode);
        addStatusLine("Has target:", getTargetDisplay());
        addStatusLine("Bloody key:", getKeyDisplay());
        addStatusLine("Gear upgraded:", "Unknown");
        addStatusLine("Safe zone:", getSafeZone());
        addStatusLine("Is at final:", "Unknown");
        addStatusLine("Is in fog:", "Unknown");

        return panelComponent.render(graphics);
    }

    private void addStatusLine(String label, String value)
    {
        String line = label + " " + value;
        panelComponent.getChildren().add(TitleComponent.builder()
            .text(line)
            .build());
    }
    
    // Game mode detection
    private String getGameMode() { return GetMode.getCurrentMode(); }
    
    // Target detection
    private boolean hasTarget() { return TargetManagement.hasTarget(); }
    private String getTargetDisplay() { return TargetManagement.getTargetInfo(); }

    // Key detection
    private boolean hasKey() { return KeyManagement.hasBloodKey(); }
    private String getKeyDisplay() { return hasKey() ? "Yes" : "No"; }

    // Safe zone detection
    private String getSafeZone() { return SafeZoneManagement.getCurrentSafeZone(); }
    
    // Placeholder methods - will be implemented later
    private boolean isGearUpgraded() { return false; }
    private boolean isInSafeZone() { return false; }
    private boolean isAtFinal() { return false; }
    private boolean isInFog() { return false; }
}
