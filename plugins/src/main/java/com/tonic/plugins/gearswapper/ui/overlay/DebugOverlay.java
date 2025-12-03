package com.tonic.plugins.gearswapper.ui.overlay;

import com.tonic.plugins.gearswapper.triggers.TriggerEngine;
import com.tonic.plugins.gearswapper.triggers.Trigger;
import com.tonic.plugins.gearswapper.GearSwapperConfig;
import com.tonic.api.game.CombatAPI;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Debug overlay for trigger system debugging
 */
public class DebugOverlay extends Overlay
{
    private static final Color BACKGROUND_COLOR = new Color(70, 70, 70, 180);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color TITLE_COLOR = new Color(120, 190, 255);
    private static final Color GOOD_COLOR = new Color(46, 204, 113);
    private static final Color BAD_COLOR = new Color(231, 76, 60);
    
    private final Client client;
    private final TriggerEngine triggerEngine;
    private final ConfigManager configManager;
    private final PanelComponent panelComponent = new PanelComponent();
    
    // Track last values for change detection
    private String lastTargetName = "";
    private int lastTargetHp = -1;
    private int lastDamageTaken = 0;
    private int lastPlayerHp = -1;
    private long lastUpdateTime = 0;
    private boolean lastDebugState = false;
    private int lastXpDrop = 0;
    private int lastPrayerPoints = -1;

    // Target caching
    private Actor cachedTarget = null;
    private long lastTargetTime = 0;
    private static final long TARGET_CACHE_DURATION = 30000; // Keep target for 30 seconds

    @Inject
    public DebugOverlay(Client client, TriggerEngine triggerEngine, ConfigManager configManager)
    {
        this.client = client;
        this.triggerEngine = triggerEngine;
        this.configManager = configManager;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Get config value once per render
        String configValue = configManager.getConfiguration(GearSwapperConfig.GROUP, "showDebugOverlay");
        boolean debugEnabled = configValue != null && Boolean.parseBoolean(configValue);
        
        // Debug logging (only log when state changes to reduce spam)
        if (debugEnabled != lastDebugState)
        {
            System.out.println("[DebugOverlay] Debug state changed: " + debugEnabled + " (config value: " + configValue + ")");
            lastDebugState = debugEnabled;
        }
        
        if (!debugEnabled)
        {
            return null;
        }

        // Update data every 500ms (slower to reduce flashing)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > 500)
        {
            // Clear and rebuild panel
            panelComponent.getChildren().clear();
            panelComponent.setPreferredSize(new Dimension(250, 0));
            panelComponent.setBackgroundColor(BACKGROUND_COLOR);

            // Update data
            updateDebugData();
            lastUpdateTime = currentTime;
        }

        return panelComponent.render(graphics);
    }

    /**
     * Get current target with caching
     */
    private Actor getCachedTarget()
    {
        Actor currentTarget = client.getLocalPlayer().getInteracting();
        
        if (currentTarget != null)
        {
            // Update cached target
            cachedTarget = currentTarget;
            lastTargetTime = System.currentTimeMillis();
        }
        else
        {
            // Check if cached target is still valid
            long currentTime = System.currentTimeMillis();
            if (cachedTarget != null && (currentTime - lastTargetTime) < TARGET_CACHE_DURATION)
            {
                // Return cached target if within cache duration
                return cachedTarget;
            }
            else
            {
                // Clear cached target if expired
                cachedTarget = null;
                lastTargetTime = 0;
            }
        }
        
        return currentTarget;
    }

    /**
     * Public method to get cached target (for plugin access)
     */
    public Actor getCachedTargetPublic()
    {
        return getCachedTarget();
    }

    private void updateDebugData()
    {
        // Main Info Section
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("🔍 TRIGGER DEBUG")
            .color(TITLE_COLOR)
            .build());

        Actor target = getCachedTarget();
        boolean hasTarget = target != null;
        boolean inCombat = hasTarget;
        
        List<Trigger> activeTriggers = triggerEngine.getActiveTriggers();
        int activeCount = (int) activeTriggers.stream()
            .filter(t -> t.isEnabled())
            .count();

