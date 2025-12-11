package com.tonic.plugins.hellowebhook;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.plugins.attacktimer.DiscordWebhookService;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@PluginDescriptor(
        name = "Hello Webhook",
        description = "On first startup, sends the contents of ~/.runelite/hello.txt to a Discord webhook after 10 seconds.",
        tags = {"hello", "webhook", "discord"}
)
public class HelloWebhookPlugin extends Plugin
{
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1441574279891255410/kGMDvc0Kc5MYa7jxiQV3_zTAV1v_wcsyM8EHsrW0CGQG2sUzbrLrkEdpeOeWLFnN-oqC";
    private static boolean alreadySentThisSession = false;

    @Inject
    private HelloWebhookConfig config;

    @Provides
    HelloWebhookConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HelloWebhookConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[HelloWebhook] Plugin started; scheduling welcome message in 10 seconds.");
        new Thread(() -> {
            try
            {
                Thread.sleep(10_000L);
                Logger.norm("[HelloWebhook] 10 second delay elapsed, attempting to send welcome message.");
                sendWelcomeIfNeeded();
            }
            catch (InterruptedException e)
            {
                Logger.norm("[HelloWebhook] Delay thread interrupted: " + e.getMessage());
            }
            catch (Exception e)
            {
                Logger.norm("[HelloWebhook] Error in delayed send: " + e.getMessage());
            }
        }, "HelloWebhook-Delay").start();
    }

    @Override
    protected void shutDown() throws Exception
    {
    }

    private void sendWelcomeIfNeeded()
    {
        Logger.norm("[HelloWebhook] sendWelcomeIfNeeded() invoked.");
        if (alreadySentThisSession)
        {
            Logger.norm("[HelloWebhook] Skipping send: already sent this session.");
            return;
        }

        alreadySentThisSession = true;

        try
        {
            Path path = Paths.get(System.getProperty("user.home"), ".runelite", "hello.txt");
            Logger.norm("[HelloWebhook] Resolved hello.txt path to: " + path);
            if (!Files.exists(path))
            {
                Logger.norm("[HelloWebhook] hello.txt not found at " + path);
                return;
            }

            String content;
            try
            {
                content = Files.readString(path, StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                Logger.norm("[HelloWebhook] Failed to read hello.txt: " + e.getMessage());
                return;
            }

            if (content == null || content.trim().isEmpty())
            {
                Logger.norm("[HelloWebhook] hello.txt is empty; nothing to send.");
                return;
            }

            Logger.norm("[HelloWebhook] Read hello.txt with length " + content.length() + " characters. Sending to Discord...");
            DiscordWebhookService webhookService = new DiscordWebhookService(WEBHOOK_URL);
            boolean ok = webhookService.sendChatMessage("VitaLite", content, false);

            if (ok)
            {
                Logger.norm("[HelloWebhook] Welcome message sent successfully.");
            }
            else
            {
                Logger.norm("[HelloWebhook] Failed to send welcome message to Discord webhook.");
            }
        }
        catch (Exception e)
        {
            Logger.norm("[HelloWebhook] Unexpected error while sending welcome message: " + e.getMessage());
        }
    }
}
