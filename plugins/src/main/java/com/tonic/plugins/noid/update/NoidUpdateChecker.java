package com.tonic.plugins.noid.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tonic.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * Checks GitHub releases for updates and downloads new versions
 */
public class NoidUpdateChecker {

    private static final String GITHUB_API = "https://api.github.com";
    private static final String CURRENT_VERSION = "1.0.0"; // Should match plugin version

    private final String repoPath; // Format: owner/repo
    private final OkHttpClient httpClient;
    private final Gson gson;

    public NoidUpdateChecker(String repoPath) {
        this.repoPath = repoPath;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * Check GitHub releases for a newer version
     */
    public CompletableFuture<UpdateInfo> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = GITHUB_API + "/repos/" + repoPath + "/releases/latest";

                Request request = new Request.Builder()
                        .url(url)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Logger.norm("[Noid Update] Failed to check updates: " + response.code());
                        return null;
                    }

                    String body = response.body() != null ? response.body().string() : "";
                    JsonObject json = gson.fromJson(body, JsonObject.class);

                    String tagName = json.get("tag_name").getAsString();
                    String releaseNotes = json.has("body") ? json.get("body").getAsString() : "";

                    // Find JAR asset
                    String downloadUrl = null;
                    JsonArray assets = json.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String name = asset.get("name").getAsString();
                        if (name.endsWith(".jar")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }

                    UpdateInfo info = new UpdateInfo();
                    info.setCurrentVersion(CURRENT_VERSION);
                    info.setLatestVersion(tagName);
                    info.setDownloadUrl(downloadUrl);
                    info.setReleaseNotes(releaseNotes);

                    return info;
                }

            } catch (Exception e) {
                Logger.error("[Noid Update] Check failed: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Download and apply an update
     */
    public void downloadUpdate(UpdateInfo info) {
        if (info == null || info.getDownloadUrl() == null) {
            Logger.norm("[Noid Update] No download URL available");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(info.getDownloadUrl())
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Logger.norm("[Noid Update] Download failed: " + response.code());
                        return;
                    }

                    // Get the plugin directory
                    String userHome = System.getProperty("user.home");
                    Path pluginsDir = Path.of(userHome, ".runelite", "plugins");

                    if (!Files.exists(pluginsDir)) {
                        Files.createDirectories(pluginsDir);
                    }

                    // Save to temp file first
                    Path tempFile = Files.createTempFile("noid-update-", ".jar");
                    try (InputStream in = response.body().byteStream()) {
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Move to plugins directory (will apply on restart)
                    Path targetPath = pluginsDir.resolve("noid-plugins.jar");
                    Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    Logger.norm("[Noid Update] Update downloaded: " + info.getLatestVersion());
                    Logger.norm("[Noid Update] Restart RuneLite to apply the update");
                }

            } catch (Exception e) {
                Logger.error("[Noid Update] Download error: " + e.getMessage());
            }
        });
    }
}
