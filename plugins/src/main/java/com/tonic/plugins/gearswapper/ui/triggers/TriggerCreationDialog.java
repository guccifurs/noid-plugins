package com.tonic.plugins.gearswapper.ui.triggers;

import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import com.tonic.plugins.gearswapper.triggers.*;
import com.tonic.plugins.gearswapper.triggers.GearSwapAction;
import com.tonic.plugins.gearswapper.ui.Theme;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Dialog for creating new triggers
 */
public class TriggerCreationDialog extends JDialog {
    private final TriggerEngine triggerEngine;

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

    // HP config
    private JTextField hpThresholdField;
    private JComboBox<TriggerConfig.HpTargetType> hpTargetTypeComboBox;
    private JComboBox<TriggerConfig.HpThresholdType> hpThresholdTypeComboBox;
    private JComboBox<String> targetFilterComboBox;

    // Special attack requirement
    private JCheckBox requireSpecialAttackCheckBox;
    private JSlider specialAttackSlider;
    private JLabel specialAttackValueLabel;

    // HP special attack components (separate to avoid conflicts)
    private JSlider hpSpecialAttackSlider;
    private JLabel hpSpecialAttackValueLabel;

    // XP special attack components (separate to avoid conflicts)
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
    private JButton testButton;

    public TriggerCreationDialog(Frame parent, TriggerEngine triggerEngine) {
        super(parent, "Create New Trigger", true);
        this.triggerEngine = triggerEngine;

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadGearLoadouts();

        // Set default trigger type to HP
        typeComboBox.setSelectedItem(TriggerType.HP);

        // Set default HP configuration values
        hpTargetTypeComboBox.setSelectedItem(TriggerConfig.HpTargetType.TARGET);
        hpThresholdTypeComboBox.setSelectedItem(TriggerConfig.HpThresholdType.BELOW);
        targetFilterComboBox.setSelectedItem("current");

        updateConfigPanel();

        setSize(600, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        // Basic trigger info
        nameField = createStyledTextField("HP Trigger");

        typeComboBox = new JComboBox<>(TriggerType.values());
        styleComboBox(typeComboBox);

        enabledCheckBox = new JCheckBox("Enabled", true);
        styleCheckBox(enabledCheckBox);

        // Animation config
        animationIdField = createStyledTextField();
        animationCooldownField = createStyledTextField();

        animationTargetFilterComboBox = new JComboBox<>(new String[] { "current", "local" });
        styleComboBox(animationTargetFilterComboBox);
        animationTargetFilterComboBox.setSelectedItem("current");

        onlyInCombatCheckBox = new JCheckBox();
        styleCheckBox(onlyInCombatCheckBox);

        cooldownField = createStyledTextField();

        // HP config
        hpThresholdField = createStyledTextField("50");

        // HP Target Type (Target/Player)
        hpTargetTypeComboBox = new JComboBox<>(TriggerConfig.HpTargetType.values());
        styleComboBox(hpTargetTypeComboBox);

        // HP Threshold Type (Above/Below)
        hpThresholdTypeComboBox = new JComboBox<>(TriggerConfig.HpThresholdType.values());
        styleComboBox(hpThresholdTypeComboBox);

        targetFilterComboBox = new JComboBox<>(new String[] { "current", "any" });
        styleComboBox(targetFilterComboBox);

        // Special attack requirement components
        requireSpecialAttackCheckBox = new JCheckBox();
        styleCheckBox(requireSpecialAttackCheckBox);

        // Main special attack slider (for animation triggers)
        specialAttackSlider = createStyledSlider(0, 100, 50);

        specialAttackValueLabel = new JLabel("50%");
        specialAttackValueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        specialAttackValueLabel.setForeground(Theme.TEXT_PRIMARY);

        // HP special attack slider (separate instance)
        hpSpecialAttackSlider = createStyledSlider(0, 100, 50);

        hpSpecialAttackValueLabel = new JLabel("50%");
        hpSpecialAttackValueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hpSpecialAttackValueLabel.setForeground(Theme.TEXT_PRIMARY);

        // XP special attack slider (separate instance)
        xpSpecialAttackSlider = createStyledSlider(0, 100, 50);

        xpSpecialAttackValueLabel = new JLabel("50%");
        xpSpecialAttackValueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        xpSpecialAttackValueLabel.setForeground(Theme.TEXT_PRIMARY);

        // Distance field
        distanceField = createStyledTextField("10");

        // XP config
        xpThresholdField = createStyledTextField("100");

        // Skill filter
        String[] skills = { "any", "Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft",
                "Construction", "Hitpoints", "Agility", "Herblore", "Thieving", "Crafting",
                "Fletching", "Slayer", "Hunter", "Mining", "Smithing", "Fishing", "Cooking",
                "Firemaking", "Woodcutting", "Farming" };
        skillFilterComboBox = new JComboBox<>(skills);
        styleComboBox(skillFilterComboBox);

        // Player Spawned Config
        playerSpawnedRadiusField = createStyledTextField("0");

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
        saveButton = new JButton("Save Trigger");
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

        testButton = new JButton("Test");
        testButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        testButton.setBackground(Theme.WARNING);
        testButton.setForeground(Color.WHITE);
        testButton.setBorder(BorderFactory.createLineBorder(Theme.WARNING.darker()));
        testButton.setFocusPainted(false);

        // Create configuration panels
        createConfigPanels();
    }

    // Helpers for consistent styling
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

    private void createConfigPanels() {
        // Animation config panel
        animationConfigPanel = createAnimationConfigPanel();

        // HP config panel
        hpConfigPanel = createHPConfigPanel();

        // Actions panel
        actionsPanel = createActionsPanel();

        // XP config panel
        xpConfigPanel = createXPConfigPanel();

        // Player Spawned config panel
        playerSpawnedConfigPanel = createPlayerSpawnedConfigPanel();

        // Main config panel (starts with animation)
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

    private JPanel createAnimationConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Animation ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Animation ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(animationIdField, gbc);

        // Target (Local / Current target)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Target:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(animationTargetFilterComboBox, gbc);

        // Cooldown
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

    private JPanel createHPConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Target HP/Player HP
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Target Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(hpTargetTypeComboBox, gbc);

        // Above/Below
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Threshold Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(hpThresholdTypeComboBox, gbc);

        // HP Threshold (whole numbers only)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("HP Threshold:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(hpThresholdField, gbc);

        // Target filter: Target/Anyone
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(createLabel("Target Filter:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(targetFilterComboBox, gbc);

        // Only in combat
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("Only in combat", onlyInCombatCheckBox), gbc);

        // Special attack requirement
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel specialAttackPanel = createLabelWithCheckbox("Require Special Attack", requireSpecialAttackCheckBox);
        panel.add(specialAttackPanel, gbc);

        // Special attack slider (indented)
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

        // Reset insets
        gbc.insets = new Insets(5, 5, 5, 5);

        // Cooldown for HP trigger
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        panel.add(createLabel("Cooldown (ms):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(cooldownField, gbc);

        // Distance check
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
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                "XP Configuration",
                0,
                0,
                getFont(),
                Theme.TEXT_PRIMARY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // XP Threshold
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panel.add(createLabel("XP Threshold:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(xpThresholdField, gbc);

        // Skill Filter
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(createLabel("Skill Filter:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(skillFilterComboBox, gbc);

        // Special attack requirement
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("Require Special Attack", requireSpecialAttackCheckBox), gbc);

        // Special attack slider (indented)
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

        // Reset insets
        gbc.insets = new Insets(5, 5, 5, 5);

        // Cooldown for XP trigger
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

        // Radius
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createLabel("Radius (0=anywhere):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(playerSpawnedRadiusField, gbc);

        // No Target Required
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(createLabelWithCheckbox("No Target Required", playerSpawnedNoTargetCheckBox), gbc);

        // Set As Target
        gbc.gridy = 2;
        panel.add(createLabelWithCheckbox("Set Spawned Player as Target", playerSpawnedSetTargetCheckBox), gbc);

        // Ignore Friends
        gbc.gridy = 3;
        panel.add(createLabelWithCheckbox("Ignore Friends/Clan", playerSpawnedIgnoreFriendsCheckBox), gbc);

        // Attackable Only
        gbc.gridy = 4;
        panel.add(createLabelWithCheckbox("Attackable Only", playerSpawnedAttackableOnlyCheckBox), gbc);

        // Cooldown
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
                "Actions",
                0,
                0,
                getFont(),
                Theme.TEXT_PRIMARY));

        // Main action panel with card layout
        JPanel mainPanel = new JPanel(new CardLayout());
        mainPanel.setBackground(Theme.SURFACE);

        // Gear swap panel
        JPanel gearSwapPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gearSwapPanel.setBackground(Theme.SURFACE);
        gearSwapPanel.add(createLabel("Loadout:"));
        gearSwapPanel.add(gearLoadoutComboBox);
        mainPanel.add(gearSwapPanel, "Gear Swap");

        // Move action panel
        JPanel movePanel = new JPanel(new GridBagLayout());
        movePanel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Action type selector
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        movePanel.add(createLabel("Action Type:"), gbc);
        gbc.gridy = 1;
        movePanel.add(actionTypeComboBox, gbc);

        // Move action options
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        movePanel.add(createLabel("Move Tiles:"), gbc);
        gbc.gridx = 1;
        movePanel.add(moveTilesField, gbc);

        mainPanel.add(movePanel, "Move");

        // Add main panel to container
        panel.add(mainPanel, BorderLayout.CENTER);

        // Store reference for card layout switching
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
        // Load actual gear loadouts from the plugin
        if (triggerEngine.getPlugin() != null) {
            GearSwapperPlugin plugin = triggerEngine.getPlugin();
            // Load up to 10 loadouts (adjust as needed)
            for (int i = 0; i < 10; i++) {
                String loadoutName = plugin.getLoadoutNameForTrigger(i);
                if (loadoutName != null && !loadoutName.trim().isEmpty()) {
                    gearLoadoutComboBox.addItem(loadoutName);
                }
            }
        }

        // If no loadouts found, add default options
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

        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Theme.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Basic info panel
        JPanel basicPanel = new JPanel(new GridBagLayout());
        basicPanel.setBackground(Theme.SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        basicPanel.add(createLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        basicPanel.add(nameField, gbc);

        // Type
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        basicPanel.add(createLabel("Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        basicPanel.add(typeComboBox, gbc);

        // Enabled
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        basicPanel.add(enabledCheckBox, gbc);

        // Combine panels
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Theme.SURFACE);
        contentPanel.add(basicPanel, BorderLayout.NORTH);
        contentPanel.add(configPanel, BorderLayout.CENTER);
        contentPanel.add(actionsPanel, BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Theme.BACKGROUND);
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // Type change handler
        typeComboBox.addActionListener(e -> updateConfigPanel());

        // Special attack checkbox handler
        requireSpecialAttackCheckBox.addActionListener(e -> {
            boolean enabled = requireSpecialAttackCheckBox.isSelected();

            // Enable/disable appropriate sliders based on current trigger type
            TriggerType selectedType = (TriggerType) typeComboBox.getSelectedItem();

            if (selectedType == TriggerType.HP) {
                hpSpecialAttackSlider.setEnabled(enabled);
                hpSpecialAttackValueLabel.setEnabled(enabled);
            } else if (selectedType == TriggerType.XP) {
                xpSpecialAttackSlider.setEnabled(enabled);
                xpSpecialAttackValueLabel.setEnabled(enabled);
            } else {
                // Animation triggers use the main slider
                specialAttackSlider.setEnabled(enabled);
                specialAttackValueLabel.setEnabled(enabled);
            }
        });

        // Special attack slider handler
        specialAttackSlider.addChangeListener(e -> {
            specialAttackValueLabel.setText(specialAttackSlider.getValue() + "%");
        });

        // HP special attack slider handler
        hpSpecialAttackSlider.addChangeListener(e -> {
            hpSpecialAttackValueLabel.setText(hpSpecialAttackSlider.getValue() + "%");
        });

        // XP special attack slider handler
        xpSpecialAttackSlider.addChangeListener(e -> {
            xpSpecialAttackValueLabel.setText(xpSpecialAttackSlider.getValue() + "%");
        });

        // Save button
        saveButton.addActionListener(e -> saveTrigger());

        // Cancel button
        cancelButton.addActionListener(e -> dispose());

        // Test button
        testButton.addActionListener(e -> testTrigger());
    }

    private void updateConfigPanel() {
        TriggerType selectedType = (TriggerType) typeComboBox.getSelectedItem();

        configPanel.removeAll();

        switch (selectedType) {
            case ANIMATION:
                configPanel.add(animationConfigPanel, BorderLayout.CENTER);
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
                // Default to animation config
                configPanel.add(animationConfigPanel, BorderLayout.CENTER);
                break;
        }

        configPanel.revalidate();
        configPanel.repaint();
    }

    private void saveTrigger() {
        try {
            // Validate input
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a trigger name", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate action selection based on action type
            String selectedActionType = (String) actionTypeComboBox.getSelectedItem();
            String selectedLoadout = null;

            if ("Gear Swap".equals(selectedActionType)) {
                selectedLoadout = (String) gearLoadoutComboBox.getSelectedItem();
                if (selectedLoadout == null || selectedLoadout.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please select a loadout to execute", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if ("Move".equals(selectedActionType)) {
                try {
                    String tilesText = moveTilesField.getText().trim();
                    if (tilesText.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Please enter number of tiles to move", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int tilesToMove = Integer.parseInt(tilesText);
                    if (tilesToMove < 1 || tilesToMove > 10) {
                        JOptionPane.showMessageDialog(this, "Move tiles must be between 1 and 10", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid move tiles format", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Create trigger
            TriggerType type = (TriggerType) typeComboBox.getSelectedItem();
            String name = nameField.getText().trim();

            Trigger trigger = new Trigger(generateTriggerId(name), name, type);
            trigger.setEnabled(enabledCheckBox.isSelected());

            // Configure based on type
            TriggerConfig config = trigger.getConfig();

            if (type == TriggerType.ANIMATION) {
                try {
                    config.setAnimationId(Integer.parseInt(animationIdField.getText().trim()));
                    config.setCooldownMs(Long.parseLong(animationCooldownField.getText().trim()));
                    config.setTargetFilterByValue((String) animationTargetFilterComboBox.getSelectedItem());
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid animation ID or cooldown", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (type == TriggerType.HP) {
                try {
                    // Validate HP threshold field (whole numbers only)
                    String hpText = hpThresholdField.getText().trim();
                    if (hpText.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "HP threshold cannot be empty", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int hpValue = Integer.parseInt(hpText);
                    if (hpValue < 0) {
                        JOptionPane.showMessageDialog(this, "HP threshold cannot be negative", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Save HP configuration
                    config.setHpThreshold(hpValue);
                    config.setHpTargetType((TriggerConfig.HpTargetType) hpTargetTypeComboBox.getSelectedItem());
                    config.setHpThresholdType(
                            (TriggerConfig.HpThresholdType) hpThresholdTypeComboBox.getSelectedItem());
                    config.setTargetFilterByValue((String) targetFilterComboBox.getSelectedItem());

                    // Save special attack requirement
                    config.setRequireSpecialAttack(requireSpecialAttackCheckBox.isSelected());
                    config.setSpecialAttackThreshold(hpSpecialAttackSlider.getValue());

                    // Save cooldown (use the existing cooldownField)
                    String cooldownText = cooldownField.getText().trim();
                    if (!cooldownText.isEmpty()) {
                        try {
                            long cooldownMs = Long.parseLong(cooldownText);
                            config.setCooldownMs(cooldownMs);
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(this, "Invalid cooldown format", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    // Save distance
                    String distanceText = distanceField.getText().trim();
                    if (!distanceText.isEmpty()) {
                        try {
                            int distance = Integer.parseInt(distanceText);
                            config.setMaxDistance(distance);
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(this, "Invalid distance format", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid HP threshold format", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error saving HP trigger: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (type == TriggerType.XP) {
                try {
                    // Validate XP threshold field
                    String xpText = xpThresholdField.getText().trim();
                    if (xpText.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "XP threshold cannot be empty", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int xpValue = Integer.parseInt(xpText);
                    if (xpValue < 0) {
                        JOptionPane.showMessageDialog(this, "XP threshold cannot be negative", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Save XP configuration
                    config.setXpThreshold(xpValue);
                    config.setSkillFilter((String) skillFilterComboBox.getSelectedItem());

                    // Save special attack requirement
                    config.setRequireSpecialAttack(requireSpecialAttackCheckBox.isSelected());
                    config.setSpecialAttackThreshold(xpSpecialAttackSlider.getValue());

                    // Save cooldown (use the existing cooldownField)
                    String cooldownText = cooldownField.getText().trim();
                    if (!cooldownText.isEmpty()) {
                        try {
                            long cooldownMs = Long.parseLong(cooldownText);
                            config.setCooldownMs(cooldownMs);
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(this, "Invalid cooldown format", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid XP threshold format", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error saving XP trigger: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (type == TriggerType.PLAYER_SPAWNED) {
                try {
                    // Validate Radius
                    String radiusText = playerSpawnedRadiusField.getText().trim();
                    int radius = Integer.parseInt(radiusText);
                    if (radius < 0 || radius > 15)
                        throw new NumberFormatException();

                    config.setPlayerSpawnedRadius(radius);
                    config.setPlayerSpawnedNoTarget(playerSpawnedNoTargetCheckBox.isSelected());
                    config.setPlayerSpawnedSetTarget(playerSpawnedSetTargetCheckBox.isSelected());
                    config.setPlayerSpawnedIgnoreFriends(playerSpawnedIgnoreFriendsCheckBox.isSelected());
                    config.setPlayerSpawnedAttackableOnly(playerSpawnedAttackableOnlyCheckBox.isSelected());

                    // Cooldown
                    String cooldownText = cooldownField.getText().trim();
                    if (!cooldownText.isEmpty())
                        config.setCooldownMs(Long.parseLong(cooldownText));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Radius must be 0-15 and valid cooldown", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Add actions based on selected action type
            if ("Gear Swap".equals(selectedActionType)) {
                if (selectedLoadout != null && !selectedLoadout.isEmpty()) {
                    GearSwapAction action = new GearSwapAction(selectedLoadout);
                    trigger.addAction(action);
                }
            } else if ("Move".equals(selectedActionType)) {
                try {
                    String tilesText = moveTilesField.getText().trim();
                    int tilesToMove = Integer.parseInt(tilesText);

                    com.tonic.plugins.gearswapper.triggers.actions.MoveAction moveAction = new com.tonic.plugins.gearswapper.triggers.actions.MoveAction(
                            tilesToMove);
                    trigger.addAction(moveAction);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid move tiles format", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Save trigger
            triggerEngine.addTrigger(trigger);

            JOptionPane.showMessageDialog(this, "Trigger created successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating trigger: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void testTrigger() {
        // Create a temporary trigger for testing
        try {
            TriggerType type = (TriggerType) typeComboBox.getSelectedItem();
            String name = "TEST_" + nameField.getText().trim();

            Trigger testTrigger = new Trigger("test_" + System.currentTimeMillis(), name, type);
            testTrigger.setEnabled(true);

            // Configure based on type
            TriggerConfig config = testTrigger.getConfig();

            if (type == TriggerType.ANIMATION) {
                config.setAnimationId(Integer.parseInt(animationIdField.getText().trim()));
                config.setCooldownMs(0); // No cooldown for testing
                config.setTargetFilterByValue((String) animationTargetFilterComboBox.getSelectedItem());
            }

            config.setOnlyInCombat(onlyInCombatCheckBox.isSelected());
            config.setTestMode(true); // Enable test mode

            // Add actions for test based on selected action type
            String selectedActionType = (String) actionTypeComboBox.getSelectedItem();
            String testSelectedLoadout = null;

            if ("Gear Swap".equals(selectedActionType)) {
                testSelectedLoadout = (String) gearLoadoutComboBox.getSelectedItem();
                if (testSelectedLoadout != null && !testSelectedLoadout.isEmpty()) {
                    GearSwapAction action = new GearSwapAction(testSelectedLoadout);
                    testTrigger.addAction(action);
                }
            } else if ("Move".equals(selectedActionType)) {
                try {
                    String tilesText = moveTilesField.getText().trim();
                    if (!tilesText.isEmpty()) {
                        int tilesToMove = Integer.parseInt(tilesText);
                        if (tilesToMove >= 1 && tilesToMove <= 10) {
                            com.tonic.plugins.gearswapper.triggers.actions.MoveAction moveAction = new com.tonic.plugins.gearswapper.triggers.actions.MoveAction(
                                    tilesToMove);
                            testTrigger.addAction(moveAction);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip move action if invalid
                }
            }

            // Add temporary trigger
            triggerEngine.addTrigger(testTrigger);

            JOptionPane.showMessageDialog(this,
                    "Test trigger created! It will be automatically removed after 30 seconds.\n\n" +
                            "Trigger: " + name + "\n" +
                            "Type: " + type + "\n" +
                            "Action: " + selectedActionType +
                            (selectedActionType.equals("Gear Swap")
                                    ? "\nLoadout: " + (testSelectedLoadout != null ? testSelectedLoadout : "None")
                                    : selectedActionType.equals("Move") ? "\nTiles: " + moveTilesField.getText() : ""),
                    "Test Trigger Created",
                    JOptionPane.INFORMATION_MESSAGE);

            // Schedule removal of test trigger
            new Timer(30000, e -> {
                try {
                    boolean removed = triggerEngine.removeTrigger(testTrigger.getId());
                    if (!removed) {
                        // Trigger might already be removed, this is not an error
                        System.out.println("[Test Trigger] Test trigger " + testTrigger.getId()
                                + " was already removed or not found");
                    }
                } catch (Exception ex) {
                    // Log but don't show error to user for test trigger cleanup
                    System.out.println("[Test Trigger] Error removing test trigger: " + ex.getMessage());
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating test trigger: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String generateTriggerId(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_") + "_" + System.currentTimeMillis();
    }
}
