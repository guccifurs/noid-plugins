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
    private static final float JPEG_QUALITY = 0.6f;
    private static final int MAX_WIDTH = 800;

    private Robot robot;
    private Frame runeliteFrame;

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
        // Find RuneLite frame
        findRuneLiteFrame();
    }

    /**
     * Find the RuneLite window.
     */
    private void findRuneLiteFrame() {
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible() && frame.getTitle() != null &&
                    (frame.getTitle().contains("RuneLite") || frame.getTitle().contains("Noid"))) {
                this.runeliteFrame = frame;
                break;
            }
        }
    }

    /**
     * Start capturing frames.
     */
    public void startCapture(FrameCallback callback) {
        if (robot == null)
            return;

        this.callback = callback;

        // Find frame if not already found
        if (runeliteFrame == null || !runeliteFrame.isVisible()) {
            findRuneLiteFrame();
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
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
                return;

            // Capture screen
            BufferedImage capture = robot.createScreenCapture(bounds);

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

            callback.onFrame(jpegData);

        } catch (Exception e) {
            System.err.println("[FriendShare] Capture error: " + e.getMessage());
        }
    }

    /**
     * Get the client window bounds.
     */
    private Rectangle getClientBounds() {
        if (runeliteFrame != null && runeliteFrame.isVisible()) {
            return runeliteFrame.getBounds();
        }
        // Try to find it again
        findRuneLiteFrame();
        if (runeliteFrame != null && runeliteFrame.isVisible()) {
            return runeliteFrame.getBounds();
        }
        return null;
    }

    /**
     * Compress image to JPEG bytes.
     */
    private byte[] compressToJpeg(BufferedImage image) throws Exception {
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
