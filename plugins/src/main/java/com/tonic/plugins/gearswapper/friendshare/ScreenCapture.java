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
 * Uses component painting method which works better on some systems.
 */
@Singleton
public class ScreenCapture {

    private static final int TARGET_FPS = 8;
    private static final float JPEG_QUALITY = 0.5f;
    private static final int MAX_WIDTH = 800;

    private Robot robot;
    private Window targetWindow;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> captureTask;

    private FrameCallback callback;
    private int framesSent = 0;

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
    private Window findTargetWindow() {
        // First try to find by title
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window.getWidth() > 100 && window.getHeight() > 100) {
                String title = getWindowTitle(window);
                if (title.contains("RuneLite") || title.contains("Noid") ||
                        title.contains("OSRS") || title.contains("Old School")) {
                    System.out.println("[FriendShare] Found target window: " + title);
                    return window;
                }
            }
        }

        // Fallback: use largest visible Frame
        Frame largest = null;
        int largestArea = 0;
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible()) {
                int area = frame.getWidth() * frame.getHeight();
                if (area > largestArea) {
                    largestArea = area;
                    largest = frame;
                }
            }
        }

        if (largest != null) {
            System.out.println("[FriendShare] Using largest frame: " + largest.getTitle());
        }
        return largest;
    }

    private String getWindowTitle(Window window) {
        if (window instanceof Frame)
            return ((Frame) window).getTitle();
        if (window instanceof Dialog)
            return ((Dialog) window).getTitle();
        return "";
    }

    /**
     * Start capturing frames.
     */
    public void startCapture(FrameCallback callback) {
        this.callback = callback;
        this.framesSent = 0;

        targetWindow = findTargetWindow();
        if (targetWindow == null) {
            System.err.println("[FriendShare] No window found to capture!");
            return;
        }

        System.out.println("[FriendShare] Capturing: " + getWindowTitle(targetWindow) +
                " size=" + targetWindow.getWidth() + "x" + targetWindow.getHeight());

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
     * Capture a single frame using component painting.
     */
    private void captureFrame() {
        if (callback == null || targetWindow == null)
            return;

        try {
            // Re-find if window is gone
            if (!targetWindow.isVisible()) {
                targetWindow = findTargetWindow();
                if (targetWindow == null)
                    return;
            }

            int w = targetWindow.getWidth();
            int h = targetWindow.getHeight();

            if (w <= 0 || h <= 0)
                return;

            // Method 1: Try component painting (works better on some systems)
            BufferedImage capture = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = capture.createGraphics();

            // Paint the window contents
            targetWindow.printAll(g);
            g.dispose();

            // Check if image is all black (painting failed)
            if (isImageBlack(capture)) {
                // Method 2: Fall back to Robot screen capture
                if (robot != null) {
                    try {
                        Point loc = targetWindow.getLocationOnScreen();
                        Rectangle bounds = new Rectangle(loc.x, loc.y, w, h);
                        capture = robot.createScreenCapture(bounds);
                    } catch (Exception e) {
                        // Window might not be on screen
                    }
                }
            }

            // Scale down if needed
            if (capture.getWidth() > MAX_WIDTH) {
                double scale = (double) MAX_WIDTH / capture.getWidth();
                int newHeight = (int) (capture.getHeight() * scale);

                BufferedImage scaled = new BufferedImage(MAX_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D sg = scaled.createGraphics();
                sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                sg.drawImage(capture, 0, 0, MAX_WIDTH, newHeight, null);
                sg.dispose();
                capture = scaled;
            }

            // Compress to JPEG
            byte[] jpegData = compressToJpeg(capture);

            if (jpegData != null && jpegData.length > 100) {
                callback.onFrame(jpegData);
                framesSent++;

                if (framesSent == 1 || framesSent % 50 == 0) {
                    System.out.println("[FriendShare] Sent frame #" + framesSent + " (" + jpegData.length + " bytes)");
                }
            }

        } catch (Exception e) {
            System.err.println("[FriendShare] Capture error: " + e.getMessage());
        }
    }

    /**
     * Check if an image is mostly black (capture failed).
     */
    private boolean isImageBlack(BufferedImage img) {
        int samples = 0;
        int blackCount = 0;

        // Sample some pixels
        for (int y = 0; y < img.getHeight(); y += img.getHeight() / 10) {
            for (int x = 0; x < img.getWidth(); x += img.getWidth() / 10) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                samples++;
                if (r < 10 && g < 10 && b < 10) {
                    blackCount++;
                }
            }
        }

        // If more than 90% black, consider it failed
        return samples > 0 && (blackCount * 100 / samples) > 90;
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
