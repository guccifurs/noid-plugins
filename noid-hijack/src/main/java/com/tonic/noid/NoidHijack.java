package com.tonic.noid;

import javax.swing.UIManager;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * NoidHijack - Launcher hijack for loading VitaLite API on vanilla RuneLite
 * 
 * Based on RuneLiteHijack by Arnuh
 * 
 * This class intercepts RuneLite's startup and injects our classes into
 * the client's classloader, allowing VitaLite plugins to work on vanilla
 * RuneLite.
 */
public class NoidHijack {

    public NoidHijack() {
        new Thread(() -> {
            // Wait for RuneLite's ClassLoader to be available
            ClassLoader objClassLoader;
            loop: while (true) {
                objClassLoader = (ClassLoader) UIManager.get("ClassLoader");
                if (objClassLoader != null) {
                    for (Package pack : objClassLoader.getDefinedPackages()) {
                        if (pack.getName().equals("net.runelite.client.rs")) {
                            break loop;
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            System.out.println("[Noid] RuneLite ClassLoader found");

            try {
                URLClassLoader classLoader = (URLClassLoader) objClassLoader;

                // Add our hijack JAR to the classloader
                Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);

                URI uri = NoidHijack.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                if (uri.getPath().endsWith("classes/")) { // IntelliJ development
                    uri = uri.resolve("..");
                }
                if (!uri.getPath().endsWith(".jar")) {
                    uri = uri.resolve("NoidHijack.jar");
                }

                addUrl.invoke(classLoader, uri.toURL());
                System.out.println("[Noid] Added JAR to classloader: " + uri.getPath());

                // Load and initialize the client hijack
                Class<?> clazz = classLoader.loadClass(NoidClientHijack.class.getName());
                clazz.getConstructor().newInstance();

                System.out.println("[Noid] Client hijack initialized");

            } catch (Exception ex) {
                System.err.println("[Noid] Failed to hijack client: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        // Disable the JVM Launcher to ensure we control the startup
        System.setProperty("runelite.launcher.reflect", "true");

        // Check for updates before starting
        boolean needsRestart = NoidUpdater.checkAndUpdate();
        if (needsRestart) {
            System.out.println("[Noid] Restarting for update...");
            System.exit(0);
            return;
        }

        // Initialize our hijack
        new NoidHijack();

        // Launch the real RuneLite Launcher
        try {
            Class<?> clazz = Class.forName("net.runelite.launcher.Launcher");
            clazz.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Exception e) {
            System.err.println("[Noid] Failed to start RuneLite Launcher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
