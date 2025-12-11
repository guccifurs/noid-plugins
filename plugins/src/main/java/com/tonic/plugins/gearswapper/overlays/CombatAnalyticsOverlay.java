package com.tonic.plugins.gearswapper.overlays;

import com.tonic.plugins.gearswapper.GearSwapperConfig;
import com.tonic.plugins.gearswapper.combat.CombatAnalyticsTracker;
import com.tonic.plugins.gearswapper.combat.CombatAnalyticsTracker.AttackRecord;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

/**
 * Overlay that displays comprehensive combat analytics data
 * Shows attack animations, projectiles, hitsplats, distances, and timing
 */
public class CombatAnalyticsOverlay extends Overlay {

        private final Client client;
        private final GearSwapperConfig config;
        private final PanelComponent panelComponent = new PanelComponent();

        private CombatAnalyticsTracker tracker;

        // Colors
        private static final Color COLOR_TITLE = new Color(255, 176, 59);
        private static final Color COLOR_HEADER = new Color(100, 200, 255);
        private static final Color COLOR_VALUE = Color.WHITE;
        private static final Color COLOR_SPLASH = new Color(255, 100, 100);
        private static final Color COLOR_HIT = new Color(100, 255, 100);
        private static final Color COLOR_MUTED = new Color(180, 180, 180);
        private static final Color COLOR_MAGIC = new Color(100, 150, 255);
        private static final Color COLOR_RANGED = new Color(100, 255, 100);
        private static final Color COLOR_MELEE = new Color(255, 150, 100);

        @Inject
        public CombatAnalyticsOverlay(Client client, GearSwapperConfig config) {
                this.client = client;
                this.config = config;

                setPosition(OverlayPosition.TOP_LEFT);
                setLayer(OverlayLayer.ABOVE_WIDGETS);
                setPriority(OverlayPriority.HIGH);
        }

        public void setTracker(CombatAnalyticsTracker tracker) {
                this.tracker = tracker;
        }

        @Override
        public Dimension render(Graphics2D graphics) {
                if (!config.showCombatAnalytics() || tracker == null) {
                        return null;
                }

                panelComponent.getChildren().clear();
                panelComponent.setBackgroundColor(new Color(30, 30, 30, 220));
                panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
                panelComponent.setPreferredSize(new Dimension(280, 0)); // Make overlay wider

                // Title
                panelComponent.getChildren().add(TitleComponent.builder()
                                .text("⚔ COMBAT ANALYTICS")
                                .color(COLOR_TITLE)
                                .build());

                // Check for current or last attack
                AttackRecord currentAttack = tracker.getCurrentAttack();
                AttackRecord lastAttack = tracker.getLastAttack();

                if (currentAttack != null) {
                        renderAttackRecord(currentAttack, "TRACKING...");
                } else if (lastAttack != null) {
                        renderAttackRecord(lastAttack, "LAST ATTACK");
                } else {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Waiting for attack...")
                                        .leftColor(COLOR_MUTED)
                                        .build());
                }

