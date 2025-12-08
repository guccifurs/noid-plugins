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
 * Captures the RuneLite game screen.
 * Uses Robot to capture the screen at canvas location.
 * Note: On Linux with Wayland/GPU compositor, game content may appear black.
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

        System.out.println("[FriendShare] Starting simple Robot capture...");

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
     * Capture a single frame using Robot only.
     */
    private void captureFrame() {
        if (callback == null || robot == null)
            return;

        try {
            BufferedImage capture = null;

            // Get canvas bounds and capture with Robot
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
                            System.out.println("[FriendShare] Captured at " + loc + " size=" + w + "x" + h);
                        }
                    }
                } catch (Exception e) {
                    if (framesSent == 0) {
                        System.err.println("[FriendShare] Canvas capture failed: " + e.getMessage());
                    }
                }
            }

            if (capture == null) {
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
        }
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
