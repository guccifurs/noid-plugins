package com.tonic.plugins.gearswapper.friendshare;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Window that displays received screen share frames.
 * Uses double buffering to prevent flashing.
 */
public class ScreenViewer extends JFrame {

    private final ImagePanel imagePanel;
    private final JLabel statusLabel;
    private String peerName;
    private Runnable onClose;

    private long lastFrameTime = 0;
    private int frameCount = 0;
    private int totalFrames = 0;
    private int fps = 0;

    public ScreenViewer(String peerName) {
        super("Viewing: " + peerName);
        this.peerName = peerName;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Dark theme
        getContentPane().setBackground(new Color(30, 30, 30));
        setLayout(new BorderLayout());

        // Custom image panel with double buffering
        imagePanel = new ImagePanel();
        imagePanel.setBackground(Color.BLACK);
        add(imagePanel, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(45, 45, 48));
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("Waiting for frames...");
        statusLabel.setForeground(new Color(150, 150, 150));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("Close");
        closeButton.setBackground(new Color(100, 50, 50));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> dispose());
        statusBar.add(closeButton, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);

        // Handle window close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        });
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (title.startsWith("Viewing: ")) {
            this.peerName = title.substring(9);
        }
    }

    /**
     * Display a received JPEG frame.
     */
    public void displayFrame(byte[] jpegData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpegData));
            if (image != null) {
                totalFrames++;
                imagePanel.setImage(image);

                // Update FPS counter
                frameCount++;
                long now = System.currentTimeMillis();
                if (now - lastFrameTime >= 1000) {
                    fps = frameCount;
                    frameCount = 0;
                    lastFrameTime = now;

                    final int currentFps = fps;
                    final int size = jpegData.length;
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(String.format("Viewing %s | %d FPS | %d KB",
                                peerName, currentFps, size / 1024));
                    });
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Set callback for when window is closed.
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Custom panel that draws images with double buffering.
     */
    private static class ImagePanel extends JPanel {
        private BufferedImage scaledImage;

        public ImagePanel() {
            setDoubleBuffered(true);
        }

        public void setImage(BufferedImage image) {
            int panelW = getWidth();
            int panelH = getHeight();

            if (panelW > 0 && panelH > 0 && image != null) {
                // Calculate scaled size maintaining aspect ratio
                double scale = Math.min((double) panelW / image.getWidth(),
                        (double) panelH / image.getHeight());
                int newW = (int) (image.getWidth() * scale);
                int newH = (int) (image.getHeight() * scale);

                scaledImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scaledImage.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, newW, newH, null);
                g.dispose();
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (scaledImage != null) {
                int x = (getWidth() - scaledImage.getWidth()) / 2;
                int y = (getHeight() - scaledImage.getHeight()) / 2;
                g.drawImage(scaledImage, x, y, null);
            } else {
                g.setColor(Color.GRAY);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                String msg = "Waiting for frames...";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(msg)) / 2;
                int y = getHeight() / 2;
                g.drawString(msg, x, y);
            }
        }
    }
}
