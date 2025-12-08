package com.tonic.plugins.gearswapper.sdn;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tonic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for communicating with the Script SDN API
 */
public class ScriptSDNService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String API_URL = "http://172.233.59.91:3001/api/scripts";

    private final OkHttpClient httpClient;
    private final Gson gson;

    public ScriptSDNService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Add authentication headers to request
     */
    private Request.Builder addAuthHeaders(Request.Builder builder, SDNUserInfo user) {
        if (user == null) {
            return builder;
        }
        return builder
                .header("x-session-token", user.getSessionToken() != null ? user.getSessionToken() : "")
                .header("x-discord-id", user.getDiscordId() != null ? user.getDiscordId() : "")
                .header("x-discord-name", user.getDiscordName() != null ? user.getDiscordName() : "");
    }

    /**
     * Safe response body reader
     */
    private String readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        return body != null ? body.string() : "";
    }

    /**
     * Fetch all scripts from the SDN
     */
    public CompletableFuture<List<ScriptEntry>> fetchScripts(SDNUserInfo user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = addAuthHeaders(new Request.Builder(), user)
                        .url(API_URL)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Logger.error("[Script SDN] Failed to fetch scripts: " + response.code());
                        return new ArrayList<>();
                    }

                    String responseBody = readBody(response);
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);

                    if (!json.has("scripts")) {
                        return new ArrayList<>();
                    }

                    JsonArray scriptsArray = json.getAsJsonArray("scripts");
                    Type listType = new TypeToken<List<ScriptEntry>>() {
                    }.getType();
                    return gson.fromJson(scriptsArray, listType);
                }

            } catch (IOException e) {
                Logger.error("[Script SDN] Connection error: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Submit a new script
     */
    public CompletableFuture<ScriptEntry> submitScript(SDNUserInfo user, String name, String description,
            String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("name", name);
                body.addProperty("description", description);
                body.addProperty("content", content);

                Request request = addAuthHeaders(new Request.Builder(), user)
                        .url(API_URL)
                        .post(RequestBody.create(JSON, gson.toJson(body)))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = readBody(response);

                    if (!response.isSuccessful()) {
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        String error = errorJson.has("error") ? errorJson.get("error").getAsString() : "Unknown error";
                        throw new RuntimeException(error);
                    }

                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    return gson.fromJson(json.getAsJsonObject("script"), ScriptEntry.class);
                }

            } catch (IOException e) {
                throw new RuntimeException("Connection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Update an existing script (owner only)
     */
    public CompletableFuture<ScriptEntry> updateScript(SDNUserInfo user, String scriptId, String name,
            String description, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                if (name != null)
                    body.addProperty("name", name);
                if (description != null)
                    body.addProperty("description", description);
                if (content != null)
                    body.addProperty("content", content);

                Request request = addAuthHeaders(new Request.Builder(), user)
                        .url(API_URL + "/" + scriptId)
                        .put(RequestBody.create(JSON, gson.toJson(body)))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = readBody(response);

                    if (!response.isSuccessful()) {
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        String error = errorJson.has("error") ? errorJson.get("error").getAsString() : "Unknown error";
                        throw new RuntimeException(error);
                    }

                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    return gson.fromJson(json.getAsJsonObject("script"), ScriptEntry.class);
                }

            } catch (IOException e) {
                throw new RuntimeException("Connection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Delete a script (owner only)
     */
    public CompletableFuture<Boolean> deleteScript(SDNUserInfo user, String scriptId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = addAuthHeaders(new Request.Builder(), user)
                        .url(API_URL + "/" + scriptId)
                        .delete()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String responseBody = readBody(response);
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        String error = errorJson.has("error") ? errorJson.get("error").getAsString() : "Unknown error";
                        throw new RuntimeException(error);
                    }
                    return true;
                }

            } catch (IOException e) {
                throw new RuntimeException("Connection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Upvote a script (12hr cooldown per user per script)
     */
    public CompletableFuture<VoteResult> voteScript(SDNUserInfo user, String scriptId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = addAuthHeaders(new Request.Builder(), user)
                        .url(API_URL + "/" + scriptId + "/vote")
                        .post(RequestBody.create(JSON, "{}"))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = readBody(response);

                    if (!response.isSuccessful()) {
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        String error = errorJson.has("error") ? errorJson.get("error").getAsString() : "Unknown error";
                        throw new RuntimeException(error);
                    }

                    return gson.fromJson(responseBody, VoteResult.class);
                }

            } catch (IOException e) {
                throw new RuntimeException("Connection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Vote result from API
     */
    public static class VoteResult {
        public int votes;
        public boolean hasVoted;
    }
}