                // Show recent history (last 3)
                List<AttackRecord> history = tracker.getAttackHistory();
                if (history.size() > 1) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("─── HISTORY ───")
                                        .leftColor(COLOR_HEADER)
                                        .build());

                        int count = 0;
                        for (AttackRecord record : history) {
                                if (count == 0) {
                                        count++; // Skip first (already shown above)
                                        continue;
                                }
                                if (count > 3)
                                        break; // Max 3 history items

                                panelComponent.getChildren().add(LineComponent.builder()
                                                .left(record.getSummary())
                                                .leftColor(COLOR_MUTED)
                                                .build());
                                count++;
                        }
                }

                return panelComponent.render(graphics);
        }

        private void renderAttackRecord(AttackRecord record, String header) {
                // Header
                panelComponent.getChildren().add(LineComponent.builder()
                                .left("─── " + header + " ───")
                                .leftColor(COLOR_HEADER)
                                .build());

                // Attack type color
                Color attackColor = getAttackTypeColor(record.attackType);

                // Animation ID
                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Animation ID")
                                .leftColor(COLOR_VALUE)
                                .right(String.valueOf(record.playerAnimationId))
                                .rightColor(attackColor)
                                .build());

                // Attack type
                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Type")
                                .leftColor(COLOR_VALUE)
                                .right(record.attackType)
                                .rightColor(attackColor)
                                .build());

                // Target
                if (record.targetName != null) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Target")
                                        .leftColor(COLOR_VALUE)
                                        .right(record.targetName)
                                        .rightColor(COLOR_VALUE)
                                        .build());
                }

                // Distance
                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Distance")
                                .leftColor(COLOR_VALUE)
                                .right(record.distanceToTarget + " tiles")
                                .rightColor(COLOR_VALUE)
                                .build());

                // Cast tick
                panelComponent.getChildren().add(LineComponent.builder()
                                .left("Cast Tick")
                                .leftColor(COLOR_VALUE)
                                .right(String.valueOf(record.castTick))
                                .rightColor(COLOR_MUTED)
                                .build());

                // Projectile section
                if (record.hasProjectile) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("─ PROJECTILE ─")
                                        .leftColor(COLOR_HEADER)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Projectile ID")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.valueOf(record.projectileId))
                                        .rightColor(COLOR_VALUE)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Start Cycle")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.valueOf(record.projectileStartCycle))
                                        .rightColor(COLOR_MUTED)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("End Cycle")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.valueOf(record.projectileEndCycle))
                                        .rightColor(COLOR_MUTED)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Travel Time")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.format("%.1f ticks (%d cycles)",
                                                        record.projectileTravelTicks, record.projectileTravelCycles))
                                        .rightColor(COLOR_TITLE)
                                        .build());
                }

                // Hitsplat section
                if (record.hasHitsplat) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("─── RESULT ───")
                                        .leftColor(COLOR_HEADER)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Hit Tick")
                                        .leftColor(COLOR_VALUE)
                                        .right(record.hitsplatTick + " (+" + record.ticksFromCastToHit + " ticks)")
                                        .rightColor(COLOR_VALUE)
                                        .build());

                        Color resultColor = record.isSplash ? COLOR_SPLASH : COLOR_HIT;
                        String resultText = record.isSplash ? "SPLASH (0)" : "HIT (" + record.hitsplatAmount + ")";

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Result")
                                        .leftColor(COLOR_VALUE)
                                        .right(resultText)
                                        .rightColor(resultColor)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Hitsplat Type")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.valueOf(record.hitsplatType))
                                        .rightColor(COLOR_MUTED)
                                        .build());
                }

                // GFX section (Splash timing)
                if (record.hasTargetGfx) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("─ TARGET GFX ─")
                                        .leftColor(COLOR_HEADER)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("GFX ID")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.valueOf(record.targetGfxId))
                                        .rightColor(COLOR_MAGIC)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("GFX Tick")
                                        .leftColor(COLOR_VALUE)
                                        .right(String.valueOf(record.targetGfxTick))
                                        .rightColor(COLOR_MUTED)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Cast → GFX")
                                        .leftColor(COLOR_VALUE)
                                        .right(record.ticksFromCastToGfx + " ticks")
                                        .rightColor(COLOR_TITLE)
                                        .build());
                }

                // Summary
                if (record.hasHitsplat) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("─── SUMMARY ───")
                                        .leftColor(COLOR_HEADER)
                                        .build());

                        panelComponent.getChildren().add(LineComponent.builder()
                                        .left("Cast → Hit")
                                        .leftColor(COLOR_VALUE)
                                        .right(record.ticksFromCastToHit + " ticks")
                                        .rightColor(COLOR_TITLE)
                                        .build());
                }
        }

        private Color getAttackTypeColor(String type) {
                switch (type) {
                        case "Magic":
                                return COLOR_MAGIC;
                        case "Ranged":
                                return COLOR_RANGED;
                        case "Melee":
                                return COLOR_MELEE;
                        default:
                                return COLOR_VALUE;
                }
        }
}
