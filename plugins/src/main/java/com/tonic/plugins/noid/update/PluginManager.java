package com.tonic.plugins.noid.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tonic.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages auto-updates for child plugins from GitHub releases
 */
public class PluginManager {

    private static final String GITHUB_API = "https://api.github.com";
    private static final Path PLUGINS_DIR = Path.of(
            System.getProperty("user.home"), ".runelite", "sideloaded-plugins");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final NetworkPluginLoader networkLoader;

    // Map of plugin name -> PluginInfo (repo, current version, jar name)
    private final Map<String, PluginInfo> managedPlugins = new HashMap<>();

    public PluginManager() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.networkLoader = new NetworkPluginLoader();

        // Register managed plugins (version is for logging only - always fetches latest
        // release)
        registerPlugin("GearSwapper", "guccifurs/noid-plugins", "1.3.4", "gearswapper.jar");
    }

    /**
     * Register a plugin to be managed for updates
     */
    public void registerPlugin(String name, String githubRepo, String currentVersion, String jarName) {
        managedPlugins.put(name, new PluginInfo(name, githubRepo, currentVersion, jarName));
        Logger.norm("[Noid] Registered plugin: " + name);
    }

    /**
     * Check and load all managed plugins from network.
     * No JAR files are saved to disk - everything loads into memory.
     */
    public void checkAndUpdateAll() {
        // Clean up any old JAR files from before we used network loading
        cleanupOldVersions();

        for (PluginInfo plugin : managedPlugins.values()) {
            loadPluginFromNetwork(plugin);
        }
    }

    /**
     * Load a plugin directly from GitHub using network classloading
     */
    private void loadPluginFromNetwork(PluginInfo plugin) {
        try {
            Logger.norm("[Noid] Loading " + plugin.name + " from network...");

            String url = GITHUB_API + "/repos/" + plugin.githubRepo + "/releases/latest";

            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Logger.warn("[Noid] Cannot reach GitHub for " + plugin.name);
                    return;
                }

                String body = response.body().string();
                JsonObject json = gson.fromJson(body, JsonObject.class);

                String latestVersion = json.get("tag_name").getAsString();
                JsonArray assets = json.getAsJsonArray("assets");

                // Find JAR asset
                String downloadUrl = null;
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString().toLowerCase();
                    if (name.contains(plugin.name.toLowerCase()) && name.endsWith(".jar")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }

                if (downloadUrl == null) {
                    Logger.warn("[Noid] No JAR found for " + plugin.name);
                    return;
                }

                // Load using network loader (downloads to temp, loads classes, deletes temp)
                boolean success = networkLoader.loadFromUrl(plugin.name, downloadUrl, latestVersion);

                if (success) {
                    Logger.norm("[Noid] ✅ " + plugin.name + " loaded from network");
                } else {
                    Logger.error("[Noid] Failed to load " + plugin.name);
                }
            }
        } catch (Exception e) {
            Logger.error("[Noid] Error loading " + plugin.name + ": " + e.getMessage());
        }
    }

    /**
     * Get list of available updates (non-blocking, returns immediately)
     * Use this for UI to show available updates without auto-installing
     */
    public java.util.List<UpdateInfo> getAvailableUpdates() {
        java.util.List<UpdateInfo> updates = new java.util.ArrayList<>();

        for (PluginInfo plugin : managedPlugins.values()) {
            try {
                UpdateInfo update = checkForUpdate(plugin);
                if (update != null && update.hasUpdate()) {
                    updates.add(update);
                }
            } catch (Exception e) {
                Logger.error("[Noid] Error checking update for " + plugin.name + ": " + e.getMessage());
            }
        }

        return updates;
    }

    /**
     * Check for update for a specific plugin (synchronous)
     */
    private UpdateInfo checkForUpdate(PluginInfo plugin) {
        try {
            String url = GITHUB_API + "/repos/" + plugin.githubRepo + "/releases/latest";

            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                String body = response.body().string();
                JsonObject json = gson.fromJson(body, JsonObject.class);

                String latestVersion = json.get("tag_name").getAsString();
                JsonArray assets = json.getAsJsonArray("assets");

                // Find JAR asset
                String downloadUrl = null;
                String assetName = null;
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString().toLowerCase();
                    if (name.contains(plugin.name.toLowerCase()) && name.endsWith(".jar")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        assetName = asset.get("name").getAsString();
                        break;
                    }
                }

                if (downloadUrl == null)
                    return null;

                // Check what version we have loaded via network loader
                String currentVersion = "none";
                if (networkLoader.isLoaded(plugin.name)) {
                    currentVersion = networkLoader.getLoadedPlugin(plugin.name).version;
                }

                UpdateInfo info = new UpdateInfo();
                info.setPluginName(plugin.name);
                info.setCurrentVersion(currentVersion);
                info.setLatestVersion(latestVersion);
                info.setDownloadUrl(downloadUrl);
                info.setJarName(assetName);

                return info;
            }
        } catch (Exception e) {
            Logger.error("[Noid] Update check failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Download a plugin update and hot-reload it using network classloading.
     * No JAR files are left on disk - everything loads into memory.
     */
    public boolean downloadAndReload(UpdateInfo update) {
        Logger.norm("[Noid] Updating " + update.getPluginName() + " to " + update.getLatestVersion() + "...");

        // Use network loader - downloads to temp, loads classes, deletes temp
        boolean success = networkLoader.reloadFromUrl(
                update.getPluginName(),
                update.getDownloadUrl(),
                update.getLatestVersion());

        if (success) {
            // Also try to delete any old JAR files that might be on disk from before
            cleanupOldJars(update.getPluginName());
            com.tonic.services.hotswapper.PluginReloader.forceRebuildPluginList();
        }

        return success;
    }

    /**
     * Clean up any old JAR files for a plugin (from before we used network loading)
     */
    private void cleanupOldJars(String pluginName) {
        try (var files = Files.list(PLUGINS_DIR)) {
            files.filter(f -> f.getFileName().toString().toLowerCase()
                    .contains(pluginName.toLowerCase()) &&
                    f.toString().endsWith(".jar"))
                    .forEach(f -> {
                        try {
                            Files.delete(f);
                            Logger.norm("[Noid] Cleaned up old JAR: " + f.getFileName());
                        } catch (Exception e) {
                            f.toFile().deleteOnExit();
                            Logger.norm("[Noid] Marked for cleanup: " + f.getFileName());
                        }
                    });
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Find the JAR file for a plugin by name
     */
    private java.io.File findPluginJar(String pluginName) {
        try (var files = Files.list(PLUGINS_DIR)) {
            return files.filter(f -> f.getFileName().toString().toLowerCase()
                    .contains(pluginName.toLowerCase()) &&
                    f.toString().endsWith(".jar"))
                    .findFirst()
                    .map(Path::toFile)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clean up any leftover .deleteme or .old.jar files
     */
    private void cleanupOldFiles() {
        try (var files = Files.list(PLUGINS_DIR)) {
            files.filter(f -> f.toString().endsWith(".deleteme") || f.toString().endsWith(".old.jar"))
                    .forEach(f -> {
                        try {
                            Files.delete(f);
                            Logger.norm("[Noid] Cleaned up: " + f.getFileName());
                        } catch (Exception e) {
                            // Will clean next time
                        }
                    });
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Clean up old versions of managed plugins.
     * This runs FIRST to remove any leftover duplicates from failed deletes.
     */
    private void cleanupOldVersions() {
        Logger.norm("[Noid] Running cleanup for old plugin versions...");

        for (PluginInfo plugin : managedPlugins.values()) {
            try {
                // Find all JARs matching this plugin
                java.util.List<Path> matchingJars = new java.util.ArrayList<>();

                try (var files = Files.list(PLUGINS_DIR)) {
                    files.filter(f -> f.getFileName().toString().toLowerCase()
                            .contains(plugin.name.toLowerCase()) &&
                            f.toString().endsWith(".jar"))
                            .forEach(matchingJars::add);
                }

                if (matchingJars.size() > 1) {
                    Logger.norm("[Noid] Found " + matchingJars.size() + " versions of " + plugin.name
                            + ", cleaning up duplicates...");

                    // Sort by version (extract version from filename like gearswapper-1.0.4.jar)
                    matchingJars.sort((a, b) -> {
                        String aVer = extractVersion(a.getFileName().toString());
                        String bVer = extractVersion(b.getFileName().toString());
                        return compareVersions(bVer, aVer); // Descending - newest first
                    });

                    // Keep the first (newest), delete the rest
                    for (int i = 1; i < matchingJars.size(); i++) {
                        Path oldJar = matchingJars.get(i);
                        try {
                            Files.delete(oldJar);
                            Logger.norm("[Noid] Deleted old version: " + oldJar.getFileName());
                        } catch (Exception e) {
                            Logger.warn("[Noid] Could not delete " + oldJar.getFileName() + " (may be locked): "
                                    + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error("[Noid] Cleanup error for " + plugin.name + ": " + e.getMessage());
            }
        }
    }

    /**
     * Extract version from filename like "gearswapper-1.0.4.jar" -> "1.0.4"
     */
    private String extractVersion(String filename) {
        // Remove .jar extension
        String noExt = filename.replace(".jar", "");
        // Find last dash and take everything after
        int lastDash = noExt.lastIndexOf('-');
        if (lastDash >= 0 && lastDash < noExt.length() - 1) {
            return noExt.substring(lastDash + 1);
        }
        return "0.0.0";
    }

    /**
     * Compare two version strings (e.g., "1.0.4" vs "1.0.2")
     * Returns positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int p1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;
            if (p1 != p2)
                return p1 - p2;
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check and update a specific plugin
     */
    public void checkAndUpdate(String pluginName) {
        PluginInfo plugin = managedPlugins.get(pluginName);
        if (plugin != null) {
            checkAndUpdate(plugin);
        }
    }

    private void checkAndUpdate(PluginInfo plugin) {
        // Run SYNCHRONOUSLY so plugins are downloaded before client fully loads
        try {
            Logger.norm("[Noid] Checking updates for: " + plugin.name);

            String url = GITHUB_API + "/repos/" + plugin.githubRepo + "/releases/latest";

            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Logger.norm("[Noid] Failed to check " + plugin.name + ": " + response.code());
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                JsonObject json = gson.fromJson(body, JsonObject.class);

                String latestVersion = json.get("tag_name").getAsString();

                // Find JAR asset matching plugin name
                String downloadUrl = null;
                String assetName = null;
                JsonArray assets = json.getAsJsonArray("assets");

                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString().toLowerCase();
                    if (name.contains(plugin.name.toLowerCase()) && name.endsWith(".jar")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        assetName = asset.get("name").getAsString();
                        break;
                    }
                }

                // If no matching asset, try first JAR
                if (downloadUrl == null) {
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String name = asset.get("name").getAsString();
                        if (name.endsWith(".jar")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            assetName = name;
                            break;
                        }
                    }
                }

                if (downloadUrl == null) {
                    Logger.norm("[Noid] No JAR found for " + plugin.name);
                    return;
                }

                // Check if plugin JAR exists locally with EXACT asset name
                Path localJar = PLUGINS_DIR.resolve(assetName);
                boolean exactJarExists = Files.exists(localJar);

                // Download if: exact JAR from GitHub doesn't exist locally
                if (!exactJarExists) {
                    // Remove or rename any old versions first
                    // Note: Delete may fail if RuneLite has the JAR locked, so we rename to .old
                    try (var files = Files.list(PLUGINS_DIR)) {
                        files.filter(f -> f.getFileName().toString().toLowerCase()
                                .contains(plugin.name.toLowerCase()) &&
                                f.toString().endsWith(".jar"))
                                .forEach(f -> {
                                    try {
                                        Logger.norm("[Noid] Removing old version: " + f.getFileName());
                                        Files.delete(f);
                                    } catch (Exception e) {
                                        // If delete fails (file locked), rename to .old so it won't load next time
                                        try {
                                            Path oldPath = f.resolveSibling(f.getFileName() + ".old");
                                            Files.move(f, oldPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                            Logger.norm("[Noid] Renamed locked file to: " + oldPath.getFileName());
                                        } catch (Exception e2) {
                                            Logger.warn("[Noid] Could not remove or rename: " + f.getFileName());
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        // Ignore
                    }

                    // Also clean up any old .old files from previous runs
                    try (var oldFiles = Files.list(PLUGINS_DIR)) {
                        oldFiles.filter(f -> f.toString().endsWith(".old"))
                                .forEach(f -> {
                                    try {
                                        Files.delete(f);
                                        Logger.norm("[Noid] Cleaned up old file: " + f.getFileName());
                                    } catch (Exception e) {
                                        // Ignore - will clean next time
                                    }
                                });
                    } catch (Exception e) {
                        // Ignore
                    }

                    Logger.norm("[Noid] Downloading " + plugin.name + ": " + assetName);
                    downloadPlugin(plugin, downloadUrl, assetName);
                } else {
                    Logger.norm("[Noid] " + plugin.name + " is up to date (" + assetName + ")");
                }
            }

        } catch (Exception e) {
            Logger.error("[Noid] Update check failed for " + plugin.name + ": " + e.getMessage());
        }
    }

    private void downloadPlugin(PluginInfo plugin, String downloadUrl, String jarName) {
        try {
            Logger.norm("[Noid] Downloading " + plugin.name + "...");

            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Logger.norm("[Noid] Download failed for " + plugin.name + ": " + response.code());
                    return;
                }

                if (!Files.exists(PLUGINS_DIR)) {
                    Files.createDirectories(PLUGINS_DIR);
                }

                // Download to temp file first
                Path tempFile = Files.createTempFile("noid-plugin-", ".jar");
                try (InputStream in = response.body().byteStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Move to plugins directory
                Path targetPath = PLUGINS_DIR.resolve(jarName);
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

                Logger.norm("[Noid] ✅ Updated " + plugin.name + " - Restart RuneLite to apply");
            }

        } catch (Exception e) {
            Logger.error("[Noid] Download failed for " + plugin.name + ": " + e.getMessage());
        }
    }

    private boolean isNewerVersion(String v1, String v2) {
        // Remove 'v' prefix
        v1 = v1.startsWith("v") ? v1.substring(1) : v1;
        v2 = v2.startsWith("v") ? v2.substring(1) : v2;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 > num2)
                return true;
            if (num1 < num2)
                return false;
        }

        return false;
    }

    private int parseVersionPart(String part) {
        try {
            String digits = part.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Plugin info holder
     */
    private static class PluginInfo {
        final String name;
        final String githubRepo;
        final String currentVersion;
        final String jarName;

        PluginInfo(String name, String githubRepo, String currentVersion, String jarName) {
            this.name = name;
            this.githubRepo = githubRepo;
            this.currentVersion = currentVersion;
            this.jarName = jarName;
        }
    }
}
