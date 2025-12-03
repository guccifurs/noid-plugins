package com.tonic.plugins.helperbox;

import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class HelperBoxOverlay extends Overlay
{
    private static final int WIDTH = 220;
    private static final int HEIGHT = 100;

    private final Client client;
    private final HelperBoxConfig config;
    private final HelperBoxPlugin plugin;

    private long sessionStartMs = 0L;
    private int sessionStartXp = 0;

    @Inject
    public HelperBoxOverlay(Client client, HelperBoxConfig config, HelperBoxPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.enabled())
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

        int stepIndex = plugin != null ? plugin.getStepIndex() : 0;
        String stepLabel = plugin != null ? plugin.getStepLabel() : "Idle";
        int totalSteps = plugin != null ? plugin.getTotalSteps() : AgilityDraynorTask.TOTAL_STEPS;
        double stepProgress = 0.0;
        if (stepIndex > 0 && totalSteps > 0)
        {
            stepProgress = stepIndex / (double) totalSteps;
            stepProgress = Math.max(0.0, Math.min(1.0, stepProgress));
        }

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
        graphics.drawString("HelperBox Agility", x, y);
        y += 14;

        // XP line
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 11));
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(String.format("XP: %,d (%.0f xp/hr)", xpGained, xpPerHour), x, y);
        y += 14;

        // Step line
        graphics.setColor(Color.CYAN);
        graphics.drawString(String.format("Step %d/%d: %s", stepIndex, totalSteps, stepLabel), x, y);
        y += 16;

        // Level progress bar
        int barX = x;
        int barY = y;
        int barWidth = WIDTH - 2 * x;
        int barHeight = 12;
        graphics.setColor(new Color(40, 40, 40, 220));
        graphics.fillRoundRect(barX, barY, barWidth, barHeight, 6, 6);

        int fillWidth = (int) (barWidth * levelProgress);
        graphics.setColor(new Color(80, 180, 80, 220));
        graphics.fillRoundRect(barX, barY, fillWidth, barHeight, 6, 6);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String progressText = String.format("Lvl %d → %d (%.1f%%)", currentLevel, nextLevel, levelProgress * 100.0);
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(progressText);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY + ((barHeight + fm.getAscent()) / 2) - 2;
        graphics.drawString(progressText, textX, textY);

        // Step progress bar
        int stepBarY = barY + barHeight + 4;
        int stepBarHeight = 8;
        graphics.setColor(new Color(40, 40, 40, 220));
        graphics.fillRoundRect(barX, stepBarY, barWidth, stepBarHeight, 6, 6);

        int stepFillWidth = (int) (barWidth * stepProgress);
        graphics.setColor(new Color(180, 140, 80, 220));
        graphics.fillRoundRect(barX, stepBarY, stepFillWidth, stepBarHeight, 6, 6);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 9));
        String stepProgressText = String.format("%d/%d", stepIndex, totalSteps);
        FontMetrics sfm = graphics.getFontMetrics();
        int sTextWidth = sfm.stringWidth(stepProgressText);
        int sTextX = barX + (barWidth - sTextWidth) / 2;
        int sTextY = stepBarY + ((stepBarHeight + sfm.getAscent()) / 2) - 2;
        graphics.drawString(stepProgressText, sTextX, sTextY);

        return new Dimension(WIDTH, HEIGHT);
    }
}
