package com.tonic.plugins.gearswapper.friendshare;

import net.runelite.api.Client;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Captures the RuneLite game screen using Robot directly on the full screen,
 * then crops to the window bounds.
 */
@Singleton
public class ScreenCapture {

    private static final int TARGET_FPS = 8;
    private static final float JPEG_QUALITY = 0.7f;
    private static final int MAX_WIDTH = 800;

    private final Client client;
    private Robot robot;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> captureTask;

    private FrameCallback callback;
    private int framesSent = 0;

    @Inject
    public ScreenCapture(Client client) {
        this.client = client;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            System.err.println("[FriendShare] Failed to create Robot: " + e.getMessage());
        }
    }

    /**
     * Start capturing frames.
     */
    public void startCapture(FrameCallback callback) {
        this.callback = callback;
        this.framesSent = 0;

        System.out.println("[FriendShare] Starting capture...");

        if (robot == null) {
            System.err.println("[FriendShare] Robot is null!");
            return;
        }

        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        long periodMs = 1000 / TARGET_FPS;
        captureTask = executor.scheduleAtFixedRate(this::captureFrame, 100, periodMs, TimeUnit.MILLISECONDS);

        System.out.println("[FriendShare] Screen capture started at " + TARGET_FPS + " FPS");
    }

    /**
     * Stop capturing frames.
     */
    public void stopCapture() {
        if (captureTask != null) {
            captureTask.cancel(false);
            captureTask = null;
        }
        System.out.println("[FriendShare] Screen capture stopped after " + framesSent + " frames");
        callback = null;
    }

    /**
     * Capture a single frame.
     */
    private void captureFrame() {
        if (callback == null)
            return;

        try {
            BufferedImage capture = null;

            // Method 1: Try to capture from game canvas location
            Canvas canvas = client.getCanvas();
            if (canvas != null && canvas.isShowing()) {
                try {
                    Point loc = canvas.getLocationOnScreen();
                    int w = canvas.getWidth();
                    int h = canvas.getHeight();

                    if (w > 50 && h > 50) {
                        Rectangle bounds = new Rectangle(loc.x, loc.y, w, h);
                        capture = robot.createScreenCapture(bounds);

                        if (framesSent == 0) {
                            System.out.println("[FriendShare] Captured canvas at " + loc + " size=" + w + "x" + h);
                        }
                    }
                } catch (Exception e) {
                    if (framesSent == 0) {
                        System.err.println("[FriendShare] Canvas capture failed: " + e.getMessage());
                    }
                }
            }

            // Method 2: Fallback to full window
            if (capture == null || isImageBlack(capture)) {
                Window window = findGameWindow();
                if (window != null) {
                    try {
                        Point loc = window.getLocationOnScreen();
                        int w = window.getWidth();
                        int h = window.getHeight();
                        Rectangle bounds = new Rectangle(loc.x, loc.y, w, h);
                        capture = robot.createScreenCapture(bounds);

                        if (framesSent == 0) {
                            System.out.println("[FriendShare] Captured window at " + loc + " size=" + w + "x" + h);
                        }
                    } catch (Exception e) {
                        if (framesSent == 0) {
                            System.err.println("[FriendShare] Window capture failed: " + e.getMessage());
                        }
                    }
                }
            }

            if (capture == null) {
                if (framesSent == 0) {
                    System.err.println("[FriendShare] No capture available");
                }
                return;
            }

            // Debug: check if captured image is black
            if (framesSent == 0) {
                boolean black = isImageBlack(capture);
                System.out.println("[FriendShare] First capture: " + capture.getWidth() + "x" + capture.getHeight() +
                        ", isBlack=" + black);
            }

            // Scale down if needed
            if (capture.getWidth() > MAX_WIDTH) {
                double scale = (double) MAX_WIDTH / capture.getWidth();
                int newHeight = (int) (capture.getHeight() * scale);

                BufferedImage scaled = new BufferedImage(MAX_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(capture, 0, 0, MAX_WIDTH, newHeight, null);
                g.dispose();
                capture = scaled;
            }

            // Compress to JPEG
            byte[] jpegData = compressToJpeg(capture);

            if (jpegData != null && jpegData.length > 100) {
                callback.onFrame(jpegData);
                framesSent++;

                if (framesSent == 1) {
                    System.out.println("[FriendShare] Sent first frame: " + jpegData.length + " bytes");
                } else if (framesSent % 50 == 0) {
                    System.out.println("[FriendShare] Sent frame #" + framesSent + " (" + jpegData.length + " bytes)");
                }
            }

        } catch (Exception e) {
            System.err.println("[FriendShare] Capture error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Find the game window.
     */
    private Window findGameWindow() {
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window.getWidth() > 100) {
                String title = "";
                if (window instanceof Frame) {
                    title = ((Frame) window).getTitle();
                }
                if (title != null
                        && (title.contains("RuneLite") || title.contains("Noid") || title.contains("Old School"))) {
                    return window;
                }
            }
        }

        // Fallback to largest
        Frame largest = null;
        int maxArea = 0;
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible()) {
                int area = frame.getWidth() * frame.getHeight();
                if (area > maxArea) {
                    maxArea = area;
                    largest = frame;
                }
            }
        }
        return largest;
    }

    /**
     * Check if an image is mostly black.
     */
    private boolean isImageBlack(BufferedImage img) {
        if (img == null)
            return true;

        int samples = 0;
        int blackCount = 0;
        int step = Math.max(1, Math.min(img.getWidth(), img.getHeight()) / 10);

        for (int y = step; y < img.getHeight() - step; y += step) {
            for (int x = step; x < img.getWidth() - step; x += step) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                samples++;
                if (r < 15 && g < 15 && b < 15) {
                    blackCount++;
                }
            }
        }

        return samples > 0 && (blackCount * 100 / samples) > 85;
    }

    /**
     * Compress image to JPEG bytes.
     */
    private byte[] compressToJpeg(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            var jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
            var jpegParams = jpegWriter.getDefaultWriteParam();
            jpegParams.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            jpegParams.setCompressionQuality(JPEG_QUALITY);

            var ios = ImageIO.createImageOutputStream(baos);
            jpegWriter.setOutput(ios);
            jpegWriter.write(null, new javax.imageio.IIOImage(image, null, null), jpegParams);
            jpegWriter.dispose();
            ios.close();

            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("[FriendShare] JPEG compression failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        stopCapture();
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    /**
     * Callback for received frames.
     */
    public interface FrameCallback {
        void onFrame(byte[] jpegData);
    }
}
