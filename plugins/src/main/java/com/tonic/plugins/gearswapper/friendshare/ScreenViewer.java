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
    private final String peerName;
    private Runnable onClose;

    private long lastFrameTime = 0;
    private int frameCount = 0;
    private int fps = 0;

    public ScreenViewer(String peerName) {
        super("Viewing: " + peerName + "'s Screen");
        this.peerName = peerName;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 550);
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

        statusLabel = new JLabel("Connecting...");
        statusLabel.setForeground(new Color(150, 150, 150));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.WEST);

        JButton endButton = new JButton("End");
        endButton.setBackground(new Color(200, 50, 50));
        endButton.setForeground(Color.WHITE);
        endButton.setFocusPainted(false);
        endButton.addActionListener(e -> dispose());
        statusBar.add(endButton, BorderLayout.EAST);

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

    /**
     * Display a received JPEG frame.
     */
    public void displayFrame(byte[] jpegData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpegData));
            if (image != null) {
                imagePanel.setImage(image);

                // Update FPS counter
                frameCount++;
                long now = System.currentTimeMillis();
                if (now - lastFrameTime >= 1000) {
                    fps = frameCount;
                    frameCount = 0;
                    lastFrameTime = now;

                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(String.format("Viewing %s | %d FPS | %d KB",
                                peerName, fps, jpegData.length / 1024));
                    });
                }
            }
        } catch (Exception e) {
            // Ignore bad frames
        }
    }

    /**
     * Set callback for when window is closed.
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Show a message in the viewer.
     */
    public void showMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Custom panel that draws images with double buffering.
     */
    private static class ImagePanel extends JPanel {
        private BufferedImage currentImage;
        private BufferedImage scaledImage;
        private int lastWidth = 0;
        private int lastHeight = 0;

        public ImagePanel() {
            setDoubleBuffered(true);
        }

        public void setImage(BufferedImage image) {
            this.currentImage = image;

            // Only rescale if panel size changed or first image
            int panelW = getWidth();
            int panelH = getHeight();

            if (panelW > 0 && panelH > 0 && image != null) {
                // Calculate scaled size maintaining aspect ratio
                double scale = Math.min((double) panelW / image.getWidth(),
                        (double) panelH / image.getHeight());
                int newW = (int) (image.getWidth() * scale);
                int newH = (int) (image.getHeight() * scale);

                // Only create new scaled image if size changed significantly
                if (scaledImage == null ||
                        Math.abs(lastWidth - newW) > 5 ||
                        Math.abs(lastHeight - newH) > 5) {

                    scaledImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                    lastWidth = newW;
                    lastHeight = newH;
                }

                // Draw to scaled buffer
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
                // Center the image
                int x = (getWidth() - scaledImage.getWidth()) / 2;
                int y = (getHeight() - scaledImage.getHeight()) / 2;
                g.drawImage(scaledImage, x, y, null);
            } else {
                // Show waiting message
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
