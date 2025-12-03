package com.tonic.plugins.attacktimer.ui;

import com.tonic.model.ui.components.CollapsiblePanel;
import com.tonic.model.ui.components.OptionPanel;
import com.tonic.model.ui.components.ToggleSlider;
import com.tonic.plugins.attacktimer.AttackTimerConfig;
import com.tonic.plugins.attacktimer.AttackTimerPlugin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class AttackTimerConfigPanel extends PluginPanel
{
    private final AttackTimerConfig config;
    private final ConfigManager configManager;

    private AttackTimerPlugin plugin;

    private JLabel targetNameValueLabel;
    private JLabel playerTimerValueLabel;
    private JLabel opponentTimerValueLabel;
    private JLabel eatCooldownValueLabel;
    private JLabel playerFreezeValueLabel;
    private JLabel opponentFreezeValueLabel;
    private JLabel pidStatusValueLabel;
    private JLabel targetSpecValueLabel;
    private JLabel distanceValueLabel;
    private JLabel mindActionValueLabel;
    private JLabel targetFrozenValueLabel;
    private JLabel weFrozenValueLabel;
    private JLabel canMeleeValueLabel;
    private JLabel modusValueLabel;
    private JLabel rangedPercentValueLabel;
    private JLabel meleePercentValueLabel;
    private JLabel magicPercentValueLabel;
    private JLabel specModusTimerValueLabel;
    private JLabel specModusStatusValueLabel;
    private JLabel totalDamageValueLabel;
    private JPanel aiPrayersCard;

    @Inject
    public AttackTimerConfigPanel(final AttackTimerConfig config, final ConfigManager configManager)
    {
        this.config = config;
        this.configManager = configManager;

        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new BoxLayout((Container) this, BoxLayout.Y_AXIS));

        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setAlignmentX(0f);

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(10));
        content.add(buildCombatOverview());
        content.add(Box.createVerticalStrut(10));
        content.add(buildQuickActions());
        content.add(Box.createVerticalStrut(10));
        content.add(buildSpecialMoves());
        content.add(Box.createVerticalStrut(10));
        content.add(buildLogging());
        content.add(Box.createVerticalStrut(10));
        content.add(buildCombatResponses());
        content.add(Box.createVerticalStrut(10));
        content.add(buildDiscordSection());

        add(content);
        add(Box.createVerticalGlue());
    }

    public void setPlugin(final AttackTimerPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void updateCombatInfo()
    {
        if (plugin == null || !config.enabled())
        {
            setTargetName("None", Color.GRAY);
            setPlayerTimer("---", Color.GRAY);
            setOpponentTimer("---", Color.GRAY);
            setEatCooldown(0);
            setPlayerFreeze("---", Color.GRAY);
            setOpponentFreeze("---", Color.GRAY);
            setPidStatus("Unknown", Color.YELLOW);
            setTargetSpec("---", Color.GRAY);
            setDistance(-1);
            setAIPrayersVisible(false);
            setTotalDamage(0);
            return;
        }

        plugin.updatePanelInfo(this);
    }

    private JPanel buildHeader()
    {
        final JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(new Color(45, 46, 50));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        final JLabel title = new JLabel("⚔️ Attack Timer");
        title.setFont(new Font("Whitney", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        final JLabel subtitle = new JLabel("Combat automation & timing dashboard");
        subtitle.setFont(new Font("Whitney", Font.PLAIN, 12));
        subtitle.setForeground(new Color(160, 170, 185));

        final JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitle);

        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private Component buildCombatOverview()
    {
        final CollapsiblePanel combatPanel = new CollapsiblePanel("📊 Combat Overview");
        combatPanel.setExpanded(true);
        combatPanel.addContent(buildTimersCard());
        combatPanel.addVerticalStrut(8);
        combatPanel.addContent(buildFreezeCard());
        combatPanel.addVerticalStrut(8);
        combatPanel.addContent(buildStatusCard());
        combatPanel.addVerticalStrut(8);
        combatPanel.addContent(buildAiCard());
        combatPanel.addVerticalStrut(8);
        combatPanel.addContent(buildSpecCard());
        combatPanel.addVerticalStrut(8);
        combatPanel.addContent(buildDamageCard());
        return combatPanel;
    }

    private JPanel buildTimersCard()
    {
        final JPanel card = createInfoCard("⚔️ Combat Timers");
        targetNameValueLabel = addInfoRow(card, "Target", "None", Color.GRAY, Color.WHITE);
        playerTimerValueLabel = addInfoRow(card, "Me Timer", "---", Color.GRAY, new Color(120, 210, 255));
        opponentTimerValueLabel = addInfoRow(card, "Opp Timer", "---", Color.GRAY, new Color(255, 170, 120));
        eatCooldownValueLabel = addInfoRow(card, "Eat CD", "---", Color.GRAY, new Color(120, 255, 170));
        return card;
    }

    private JPanel buildFreezeCard()
    {
        final JPanel card = createInfoCard("❄️ Freeze Status");
        playerFreezeValueLabel = addInfoRow(card, "Me Frozen", "---", Color.GRAY, Color.CYAN);
        opponentFreezeValueLabel = addInfoRow(card, "Opp Frozen", "---", Color.GRAY, Color.CYAN);
        return card;
    }

    private JPanel buildStatusCard()
    {
        final JPanel card = createInfoCard("📍 Combat Status");
        pidStatusValueLabel = addInfoRow(card, "PID", "Unknown", Color.GRAY, Color.YELLOW);
        targetSpecValueLabel = addInfoRow(card, "Target Spec", "---", Color.GRAY, Color.ORANGE);
        distanceValueLabel = addInfoRow(card, "Distance", "---", Color.GRAY, Color.WHITE);
        mindActionValueLabel = addInfoRow(card, "Action", "Idle", Color.GRAY, Color.CYAN);
        return card;
    }

    private JPanel buildAiCard()
    {
        aiPrayersCard = createInfoCard("🙏 AI Prayers");
        targetFrozenValueLabel = addInfoRow(aiPrayersCard, "Target Frozen", "---", Color.GRAY, Color.WHITE);
        weFrozenValueLabel = addInfoRow(aiPrayersCard, "We Frozen", "---", Color.GRAY, Color.WHITE);
        canMeleeValueLabel = addInfoRow(aiPrayersCard, "Can Melee", "---", Color.GRAY, Color.WHITE);
        modusValueLabel = addInfoRow(aiPrayersCard, "Modus", "---", Color.GRAY, Color.WHITE);
        rangedPercentValueLabel = addInfoRow(aiPrayersCard, "Ranged", "---", Color.GRAY, new Color(120, 255, 160));
        meleePercentValueLabel = addInfoRow(aiPrayersCard, "Melee", "---", Color.GRAY, new Color(255, 140, 140));
        magicPercentValueLabel = addInfoRow(aiPrayersCard, "Magic", "---", Color.GRAY, new Color(140, 180, 255));
        return aiPrayersCard;
    }

    private JPanel buildSpecCard()
    {
        final JPanel card = createInfoCard("✨ Spec Modus");
        specModusTimerValueLabel = addInfoRow(card, "75 Tick Timer", "---", Color.GRAY, Color.WHITE);
        specModusStatusValueLabel = addInfoRow(card, "Status", "Inactive", Color.GRAY, Color.RED);
        return card;
    }

    private JPanel buildDamageCard()
    {
        final JPanel card = createInfoCard("💥 Damage Tracker");
        totalDamageValueLabel = addInfoRow(card, "Total Damage", "0", Color.GRAY, Color.ORANGE);
        return card;
    }

    private Component buildQuickActions()
    {
        final CollapsiblePanel panel = new CollapsiblePanel("⚡ Quick Actions");
        panel.setExpanded(true);
        panel.addContent(createSpinnerOption("Diagonal Step", "DD chance when target is frozen (1-50%)", config.ddAttackChance(), 1, 50, "ddAttackChance"));
        panel.addVerticalStrut(8);
        panel.addContent(createSpinnerOption("Fakie", "Tentacle / CBow fakie chance (0-100%)", config.fakieChance(), 0, 100, "fakieChance"));
        return panel;
    }

    private Component buildSpecialMoves()
    {
        final CollapsiblePanel panel = new CollapsiblePanel("🎯 Special Moves");
        panel.addContent(createSpinnerOption("AGS Spec", "Trigger AGS special when timer = 3", config.specialMove1Ags(), 0, 100, "specialMove1Ags"));
        panel.addVerticalStrut(8);
        panel.addContent(createSpinnerOption("Walk-away", "Walk-out pattern chance", config.specialMove2(), 0, 100, "specialMove2"));
        panel.addVerticalStrut(8);
        panel.addContent(createToggleOption("Free Hit", "Detect free hit opportunities", config.freeHit(), "freeHit"));
        panel.addVerticalStrut(8);
        panel.addContent(createSpinnerOption("Rackson", "Advanced Rackson sequence", config.rackson(), 0, 100, "rackson"));
        return panel;
    }

    private Component buildLogging()
    {
        final CollapsiblePanel panel = new CollapsiblePanel("📝 Logging Options");
        panel.addContent(createToggleOption("Combat Automation", "Log combat automation actions", config.combatAutomationLogging(), "combatAutomationLogging"));
        panel.addVerticalStrut(8);
        panel.addContent(createToggleOption("AI Prayers", "Log AI prayer activity", config.aiPrayersLogging(), "aiPrayersLogging"));
        panel.addVerticalStrut(8);
        panel.addContent(createToggleOption("Handler Actions", "Log handler specific actions", config.logHandlerActions(), "logHandlerActions"));
        panel.addVerticalStrut(8);
        panel.addContent(createToggleOption("Delay Detection", "Log delay detection flags", config.logDelayDetection(), "logDelayDetection"));
        panel.addVerticalStrut(8);
        panel.addContent(createToggleOption("Debug HP Check", "Periodic HP debug output", config.debugHpCheck(), "debugHpCheck"));
        return panel;
    }

    private Component buildCombatResponses()
    {
        final CollapsiblePanel panel = new CollapsiblePanel("💬 Combat Responses");
        panel.addContent(createToggleOption("Responses", "Enable combat-based responses", config.combatBasedResponses(), "combatBasedResponses"));
        panel.addVerticalStrut(8);
        panel.addContent(createToggleOption("Response Logging", "Log combat response decisions", config.combatResponseLogging(), "combatResponseLogging"));
        return panel;
    }

    private Component buildDiscordSection()
    {
        final CollapsiblePanel panel = new CollapsiblePanel("🔗 Discord Bot API");
        panel.addContent(createToggleOption("Use Bot API", "Send messages via bot API with buttons", config.useBotApiForMessages(), "useBotApiForMessages"));
        panel.addVerticalStrut(8);
        panel.addContent(createTextInputOption("Bot API URL", "Example: http://localhost:8081", config.discordBotApiUrl(), "discordBotApiUrl"));
        panel.addVerticalStrut(8);
        panel.addContent(createTextInputOption("Channel ID", "Discord channel for bot output", config.discordChannelId(), "discordChannelId"));
        return panel;
    }

    private JPanel createInfoCard(final String title)
    {
        final JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(new Color(52, 53, 58));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(65, 67, 73)), new EmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        final JLabel label = new JLabel(title);
        label.setFont(new Font("Whitney", Font.BOLD, 13));
        label.setForeground(new Color(120, 190, 255));
        label.setAlignmentX(0f);

        card.add(label);
        card.add(Box.createVerticalStrut(6));
        return card;
    }

    private JLabel addInfoRow(final JPanel card, final String label, final String value, final Color labelColor, final Color valueColor)
    {
        final JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));

        final JLabel left = new JLabel(label);
        left.setFont(new Font("Whitney", Font.PLAIN, 12));
        left.setForeground(labelColor);

        final JLabel right = new JLabel(value);
        right.setFont(new Font("Whitney", Font.BOLD, 12));
        right.setForeground(valueColor);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        card.add(row);
        return right;
    }

    private JPanel createSpinnerOption(final String title, final String description, final int initial, final int min, final int max, final String key)
    {
        final JPanel holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));
        holder.setOpaque(false);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Whitney", Font.BOLD, 12));
        titleLabel.setForeground(Color.WHITE);

        final JLabel descLabel = new JLabel("<html><body style='width:360px;'>" + description + "</body></html>");
        descLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
        descLabel.setForeground(new Color(170, 175, 185));

        final SpinnerNumberModel model = new SpinnerNumberModel(initial, min, max, 1);
        final JSpinner spinner = new JSpinner(model);
        spinner.setMaximumSize(new Dimension(100, 28));
        styleSpinner(spinner);
        spinner.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(final ChangeEvent e)
            {
                updateConfig(key, ((Number) spinner.getValue()).intValue());
            }
        });

        holder.add(titleLabel);
        holder.add(Box.createVerticalStrut(3));
        holder.add(descLabel);
        holder.add(Box.createVerticalStrut(6));
        holder.add(spinner);
        return holder;
    }

    private JPanel createToggleOption(final String title, final String description, final boolean initial, final String key)
    {
        final ToggleSlider toggle = new ToggleSlider();
        toggle.setSelected(initial);
        final OptionPanel optionPanel = new OptionPanel();
        optionPanel.init(title, description, toggle, () -> updateConfig(key, toggle.isSelected()));
        return optionPanel;
    }

    private JPanel createTextInputOption(final String title, final String description, final String initial, final String key)
    {
        final JPanel holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));
        holder.setOpaque(false);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Whitney", Font.BOLD, 12));
        titleLabel.setForeground(Color.WHITE);

        final JLabel descLabel = new JLabel("<html><body style='width:360px;'>" + description + "</body></html>");
        descLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
        descLabel.setForeground(new Color(170, 175, 185));

        final JTextField field = new JTextField(initial != null ? initial : "");
        field.setFont(new Font("Whitney", Font.PLAIN, 11));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        field.setBorder(new EmptyBorder(4, 6, 4, 6));
        field.setBackground(new Color(50, 52, 58));
        field.setForeground(Color.WHITE);

        final ActionListener save = e -> updateConfig(key, field.getText());
        field.addActionListener(save);
        field.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(final DocumentEvent e)
            {
                save.actionPerformed(null);
            }

            @Override
            public void removeUpdate(final DocumentEvent e)
            {
                save.actionPerformed(null);
            }

            @Override
            public void changedUpdate(final DocumentEvent e)
            {
                save.actionPerformed(null);
            }
        });

        holder.add(titleLabel);
        holder.add(Box.createVerticalStrut(3));
        holder.add(descLabel);
        holder.add(Box.createVerticalStrut(6));
        holder.add(field);
        return holder;
    }

    private void styleSpinner(final JSpinner spinner)
    {
        spinner.setBackground(new Color(52, 53, 58));
        spinner.setForeground(Color.WHITE);
        spinner.setBorder(BorderFactory.createLineBorder(new Color(70, 72, 78)));
        final JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor)
        {
            final JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setForeground(Color.WHITE);
            tf.setBackground(new Color(52, 53, 58));
            tf.setBorder(new EmptyBorder(2, 6, 2, 6));
        }
    }

    private void updateConfig(final String key, final Object value)
    {
        try
        {
            final Method method = configManager.getClass().getMethod("setConfiguration", String.class, String.class, Object.class);
            method.invoke(configManager, "attacktimer", key, value);
        }
        catch (final Exception ex)
        {
            System.err.println("[AttackTimerConfigPanel] Could not persist config: " + ex.getMessage());
        }
    }

    public void setTargetName(final String name, final Color color)
    {
        if (targetNameValueLabel != null)
        {
            targetNameValueLabel.setText(name);
            targetNameValueLabel.setForeground(color);
        }
    }

    public void setPlayerTimer(final String text, final Color color)
    {
        if (playerTimerValueLabel != null)
        {
            playerTimerValueLabel.setText(text);
            playerTimerValueLabel.setForeground(color);
        }
    }

    public void setOpponentTimer(final String text, final Color color)
    {
        if (opponentTimerValueLabel != null)
        {
            opponentTimerValueLabel.setText(text);
            opponentTimerValueLabel.setForeground(color);
        }
    }

    public void setEatCooldown(final int ticks)
    {
        if (eatCooldownValueLabel != null)
        {
            if (ticks <= 0)
            {
                eatCooldownValueLabel.setText("---");
                eatCooldownValueLabel.setForeground(Color.GRAY);
            }
            else
            {
                eatCooldownValueLabel.setText(String.valueOf(ticks));
                eatCooldownValueLabel.setForeground(Color.RED);
            }
        }
    }

    public void setPlayerFreeze(final String text, final Color color)
    {
        if (playerFreezeValueLabel != null)
        {
            playerFreezeValueLabel.setText(text);
            playerFreezeValueLabel.setForeground(color);
            playerFreezeValueLabel.getParent().setVisible(true);
        }
    }

    public void hidePlayerFreeze()
    {
        if (playerFreezeValueLabel != null)
        {
            playerFreezeValueLabel.getParent().setVisible(false);
        }
    }

    public void setOpponentFreeze(final String text, final Color color)
    {
        if (opponentFreezeValueLabel != null)
        {
            opponentFreezeValueLabel.setText(text);
            opponentFreezeValueLabel.setForeground(color);
            opponentFreezeValueLabel.getParent().setVisible(true);
        }
    }

    public void hideOpponentFreeze()
    {
        if (opponentFreezeValueLabel != null)
        {
            opponentFreezeValueLabel.getParent().setVisible(false);
        }
    }

    public void setPidStatus(final String text, final Color color)
    {
        if (pidStatusValueLabel != null)
        {
            pidStatusValueLabel.setText(text);
            pidStatusValueLabel.setForeground(color);
        }
    }

    public void setTargetSpec(final String text, final Color color)
    {
        if (targetSpecValueLabel != null)
        {
            targetSpecValueLabel.setText(text);
            targetSpecValueLabel.setForeground(color);
        }
    }

    public void setDistance(final int distance)
    {
        if (distanceValueLabel != null)
        {
            if (distance < 0)
            {
                distanceValueLabel.setText("---");
                distanceValueLabel.setForeground(Color.GRAY);
            }
            else
            {
                distanceValueLabel.setText(distance + " tiles");
                if (distance <= 1)
                {
                    distanceValueLabel.setForeground(Color.GREEN);
                }
                else if (distance <= 7)
                {
                    distanceValueLabel.setForeground(Color.YELLOW);
                }
                else
                {
                    distanceValueLabel.setForeground(Color.RED);
                }
            }
        }
    }

    public void setMindAction(final String action)
    {
        if (mindActionValueLabel != null)
        {
            if (action == null || action.trim().isEmpty())
            {
                mindActionValueLabel.setText("Idle");
                mindActionValueLabel.setForeground(Color.GRAY);
            }
            else
            {
                mindActionValueLabel.setText(action);
                mindActionValueLabel.setForeground(Color.CYAN);
            }
        }
    }

    public void setAIPrayersVisible(final boolean visible)
    {
        if (aiPrayersCard != null)
        {
            aiPrayersCard.setVisible(visible);
        }
    }

    public void setTargetFrozen(final boolean frozen)
    {
        if (targetFrozenValueLabel != null)
        {
            targetFrozenValueLabel.setText(frozen ? "True" : "False");
            targetFrozenValueLabel.setForeground(frozen ? Color.GREEN : Color.RED);
        }
    }

    public void setWeFrozen(final boolean frozen)
    {
        if (weFrozenValueLabel != null)
        {
            weFrozenValueLabel.setText(frozen ? "True" : "False");
            weFrozenValueLabel.setForeground(frozen ? Color.GREEN : Color.RED);
        }
    }

    public void setCanMelee(final boolean canMelee)
    {
        if (canMeleeValueLabel != null)
        {
            canMeleeValueLabel.setText(canMelee ? "True" : "False");
            canMeleeValueLabel.setForeground(Color.WHITE);
        }
    }

    public void setModus(final String modus)
    {
        if (modusValueLabel != null)
        {
            modusValueLabel.setText(modus == null ? "---" : modus);
            modusValueLabel.setForeground(Color.WHITE);
        }
    }

    public void setRangedPercent(final double percent)
    {
        if (rangedPercentValueLabel != null)
        {
            rangedPercentValueLabel.setText(String.format("%.0f%%", percent));
        }
    }

    public void setMeleePercent(final double percent)
    {
        if (meleePercentValueLabel != null)
        {
            meleePercentValueLabel.setText(String.format("%.0f%%", percent));
        }
    }

    public void setMagicPercent(final double percent)
    {
        if (magicPercentValueLabel != null)
        {
            magicPercentValueLabel.setText(String.format("%.0f%%", percent));
        }
    }

    public void setPrayerOdds(final int rangedPercent, final int meleePercent, final int magicPercent)
    {
        setRangedPercent(rangedPercent);
        setMeleePercent(meleePercent);
        setMagicPercent(magicPercent);
    }

    public void setSpecModusTimer(final int ticks)
    {
        if (specModusTimerValueLabel != null)
        {
            if (ticks < 0)
            {
                specModusTimerValueLabel.setText("---");
                specModusTimerValueLabel.setForeground(Color.GRAY);
            }
            else
            {
                specModusTimerValueLabel.setText(String.valueOf(ticks));
                specModusTimerValueLabel.setForeground(ticks >= 75 ? Color.YELLOW : Color.GREEN);
            }
        }
    }

    public void setSpecModusStatus(final boolean active)
    {
        if (specModusStatusValueLabel != null)
        {
            specModusStatusValueLabel.setText(active ? "Active" : "Inactive");
            specModusStatusValueLabel.setForeground(active ? Color.GREEN : Color.RED);
        }
    }

    public void setTotalDamage(final int damage)
    {
        if (totalDamageValueLabel != null)
        {
            totalDamageValueLabel.setText(String.valueOf(damage));
            if (damage == 0)
            {
                totalDamageValueLabel.setForeground(Color.GRAY);
            }
            else if (damage < 100)
            {
                totalDamageValueLabel.setForeground(Color.ORANGE);
            }
            else
            {
                totalDamageValueLabel.setForeground(Color.GREEN);
            }
        }
    }
}
