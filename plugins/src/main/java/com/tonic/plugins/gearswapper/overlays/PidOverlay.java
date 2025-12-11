package com.tonic.plugins.gearswapper.overlays;

import com.tonic.plugins.gearswapper.GearSwapperConfig;
import com.tonic.plugins.gearswapper.combat.PidDetector.PidStatus;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

/**
 * Overlay that shows a colored square indicating PID status.
 * Green = ON PID (you attack first)
 * Red = OFF PID (opponent attacks first)
 */
public class PidOverlay extends Overlay {

    private final Client client;
    private final GearSwapperConfig config;

    private PidStatus currentPidStatus = PidStatus.UNKNOWN;
    private long lastUpdateTime = 0;
    private static final long DISPLAY_DURATION_MS = 3000; // Show for 3 seconds after update

    // Square size
    private static final int SQUARE_SIZE = 30;

    // Colors
    private static final Color COLOR_ON_PID = new Color(0, 200, 0, 200); // Green
    private static final Color COLOR_OFF_PID = new Color(200, 0, 0, 200); // Red
    private static final Color COLOR_UNKNOWN = new Color(128, 128, 128, 150); // Gray

    @Inject
    public PidOverlay(Client client, GearSwapperConfig config) {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    /**
     * Update the PID status to display.
     */
    public void updatePidStatus(PidStatus status) {
        this.currentPidStatus = status;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.pidOverlayEnabled()) {
            return null;
        }

        // Check if display has expired and reset to unknown
        long elapsed = System.currentTimeMillis() - lastUpdateTime;
        if (currentPidStatus != PidStatus.UNKNOWN && elapsed > DISPLAY_DURATION_MS) {
            currentPidStatus = PidStatus.UNKNOWN;
        }

        // Choose color based on status
        Color color;
        String text;
        switch (currentPidStatus) {
            case ON_PID:
                color = COLOR_ON_PID;
                text = "PID";
                break;
            case OFF_PID:
                color = COLOR_OFF_PID;
                text = "NO";
                break;
            default:
                color = COLOR_UNKNOWN;
                text = "?";
                break;
        }

        // Draw the square
        graphics.setColor(color);
        graphics.fillRect(0, 0, SQUARE_SIZE, SQUARE_SIZE);

        // Draw border
        graphics.setColor(Color.BLACK);
        graphics.drawRect(0, 0, SQUARE_SIZE - 1, SQUARE_SIZE - 1);

        // Draw text
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = (SQUARE_SIZE - textWidth) / 2;
        int textY = (SQUARE_SIZE + fm.getAscent() - fm.getDescent()) / 2;
        graphics.drawString(text, textX, textY);

        return new Dimension(SQUARE_SIZE, SQUARE_SIZE);
    }
}