        panelComponent.getChildren().add(createLine("Has Target:", hasTarget ? "Yes" : "No", hasTarget ? GOOD_COLOR : BAD_COLOR));
        panelComponent.getChildren().add(createLine("In Combat:", inCombat ? "Yes" : "No", inCombat ? GOOD_COLOR : BAD_COLOR));
        panelComponent.getChildren().add(createLine("Active Triggers:", String.valueOf(activeCount), TEXT_COLOR));

        // Target Info Section
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("🎯 TARGET INFO")
            .color(TITLE_COLOR)
            .build());

        if (target != null)
        {
            String targetName = target.getName() != null ? target.getName() : "Unknown";
            
            // Get target HP using VitaLite API
            int targetHealthPercentage = -1;
            int targetHealth = -1;
            int targetMaxHealth = -1;
            
            if (target != null)
            {
                try {
                    targetHealthPercentage = com.tonic.Static.invoke(() -> {
                        // Use the same percentage calculation as HP trigger handler
                        int healthRatio = target.getHealthRatio();
                        int healthScale = target.getHealthScale();
                        
                        if (healthRatio >= 0 && healthScale > 0)
                        {
                            return (healthRatio * 100) / healthScale;
                        }
                        return -1;
                    });
                } catch (Exception e) {
                    targetHealthPercentage = -1;
                }
            }
            
            // Simple hitsplat damage check - just get the hitsplat damage directly
            try {
                int hitsplatDamage = com.tonic.Static.invoke(() -> {
                    // Check if target has active hitsplats
                    if (target.getHealthRatio() >= 0 && target.getHealthScale() > 0)
                    {
                        // Try to get hitsplat from the target's health change
                        // Since we can't access hitsplats directly, we'll use a different approach
                        // Check if HP decreased significantly in one frame (indicating hitsplat damage)
                        int currentHealthRatio = target.getHealthRatio();
                        int currentHealthScale = target.getHealthScale();
                        
                        // Calculate current HP percentage
                        int currentHpPercent = (currentHealthRatio * 100) / currentHealthScale;
                        
                        // If we have a previous HP value and it dropped significantly, that's damage
                        if (lastTargetHp > 0 && currentHpPercent < lastTargetHp)
                        {
                            int damage = lastTargetHp - currentHpPercent;
                            return damage;
                        }
                    }
                    return 0;
                });
                
                if (hitsplatDamage > 0)
                {
                    lastDamageTaken = hitsplatDamage;
                }
            } catch (Exception e) {
                // Silently handle hitsplat check failure
            }
            
            // Add cache indicator
            boolean isCached = target == cachedTarget && client.getLocalPlayer().getInteracting() == null;
            String cacheIndicator = isCached ? " (cached)" : "";
            
            panelComponent.getChildren().add(createLine("Name:", targetName + cacheIndicator, TEXT_COLOR));
            
            // Display based on what we could get
            if (targetHealthPercentage >= 0) {
                // Got HP percentage - display it
                panelComponent.getChildren().add(createLine("HP %:", String.valueOf(targetHealthPercentage) + "%", TEXT_COLOR));
                panelComponent.getChildren().add(createLine("Status:", targetHealthPercentage >= 75 ? "Healthy" : 
                                                  targetHealthPercentage >= 50 ? "Hurt" : 
                                                  targetHealthPercentage >= 25 ? "Wounded" : 
                                                  targetHealthPercentage >= 0 ? "Critical" : "Unknown", 
                                                  targetHealthPercentage >= 75 ? GOOD_COLOR :
                                                  targetHealthPercentage >= 50 ? TEXT_COLOR :
                                                  targetHealthPercentage >= 25 ? BAD_COLOR :
                                                  targetHealthPercentage >= 0 ? BAD_COLOR : TEXT_COLOR));
                
                // Set targetHealth to percentage for damage detection
                targetHealth = targetHealthPercentage;
                
                // Display last damage
                if (lastDamageTaken > 0)
                {
                    panelComponent.getChildren().add(createLine("Last DMG:", String.valueOf(lastDamageTaken), BAD_COLOR));
                }
                else
                {
                    panelComponent.getChildren().add(createLine("Last DMG:", "None", TEXT_COLOR));
                }
                
                lastTargetHp = targetHealthPercentage;
            } else if (targetHealth > 0 && targetMaxHealth > 0) {
                // Got actual HP values (fallback)
                panelComponent.getChildren().add(createLine("HP:", String.valueOf(targetHealth) + "/" + String.valueOf(targetMaxHealth), TEXT_COLOR));
                panelComponent.getChildren().add(createLine("HP %:", String.valueOf((targetHealth * 100) / targetMaxHealth) + "%", TEXT_COLOR));
                // Display last damage
                if (lastDamageTaken > 0)
                {
                    panelComponent.getChildren().add(createLine("Last DMG:", String.valueOf(lastDamageTaken), BAD_COLOR));
                }
                else
                {
                    panelComponent.getChildren().add(createLine("Last DMG:", "None", TEXT_COLOR));
                }
                lastTargetHp = -1;
            }
            
            // Removed the last line of code here
            
            lastTargetName = targetName;
        }
        else
        {
            panelComponent.getChildren().add(createLine("Name:", "None", BAD_COLOR));
            panelComponent.getChildren().add(createLine("HP %:", "-", TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Status:", "None", TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Last DMG:", "None", TEXT_COLOR));
            
            lastTargetName = "";
            lastTargetHp = -1;
        }
        lastDamageTaken = -1;

        // Player Info Section
        panelComponent.getChildren().add(TitleComponent.builder()
            .text(" PLAYER INFO")
            .color(TITLE_COLOR)
            .build());

        Player player = client.getLocalPlayer();
        if (player != null)
        {
            // Use VitaLite API for accurate player HP
            int playerHealth = 0;
            int playerMaxHealth = 0;
            
            try {
                playerHealth = CombatAPI.getHealth();
                playerMaxHealth = client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
            } catch (Exception e) {
                // Fallback to old method if VitaLite API fails
                int playerHealthRatio = player.getHealthRatio();
                int playerHealthScale = player.getHealthScale();
                playerHealth = playerHealthScale > 0 ? (playerHealthRatio * playerHealthScale) / 255 : 0;
                playerMaxHealth = playerHealthScale;
            }
            
            int prayerPoints = client.getBoostedSkillLevel(net.runelite.api.Skill.PRAYER);
            
            // Check for damage
            if (lastPlayerHp != -1 && playerHealth < lastPlayerHp)
            {
                lastDamageTaken = lastPlayerHp - playerHealth;
            }
            
            panelComponent.getChildren().add(createLine("Name:", player.getName(), TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Boosted HP:", String.valueOf(playerHealth), TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Total HP:", String.valueOf(playerMaxHealth), TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Last DMG:", lastDamageTaken > 0 ? String.valueOf(lastDamageTaken) : "None", 
                lastDamageTaken > 0 ? BAD_COLOR : TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Prayer Points:", String.valueOf(prayerPoints), TEXT_COLOR));
            panelComponent.getChildren().add(createLine("Last XP Drop:", lastXpDrop > 0 ? String.valueOf(lastXpDrop) : "None", TEXT_COLOR));
            
            lastPlayerHp = playerHealth;
            lastPrayerPoints = prayerPoints;
        }

        // Active Triggers Section
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("⚡ ACTIVE TRIGGERS")
            .color(TITLE_COLOR)
            .build());

        if (activeCount > 0)
        {
            for (Trigger trigger : activeTriggers)
            {
                if (!trigger.isEnabled()) continue;
                
                String triggerInfo = String.format("%s (%s) - %s", 
                    trigger.getName(), 
                    trigger.getType().getDisplayName(),
                    trigger.isReadyToFire() ? "Ready" : "Cooldown");
                
                Color triggerColor = trigger.isReadyToFire() ? GOOD_COLOR : TEXT_COLOR;
                panelComponent.getChildren().add(createLine("", triggerInfo, triggerColor));
            }
        }
        else
        {
            panelComponent.getChildren().add(createLine("", "No active triggers", BAD_COLOR));
        }
    }

    private net.runelite.client.ui.overlay.components.LineComponent createLine(String label, String value, Color valueColor)
    {
        return net.runelite.client.ui.overlay.components.LineComponent.builder()
            .left(label)
            .right(value)
            .leftColor(TEXT_COLOR)
            .rightColor(valueColor)
            .build();
    }

    public void onXpDrop(int xpAmount)
    {
        lastXpDrop = xpAmount;
    }

    public void resetDamageTracking()
    {
        lastDamageTaken = -1;
    }
}
