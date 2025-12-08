package com.tonic.plugins.gearswapper.friendshare;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * WebSocket client for FriendShare feature.
 * Connects to relay server, handles friend detection and screen sharing
 * coordination.
 */
@Singleton
public class FriendShareService implements WebSocket.Listener {

    // Server URL - change this to your CentOS server
    private static final String SERVER_URL = "ws://localhost:8765";

    private final Client client;
    private final ClientThread clientThread;
    private final Gson gson = new Gson();

    private WebSocket webSocket;
    private boolean connected = false;
    private String currentRsn;

    // Online friends who also use GearSwapper
    private final Set<String> onlineFriends = ConcurrentHashMap.newKeySet();

    // Sharing state
    private String sharingWith = null;
    private boolean isSender = false;

    // Callbacks
    private Consumer<Set<String>> onFriendsUpdated;
    private Consumer<String> onShareRequest;
    private Consumer<String> onShareStart;
    private Runnable onShareEnd;
    private Consumer<byte[]> onFrameReceived;

    private final StringBuilder messageBuffer = new StringBuilder();

    @Inject
    public FriendShareService(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Connect to the FriendShare server.
     */
    public void connect() {
        if (connected)
            return;

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            webSocket = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(SERVER_URL), this)
                    .join();

            System.out.println("[FriendShare] Connected to server");
        } catch (Exception e) {
            System.err.println("[FriendShare] Failed to connect: " + e.getMessage());
        }
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Goodbye");
            } catch (Exception e) {
                // Ignore
            }
            webSocket = null;
        }
        connected = false;
        onlineFriends.clear();
    }

    /**
     * Authenticate with the server using current RSN and friend list.
     */
    public void authenticate() {
        if (!connected || client.getLocalPlayer() == null)
            return;

        currentRsn = client.getLocalPlayer().getName();
        if (currentRsn == null)
            return;

        List<String> friends = getFriendList();

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "auth");
        msg.addProperty("rsn", currentRsn);
        msg.add("friends", gson.toJsonTree(friends));

        send(msg.toString());
        System.out.println("[FriendShare] Authenticated as " + currentRsn + " with " + friends.size() + " friends");
    }

    /**
     * Update server with current friend list.
     */
    public void updateFriends() {
        if (!connected)
            return;

        List<String> friends = getFriendList();

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "update_friends");
        msg.add("friends", gson.toJsonTree(friends));

        send(msg.toString());
    }

    /**
     * Request to share screen with a friend.
     */
    public void requestShare(String targetRsn) {
        if (!connected)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "share_request");
        msg.addProperty("target", targetRsn);

        send(msg.toString());
        System.out.println("[FriendShare] Requested share with " + targetRsn);
    }

    /**
     * Accept a share request.
     */
    public void acceptShare(String fromRsn) {
        if (!connected)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "share_accept");
        msg.addProperty("from", fromRsn);

        send(msg.toString());
        System.out.println("[FriendShare] Accepted share from " + fromRsn);
    }

    /**
     * Decline a share request.
     */
    public void declineShare(String fromRsn) {
        if (!connected)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "share_decline");
        msg.addProperty("from", fromRsn);

        send(msg.toString());
    }

    /**
     * Send a screen frame (as base64 JPEG).
     */
    public void sendFrame(byte[] jpegData) {
        if (!connected || !isSender || sharingWith == null)
            return;

        String base64 = Base64.getEncoder().encodeToString(jpegData);

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "frame");
        msg.addProperty("data", base64);

        send(msg.toString());
    }

    /**
     * End the current share session.
     */
    public void endShare() {
        if (!connected)
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "share_end");

        send(msg.toString());

        sharingWith = null;
        isSender = false;
    }

    // ========== Getters ==========

    public boolean isConnected() {
        return connected;
    }

    public Set<String> getOnlineFriends() {
        return Collections.unmodifiableSet(onlineFriends);
    }

    public boolean isSharing() {
        return sharingWith != null;
    }

    public boolean isSender() {
        return isSender;
    }

    public String getSharingWith() {
        return sharingWith;
    }

    // ========== Callbacks ==========

    public void setOnFriendsUpdated(Consumer<Set<String>> callback) {
        this.onFriendsUpdated = callback;
    }

    public void setOnShareRequest(Consumer<String> callback) {
        this.onShareRequest = callback;
    }

    public void setOnShareStart(Consumer<String> callback) {
        this.onShareStart = callback;
    }

    public void setOnShareEnd(Runnable callback) {
        this.onShareEnd = callback;
    }

    public void setOnFrameReceived(Consumer<byte[]> callback) {
        this.onFrameReceived = callback;
    }

    // ========== WebSocket Listener ==========

    @Override
    public void onOpen(WebSocket webSocket) {
        connected = true;
        webSocket.request(1);

        // Authenticate after connection
        SwingUtilities.invokeLater(this::authenticate);
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
        onlineFriends.clear();
        sharingWith = null;
        isSender = false;
        System.out.println("[FriendShare] Disconnected: " + reason);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.err.println("[FriendShare] Error: " + error.getMessage());
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
                case "auth_success":
                    System.out.println("[FriendShare] Authentication successful");
                    break;

                case "online_friends":
                    handleOnlineFriends(json);
                    break;

                case "share_incoming":
                    handleShareIncoming(json);
                    break;

                case "share_start":
                    handleShareStart(json);
                    break;

                case "share_declined":
                    System.out.println("[FriendShare] Share request declined");
                    break;

                case "share_ended":
                    handleShareEnded();
                    break;

                case "frame":
                    handleFrameReceived(json);
                    break;

                case "share_error":
                    String error = json.has("message") ? json.get("message").getAsString() : "Unknown error";
                    System.err.println("[FriendShare] Error: " + error);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[FriendShare] Failed to parse message: " + e.getMessage());
        }
    }

    private void handleOnlineFriends(JsonObject json) {
        onlineFriends.clear();

        if (json.has("friends")) {
            for (var elem : json.getAsJsonArray("friends")) {
                onlineFriends.add(elem.getAsString());
            }
        }

        System.out.println("[FriendShare] Online friends with GearSwapper: " + onlineFriends);

        if (onFriendsUpdated != null) {
            SwingUtilities.invokeLater(() -> onFriendsUpdated.accept(onlineFriends));
        }
    }

    private void handleShareIncoming(JsonObject json) {
        String from = json.get("from").getAsString();
        System.out.println("[FriendShare] Share request from " + from);

        if (onShareRequest != null) {
            SwingUtilities.invokeLater(() -> onShareRequest.accept(from));
        }
    }

    private void handleShareStart(JsonObject json) {
        sharingWith = json.get("peer").getAsString();
        isSender = "sender".equals(json.get("role").getAsString());

        System.out.println(
                "[FriendShare] Share started with " + sharingWith + " as " + (isSender ? "sender" : "receiver"));

        if (onShareStart != null) {
            SwingUtilities.invokeLater(() -> onShareStart.accept(sharingWith));
        }
    }

    private void handleShareEnded() {
        sharingWith = null;
        isSender = false;

        System.out.println("[FriendShare] Share ended");

        if (onShareEnd != null) {
            SwingUtilities.invokeLater(onShareEnd);
        }
    }

    private void handleFrameReceived(JsonObject json) {
        if (!json.has("data") || onFrameReceived == null)
            return;

        try {
            String base64 = json.get("data").getAsString();
            byte[] data = Base64.getDecoder().decode(base64);
            onFrameReceived.accept(data);
        } catch (Exception e) {
            System.err.println("[FriendShare] Failed to decode frame: " + e.getMessage());
        }
    }

    private List<String> getFriendList() {
        List<String> friends = new ArrayList<>();

        try {
            Friend[] friendArray = client.getFriendContainer().getMembers();
            if (friendArray != null) {
                for (Friend friend : friendArray) {
                    if (friend != null && friend.getName() != null) {
                        friends.add(friend.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Friend list not available
        }

        return friends;
    }
}
