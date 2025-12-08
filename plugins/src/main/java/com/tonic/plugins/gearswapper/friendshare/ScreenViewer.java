package com.tonic.plugins.gearswapper.friendshare;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Window that displays received screen share frames.
 */
public class ScreenViewer extends JFrame {

    private final JLabel imageLabel;
    private final JLabel statusLabel;
    private final String peerName;
    private Runnable onClose;

    private long lastFrameTime = 0;
    private int frameCount = 0;

    public ScreenViewer(String peerName) {
        super("Viewing: " + peerName + "'s Screen");
        this.peerName = peerName;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);

        // Dark theme
        getContentPane().setBackground(new Color(30, 30, 30));
        setLayout(new BorderLayout());

        // Image display
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setBackground(Color.BLACK);
        imageLabel.setOpaque(true);
        add(imageLabel, BorderLayout.CENTER);

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
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
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
        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpegData));
                if (image != null) {
                    // Scale to fit
                    int maxW = imageLabel.getWidth();
                    int maxH = imageLabel.getHeight();

                    if (maxW > 0 && maxH > 0) {
                        double scale = Math.min((double) maxW / image.getWidth(),
                                (double) maxH / image.getHeight());
                        int newW = (int) (image.getWidth() * scale);
                        int newH = (int) (image.getHeight() * scale);

                        Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_FAST);
                        imageLabel.setIcon(new ImageIcon(scaled));
                    } else {
                        imageLabel.setIcon(new ImageIcon(image));
                    }

                    // Update FPS counter
                    frameCount++;
                    long now = System.currentTimeMillis();
                    if (now - lastFrameTime >= 1000) {
                        statusLabel.setText(String.format("Viewing %s | %d FPS | %d KB",
                                peerName, frameCount, jpegData.length / 1024));
                        frameCount = 0;
                        lastFrameTime = now;
                    }
                }
            } catch (Exception e) {
                // Ignore bad frames
            }
        });
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
}
