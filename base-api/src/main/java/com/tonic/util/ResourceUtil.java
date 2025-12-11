package com.tonic.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ResourceUtil
{
    /**
     * Loads a PNG image from resources with a default null return on error
     *
     * @param clazz The class whose package location will be used to find the resource
     * @param imageName The name of the image file
     * @return BufferedImage or null if loading fails
     */
    public static BufferedImage getImage(Class<?> clazz, String imageName) {
        InputStream inputStream = clazz.getResourceAsStream(imageName);
        try (inputStream) {
            if (inputStream == null) {
                throw new IllegalArgumentException(
                        "Resource not found: " + imageName + " for class: " + clazz.getName()
                );
            }
            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new IOException("Failed to decode image: " + imageName);
            }

            return image;

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load image: " + imageName + " - " + e.getMessage());
            return null;
        }
    }
}
