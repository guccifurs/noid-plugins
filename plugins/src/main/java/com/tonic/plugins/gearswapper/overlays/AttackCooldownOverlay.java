package com.tonic.plugins.gearswapper.overlays;

import com.tonic.plugins.gearswapper.GearSwapperConfig;
import com.tonic.plugins.gearswapper.combat.AttackCooldownTracker;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

/**
 * Overlay that displays attack cooldown timers above player and target heads
 */
public class AttackCooldownOverlay extends Overlay {

    private final Client client;
    private final GearSwapperConfig config;
    private AttackCooldownTracker tracker;

    // Colors
    private static final Color COLOR_READY = new Color(100, 255, 100); // Green
    private static final Color COLOR_COOLDOWN = new Color(255, 100, 100); // Red
    private static final Color COLOR_BACKGROUND = new Color(0, 0, 0, 180);

    @Inject
    public AttackCooldownOverlay(Client client, GearSwapperConfig config) {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    public void setTracker(AttackCooldownTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showAttackCooldown() || tracker == null) {
            return null;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return null;
        }

        // Render player cooldown
        renderPlayerCooldown(graphics, localPlayer);

        // Render target cooldown (use cached actor if getInteracting() is null)
        Actor target = tracker.getLastTargetActor();
        if (target != null) {
            renderTargetCooldown(graphics, target);
        }

        return null;
    }

    /**
     * Render cooldown above player's head
     */
    private void renderPlayerCooldown(Graphics2D graphics, Player player) {
        int cooldownTicks = tracker.getPlayerCooldownTicks();
        int weaponSpeed = tracker.getPlayerWeaponSpeed();
        boolean isReady = tracker.isPlayerReady();

        // User requested to hide "READY" text. Only show when on cooldown.
        if (isReady) {
            return;
        }

        String text = String.format("⚔ %d/%d", cooldownTicks, weaponSpeed);
        Color textColor = COLOR_COOLDOWN;

        renderTextAboveHead(graphics, player, text, textColor, 40);
    }

    /**
     * Render cooldown above target's head
     */
    private void renderTargetCooldown(Graphics2D graphics, Actor target) {
        int cooldownTicks = tracker.getTargetCooldownTicks();
        int weaponSpeed = tracker.getTargetWeaponSpeed();
        boolean isReady = tracker.isTargetReady();

        // Only show when on cooldown (hide "0/X" when ready)
        if (isReady) {
            return;
        }

        String text = String.format("⚔ %d/%d", cooldownTicks, weaponSpeed);
        Color textColor = COLOR_COOLDOWN;

        renderTextAboveHead(graphics, target, text, textColor, 40);
    }

    /**
     * Render text above an actor's head
     */
    private void renderTextAboveHead(Graphics2D graphics, Actor actor, String text, Color textColor, int zOffset) {
        LocalPoint localPoint = actor.getLocalLocation();
        if (localPoint == null) {
            return;
        }

        Point point = Perspective.getCanvasTextLocation(
                client,
                graphics,
                localPoint,
                text,
                zOffset);

        if (point == null) {
            return;
        }

        // Draw background
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        graphics.setColor(COLOR_BACKGROUND);
        graphics.fillRect(
                point.getX() - textWidth / 2 - 4,
                point.getY() - textHeight + 2,
                textWidth + 8,
                textHeight + 4);

        // Draw text
        graphics.setColor(textColor);
        graphics.setFont(new Font("Arial", Font.BOLD, 14));
        graphics.drawString(
                text,
                point.getX() - textWidth / 2,
                point.getY());
    }
}
