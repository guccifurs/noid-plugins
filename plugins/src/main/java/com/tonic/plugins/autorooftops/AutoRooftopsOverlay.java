package com.tonic.plugins.autorooftops;

import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class AutoRooftopsOverlay extends Overlay
{
    private static final int WIDTH = 200;
    private static final int HEIGHT = 80;

    private final Client client;
    private final AutoRooftopsPlugin plugin;
    private final AutoRooftopsConfig config;

    private long sessionStartMs = 0L;
    private int sessionStartXp = 0;

    @Inject
    public AutoRooftopsOverlay(Client client, AutoRooftopsPlugin plugin, AutoRooftopsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.enabled() || !plugin.isRunning())
        {
            // Reset session when not running
            sessionStartMs = 0L;
            sessionStartXp = 0;
            return null;
        }

        if (sessionStartMs == 0L)
        {
            sessionStartMs = System.currentTimeMillis();
            sessionStartXp = client.getSkillExperience(Skill.AGILITY);
        }

        int currentXp = client.getSkillExperience(Skill.AGILITY);
        int xpGained = Math.max(0, currentXp - sessionStartXp);
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - sessionStartMs);
        double xpPerHour = xpGained * 3600000.0 / elapsedMs;

        int currentLevel = client.getRealSkillLevel(Skill.AGILITY);
        int nextLevel = Math.min(99, currentLevel + 1);
        int levelStartXp = Experience.getXpForLevel(currentLevel);
        int levelEndXp = Experience.getXpForLevel(nextLevel);
        double levelProgress = 0.0;
        if (levelEndXp > levelStartXp)
        {
            levelProgress = (currentXp - levelStartXp) / (double) (levelEndXp - levelStartXp);
            levelProgress = Math.max(0.0, Math.min(1.0, levelProgress));
        }

        int laps = plugin.getLapsCompleted();
        int marks = plugin.getMarksCollected();

        // Background
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRoundRect(0, 0, WIDTH, HEIGHT, 10, 10);

        // Border
        graphics.setColor(new Color(200, 170, 100, 220));
        graphics.drawRoundRect(0, 0, WIDTH, HEIGHT, 10, 10);

        int x = 8;
        int y = 18;

        graphics.setFont(new Font("SansSerif", Font.BOLD, 12));
        graphics.setColor(Color.WHITE);
        graphics.drawString("Auto Rooftops (Agility)", x, y);
        y += 14;

        // XP line
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 11));
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(String.format("XP: %,d (%.0f xp/hr)", xpGained, xpPerHour), x, y);
        y += 14;

        // Laps / marks
        graphics.drawString(String.format("Laps: %d   Marks: %d", laps, marks), x, y);
        y += 16;

        // Progress bar background
        int barX = x;
        int barY = y;
        int barWidth = WIDTH - 2 * x;
        int barHeight = 12;
        graphics.setColor(new Color(40, 40, 40, 220));
        graphics.fillRoundRect(barX, barY, barWidth, barHeight, 6, 6);

        // Progress fill
        int fillWidth = (int) (barWidth * levelProgress);
        graphics.setColor(new Color(80, 180, 80, 220));
        graphics.fillRoundRect(barX, barY, fillWidth, barHeight, 6, 6);

        // Progress text
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String progressText = String.format("Lvl %d → %d (%.1f%%)", currentLevel, nextLevel, levelProgress * 100.0);
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(progressText);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY + ((barHeight + fm.getAscent()) / 2) - 2;
        graphics.drawString(progressText, textX, textY);

        return new Dimension(WIDTH, HEIGHT);
    }
}
