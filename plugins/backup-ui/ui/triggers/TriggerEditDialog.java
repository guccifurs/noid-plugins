package com.tonic.plugins.gearswapper.ui.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import com.tonic.plugins.gearswapper.triggers.*;
import com.tonic.plugins.gearswapper.triggers.GearSwapAction;
import com.tonic.plugins.gearswapper.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

/**
 * Dialog for editing existing triggers
 */
public class TriggerEditDialog extends JDialog {
    private final TriggerEngine triggerEngine;
    private final Trigger originalTrigger;

    // UI Components
    private JTextField nameField;
    private JComboBox<TriggerType> typeComboBox;
    private JCheckBox enabledCheckBox;

    // Configuration panels
    private JPanel configPanel;
    private JPanel animationConfigPanel;
    private JPanel hpConfigPanel;
    private JPanel xpConfigPanel;

    // Animation config
    private JTextField animationIdField;
    private JTextField animationCooldownField;
    private JComboBox<String> animationTargetFilterComboBox;
    private JCheckBox onlyInCombatCheckBox;
    private JTextField cooldownField;

    // GFX config (Dedicated fields)
    private JTextField gfxIdField;
    private JTextField gfxCooldownField;
    private JComboBox<String> gfxTargetFilterComboBox;

    // HP config
    private JTextField hpThresholdField;
    private JComboBox<TriggerConfig.HpTargetType> hpTargetTypeComboBox;
    private JComboBox<TriggerConfig.HpThresholdType> hpThresholdTypeComboBox;
    private JComboBox<String> targetFilterComboBox;

    // Special attack requirement
    private JCheckBox requireSpecialAttackCheckBox;
    private JSlider specialAttackSlider;
    private JLabel specialAttackValueLabel;

    // HP special attack components
    private JSlider hpSpecialAttackSlider;
    private JLabel hpSpecialAttackValueLabel;

    // XP special attack components
    private JSlider xpSpecialAttackSlider;
    private JLabel xpSpecialAttackValueLabel;

    // Distance check
    private JTextField distanceField;

    // XP config
    private JTextField xpThresholdField;
    private JComboBox<String> skillFilterComboBox;

    // Player Spawned config
    private JPanel playerSpawnedConfigPanel;
    private JTextField playerSpawnedRadiusField;
    private JCheckBox playerSpawnedNoTargetCheckBox;
    private JCheckBox playerSpawnedSetTargetCheckBox;
    private JCheckBox playerSpawnedIgnoreFriendsCheckBox;
    private JCheckBox playerSpawnedAttackableOnlyCheckBox;

    // Actions
    private JComboBox<String> gearLoadoutComboBox;
    private JPanel actionsPanel;

    // Move action components
    private JCheckBox enableMoveActionCheckBox;
    private JTextField moveTilesField;
    private JComboBox<String> actionTypeComboBox;

    // Buttons
    private JButton saveButton;
    private JButton cancelButton;

