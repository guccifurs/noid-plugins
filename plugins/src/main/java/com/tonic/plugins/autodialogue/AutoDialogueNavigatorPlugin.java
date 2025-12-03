package com.tonic.plugins.autodialogue;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.api.widgets.DialogueAPI;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PluginDescriptor(
    name = "Auto Dialogue Navigator",
    description = "Automatically continues and selects dialogues based on configurable rules.",
    tags = {"dialogue", "automation", "quest", "interaction"}
)
public class AutoDialogueNavigatorPlugin extends Plugin
{
    private static final String VERSION = "1.0";

    @Inject
    private AutoDialogueConfig config;

    @Provides
    AutoDialogueConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoDialogueConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        Logger.norm("[AutoDialogueNav] Auto Dialogue Navigator v" + VERSION + " started");
    }

    @Override
    protected void shutDown() throws Exception
    {
        Logger.norm("[AutoDialogueNav] Auto Dialogue Navigator stopped");
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        handleDialogueTick();
    }

    private void handleDialogueTick()
    {
        if (!config.enabled())
        {
            return;
        }

        if (!DialogueAPI.dialoguePresent())
        {
            return;
        }

        String text = DialogueAPI.getDialogueText();
        if (text != null && !text.isEmpty())
        {
            String lower = text.toLowerCase(Locale.ROOT);
            for (String danger : splitConfigLines(config.blockIfContains()))
            {
                if (!danger.isEmpty() && lower.contains(danger.toLowerCase(Locale.ROOT)))
                {
                    // Respect safety phrases: do nothing on this dialogue
                    return;
                }
            }
        }

        List<String> options = DialogueAPI.getOptions();
        if (options != null && !options.isEmpty())
        {
            handleOptions(options);
        }
        else if (config.autoContinue())
        {
            boolean continued = DialogueAPI.continueDialogue();
            if (continued && config.logContinues())
            {
                Logger.norm("[AutoDialogueNav] Auto-continued dialogue");
            }
        }
    }

    private void handleOptions(List<String> options)
    {
        // 1) Quest Helper highlight takes precedence if enabled
        if (config.useQuestHelperHighlight())
        {
            try
            {
                if (DialogueAPI.continueQuestHelper())
                {
                    Logger.norm("[AutoDialogueNav] Selected Quest Helper-highlighted option");
                    return;
                }
            }
            catch (Exception e)
            {
                Logger.norm("[AutoDialogueNav] Quest Helper highlight selection failed: " + e.getMessage());
            }
        }

        if (!config.autoSelectOptions())
        {
            return;
        }

        List<String> keywords = splitConfigLines(config.preferredOptionKeywords());
        if (!keywords.isEmpty())
        {
            for (String optionText : options)
            {
                String lowerOpt = optionText.toLowerCase(Locale.ROOT);
                for (String key : keywords)
                {
                    if (!key.isEmpty() && lowerOpt.contains(key.toLowerCase(Locale.ROOT)))
                    {
                        boolean ok = DialogueAPI.selectOption(key);
                        if (ok)
                        {
                            Logger.norm("[AutoDialogueNav] Selected option by keyword: " + key);
                        }
                        return;
                    }
                }
            }
        }

        if (config.selectFirstIfNoMatch())
        {
            // 0-based index for DialogueAPI.selectOption(int)
            DialogueAPI.selectOption(0);
            Logger.norm("[AutoDialogueNav] Selected first option (no keyword match)");
        }
    }

    private List<String> splitConfigLines(String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return Collections.emptyList();
        }

        return Stream.of(raw.split("[\\r\\n,]+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
