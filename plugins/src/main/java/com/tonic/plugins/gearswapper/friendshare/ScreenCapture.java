package com.tonic.plugins.gearswapper.friendshare;

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
 * Captures the RuneLite client screen and compresses to JPEG.
 */
@Singleton
public class ScreenCapture {

    private static final int TARGET_FPS = 10;
    private static final float JPEG_QUALITY = 0.5f;
    private static final int MAX_WIDTH = 800;

    private Robot robot;
    private Window runeliteWindow;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> captureTask;

    private FrameCallback callback;

    @Inject
    public ScreenCapture() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            System.err.println("[FriendShare] Failed to create Robot: " + e.getMessage());
        }
    }

    /**
     * Find any visible RuneLite/Noid window.
     */
    private Window findRuneLiteWindow() {
        // Try all windows, not just frames
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window.getWidth() > 100 && window.getHeight() > 100) {
                String title = "";
                if (window instanceof Frame) {
                    title = ((Frame) window).getTitle();
                } else if (window instanceof Dialog) {
                    title = ((Dialog) window).getTitle();
                }

                // Check for RuneLite or Noid in title
                if (title != null && (title.contains("RuneLite") || title.contains("Noid") ||
                        title.contains("OSRS") || title.contains("Old School"))) {
                    System.out.println("[FriendShare] Found window: " + title + " (" + window.getWidth() + "x"
                            + window.getHeight() + ")");
                    return window;
                }
            }
        }

        // Fallback: find largest visible window
        Window largest = null;
        int largestArea = 0;
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window instanceof Frame) {
                int area = window.getWidth() * window.getHeight();
                if (area > largestArea) {
                    largestArea = area;
                    largest = window;
                }
            }
        }

        if (largest != null) {
            String title = (largest instanceof Frame) ? ((Frame) largest).getTitle() : "Unknown";
            System.out.println("[FriendShare] Using largest window: " + title + " (" + largest.getWidth() + "x"
                    + largest.getHeight() + ")");
        }

        return largest;
    }

    /**
     * Start capturing frames.
     */
    public void startCapture(FrameCallback callback) {
        if (robot == null) {
            System.err.println("[FriendShare] Robot is null, cannot capture");
            return;
        }

        this.callback = callback;

        // Find window
        runeliteWindow = findRuneLiteWindow();
        if (runeliteWindow == null) {
            System.err.println("[FriendShare] No suitable window found!");
        } else {
            System.out.println("[FriendShare] Will capture window at " + runeliteWindow.getBounds());
        }

        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        long periodMs = 1000 / TARGET_FPS;
        captureTask = executor.scheduleAtFixedRate(this::captureFrame, 0, periodMs, TimeUnit.MILLISECONDS);

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
        callback = null;
        System.out.println("[FriendShare] Screen capture stopped");
    }

    /**
     * Capture a single frame and send to callback.
     */
    private void captureFrame() {
        if (callback == null)
            return;

        try {
            // Get client window bounds
            Rectangle bounds = getClientBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
                System.err.println("[FriendShare] Invalid bounds: " + bounds);
                return;
            }

            // Capture screen region
            BufferedImage capture = robot.createScreenCapture(bounds);

            if (capture == null) {
                System.err.println("[FriendShare] Capture returned null");
                return;
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

            if (jpegData != null && jpegData.length > 0) {
                callback.onFrame(jpegData);
            }

        } catch (Exception e) {
            System.err.println("[FriendShare] Capture error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the client window bounds.
     */
    private Rectangle getClientBounds() {
        // Re-find window if lost
        if (runeliteWindow == null || !runeliteWindow.isVisible()) {
            runeliteWindow = findRuneLiteWindow();
        }

        if (runeliteWindow != null && runeliteWindow.isVisible()) {
            Point loc = runeliteWindow.getLocationOnScreen();
            Dimension size = runeliteWindow.getSize();
            return new Rectangle(loc.x, loc.y, size.width, size.height);
        }

        return null;
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