    public TriggerEditDialog(Frame parent, TriggerEngine triggerEngine, Trigger trigger) {
        super(parent, "Edit Trigger", true);
        this.triggerEngine = triggerEngine;
        this.originalTrigger = trigger;

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadGearLoadouts();
        loadTriggerData();

        updateConfigPanel();

        setSize(600, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        // Basic trigger info
        nameField = createStyledTextField();

        typeComboBox = new JComboBox<>(TriggerType.values());
        styleComboBox(typeComboBox);

        enabledCheckBox = new JCheckBox("Enabled");
        styleCheckBox(enabledCheckBox);

        // Animation config
        animationIdField = createStyledTextField();
        animationCooldownField = createStyledTextField();

        animationTargetFilterComboBox = new JComboBox<>(new String[] { "current", "local" });
        styleComboBox(animationTargetFilterComboBox);

        onlyInCombatCheckBox = new JCheckBox();
        styleCheckBox(onlyInCombatCheckBox);

        cooldownField = createStyledTextField();

        // GFX config
        gfxIdField = createStyledTextField();
        gfxCooldownField = createStyledTextField();

        gfxTargetFilterComboBox = new JComboBox<>(new String[] { "current", "local" });
        styleComboBox(gfxTargetFilterComboBox);
        gfxTargetFilterComboBox.setSelectedItem("current");

        // HP config
        hpThresholdField = createStyledTextField();

        hpTargetTypeComboBox = new JComboBox<>(TriggerConfig.HpTargetType.values());
        styleComboBox(hpTargetTypeComboBox);

        hpThresholdTypeComboBox = new JComboBox<>(TriggerConfig.HpThresholdType.values());
        styleComboBox(hpThresholdTypeComboBox);

        targetFilterComboBox = new JComboBox<>(new String[] { "current", "any" });
        styleComboBox(targetFilterComboBox);

        // Special attack requirement components
        requireSpecialAttackCheckBox = new JCheckBox();
        styleCheckBox(requireSpecialAttackCheckBox);

        // Sliders
        specialAttackSlider = createStyledSlider(0, 100, 50);
        specialAttackValueLabel = createStyledLabel("50%");

        hpSpecialAttackSlider = createStyledSlider(0, 100, 50);
        hpSpecialAttackValueLabel = createStyledLabel("50%");

        xpSpecialAttackSlider = createStyledSlider(0, 100, 50);
        xpSpecialAttackValueLabel = createStyledLabel("50%");

        // Distance field
        distanceField = createStyledTextField();

        // XP config
        xpThresholdField = createStyledTextField();

        // Skill filter
        String[] skills = { "any", "Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft",
                "Construction", "Hitpoints", "Agility", "Herblore", "Thieving", "Crafting",
                "Fletching", "Slayer", "Hunter", "Mining", "Smithing", "Fishing", "Cooking",
                "Firemaking", "Woodcutting", "Farming" };
        skillFilterComboBox = new JComboBox<>(skills);
        styleComboBox(skillFilterComboBox);

        // Player Spawned Config
        playerSpawnedRadiusField = createStyledTextField();
        playerSpawnedNoTargetCheckBox = new JCheckBox();
        styleCheckBox(playerSpawnedNoTargetCheckBox);
        playerSpawnedSetTargetCheckBox = new JCheckBox();
        styleCheckBox(playerSpawnedSetTargetCheckBox);
        playerSpawnedIgnoreFriendsCheckBox = new JCheckBox();
        styleCheckBox(playerSpawnedIgnoreFriendsCheckBox);
        playerSpawnedAttackableOnlyCheckBox = new JCheckBox();
        styleCheckBox(playerSpawnedAttackableOnlyCheckBox);

        // Actions
        gearLoadoutComboBox = new JComboBox<>();
        styleComboBox(gearLoadoutComboBox);

        // Action type selector
        actionTypeComboBox = new JComboBox<>();
        styleComboBox(actionTypeComboBox);
        actionTypeComboBox.addItem("Gear Swap");
        actionTypeComboBox.addItem("Move");
        actionTypeComboBox.addActionListener(e -> updateActionPanel());

        // Move action components
        enableMoveActionCheckBox = new JCheckBox();
        styleCheckBox(enableMoveActionCheckBox);
        moveTilesField = createStyledTextField("3");

        // Buttons
        saveButton = new JButton("Save Changes");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveButton.setBackground(Theme.SUCCESS);
        saveButton.setForeground(Color.WHITE);
        saveButton.setBorder(BorderFactory.createLineBorder(Theme.SUCCESS.darker()));
        saveButton.setFocusPainted(false);

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancelButton.setBackground(Theme.DANGER);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBorder(BorderFactory.createLineBorder(Theme.DANGER.darker()));
        cancelButton.setFocusPainted(false);

        // Create configuration panels
        createConfigPanels();
    }

    // UI Helpers (Duplicated from CreationDialog for independence)
    private JTextField createStyledTextField() {
        return createStyledTextField("");
    }

    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBackground(Theme.SURFACE);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return field;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        box.setBackground(Theme.SURFACE);
        box.setForeground(Theme.TEXT_PRIMARY);
        box.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
    }

