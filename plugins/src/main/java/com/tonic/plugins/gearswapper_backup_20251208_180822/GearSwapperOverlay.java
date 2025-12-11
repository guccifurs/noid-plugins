package com.tonic.plugins.gearswapper;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

public class GearSwapperOverlay extends Overlay
{
    private final Client client;
    private final GearSwapperPlugin plugin;
    private final GearSwapperConfig config;
    private final BufferedImage freezeIcon;
    private final BufferedImage freezeImmuneIcon;

    @Inject
    public GearSwapperOverlay(Client client, GearSwapperPlugin plugin, GearSwapperConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        this.freezeIcon = ImageUtil.loadImageResource(GearSwapperOverlay.class, "freeze.png");
        this.freezeImmuneIcon = ImageUtil.loadImageResource(GearSwapperOverlay.class, "freezeimmune.png");

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    @SuppressWarnings({"unchecked", "all"})
    public Dimension render(Graphics2D graphics)
    {
        boolean freezeOverlayEnabled = config.showFreezeOverlay();
        boolean animationOverlayEnabled = config.showAnimationIdOverlay();
        boolean targetOverlayEnabled = config.showTargetOverlay();
        boolean mouseTest = plugin.isMouseCircleTestRunning();
        if (!freezeOverlayEnabled && !mouseTest && !animationOverlayEnabled && !targetOverlayEnabled)
        {
            return null;
        }

        int playerFreeze = 0;
        int playerImmune = 0;
        Player target = null;
        int targetFreeze = 0;
        int targetImmune = 0;

        if (freezeOverlayEnabled)
        {
            try
            {
                Method getTargetMethod = plugin.getClass().getMethod("getCurrentTarget");
                Object t = getTargetMethod.invoke(plugin);
                if (t instanceof Player)
                {
                    target = (Player) t;
                }

                Method getPlayerFreezeMethod = plugin.getClass().getMethod("getPlayerFreezeTicks");
                Object pf = getPlayerFreezeMethod.invoke(plugin);
                if (pf instanceof Integer)
                {
                    playerFreeze = (Integer) pf;
                }

                Method getPlayerImmMethod = plugin.getClass().getMethod("getPlayerFreezeImmunityTicks");
                Object pi = getPlayerImmMethod.invoke(plugin);
                if (pi instanceof Integer)
                {
                    playerImmune = (Integer) pi;
                }

                Method getFreezeMethod = plugin.getClass().getMethod("getTargetFreezeTicks");
                Object f = getFreezeMethod.invoke(plugin);
                if (f instanceof Integer)
                {
                    targetFreeze = (Integer) f;
                }

                Method getTargetImmMethod = plugin.getClass().getMethod("getTargetFreezeImmunityTicks");
                Object ti = getTargetImmMethod.invoke(plugin);
                if (ti instanceof Integer)
                {
                    targetImmune = (Integer) ti;
                }
            }
            catch (Exception ignored)
            {
            }

            Player local = client.getLocalPlayer();
            if (local != null && (playerFreeze > 0 || playerImmune > 0))
            {
                LocalPoint lpLocal = local.getLocalLocation();
                if (lpLocal != null)
                {
                    String playerLabel;
                    BufferedImage icon;

                    if (playerFreeze > 0)
                    {
                        playerLabel = "FZ " + playerFreeze;
                        icon = freezeIcon;
                    }
                    else
                    {
                        playerLabel = "IM " + playerImmune;
                        icon = freezeImmuneIcon;
                    }

                    Point playerTextLoc = Perspective.getCanvasTextLocation(client, graphics, lpLocal, playerLabel, 40);
                    if (playerTextLoc != null)
                    {
                        if (icon != null)
                        {
                            int ix = playerTextLoc.getX() - icon.getWidth() / 2;
                            int iy = playerTextLoc.getY() - icon.getHeight();
                            graphics.drawImage(icon, ix, iy, null);
                        }

                        OverlayUtil.renderTextLocation(graphics, playerTextLoc, playerLabel, Color.CYAN);
                    }
                }
            }

            if (target != null)
            {
                String freezeText = "";
                BufferedImage targetIcon = null;

                if (targetFreeze > 0)
                {
                    freezeText = "FZ " + targetFreeze;
                    targetIcon = freezeIcon;
                }
                else if (targetImmune > 0)
                {
                    freezeText = "IM " + targetImmune;
                    targetIcon = freezeImmuneIcon;
                }

                if (!freezeText.isEmpty())
                {
                    String label = freezeText;
                    LocalPoint lp = target.getLocalLocation();
                    if (lp != null)
                    {
                        Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, label, 40);
                        if (textLoc != null)
                        {
                            Color color = (targetFreeze > 0 || targetImmune > 0) ? Color.CYAN : Color.WHITE;

                            if (targetIcon != null)
                            {
                                int ix = textLoc.getX() - targetIcon.getWidth() / 2;
                                int iy = textLoc.getY() - targetIcon.getHeight();
                                graphics.drawImage(targetIcon, ix, iy, null);
                            }

                            OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
                        }
                    }
                }
            }
        }

        if (targetOverlayEnabled)
        {
            try
            {
                Player targetForOverlay = plugin.getCurrentTarget();
                if (targetForOverlay != null)
                {
                    LocalPoint lp = targetForOverlay.getLocalLocation();
                    if (lp != null)
                    {
                        String name = targetForOverlay.getName();
                        if (name != null)
                        {
                            Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, name, 0);
                            if (textLoc != null)
                            {
                                int offset = 0;
                                try
                                {
                                    offset = config.targetTextOffset();
                                }
                                catch (Exception ignored)
                                {
                                }

                                if (offset != 0)
                                {
                                    textLoc = new Point(textLoc.getX(), textLoc.getY() - offset);
                                }

                                OverlayUtil.renderTextLocation(graphics, textLoc, name, Color.RED);
                            }
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
            }
        }

        if (animationOverlayEnabled)
        {
            java.util.List<GearSwapperPlugin.AnimationDebugEntry> entries = plugin.getRecentAnimations();
            if (!entries.isEmpty())
            {
                int maxLines = Math.min(5, entries.size());
                int lineHeight = 22;
                int padding = 12;
                int width = 320;
                int height = padding * 2 + lineHeight * (maxLines + 1) + 10; // extra room for header
                int x = 30;
                int y = 90;

                Color background = new Color(10, 20, 40, 230);
                Color border = new Color(80, 160, 255, 255);
                Color idColor = new Color(120, 190, 255, 255);

                graphics.setColor(background);
                graphics.fillRoundRect(x, y, width, height, 10, 10);
                graphics.setColor(border);
                graphics.drawRoundRect(x, y, width, height, 10, 10);

                int textX = x + padding;
                int textY = y + padding + 18;

                graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 14f));
                graphics.setColor(Color.WHITE);
                graphics.drawString("Last Animations", textX, textY);
                textY += lineHeight;

                graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 14f));

                for (int i = entries.size() - 1; i >= 0 && maxLines > 0; i--, maxLines--)
                {
                    GearSwapperPlugin.AnimationDebugEntry entry = entries.get(i);
                    String idText = String.valueOf(entry.animationId);
                    String label = entry.ours ? "(Ours)" : (entry.targ ? "(Targ)" : "(Other)");

                    graphics.setColor(idColor);
                    graphics.drawString(idText, textX, textY);

                    int idWidth = graphics.getFontMetrics().stringWidth(idText + " ");
                    graphics.setColor(Color.WHITE);
                    graphics.drawString(label, textX + idWidth, textY);

                    textY += lineHeight;
                }
            }
        }

        if (mouseTest)
        {
            long startMs = plugin.getMouseCircleTestStartMs();
            long durationMs = plugin.getMouseCircleTestDurationMs();
            int centerX = plugin.getMouseCircleCenterX();
            int centerY = plugin.getMouseCircleCenterY();
            int radius = plugin.getMouseCircleRadius();

            if (startMs > 0 && durationMs > 0 && centerX >= 0 && centerY >= 0 && radius > 0)
            {
                long now = System.currentTimeMillis();
                double t = (double) (now - startMs) / (double) durationMs;
                if (t < 0.0)
                {
                    t = 0.0;
                }
                if (t > 1.0)
                {
                    t = 1.0;
                }

                double angle = 2.0 * Math.PI * t;
                int x = centerX + (int) Math.round(radius * Math.cos(angle));
                int y = centerY + (int) Math.round(radius * Math.sin(angle));

                int size = 16;
                graphics.setColor(Color.YELLOW);
                graphics.drawOval(x - size / 2, y - size / 2, size, size);
                graphics.fillOval(x - 2, y - 2, 4, 4);
            }
        }

        return null;
    }
}
