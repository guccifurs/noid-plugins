package com.tonic.noid;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * NoidClientHijack - Runs inside RuneLite's classloader
 * 
 * This class loads sideloaded plugins from the RuneLite sideloaded-plugins
 * folder
 * and initializes the Noid authentication system.
 */
public class NoidClientHijack {

    private static final String SIDELOAD_FOLDER = "sideloaded-plugins";

    public NoidClientHijack() {
        System.out.println("[Noid] Client hijack starting...");

        new Thread(() -> {
            try {
                // Wait for RuneLite to fully initialize
                Thread.sleep(3000);

                // Load plugins from sideloaded-plugins folder
                loadSideloadedPlugins();

                System.out.println("[Noid] Client hijack complete");

            } catch (Exception e) {
                System.err.println("[Noid] Client hijack failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSideloadedPlugins() {
        try {
            String userHome = System.getProperty("user.home");
            Path sideloadPath = Paths.get(userHome, ".runelite", SIDELOAD_FOLDER);

            if (!Files.exists(sideloadPath)) {
                Files.createDirectories(sideloadPath);
                System.out.println("[Noid] Created sideloaded-plugins folder: " + sideloadPath);
                return;
            }

            File[] jarFiles = sideloadPath.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                System.out.println("[Noid] No plugins found in sideloaded-plugins folder");
                return;
            }

            // Get the current classloader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (!(classLoader instanceof URLClassLoader)) {
                System.out.println("[Noid] Cannot add JARs - not a URLClassLoader");
                return;
            }

            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);

            for (File jarFile : jarFiles) {
                try {
                    addUrl.invoke(urlClassLoader, jarFile.toURI().toURL());
                    System.out.println("[Noid] Loaded plugin: " + jarFile.getName());
                } catch (Exception e) {
                    System.err.println("[Noid] Failed to load: " + jarFile.getName() + " - " + e.getMessage());
                }
            }

            System.out.println("[Noid] Loaded " + jarFiles.length + " plugins from sideloaded-plugins");

        } catch (Exception e) {
            System.err.println("[Noid] Failed to load sideloaded plugins: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
