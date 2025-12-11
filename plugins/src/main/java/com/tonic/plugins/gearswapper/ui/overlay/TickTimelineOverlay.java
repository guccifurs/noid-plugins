package com.tonic.plugins.gearswapper.ui.overlay;

import com.tonic.plugins.gearswapper.GearSwapperConfig;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Overlay that visualizes game ticks as a scrolling timeline with click events.
 * Helps users understand when their clicks occur relative to tick boundaries.
 */
public class TickTimelineOverlay extends Overlay {

    private static final int TICK_DURATION_MS = 600;
    private static final int TIMELINE_WIDTH = 400;
    private static final int TIMELINE_HEIGHT = 60;
    private static final int TICKS_TO_SHOW = 5; // Show last N ticks worth of time
    private static final long HISTORY_DURATION_MS = TICK_DURATION_MS * TICKS_TO_SHOW;

    private final GearSwapperConfig config;

    // Click event tracking
    private static class ClickEvent {
        final long timestamp;
        final String label;
        final Color color;

        ClickEvent(long timestamp, String label, Color color) {
            this.timestamp = timestamp;
            this.label = label;
            this.color = color;
        }
    }

    private final ConcurrentLinkedDeque<ClickEvent> clickEvents = new ConcurrentLinkedDeque<>();
    private volatile long lastTickTimestamp = 0;
    private volatile int currentTick = 0;

    @Inject
    public TickTimelineOverlay(Client client, GearSwapperConfig config) {
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    public void onGameTick(int tickCount) {
        this.lastTickTimestamp = System.currentTimeMillis();
        this.currentTick = tickCount;
    }

    public void recordClick(String label, Color color) {
        clickEvents.add(new ClickEvent(System.currentTimeMillis(), label, color));
        // Prune old events
        long cutoff = System.currentTimeMillis() - HISTORY_DURATION_MS;
        Iterator<ClickEvent> it = clickEvents.iterator();
        while (it.hasNext()) {
            if (it.next().timestamp < cutoff) {
                it.remove();
            } else {
                break; // Events are in order, so we can stop early
            }
        }
    }

    public void recordClick(String label) {
        recordClick(label, Color.CYAN);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Check if enabled
        try {
            if (!config.showTickTimeline()) {
                return null;
            }
        } catch (Exception e) {
            return null; // Config method doesn't exist yet
        }

        long now = System.currentTimeMillis();

        // Background
        int x = 10;
        int y = 10;
        graphics.setColor(new Color(20, 20, 30, 220));
        graphics.fillRoundRect(x, y, TIMELINE_WIDTH, TIMELINE_HEIGHT, 8, 8);
        graphics.setColor(new Color(60, 100, 180, 200));
        graphics.drawRoundRect(x, y, TIMELINE_WIDTH, TIMELINE_HEIGHT, 8, 8);

        // Title
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 11f));
        graphics.setColor(Color.WHITE);
        graphics.drawString("Tick Timeline", x + 5, y + 12);

        // Timeline area
        int tlX = x + 10;
        int tlY = y + 20;
        int tlWidth = TIMELINE_WIDTH - 20;
        int tlHeight = 30;

        // Timeline background
        graphics.setColor(new Color(10, 10, 20, 200));
        graphics.fillRect(tlX, tlY, tlWidth, tlHeight);

        // Draw tick markers
        if (lastTickTimestamp > 0) {
            long timelineStart = now - HISTORY_DURATION_MS;

            // Find the first tick that would appear on the timeline
            long firstTickTime = lastTickTimestamp;
            while (firstTickTime > timelineStart) {
                firstTickTime -= TICK_DURATION_MS;
            }

            // Draw tick lines
            graphics.setStroke(new BasicStroke(1));
            for (long tickTime = firstTickTime; tickTime <= now + TICK_DURATION_MS; tickTime += TICK_DURATION_MS) {
                float progress = (float) (tickTime - timelineStart) / HISTORY_DURATION_MS;
                int tickX = tlX + (int) (progress * tlWidth);

                if (tickX >= tlX && tickX <= tlX + tlWidth) {
                    // Tick line
                    graphics.setColor(new Color(100, 150, 255, 150));
                    graphics.drawLine(tickX, tlY, tickX, tlY + tlHeight);

                    // Tick number (approximate)
                    int tickNum = currentTick - (int) ((now - tickTime) / TICK_DURATION_MS);
                    if (tickNum > 0) {
                        graphics.setColor(new Color(150, 180, 255, 180));
                        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 9f));
                        graphics.drawString(String.valueOf(tickNum % 100), tickX + 2, tlY + tlHeight - 2);
                    }
                }
            }

            // Draw "now" marker
            graphics.setColor(Color.YELLOW);
            graphics.setStroke(new BasicStroke(2));
            int nowX = tlX + tlWidth - 10; // Now is near the right edge
            graphics.drawLine(nowX, tlY - 2, nowX, tlY + tlHeight + 2);

            // Draw click events
            for (ClickEvent event : clickEvents) {
                float progress = (float) (event.timestamp - timelineStart) / HISTORY_DURATION_MS;
                int eventX = tlX + (int) (progress * tlWidth);

                if (eventX >= tlX && eventX <= tlX + tlWidth) {
                    // Click dot
                    graphics.setColor(event.color);
                    graphics.fillOval(eventX - 4, tlY + tlHeight / 2 - 4, 8, 8);

                    // Label (if space)
                    if (event.label != null && event.label.length() <= 8) {
                        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 8f));
                        graphics.drawString(event.label, eventX - 10, tlY - 2);
                    }
                }
            }
        }

        // Current tick info
        graphics.setColor(Color.WHITE);
        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 10f));
        long msSinceTick = lastTickTimestamp > 0 ? now - lastTickTimestamp : 0;
        String info = String.format("Tick %d | %dms", currentTick, msSinceTick);
        graphics.drawString(info, x + TIMELINE_WIDTH - 90, y + 12);

        return new Dimension(TIMELINE_WIDTH + 20, TIMELINE_HEIGHT + 20);
    }
}
