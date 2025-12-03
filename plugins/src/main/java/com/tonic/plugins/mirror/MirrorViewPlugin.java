package com.tonic.plugins.mirror;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

@PluginDescriptor(
    name = "Mirror View",
    description = "Opens a second window that mirrors the game canvas (including overlays).",
    tags = {"mirror", "view", "debug"}
)
public class MirrorViewPlugin extends Plugin
{
    @Inject
    private Client client;

    private JFrame frame;
    private MirrorPanel mirrorPanel;
    private Timer repaintTimer;

    @Override
    protected void startUp() throws Exception
    {
        SwingUtilities.invokeLater(() ->
        {
            frame = new JFrame("VitaLite Spy View");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            mirrorPanel = new MirrorPanel();
            frame.setContentPane(mirrorPanel);
            frame.setSize(new Dimension(800, 600));
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });

        repaintTimer = new Timer(33, e ->
        {
            if (mirrorPanel != null)
            {
                mirrorPanel.repaint();
            }
        });
        repaintTimer.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (repaintTimer != null)
        {
            repaintTimer.stop();
            repaintTimer = null;
        }

        SwingUtilities.invokeLater(() ->
        {
            if (frame != null)
            {
                frame.dispose();
                frame = null;
                mirrorPanel = null;
            }
        });
    }

    private class MirrorPanel extends JPanel
    {
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Client c = client;
            if (c == null)
            {
                return;
            }

            Player local = c.getLocalPlayer();
            if (local == null)
            {
                return;
            }

            WorldPoint center = local.getWorldLocation();
            if (center == null)
            {
                return;
            }

            int panelWidth = getWidth();
            int panelHeight = getHeight();
            if (panelWidth <= 0 || panelHeight <= 0)
            {
                return;
            }

            // Background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, panelWidth, panelHeight);

            int centerX = center.getX();
            int centerY = center.getY();
            int plane = center.getPlane();

            // Determine radius needed to include all players on this plane
            int maxRadius = 10;
            java.util.List<Player> players = c.getPlayers();
            for (Player p : players)
            {
                if (p == null)
                {
                    continue;
                }
                WorldPoint wp = p.getWorldLocation();
                if (wp == null || wp.getPlane() != plane)
                {
                    continue;
                }
                int dx = wp.getX() - centerX;
                int dy = wp.getY() - centerY;
                int r = Math.max(Math.abs(dx), Math.abs(dy));
                if (r > maxRadius)
                {
                    maxRadius = r;
                }
            }

            double marginFactor = 0.9;
            double scale = marginFactor * Math.min(panelWidth, panelHeight) / (maxRadius * 2.0);
            int centerPx = panelWidth / 2;
            int centerPy = panelHeight / 2;

            // Draw players
            for (Player p : players)
            {
                if (p == null)
                {
                    continue;
                }
                WorldPoint wp = p.getWorldLocation();
                if (wp == null || wp.getPlane() != plane)
                {
                    continue;
                }

                int dx = wp.getX() - centerX;
                int dy = wp.getY() - centerY;

                int sx = centerPx + (int) Math.round(dx * scale);
                int sy = centerPy - (int) Math.round(dy * scale);

                if (p == local)
                {
                    g.setColor(Color.GREEN);
                }
                else
                {
                    g.setColor(Color.RED);
                }

                int r = 4;
                g.fillOval(sx - r, sy - r, r * 2, r * 2);
            }
        }
    }
}