    private void styleCheckBox(JCheckBox box) {
        box.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        box.setBackground(Theme.SURFACE);
        box.setForeground(Theme.TEXT_PRIMARY);
        box.setOpaque(true);
    }

    private JSlider createStyledSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setBackground(Theme.SURFACE);
        slider.setForeground(Theme.TEXT_PRIMARY);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setEnabled(false);
        return slider;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(Theme.TEXT_PRIMARY);
        return label;
    }

    private JPanel gfxConfigPanel;

    private void createConfigPanels() {
        animationConfigPanel = createAnimationConfigPanel();
        gfxConfigPanel = createGfxConfigPanel();
        hpConfigPanel = createHPConfigPanel();
        xpConfigPanel = createXPConfigPanel();
        playerSpawnedConfigPanel = createPlayerSpawnedConfigPanel();
        actionsPanel = createActionsPanel();

        configPanel = new JPanel(new BorderLayout());
        configPanel.setBackground(Theme.SURFACE);
        configPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Configuration",
                0,
                0,
                getFont(),
                Theme.TEXT_PRIMARY));
        configPanel.add(animationConfigPanel, BorderLayout.CENTER);
    }

    // Panel creation methods identical to CreationDialog but kept here for class
    // completeness
    // I'll condense them slightly since I provided full code in CreationDialog
    // but for correctness I must implement them fully.

    private JPanel createAnimationConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Animation ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(animationIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Target:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(animationTargetFilterComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Cooldown (ms):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(animationCooldownField, gbc);

        return panel;
    }

    private JPanel createGfxConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);

        // Premium border: Left accent stripe + subtle surrounding
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.PURPLE), // Left accent
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 1, 1, Theme.BORDER), // Top/Right/Bottom subtle
                        new EmptyBorder(10, 15, 10, 15) // Inner padding
                )));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 10, 0); // Spacing for header
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Custom Header
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.SURFACE);

        JLabel titleLabel = new JLabel("✨ Graphic Settings");
        titleLabel.setFont(Theme.FONT_BOLD.deriveFont(13f));
        titleLabel.setForeground(Color.WHITE);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(new JSeparator(), BorderLayout.SOUTH);

        panel.add(headerPanel, gbc);

        // Description / Help Box
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 15, 0);

        JLabel helpLabel = new JLabel(
                "<html><div style='color: #b9bbbe; padding: 2px;'>Trigger based on a <b>SpotAnim (GFX)</b> appearing on the target.</div></html>");
        helpLabel.setFont(Theme.FONT_REGULAR);
        panel.add(helpLabel, gbc);

        // Reset for fields
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 0, 5, 10); // Gap between label and field
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // GFX ID
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel gfxIdLabel = createLabel("GFX ID:");
        gfxIdLabel.setFont(Theme.FONT_BOLD);
        panel.add(gfxIdLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(gfxIdField, gbc); // Dedicated field

        // Target (Local / Current target)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 0, 5, 10);
        JLabel trackLabel = createLabel("Track On:");
        trackLabel.setFont(Theme.FONT_BOLD);
        panel.add(trackLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(gfxTargetFilterComboBox, gbc); // Dedicated field

        // Cooldown
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 0, 5, 10);
        JLabel cdLabel = createLabel("Cooldown (ms):");
        cdLabel.setFont(Theme.FONT_BOLD);
        panel.add(cdLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(gfxCooldownField, gbc); // Dedicated field

        return panel;
    }

    private JPanel createHPConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Target Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(hpTargetTypeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Threshold Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(hpThresholdTypeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("HP Threshold:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(hpThresholdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Target Filter:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(targetFilterComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("Only in combat", onlyInCombatCheckBox), gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("Require Special Attack", requireSpecialAttackCheckBox), gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 20, 5, 5);
        panel.add(createLabel("Min Special %:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel hpSliderPanel = new JPanel(new BorderLayout());
        hpSliderPanel.setBackground(Theme.SURFACE);
        hpSliderPanel.add(hpSpecialAttackSlider, BorderLayout.CENTER);
        hpSliderPanel.add(hpSpecialAttackValueLabel, BorderLayout.EAST);
        panel.add(hpSliderPanel, gbc);

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        panel.add(createLabel("Cooldown (ms):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(cooldownField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        panel.add(createLabel("Max Distance:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(distanceField, gbc);

        return panel;
    }

    private JPanel createXPConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panel.add(createLabel("XP Threshold:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(xpThresholdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(createLabel("Skill Filter:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(skillFilterComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("Require Special Attack", requireSpecialAttackCheckBox), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 20, 5, 5);
        panel.add(createLabel("Min Special %:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel xpSliderPanel = new JPanel(new BorderLayout());
        xpSliderPanel.setBackground(Theme.SURFACE);
        xpSliderPanel.add(xpSpecialAttackSlider, BorderLayout.CENTER);
        xpSliderPanel.add(xpSpecialAttackValueLabel, BorderLayout.EAST);
        panel.add(xpSliderPanel, gbc);

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(createLabel("Cooldown (ms):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(cooldownField, gbc);

        return panel;
    }

    private JPanel createPlayerSpawnedConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Radius (0=anywhere):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(playerSpawnedRadiusField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("No Target Required", playerSpawnedNoTargetCheckBox), gbc);

        gbc.gridy = 2;
        panel.add(createLabelWithCheckbox("Set Spawned Player as Target", playerSpawnedSetTargetCheckBox), gbc);

        gbc.gridy = 3;
        panel.add(createLabelWithCheckbox("Ignore Friends/Clan", playerSpawnedIgnoreFriendsCheckBox), gbc);

        gbc.gridy = 4;
        panel.add(createLabelWithCheckbox("Attackable Only", playerSpawnedAttackableOnlyCheckBox), gbc);

        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(createLabel("Cooldown (ms):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(cooldownField, gbc);

        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "Actions", 0, 0, getFont(), Theme.TEXT_PRIMARY));

        JPanel mainPanel = new JPanel(new CardLayout());
        mainPanel.setBackground(Theme.SURFACE);

        JPanel gearSwapPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gearSwapPanel.setBackground(Theme.SURFACE);
        gearSwapPanel.add(createLabel("Loadout:"));
        gearSwapPanel.add(gearLoadoutComboBox);
        mainPanel.add(gearSwapPanel, "Gear Swap");

        JPanel movePanel = new JPanel(new GridBagLayout());
        movePanel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        movePanel.add(createLabel("Action Type:"), gbc);
        gbc.gridy = 1;
        movePanel.add(actionTypeComboBox, gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        movePanel.add(createLabel("Move Tiles:"), gbc);
        gbc.gridx = 1;
        movePanel.add(moveTilesField, gbc);

        mainPanel.add(movePanel, "Move");
        panel.add(mainPanel, BorderLayout.CENTER);
        panel.putClientProperty("mainPanel", mainPanel);

        return panel;
    }

    private void updateActionPanel() {
        JPanel mainPanel = (JPanel) actionsPanel.getClientProperty("mainPanel");
        if (mainPanel != null) {
            CardLayout cardLayout = (CardLayout) mainPanel.getLayout();
            String selectedAction = (String) actionTypeComboBox.getSelectedItem();
            cardLayout.show(mainPanel, selectedAction);
        }
    }

    private void loadGearLoadouts() {
        if (triggerEngine.getPlugin() != null) {
            GearSwapperPlugin plugin = triggerEngine.getPlugin();
            for (int i = 0; i < 10; i++) {
                String loadoutName = plugin.getLoadoutNameForTrigger(i);
                if (loadoutName != null && !loadoutName.trim().isEmpty()) {
                    gearLoadoutComboBox.addItem(loadoutName);
                }
            }
        }
        if (gearLoadoutComboBox.getItemCount() == 0) {
            gearLoadoutComboBox.addItem("Melee Setup");
            gearLoadoutComboBox.addItem("Range Setup");
            gearLoadoutComboBox.addItem("Mage Setup");
            gearLoadoutComboBox.addItem("Hybrid Setup");
        }
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(Theme.TEXT_PRIMARY);
        return label;
    }

    private JPanel createLabelWithCheckbox(String text, JCheckBox checkBox) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(Theme.SURFACE);
        panel.add(checkBox);
        panel.add(createLabel(text));
        return panel;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Theme.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel basicPanel = new JPanel(new GridBagLayout());
        basicPanel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        basicPanel.add(createLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        basicPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        basicPanel.add(createLabel("Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        basicPanel.add(typeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        basicPanel.add(enabledCheckBox, gbc);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Theme.SURFACE);
        contentPanel.add(basicPanel, BorderLayout.NORTH);
        contentPanel.add(configPanel, BorderLayout.CENTER);
        contentPanel.add(actionsPanel, BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Theme.BACKGROUND);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        typeComboBox.addActionListener(e -> updateConfigPanel());

        requireSpecialAttackCheckBox.addActionListener(e -> {
            boolean enabled = requireSpecialAttackCheckBox.isSelected();
            TriggerType selectedType = (TriggerType) typeComboBox.getSelectedItem();

            if (selectedType == TriggerType.HP) {
                hpSpecialAttackSlider.setEnabled(enabled);
                hpSpecialAttackValueLabel.setEnabled(enabled);
            } else if (selectedType == TriggerType.XP) {
                xpSpecialAttackSlider.setEnabled(enabled);
                xpSpecialAttackValueLabel.setEnabled(enabled);
            } else {
                specialAttackSlider.setEnabled(enabled);
                specialAttackValueLabel.setEnabled(enabled);
            }
        });

        specialAttackSlider
                .addChangeListener(e -> specialAttackValueLabel.setText(specialAttackSlider.getValue() + "%"));
        hpSpecialAttackSlider
                .addChangeListener(e -> hpSpecialAttackValueLabel.setText(hpSpecialAttackSlider.getValue() + "%"));
        xpSpecialAttackSlider
                .addChangeListener(e -> xpSpecialAttackValueLabel.setText(xpSpecialAttackSlider.getValue() + "%"));

        saveButton.addActionListener(e -> saveTrigger());
        cancelButton.addActionListener(e -> dispose());
    }

    private void updateConfigPanel() {
        TriggerType selectedType = (TriggerType) typeComboBox.getSelectedItem();
        configPanel.removeAll();
        switch (selectedType) {
            case ANIMATION:
                configPanel.add(animationConfigPanel, BorderLayout.CENTER);
                break;
            case GFX:
                configPanel.add(gfxConfigPanel, BorderLayout.CENTER);
                break;
            case HP:
                configPanel.add(hpConfigPanel, BorderLayout.CENTER);
                break;
            case XP:
                configPanel.add(xpConfigPanel, BorderLayout.CENTER);
                break;
            case PLAYER_SPAWNED:
                configPanel.add(playerSpawnedConfigPanel, BorderLayout.CENTER);
                break;
            default:
                configPanel.add(animationConfigPanel, BorderLayout.CENTER);
                break;
        }
        configPanel.revalidate();
        configPanel.repaint();
    }

    private void loadTriggerData() {
        // Load basic trigger data
        nameField.setText(originalTrigger.getName());
        typeComboBox.setSelectedItem(originalTrigger.getType());
        enabledCheckBox.setSelected(originalTrigger.isEnabled());

        // Load configuration
        TriggerConfig config = originalTrigger.getConfig();

        // Load animation config
        animationIdField.setText(String.valueOf(config.getAnimationId()));
        animationCooldownField.setText(String.valueOf(config.getCooldownMs()));
        onlyInCombatCheckBox.setSelected(config.isOnlyInCombat());
        animationTargetFilterComboBox.setSelectedItem(config.getTargetFilterValue());

        // Load GFX config
        gfxIdField.setText(String.valueOf(config.getGfxId()));
        gfxCooldownField.setText(String.valueOf(config.getCooldownMs()));
        gfxTargetFilterComboBox.setSelectedItem(config.getTargetFilterValue());

        // Load HP config
        hpThresholdField.setText(String.valueOf((int) config.getHpThreshold()));
        hpTargetTypeComboBox.setSelectedItem(config.getHpTargetType());
        hpThresholdTypeComboBox.setSelectedItem(config.getHpThresholdType());
        targetFilterComboBox.setSelectedItem(config.getTargetFilterValue());

        // Load special attack requirement
        requireSpecialAttackCheckBox.setSelected(config.isRequireSpecialAttack());

        // Load sliders
        int specVal = config.getSpecialAttackThreshold();
        specialAttackSlider.setValue(specVal);
        hpSpecialAttackSlider.setValue(specVal);
        xpSpecialAttackSlider.setValue(specVal);

        // Load cooldown
        cooldownField.setText(String.valueOf(config.getCooldownMs()));

        // Load distance
        distanceField.setText(String.valueOf(config.getMaxDistance()));

        // Load XP configuration
        xpThresholdField.setText(String.valueOf(config.getXpThreshold()));
        skillFilterComboBox.setSelectedItem(config.getSkillFilter());

        // Load Player Spawned configuration
        playerSpawnedRadiusField.setText(String.valueOf(config.getPlayerSpawnedRadius()));
        playerSpawnedNoTargetCheckBox.setSelected(config.isPlayerSpawnedNoTarget());
        playerSpawnedSetTargetCheckBox.setSelected(config.isPlayerSpawnedSetTarget());
        playerSpawnedIgnoreFriendsCheckBox.setSelected(config.isPlayerSpawnedIgnoreFriends());
        playerSpawnedAttackableOnlyCheckBox.setSelected(config.isPlayerSpawnedAttackableOnly());

        // Load existing gear loadout action
        if (!originalTrigger.getActions().isEmpty()) {
            TriggerAction firstAction = originalTrigger.getActions().get(0);
            if (firstAction instanceof GearSwapAction) {
                String loadoutName = ((GearSwapAction) firstAction).getGearSetName();
                gearLoadoutComboBox.setSelectedItem(loadoutName);
                if (actionTypeComboBox.getItemCount() > 0)
                    actionTypeComboBox.setSelectedItem("Gear Swap");
            } else if (firstAction instanceof com.tonic.plugins.gearswapper.triggers.actions.MoveAction) {
                int tiles = ((com.tonic.plugins.gearswapper.triggers.actions.MoveAction) firstAction).getTilesToMove();
                moveTilesField.setText(String.valueOf(tiles));
                if (actionTypeComboBox.getItemCount() > 1)
                    actionTypeComboBox.setSelectedItem("Move");
            }
            updateActionPanel();
        }

        updateConfigPanel();

        // Trigger manual update of UI state based on loaded values (e.g. slider
        // enabled)
        // Simulate checkbox action
        for (java.awt.event.ActionListener al : requireSpecialAttackCheckBox.getActionListeners()) {
            al.actionPerformed(new java.awt.event.ActionEvent(requireSpecialAttackCheckBox, 0, ""));
        }
    }

    private void saveTrigger() {
        try {
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a trigger name", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update trigger properties
            TriggerType type = (TriggerType) typeComboBox.getSelectedItem();
            String name = nameField.getText().trim();

            originalTrigger.setName(name);
            originalTrigger.setType(type);
            originalTrigger.setEnabled(enabledCheckBox.isSelected());

            // Clear existing actions (we only support one via UI currently)
            originalTrigger.getActions().clear();

            // Add new action
            String selectedActionType = (String) actionTypeComboBox.getSelectedItem();
            if ("Gear Swap".equals(selectedActionType)) {
                String selectedLoadout = (String) gearLoadoutComboBox.getSelectedItem();
                if (selectedLoadout != null && !selectedLoadout.isEmpty()) {
                    originalTrigger.addAction(new GearSwapAction(selectedLoadout));
                }
            } else if ("Move".equals(selectedActionType)) {
                try {
                    int tiles = Integer.parseInt(moveTilesField.getText().trim());
                    if (tiles >= 1 && tiles <= 10) {
                        originalTrigger.addAction(new com.tonic.plugins.gearswapper.triggers.actions.MoveAction(tiles));
                    }
                } catch (Exception ignored) {
                }
            }

            // Update configuration based on type (Logic from CreationDialog but updating
            // existing config)
            TriggerConfig config = originalTrigger.getConfig();
            config.setCooldownMs(
                    Long.parseLong(cooldownField.getText().trim().isEmpty() ? "0" : cooldownField.getText().trim()));
            config.setRequireSpecialAttack(requireSpecialAttackCheckBox.isSelected());

            if (type == TriggerType.ANIMATION) {
                config.setAnimationId(Integer.parseInt(animationIdField.getText().trim()));
                config.setCooldownMs(Long.parseLong(animationCooldownField.getText().trim().isEmpty() ? "0"
                        : animationCooldownField.getText().trim()));
                config.setTargetFilterByValue((String) animationTargetFilterComboBox.getSelectedItem());
                config.setSpecialAttackThreshold(specialAttackSlider.getValue());
                config.setOnlyInCombat(onlyInCombatCheckBox.isSelected());
            } else if (type == TriggerType.GFX) {
                config.setGfxId(Integer.parseInt(gfxIdField.getText().trim()));
                config.setCooldownMs(Long.parseLong(gfxCooldownField.getText().trim().isEmpty() ? "0"
                        : gfxCooldownField.getText().trim()));
                config.setTargetFilterByValue((String) gfxTargetFilterComboBox.getSelectedItem());
                config.setSpecialAttackThreshold(specialAttackSlider.getValue());
                config.setOnlyInCombat(onlyInCombatCheckBox.isSelected());
            } else if (type == TriggerType.HP) {
                config.setHpThreshold(Integer.parseInt(hpThresholdField.getText().trim()));
                config.setHpTargetType((TriggerConfig.HpTargetType) hpTargetTypeComboBox.getSelectedItem());
                config.setHpThresholdType((TriggerConfig.HpThresholdType) hpThresholdTypeComboBox.getSelectedItem());
                config.setTargetFilterByValue((String) targetFilterComboBox.getSelectedItem());
                config.setSpecialAttackThreshold(hpSpecialAttackSlider.getValue());
                if (!distanceField.getText().trim().isEmpty()) {
                    config.setMaxDistance(Integer.parseInt(distanceField.getText().trim()));
                }
                config.setOnlyInCombat(onlyInCombatCheckBox.isSelected());
            } else if (type == TriggerType.XP) {
                config.setXpThreshold(Integer.parseInt(xpThresholdField.getText().trim()));
                config.setSkillFilter((String) skillFilterComboBox.getSelectedItem());
                config.setSpecialAttackThreshold(xpSpecialAttackSlider.getValue());
            } else if (type == TriggerType.PLAYER_SPAWNED) {
                config.setPlayerSpawnedRadius(Integer.parseInt(playerSpawnedRadiusField.getText().trim()));
                config.setPlayerSpawnedNoTarget(playerSpawnedNoTargetCheckBox.isSelected());
                config.setPlayerSpawnedSetTarget(playerSpawnedSetTargetCheckBox.isSelected());
                config.setPlayerSpawnedIgnoreFriends(playerSpawnedIgnoreFriendsCheckBox.isSelected());
                config.setPlayerSpawnedAttackableOnly(playerSpawnedAttackableOnlyCheckBox.isSelected());
            }

            // Notify engine of update (if needed, usually object ref is enough but let's be
            // safe)
            // triggerEngine.updateTrigger(originalTrigger); // wrapper method if exists,
            // otherwise not needed if direct ref

            JOptionPane.showMessageDialog(this, "Trigger updated successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving trigger: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
