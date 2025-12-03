package com.tonic.plugins.helperbox;

import com.google.inject.Provides;
import com.tonic.util.VitaPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.eventbus.Subscribe;
import com.tonic.events.PacketSent;
import com.tonic.packets.PacketMapReader;
import com.tonic.packets.types.PacketDefinition;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
    name = "HelperBox Agility v1.7",
    description = "Automated agility training using Draynor, Al-Kharid & Varrock rooftops and VitaLite pathing.",
    tags = {"helper", "agility", "automation", "rooftop"}
)
public class HelperBoxPlugin extends VitaPlugin
{
    @Inject
    private Client client;

    @Inject
    private HelperBoxConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HelperBoxOverlay overlay;

    private AgilityDraynorTask draynorTask;
    private AgilityAlKharidTask alKharidTask;
    private AgilityVarrockTask varrockTask;
    private WitchsHouseTask witchsHouseTask;

    public int getStepIndex()
    {
        if (config.enableWitchsHouse())
        {
            return witchsHouseTask != null ? witchsHouseTask.getStepIndex() : 0;
        }

        HelperBoxCourse course = config.course();
        switch (course)
        {
            case AL_KHARID:
                return alKharidTask != null ? alKharidTask.getStepIndex() : 0;
            case VARROCK:
                return varrockTask != null ? varrockTask.getStepIndex() : 0;
            case DRAYNOR:
            default:
                return draynorTask != null ? draynorTask.getStepIndex() : 0;
        }
    }

    public String getStepLabel()
    {
        if (config.enableWitchsHouse())
        {
            return witchsHouseTask != null ? witchsHouseTask.getStepLabel() : "Idle";
        }

        HelperBoxCourse course = config.course();
        switch (course)
        {
            case AL_KHARID:
                return alKharidTask != null ? alKharidTask.getStepLabel() : "Idle";
            case VARROCK:
                return varrockTask != null ? varrockTask.getStepLabel() : "Idle";
            case DRAYNOR:
            default:
                return draynorTask != null ? draynorTask.getStepLabel() : "Idle";
        }
    }

    public int getTotalSteps()
    {
        if (config.enableWitchsHouse())
        {
            return WitchsHouseTask.TOTAL_STEPS;
        }

        HelperBoxCourse course = config.course();
        switch (course)
        {
            case AL_KHARID:
                return AgilityAlKharidTask.TOTAL_STEPS;
            case VARROCK:
                return AgilityVarrockTask.TOTAL_STEPS;
            case DRAYNOR:
            default:
                return AgilityDraynorTask.TOTAL_STEPS;
        }
    }

    @Provides
    HelperBoxConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HelperBoxConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("[HelperBox] HelperBox Agility v1.7 started");
        draynorTask = new AgilityDraynorTask(client, config);
        alKharidTask = new AgilityAlKharidTask(client, config);
        varrockTask = new AgilityVarrockTask(client, config);
        witchsHouseTask = new WitchsHouseTask(client, config);
        if (overlayManager != null && overlay != null)
        {
            overlayManager.add(overlay);
        }
    }

    @Override
    protected void shutDown()
    {
        log.info("[HelperBox] HelperBox stopped");
        draynorTask = null;
        alKharidTask = null;
        varrockTask = null;
        witchsHouseTask = null;
        if (overlayManager != null && overlay != null)
        {
            overlayManager.remove(overlay);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!HelperBoxConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        // Reset local state if needed when config changes
        if (draynorTask != null)
        {
            draynorTask.resetState();
        }
        if (alKharidTask != null)
        {
            alKharidTask.resetState();
        }
        if (varrockTask != null)
        {
            varrockTask.resetState();
        }
        if (witchsHouseTask != null)
        {
            witchsHouseTask.resetState();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        String msg = event.getMessage();
        if (msg != null && msg.contains("I can't reach that"))
        {
            if (draynorTask != null)
            {
                draynorTask.notifyCantReach();
            }
            if (alKharidTask != null)
            {
                alKharidTask.notifyCantReach();
            }
            if (varrockTask != null)
            {
                varrockTask.notifyCantReach();
            }
        }
    }

    @Override
    public void loop()
    {
        if (!config.enabled())
        {
            return;
        }

        HelperBoxCourse course = config.course();

        // Only stop on agility level when running agility, not when Witch's House helper is active
        if (!config.enableWitchsHouse() &&
            config.stopAtLevel() > 0 &&
            client.getRealSkillLevel(Skill.AGILITY) >= config.stopAtLevel())
        {
            log.info("[HelperBox] Reached target agility level {}, disabling HelperBox",
                client.getRealSkillLevel(Skill.AGILITY));
            pluginManager.setPluginEnabled(this, false);
            return;
        }

        try
        {
            // If Witch's House quest helper is enabled, run it instead of agility
            if (config.enableWitchsHouse())
            {
                if (witchsHouseTask == null)
                {
                    witchsHouseTask = new WitchsHouseTask(client, config);
                }
                witchsHouseTask.tick();
                return;
            }

            switch (course)
            {
                case AL_KHARID:
                    if (alKharidTask == null)
                    {
                        alKharidTask = new AgilityAlKharidTask(client, config);
                    }
                    alKharidTask.tick();
                    break;
                case VARROCK:
                    if (varrockTask == null)
                    {
                        varrockTask = new AgilityVarrockTask(client, config);
                    }
                    varrockTask.tick();
                    break;
                case DRAYNOR:
                default:
                    if (draynorTask == null)
                    {
                        draynorTask = new AgilityDraynorTask(client, config);
                    }
                    draynorTask.tick();
                    break;
            }
        }
        catch (Exception e)
        {
            log.error("[HelperBox] Error in HelperBox loop", e);
        }
    }

    @Subscribe
    public void onPacketSent(com.tonic.events.PacketSent event)
    {
        if (!config.enabled())
        {
            return;
        }
        if (config.enableWitchsHouse())
        {
            return;
        }
        if (config.course() != HelperBoxCourse.VARROCK)
        {
            return;
        }
        if (varrockTask == null)
        {
            return;
        }
        try
        {
            PacketDefinition def = PacketMapReader.analyze(event.getFreshBuffer());
            if (def == null)
                return;
            if (config.logSteps())
            {
                log.info("[HelperBox] [Varrock] PacketSent: {}", def.getName());
            }
            String name = def.getName();
            if (name == null || !name.startsWith("OP_GAME_OBJECT_ACTION_"))
                return;
            Object idObj = def.getMap().get("identifier");
            Object wx = def.getMap().get("worldX");
            Object wy = def.getMap().get("worldY");
            if (idObj instanceof Number && wx instanceof Number && wy instanceof Number)
            {
                int identifier = ((Number) idObj).intValue();
                int worldX = ((Number) wx).intValue();
                int worldY = ((Number) wy).intValue();
                int plane = 0;
                try
                {
                    com.tonic.data.wrappers.PlayerEx local = com.tonic.data.wrappers.PlayerEx.getLocal();
                    if (local != null && local.getWorldPoint() != null)
                    {
                        plane = local.getWorldPoint().getPlane();
                    }
                }
                catch (Exception ignored)
                {
                }
                if (varrockTask != null)
                {
                    varrockTask.onObjectActionPacket(identifier, worldX, worldY, plane);
                }
            }
        }
        catch (Exception ex)
        {
            log.debug("[HelperBox] Error while parsing packet", ex);
        }
    }
}
