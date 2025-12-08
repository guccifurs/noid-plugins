package com.tonic.plugins.noid.update;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.hotswapper.PluginClassLoader;
import com.tonic.services.hotswapper.PluginContext;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads plugins directly from network URLs without leaving JAR files on disk.
 * 
 * Flow:
 * 1. Download JAR to temp file
 * 2. Load classes into memory
 * 3. Delete temp file immediately
 * 4. Plugin runs purely in memory
 */
public class NetworkPluginLoader {

    private final OkHttpClient httpClient;

    // Track loaded network plugins for unloading
    private final Map<String, LoadedNetworkPlugin> loadedPlugins = new ConcurrentHashMap<>();

    public NetworkPluginLoader() {
        this.httpClient = new OkHttpClient();
    }

    /**
     * Represents a plugin loaded from network
     */
    public static class LoadedNetworkPlugin {
        public final String pluginName;
        public final String version;
        public final PluginClassLoader classLoader;
        public final List<Plugin> plugins;

        public LoadedNetworkPlugin(String pluginName, String version,
                PluginClassLoader classLoader, List<Plugin> plugins) {
            this.pluginName = pluginName;
            this.version = version;
            this.classLoader = classLoader;
            this.plugins = plugins;
        }
    }

    /**
     * Load a plugin from a URL
     * 
     * @param pluginName Unique name for this plugin
     * @param jarUrl     URL to download JAR from
     * @param version    Version string for tracking
     * @return true if successful
     */
    public boolean loadFromUrl(String pluginName, String jarUrl, String version) {
        // Check if already loaded to prevent double-loading
        if (loadedPlugins.containsKey(pluginName)) {
            Logger.norm("[NetworkLoader] " + pluginName + " already loaded, skipping");
            return true;
        }

        Path tempFile = null;
        try {
            Logger.norm("[NetworkLoader] Loading " + pluginName + " v" + version + " from network...");

            // Step 1: Download to temp file
            tempFile = Files.createTempFile("noid_plugin_", ".jar");

            Request request = new Request.Builder()
                    .url(jarUrl)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Logger.error("[NetworkLoader] Download failed: " + response.code());
                    return false;
                }

                try (InputStream in = response.body().byteStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Logger.norm("[NetworkLoader] Downloaded to temp: " + tempFile.getFileName());

            // Step 2: Load classes from temp file
            File jarFile = tempFile.toFile();
            PluginClassLoader classLoader = new PluginClassLoader(jarFile, Static.getClassLoader());
            List<Class<?>> classes = classLoader.getClasses();

            PluginManager pluginMgr = Static.getInjector().getInstance(PluginManager.class);
            List<Plugin> plugins = pluginMgr.loadPlugins(classes, null);

            // Track in our own map only (don't use global PluginContext to avoid red button
            // conflicts)
            loadedPlugins.put(pluginName, new LoadedNetworkPlugin(
                    pluginName, version, classLoader, plugins));

            // Step 3: Start all plugins
            pluginMgr.loadDefaultPluginConfiguration(plugins);
            for (Plugin plugin : plugins) {
                pluginMgr.startPlugin(plugin);
            }

            Logger.norm(
                    "[NetworkLoader] ✅ Loaded " + pluginName + " v" + version + " (" + plugins.size() + " plugin(s))");

            // Step 4: Delete temp file (classes are in memory now)
            try {
                Files.deleteIfExists(tempFile);
                Logger.norm("[NetworkLoader] Temp file deleted");
            } catch (Exception e) {
                // Mark for deletion on JVM exit
                tempFile.toFile().deleteOnExit();
                Logger.norm("[NetworkLoader] Temp file marked for cleanup on exit");
            }

            return true;

        } catch (Exception e) {
            Logger.error("[NetworkLoader] Failed to load " + pluginName + ": " + e.getMessage());
            e.printStackTrace();

            // Cleanup temp file on failure
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }

            return false;
        }
    }

    /**
     * Unload a network-loaded plugin
     * 
     * @param pluginName Name of plugin to unload
     * @return true if successful
     */
    public boolean unload(String pluginName) {
        try {
            Logger.norm("[NetworkLoader] Unloading " + pluginName + "...");

            LoadedNetworkPlugin loaded = loadedPlugins.remove(pluginName);
            if (loaded == null) {
                Logger.warn("[NetworkLoader] Plugin not found: " + pluginName);
                return false;
            }

            PluginManager pluginMgr = Static.getInjector().getInstance(PluginManager.class);

            // Stop and remove all plugins
            for (Plugin plugin : loaded.plugins) {
                try {
                    if (pluginMgr.isPluginActive(plugin)) {
                        pluginMgr.stopPlugin(plugin);
                    }
                    pluginMgr.remove(plugin);
                } catch (Exception e) {
                    Logger.warn("[NetworkLoader] Error stopping plugin: " + e.getMessage());
                }
            }

            // Cleanup classloader
            loaded.classLoader.close();

            Logger.norm("[NetworkLoader] ✅ Unloaded " + pluginName);
            return true;

        } catch (Exception e) {
            Logger.error("[NetworkLoader] Failed to unload " + pluginName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Reload a plugin from URL (unload old, load new)
     */
    public boolean reloadFromUrl(String pluginName, String jarUrl, String version) {
        // Unload first (ignore if not loaded)
        unload(pluginName);

        // Small delay to let OS release file handles
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        // Load new version
        return loadFromUrl(pluginName, jarUrl, version);
    }

    /**
     * Check if a plugin is loaded
     */
    public boolean isLoaded(String pluginName) {
        return loadedPlugins.containsKey(pluginName);
    }

    /**
     * Get loaded plugin info
     */
    public LoadedNetworkPlugin getLoadedPlugin(String pluginName) {
        return loadedPlugins.get(pluginName);
    }
}
