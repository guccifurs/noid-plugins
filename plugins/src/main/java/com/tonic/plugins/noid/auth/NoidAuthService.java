package com.tonic.plugins.noid.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tonic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Service for communicating with the Noid authentication backend
 */
public class NoidAuthService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String API_URL = "http://172.233.59.91:3000";

    private final OkHttpClient httpClient;
    private final Gson gson;

    private String sessionToken;
    private String jwtToken;

    public NoidAuthService() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * Authenticate with the backend using Discord ID + HWID
     * HWID is auto-linked on first login, blocked if different HWID tries to use
     * same Discord ID
     */
    public CompletableFuture<NoidUser> authenticate(String discordId, String hwid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("discordId", discordId);
                body.addProperty("hwid", hwid);

                Request request = new Request.Builder()
                        .url(API_URL + "/auth/validate")
                        .post(RequestBody.create(JSON, gson.toJson(body)))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        Logger.norm("[Noid Auth] Failed: " + response.code() + " - " + responseBody);
                        return null;
                    }

                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);

                    if (!json.get("authenticated").getAsBoolean()) {
                        return null;
                    }

                    // Store tokens
                    this.sessionToken = json.get("sessionToken").getAsString();
                    this.jwtToken = json.get("token").getAsString();

                    // Parse user
                    JsonObject userJson = json.getAsJsonObject("user");
                    NoidUser user = new NoidUser();
                    user.setDiscordId(userJson.get("discordId").getAsString());
                    user.setDiscordName(userJson.get("discordName").getAsString());
                    user.setTier(userJson.has("tier") ? userJson.get("tier").getAsString() : "standard");
                    user.setSessionToken(sessionToken);

                    if (userJson.has("expiresAt") && !userJson.get("expiresAt").isJsonNull()) {
                        user.setExpiresAt(Instant.parse(userJson.get("expiresAt").getAsString()));
                    }

                    return user;
                }

            } catch (IOException e) {
                Logger.error("[Noid Auth] Connection error: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Send heartbeat to keep session alive
     */
    public void sendHeartbeat(String pluginName) {
        if (sessionToken == null)
            return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("sessionToken", sessionToken);
                body.addProperty("pluginName", pluginName);

                Request request = new Request.Builder()
                        .url(API_URL + "/auth/heartbeat")
                        .post(RequestBody.create(JSON, gson.toJson(body)))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Logger.norm("[Noid Auth] Heartbeat failed: " + response.code());
                    }
                }

            } catch (IOException e) {
                Logger.error("[Noid Auth] Heartbeat error: " + e.getMessage());
            }
        });
    }

    /**
     * Log plugin usage for analytics
     */
    public void logUsage(String pluginName, String action) {
        if (sessionToken == null)
            return;

        // Piggyback on heartbeat with action logging
        sendHeartbeat(pluginName);
    }

    /**
     * Logout and invalidate session
     */
    public void logout() {
        if (sessionToken == null)
            return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("sessionToken", sessionToken);

                Request request = new Request.Builder()
                        .url(API_URL + "/auth/logout")
                        .post(RequestBody.create(JSON, gson.toJson(body)))
                        .build();

                httpClient.newCall(request).execute().close();

            } catch (IOException e) {
                // Ignore logout errors
            }
        });

        sessionToken = null;
        jwtToken = null;
    }

    /**
     * Get Discord OAuth URL from backend
     */
    public String getOAuthUrl() {
        try {
            Request request = new Request.Builder()
                    .url(API_URL + "/auth/discord/url")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Logger.error("[Noid Auth] Failed to get OAuth URL: " + response.code());
                    return null;
                }

                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                return json.has("url") ? json.get("url").getAsString() : null;
            }

        } catch (IOException e) {
            Logger.error("[Noid Auth] OAuth URL error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Poll for OAuth completion
     */
    public NoidUser pollForAuth(String discordId) {
        try {
            Request request = new Request.Builder()
                    .url(API_URL + "/auth/discord/poll?discordId=" + discordId)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);

                if (!json.has("authenticated") || !json.get("authenticated").getAsBoolean()) {
                    return null;
                }

                // Store tokens
                if (json.has("sessionToken")) {
                    this.sessionToken = json.get("sessionToken").getAsString();
                }
                if (json.has("token")) {
                    this.jwtToken = json.get("token").getAsString();
                }

                // Parse user
                JsonObject userJson = json.getAsJsonObject("user");
                NoidUser user = new NoidUser();
                user.setDiscordId(userJson.get("discordId").getAsString());
                user.setDiscordName(userJson.get("discordName").getAsString());
                user.setTier(userJson.has("tier") ? userJson.get("tier").getAsString() : "none");
                user.setSessionToken(sessionToken);
                user.setHasSubscription(
                        userJson.has("hasSubscription") && userJson.get("hasSubscription").getAsBoolean());

                if (userJson.has("expiresAt") && !userJson.get("expiresAt").isJsonNull()) {
                    user.setExpiresAt(Instant.parse(userJson.get("expiresAt").getAsString()));
                }

                return user;
            }

        } catch (Exception e) {
            Logger.error("[Noid Auth] Poll error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Poll for OAuth completion using state token
     */
    public NoidUser pollForAuthByState(String state) {
        if (state == null || state.isEmpty()) {
            return null;
        }

        try {
            Request request = new Request.Builder()
                    .url(API_URL + "/auth/discord/poll?state=" + state)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);

                if (!json.has("authenticated") || !json.get("authenticated").getAsBoolean()) {
                    return null;
                }

                // Store tokens
                if (json.has("sessionToken")) {
                    this.sessionToken = json.get("sessionToken").getAsString();
                }
                if (json.has("token")) {
                    this.jwtToken = json.get("token").getAsString();
                }

                // Parse user
                JsonObject userJson = json.getAsJsonObject("user");
                NoidUser user = new NoidUser();
                user.setDiscordId(userJson.get("discordId").getAsString());
                user.setDiscordName(userJson.get("discordName").getAsString());
                user.setTier(userJson.has("tier") ? userJson.get("tier").getAsString() : "none");
                user.setSessionToken(sessionToken);
                user.setHasSubscription(
                        userJson.has("hasSubscription") && userJson.get("hasSubscription").getAsBoolean());

                if (userJson.has("expiresAt") && !userJson.get("expiresAt").isJsonNull()) {
                    user.setExpiresAt(Instant.parse(userJson.get("expiresAt").getAsString()));
                }

                return user;
            }

        } catch (Exception e) {
            // Silent poll - don't spam logs
            return null;
        }
    }

    /**
     * Restore session using saved Discord ID
     */
    public NoidUser restoreSession(String discordId) {
        if (discordId == null || discordId.isEmpty()) {
            return null;
        }

        try {
            Request request = new Request.Builder()
                    .url(API_URL + "/auth/session/restore?discordId=" + discordId)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);

                if (!json.has("authenticated") || !json.get("authenticated").getAsBoolean()) {
                    return null;
                }

                // Store session token for heartbeats
                if (json.has("sessionToken")) {
                    this.sessionToken = json.get("sessionToken").getAsString();
                    Logger.norm("[Noid Auth] Session token restored successfully");
                }

                // Parse user
                JsonObject userJson = json.getAsJsonObject("user");
                NoidUser user = new NoidUser();
                user.setDiscordId(userJson.get("discordId").getAsString());
                user.setDiscordName(userJson.get("discordName").getAsString());
                user.setTier(userJson.has("tier") ? userJson.get("tier").getAsString() : "none");
                user.setHasSubscription(
                        userJson.has("hasSubscription") && userJson.get("hasSubscription").getAsBoolean());
                user.setSessionToken(this.sessionToken);

                if (userJson.has("expiresAt") && !userJson.get("expiresAt").isJsonNull()) {
                    user.setExpiresAt(Instant.parse(userJson.get("expiresAt").getAsString()));
                }

                return user;
            }

        } catch (Exception e) {
            Logger.error("[Noid Auth] Session restore error: " + e.getMessage());
            return null;
        }
    }
}
