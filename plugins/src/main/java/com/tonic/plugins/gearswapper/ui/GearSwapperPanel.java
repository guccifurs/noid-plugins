package com.tonic.plugins.gearswapper.ui;

import com.google.inject.Inject;
import com.tonic.plugins.gearswapper.GearSwapperConfig;
import com.tonic.plugins.gearswapper.GearSwapperPlugin;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.config.Keybind;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashSet;
import com.tonic.plugins.gearswapper.ui.triggers.TriggerPanel;
import com.tonic.plugins.gearswapper.sdn.ScriptSDNPanel;
import com.tonic.plugins.gearswapper.sdn.SDNUserInfo;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearSwapperPanel extends PluginPanel {
    private static final Logger logger = LoggerFactory.getLogger(GearSwapperPanel.class);

    private GearSwapperPlugin plugin;
    private ConfigManager configManager;

    // Presets system
    private JComboBox<String> presetSelector;
    private Map<String, PresetData> presets = new HashMap<>();

    // Trigger system
    private TriggerPanel triggerPanel;
    private JPanel presetsContentPanel;
    private JPanel loadoutsContentPanel;

    // Loadout data management
    private Map<Integer, LoadoutData> loadoutData = new HashMap<>();
    private Map<JButton, Integer> deleteButtons = new HashMap<>();
    private Map<JButton, Integer> keybindButtons = new HashMap<>();

    // Gear system
    private Map<String, GearSet> gearSets = new HashMap<>();

    // Central list of condition variables understood by the script engine
    private static final java.util.Set<String> CONDITION_VARIABLES = new java.util.LinkedHashSet<>();
    static {
        CONDITION_VARIABLES.add("frozen");
        CONDITION_VARIABLES.add("target_frozen");
        CONDITION_VARIABLES.add("target frozen");
        CONDITION_VARIABLES.add("self_frozen");
        CONDITION_VARIABLES.add("player_frozen");
        CONDITION_VARIABLES.add("me_frozen");
        CONDITION_VARIABLES.add("self frozen");
        CONDITION_VARIABLES.add("player frozen");
        CONDITION_VARIABLES.add("me frozen");
        CONDITION_VARIABLES.add("has_target");
        CONDITION_VARIABLES.add("target_exists");
        CONDITION_VARIABLES.add("has target");
        CONDITION_VARIABLES.add("has_cached_target");
        CONDITION_VARIABLES.add("cached_target");
        CONDITION_VARIABLES.add("spec");
        CONDITION_VARIABLES.add("spec_energy");
        CONDITION_VARIABLES.add("special");
        CONDITION_VARIABLES.add("special_energy");
        CONDITION_VARIABLES.add("hp");
        CONDITION_VARIABLES.add("health");
        CONDITION_VARIABLES.add("hitpoints");
        CONDITION_VARIABLES.add("prayer");
        CONDITION_VARIABLES.add("pray");
        CONDITION_VARIABLES.add("distance");
        CONDITION_VARIABLES.add("target_distance");
        CONDITION_VARIABLES.add("player_frozen_ticks");
        CONDITION_VARIABLES.add("self_frozen_ticks");
        CONDITION_VARIABLES.add("target_frozen_ticks");
        CONDITION_VARIABLES.add("ticks_since_swap");
        CONDITION_VARIABLES.add("swap_ticks");
    }

    // Keybind capture state
    private boolean isCapturingKeybind = false;
    private int capturingLoadoutNum = -1;
    private JButton capturingButton = null;

    @Inject
    public GearSwapperPanel() {
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add global key listener for keybind capture
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isCapturingKeybind) {
                    captureKeybind(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });

        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setAlignmentX(0f);

        // Make content panel focusable too
        content.setFocusable(true);
        content.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isCapturingKeybind) {
                    captureKeybind(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });

        content.add(buildDocumentationButton());
        System.out.println("[Gear Swapper DEBUG] Documentation button added");
        content.add(Box.createVerticalStrut(8));

        // Script SDN Button (replaces Heatmap and Gear Manager)
        content.add(buildScriptSDNButton());
        System.out.println("[Gear Swapper DEBUG] Script SDN button added");
        content.add(Box.createVerticalStrut(12));
        content.add(buildHeader());
        content.add(Box.createVerticalStrut(12));
        content.add(buildPresetsSection());
        content.add(Box.createVerticalStrut(12));
        content.add(buildLoadoutsSection());
        content.add(Box.createVerticalStrut(12));
        content.add(buildTriggersSection());
        content.add(Box.createVerticalStrut(12));
        content.add(buildLooperSection());

        add(content);
        add(Box.createVerticalGlue());
    }

    public void setPlugin(GearSwapperPlugin plugin) {
        System.out.println(
                "[Gear Swapper DEBUG] setPlugin called with plugin: " + (plugin != null ? "NOT NULL" : "NULL"));
        this.plugin = plugin;

        // Initialize trigger panel now that plugin is available
        if (triggerPanel == null && plugin != null) {
            System.out.println("[Gear Swapper DEBUG] Creating trigger panel...");
            triggerPanel = new TriggerPanel(plugin.getTriggerEngine());
        }

        // Always rebuild the entire panel when plugin is set to ensure heatmap appears
        SwingUtilities.invokeLater(() -> {
            System.out.println("[Gear Swapper DEBUG] About to rebuild entire panel...");
            rebuildEntirePanel();
        });

        updateFromPlugin(plugin);
    }

    /**
     * Update the triggers section with the actual trigger panel
     */
    private void updateTriggersSection() {
        // Force a UI refresh to show the actual trigger panel
        SwingUtilities.invokeLater(() -> {
            // Simple approach: rebuild the entire content panel
            rebuildEntirePanel();
        });
    }

    /**
     * Rebuild the entire panel to ensure trigger panel is visible
     */
    private void rebuildEntirePanel() {
        System.out.println("[Gear Swapper DEBUG] Rebuilding entire panel...");

        // Remove all components and rebuild
        removeAll();

        // Rebuild the entire panel structure (similar to constructor)
        rebuildPanelContent();

        // Ensure the triggers section is expanded if trigger panel exists
        if (triggerPanel != null) {
            expandTriggersSection();
        }

        revalidate();
        repaint();

        System.out.println("[Gear Swapper DEBUG] Panel rebuild completed");
    }

    /**
     * Rebuild panel content (similar to constructor logic)
     */
    private void rebuildPanelContent() {
        final JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setAlignmentX(0f);

        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setAlignmentX(0f);

        // Make content panel focusable too
        content.setFocusable(true);
        content.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isCapturingKeybind) {
                    captureKeybind(e);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });

        // Add documentation button
        content.add(buildDocumentationButton());
        System.out.println("[Gear Swapper DEBUG] Documentation button added (rebuild)");
        content.add(Box.createVerticalStrut(8));

        // Script SDN Button (replaces Heatmap and Gear Manager)
        content.add(buildScriptSDNButton());
        System.out.println("[Gear Swapper DEBUG] Script SDN button added (rebuild)");
        content.add(Box.createVerticalStrut(12));
        content.add(buildHeader());
        content.add(Box.createVerticalStrut(12));

        // Add presets section
        content.add(buildPresetsSection());
        content.add(Box.createVerticalStrut(12));

        // Add loadouts section
        content.add(buildLoadoutsSection());
        content.add(Box.createVerticalStrut(12));

        // Add triggers section - this will now include the actual trigger panel
        content.add(buildTriggersSectionWithPanel());
        content.add(Box.createVerticalStrut(12));

        // Add global looper section
        content.add(buildLooperSection());
        content.add(Box.createVerticalStrut(12));

        add(content);
        add(Box.createVerticalGlue());

        logger.info("[Gear Swapper] Panel content rebuilt with {} components", content.getComponentCount());
    }

    /**
     * Build triggers section with actual trigger panel (not placeholder)
     */
    private JPanel buildTriggersSectionWithPanel() {
        final CollapsiblePanel triggersPanel = new CollapsiblePanel("⚡ Triggers");
        triggersPanel.setExpanded(true); // Start expanded

        JPanel triggersContentPanel = new JPanel();
        triggersContentPanel.setLayout(new BoxLayout(triggersContentPanel, BoxLayout.Y_AXIS));
        triggersContentPanel.setOpaque(false);

        // Add the actual trigger panel directly
        if (triggerPanel != null) {
            triggersContentPanel.add(triggerPanel);
            logger.info("[Gear Swapper] Added actual trigger panel to triggers section");
        } else {
            // Fallback to placeholder (shouldn't happen now)
            JLabel placeholder = new JLabel("🎯 Initializing trigger system...");
            placeholder.setFont(new Font("Whitney", Font.ITALIC, 12));
            placeholder.setForeground(new Color(150, 150, 150));
            placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
            triggersContentPanel.add(placeholder);
            logger.warn("[Gear Swapper] Using placeholder - trigger panel is null");
        }

        triggersPanel.addContent(triggersContentPanel);

        return triggersPanel;
    }

    private JPanel buildLooperSection() {
        final CollapsiblePanel looperPanel = new CollapsiblePanel("🔁 Looper");
        looperPanel.setExpanded(false);

        JPanel looperContent = new JPanel();
        looperContent.setLayout(new BoxLayout(looperContent, BoxLayout.Y_AXIS));
        looperContent.setOpaque(false);
        looperContent.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Header row with title, toggle, and external editor button
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel titleLabel = new JLabel("Global Looper");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(Theme.TEXT_LINK);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.setOpaque(false);

        boolean enabled = plugin != null && plugin.isLooperEnabled();
        JToggleButton looperToggle = new JToggleButton("Looper");
        looperToggle.setSelected(enabled);
        looperToggle.setBackground(enabled ? Theme.DANGER : Theme.SUCCESS);
        looperToggle.setForeground(Color.WHITE);
        looperToggle.setFocusPainted(false);
        looperToggle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        looperToggle.setPreferredSize(new Dimension(80, 20));
        looperToggle.setMaximumSize(new Dimension(80, 20));
        looperToggle.setToolTipText("Toggle global looper on/off");
        looperToggle
                .setBorder(BorderFactory.createLineBorder(enabled ? Theme.DANGER.darker() : Theme.SUCCESS.darker()));

        looperToggle.addActionListener(e -> {
            boolean on = looperToggle.isSelected();
            looperToggle.setBackground(on ? Theme.DANGER : Theme.SUCCESS);
            looperToggle.setBorder(BorderFactory.createLineBorder(on ? Theme.DANGER.darker() : Theme.SUCCESS.darker()));
            if (plugin != null) {
                plugin.setLooperEnabled(on);
            }
        });

        JButton scriptBtn = new JButton("📝");
        scriptBtn.setBackground(Theme.PRIMARY);
        scriptBtn.setForeground(Color.WHITE);
        scriptBtn.setFocusPainted(false);
        scriptBtn.setPreferredSize(new Dimension(32, 22));
        scriptBtn.setMaximumSize(new Dimension(32, 22));
        scriptBtn.setFont(new Font("Segoe UI", Font.BOLD, 9));
        scriptBtn.setToolTipText("Open looper script editor");
        scriptBtn.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY.darker()));

        // Inline looper script area
        String initialScript = plugin != null ? plugin.getLooperScript() : "";
        GhostTextArea looperArea = new GhostTextArea(initialScript);
        looperArea.setBackground(Theme.BACKGROUND);
        looperArea.setForeground(Theme.TEXT_PRIMARY);
        looperArea.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        looperArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        looperArea.setLineWrap(true);
        looperArea.setWrapStyleWord(true);
        looperArea.setFocusTraversalKeysEnabled(false);

        scriptBtn.addActionListener(e -> openLooperEditor(looperArea));

        rightButtons.add(scriptBtn);
        rightButtons.add(looperToggle);

        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(rightButtons, BorderLayout.EAST);

        JLabel scriptLabel = new JLabel("Script (executes every tick when Looper is ON):");
        scriptLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        scriptLabel.setForeground(Theme.TEXT_SECONDARY);
        scriptLabel.setBorder(new EmptyBorder(4, 0, 2, 0));

        updateTextAreaHeight(looperArea);

        looperArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                sync();
            }

            private void sync() {
                String text = looperArea.getText();
                if (plugin != null) {
                    plugin.setLooperScript(text);
                }
                updateTextAreaHeight(looperArea);
            }
        });

        JScrollPane looperScroll = new JScrollPane(looperArea);
        looperScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        looperScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        looperScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        looperScroll.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));

        looperContent.add(titleRow);
        looperContent.add(Box.createVerticalStrut(6));
        looperContent.add(scriptLabel);
        looperContent.add(Box.createVerticalStrut(2));
        looperContent.add(looperScroll);

        looperPanel.addContent(looperContent);
        return looperPanel;
    }

    /**
     * Find and expand the triggers section
     */
    private void expandTriggersSection() {
        // Find the triggers panel and expand it
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                expandTriggersInPanel(panel);
            }
        }
    }

    private void openBlockEditor(Window owner, JTextArea editorArea) {
        if (editorArea == null) {
            return;
        }

        String scriptText = editorArea.getText();

        JDialog dialog = new JDialog(owner, "Script Blocks", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(520, 640);
        dialog.setLocationRelativeTo(owner);

        ScriptBlockEditorPanel blockPanel = new ScriptBlockEditorPanel(scriptText);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(blockPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton applyBtn = new JButton("Apply");
        JButton closeBtn = new JButton("Close");
        buttons.add(applyBtn);
        buttons.add(closeBtn);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);

        applyBtn.addActionListener(e -> {
            String newScript = blockPanel.buildScript();
            if (newScript != null) {
                editorArea.setText(newScript);
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void acceptGhostSuggestion(GhostTextArea ghostArea) {
        if (ghostArea == null || !ghostArea.hasGhostSuffix()) {
            return;
        }

        int pos = ghostArea.getCaretPosition();
        try {
            Document doc = ghostArea.getDocument();
            doc.insertString(pos, ghostArea.ghostSuffix, null);
        } catch (Exception e) {
            // Fallback: append at caret
            ghostArea.insert(ghostArea.ghostSuffix, pos);
        }

        ghostArea.setGhostSuffix("");
    }

    /**
     * Recursively search for and expand triggers section
     */
    private void expandTriggersInPanel(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof CollapsiblePanel) {
                CollapsiblePanel collapsiblePanel = (CollapsiblePanel) comp;

                // Check if this is the triggers panel by looking at its title
                // We'll need to access the title label somehow
                try {
                    // Look for a label with "Triggers" in the header
                    boolean isTriggersPanel = false;
                    for (Component child : collapsiblePanel.getComponents()) {
                        if (child instanceof JPanel) {
                            JPanel headerPanel = (JPanel) child;
                            for (Component headerChild : headerPanel.getComponents()) {
                                if (headerChild instanceof JLabel) {
                                    JLabel label = (JLabel) headerChild;
                                    if (label.getText().contains("Triggers")) {
                                        isTriggersPanel = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (isTriggersPanel)
                            break;
                    }

                    if (isTriggersPanel) {
                        collapsiblePanel.setExpanded(true);
                        logger.info("[Gear Swapper] Triggers section expanded");
                        return;
                    }
                } catch (Exception e) {
                    // Continue searching
                }
            } else if (comp instanceof JPanel) {
                expandTriggersInPanel((JPanel) comp);
            }
        }
    }

    public void updateFromPlugin(GearSwapperPlugin plugin) {
        // Update UI based on plugin state
    }

    private JPanel buildDocumentationButton() {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 5, 10));

        // Secondary style for docs
        JButton docButton = createStyledButton("📚 Open Documentation", Theme.SURFACE_HOVER, Theme.TEXT_PRIMARY);
        docButton.addActionListener(e -> openDocumentationPanel());

        panel.add(docButton);
        return panel;
    }

    private void openDocumentationPanel() {
        DocumentationPanel docPanel = new DocumentationPanel();

        // Create and show the documentation panel
        JFrame docFrame = new JFrame("Gear Swapper Documentation");
        docFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        docFrame.setSize(700, 600);
        docFrame.setLocationRelativeTo(this);
        docFrame.add(docPanel);
        docFrame.setVisible(true);
    }

    /**
     * Build Script SDN button - opens the community script sharing panel
     */
    private JPanel buildScriptSDNButton() {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(5, 10, 10, 10));

        // Primary style for SDN (Community)
        JButton sdnButton = createStyledButton("📜 Script SDN", Theme.PRIMARY, Color.WHITE);
        sdnButton.addActionListener(e -> openScriptSDNPanel());

        panel.add(sdnButton);
        return panel;
    }

    /**
     * Open the Script SDN panel in a new window
     */
    private void openScriptSDNPanel() {
        System.out.println("[Gear Swapper DEBUG] Opening Script SDN panel...");

        // Create user supplier that gets auth info via reflection (avoids NoidUser
        // class dependency)
        java.util.function.Supplier<SDNUserInfo> userSupplier = () -> {
            if (plugin == null) {
                System.out.println("[Gear Swapper SDN] plugin is null!");
                return null;
            }

            try {
                // Get PluginManager from plugin - it's a field on GearSwapperPlugin, not
                // superclass
                java.lang.reflect.Field pmField = null;
                Class<?> clazz = plugin.getClass();

                // Search through class hierarchy for pluginManager field
                while (clazz != null && pmField == null) {
                    try {
                        pmField = clazz.getDeclaredField("pluginManager");
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                }

                if (pmField == null) {
                    System.out.println("[Gear Swapper SDN] Could not find pluginManager field!");
                    return null;
                }

                pmField.setAccessible(true);
                PluginManager pm = (PluginManager) pmField.get(plugin);
                System.out
                        .println("[Gear Swapper SDN] Found PluginManager with " + pm.getPlugins().size() + " plugins");

                for (Plugin p : pm.getPlugins()) {
                    if (p.getClass().getSimpleName().equals("NoidPlugin")) {
                        System.out.println("[Gear Swapper SDN] Found NoidPlugin!");

                        java.lang.reflect.Method authMethod = p.getClass().getMethod("isAuthenticated");
                        boolean authenticated = (Boolean) authMethod.invoke(p);
                        System.out.println("[Gear Swapper SDN] isAuthenticated = " + authenticated);

                        if (authenticated) {
                            java.lang.reflect.Method userMethod = p.getClass().getMethod("getCurrentUser");
                            Object user = userMethod.invoke(p);
                            System.out.println("[Gear Swapper SDN] getCurrentUser = " + user);

                            if (user != null) {
                                // Extract values via reflection to avoid class dependency
                                java.lang.reflect.Method getSessionToken = user.getClass().getMethod("getSessionToken");
                                java.lang.reflect.Method getDiscordId = user.getClass().getMethod("getDiscordId");
                                java.lang.reflect.Method getDiscordName = user.getClass().getMethod("getDiscordName");

                                String sessionToken = (String) getSessionToken.invoke(user);
                                String discordId = (String) getDiscordId.invoke(user);
                                String discordName = (String) getDiscordName.invoke(user);

                                System.out.println(
                                        "[Gear Swapper SDN] Got user: " + discordName + " (" + discordId + ")");
                                return new SDNUserInfo(sessionToken, discordId, discordName);
                            }
                        }
                        break;
                    }
                }
                System.out.println("[Gear Swapper SDN] NoidPlugin not found in loaded plugins!");
            } catch (Exception ex) {
                System.out.println("[Gear Swapper SDN] Failed to get user info: " + ex.getMessage());
                ex.printStackTrace();
            }
            return null;
        };

        ScriptSDNPanel sdnPanel = new ScriptSDNPanel(userSupplier);

        JFrame sdnFrame = new JFrame("Script SDN - Community Scripts");
        sdnFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        sdnFrame.setSize(900, 700);
        sdnFrame.setLocationRelativeTo(this);
        sdnFrame.add(sdnPanel);
        sdnFrame.setVisible(true);

        System.out.println("[Gear Swapper DEBUG] Script SDN panel opened");
    }

    private JPanel buildClickHeatmapSection() {
        System.out.println("[GEAR SWAPPER DEBUG] Building click heatmap section...");

        try {
            final JPanel heatmapPanel = new JPanel(new BorderLayout());
            heatmapPanel.setOpaque(true);
            heatmapPanel.setBackground(new Color(45, 46, 50));
            heatmapPanel.setBorder(new EmptyBorder(10, 16, 10, 16));
            heatmapPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            // Title row
            JLabel titleLabel = new JLabel("🔥 Click Heatmap (Anti-Ban)");
            titleLabel.setFont(new Font("Whitney", Font.BOLD, 12));
            titleLabel.setForeground(new Color(255, 152, 0));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

            System.out.println("[GEAR SWAPPER DEBUG] Created heatmap title label");

            // Heatmap visualization panel
            JPanel heatmapVisPanel = new JPanel(new BorderLayout());
            heatmapVisPanel.setOpaque(false);
            heatmapVisPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

            // Create heatmap bar
            JPanel heatmapBar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();

                    // Draw gradient background
                    int width = getWidth();
                    int height = getHeight();

                    // Background gradient from dark to light
                    for (int i = 0; i < width; i++) {
                        float ratio = (float) i / width;
                        int red = (int) (139 + (255 - 139) * ratio);
                        int green = (int) (69 + (152 - 69) * ratio);
                        int blue = (int) (19 + (0 - 19) * ratio);
                        g2d.setColor(new Color(red, green, blue));
                        g2d.drawLine(i, 0, i, height);
                    }

                    // Draw slider positions
                    if (plugin != null) {
                        double minDelay = plugin.getMinClickDelay();
                        double maxDelay = plugin.getMaxClickDelay();

                        int minX = (int) ((minDelay / 600.0) * width);
                        int maxX = (int) ((maxDelay / 600.0) * width);

                        // Draw left slider
                        g2d.setColor(new Color(255, 255, 255, 200));
                        g2d.fillRect(minX - 2, 0, 4, height);
                        g2d.setColor(Color.WHITE);
                        g2d.drawRect(minX - 2, 0, 4, height);

                        // Draw right slider
                        g2d.setColor(new Color(255, 255, 255, 200));
                        g2d.fillRect(maxX - 2, 0, 4, height);
                        g2d.setColor(Color.WHITE);
                        g2d.drawRect(maxX - 2, 0, 4, height);

                        // Highlight range between sliders
                        g2d.setColor(new Color(255, 0, 0, 100));
                        g2d.fillRect(minX, 0, maxX - minX, height);
                    }

                    // Draw scale labels
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                    g2d.drawString("0ms", 2, height - 2);
                    g2d.drawString("600ms", width - 35, height - 2);

                    g2d.dispose();
                }
            };
            heatmapBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 25));
            heatmapBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            heatmapBar.setBorder(new LineBorder(new Color(100, 100, 100), 1));

            System.out.println("[GEAR SWAPPER DEBUG] Created heatmap bar");

            // Slider controls
            JPanel sliderControls = new JPanel(new BorderLayout());
            sliderControls.setOpaque(false);
            sliderControls.setBorder(new EmptyBorder(5, 0, 0, 0));

            // Min delay slider
            JSlider minSlider = new JSlider(0, 600, 100);
            minSlider.setOpaque(false);
            minSlider.setForeground(new Color(255, 152, 0));
            minSlider.setToolTipText("Minimum click delay (ms)");
            minSlider.addChangeListener(e -> {
                if (plugin != null) {
                    int min = minSlider.getValue();
                    int max = plugin.getMaxClickDelay();
                    if (min >= max) {
                        minSlider.setValue(max - 50);
                        return;
                    }
                    plugin.setMinClickDelay(min);
                    heatmapBar.repaint();
                }
            });

            // Max delay slider
            JSlider maxSlider = new JSlider(0, 600, 500);
            maxSlider.setOpaque(false);
            maxSlider.setForeground(new Color(255, 152, 0));
            maxSlider.setToolTipText("Maximum click delay (ms)");
            maxSlider.addChangeListener(e -> {
                if (plugin != null) {
                    int min = plugin.getMinClickDelay();
                    int max = maxSlider.getValue();
                    if (max <= min) {
                        maxSlider.setValue(min + 50);
                        return;
                    }
                    plugin.setMaxClickDelay(max);
                    heatmapBar.repaint();
                }
            });

            // Status label
            JLabel statusLabel = new JLabel("Range: 100-500ms");
            statusLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
            statusLabel.setForeground(new Color(200, 200, 200));
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

            // Force 1 tick threshold slider (initially hidden until Force 1 tick is
            // enabled)
            JPanel forcePanel = new JPanel(new BorderLayout());
            forcePanel.setOpaque(false);
            forcePanel.setBorder(new EmptyBorder(6, 0, 0, 0));

            JLabel forceLabel = new JLabel("Force 1 tick threshold: 500ms");
            forceLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
            forceLabel.setForeground(new Color(200, 200, 200));

            JSlider forceSlider = new JSlider(0, 600, 500);
            forceSlider.setOpaque(false);
            forceSlider.setForeground(new Color(255, 193, 7));
            forceSlider.setToolTipText("Force 1 tick threshold (ms)");
            forceSlider.addChangeListener(e -> {
                if (plugin != null) {
                    int v = forceSlider.getValue();
                    plugin.setForceTickThresholdMs(v);
                    forceLabel.setText("Force 1 tick threshold: " + v + "ms");
                }
            });

            JPanel forceSliderRow = new JPanel(new BorderLayout());
            forceSliderRow.setOpaque(false);
            forceSliderRow.add(forceLabel, BorderLayout.NORTH);
            forceSliderRow.add(forceSlider, BorderLayout.CENTER);

            forcePanel.add(forceSliderRow, BorderLayout.CENTER);
            forcePanel.setVisible(false);

            // Update sliders from plugin values
            if (plugin != null) {
                minSlider.setValue(plugin.getMinClickDelay());
                maxSlider.setValue(plugin.getMaxClickDelay());
                updateStatusLabel(statusLabel, plugin.getMinClickDelay(), plugin.getMaxClickDelay());
            }

            minSlider.addChangeListener(e -> {
                if (plugin != null) {
                    updateStatusLabel(statusLabel, plugin.getMinClickDelay(), plugin.getMaxClickDelay());
                }
            });

            maxSlider.addChangeListener(e -> {
                if (plugin != null) {
                    updateStatusLabel(statusLabel, plugin.getMinClickDelay(), plugin.getMaxClickDelay());
                }
            });

            // Layout
            JPanel minPanel = new JPanel(new BorderLayout());
            minPanel.setOpaque(false);
            minPanel.add(new JLabel("Min:"), BorderLayout.WEST);
            minPanel.add(minSlider, BorderLayout.CENTER);

            JPanel maxPanel = new JPanel(new BorderLayout());
            maxPanel.setOpaque(false);
            maxPanel.add(new JLabel("Max:"), BorderLayout.WEST);
            maxPanel.add(maxSlider, BorderLayout.CENTER);

            sliderControls.add(minPanel, BorderLayout.NORTH);
            sliderControls.add(maxPanel, BorderLayout.CENTER);
            sliderControls.add(statusLabel, BorderLayout.SOUTH);

            heatmapVisPanel.add(heatmapBar, BorderLayout.NORTH);
            heatmapVisPanel.add(sliderControls, BorderLayout.CENTER);
            heatmapVisPanel.add(forcePanel, BorderLayout.SOUTH);

            // Toggle buttons row
            JToggleButton heatmapToggle = new JToggleButton("Enable Heatmap");
            heatmapToggle.setBackground(new Color(76, 175, 80));
            heatmapToggle.setForeground(Color.WHITE);
            heatmapToggle.setFocusPainted(false);
            heatmapToggle.setFont(new Font("Whitney", Font.BOLD, 10));
            heatmapToggle.setPreferredSize(new Dimension(120, 22));
            heatmapToggle.addActionListener(e -> {
                boolean enabled = heatmapToggle.isSelected();
                heatmapToggle.setBackground(enabled ? new Color(244, 67, 54) : new Color(76, 175, 80));
                heatmapToggle.setText(enabled ? "Disable Heatmap" : "Enable Heatmap");
                if (plugin != null) {
                    plugin.setClickHeatmapEnabled(enabled);
                }
            });

            JToggleButton forceToggle = new JToggleButton("Force 1 tick");
            forceToggle.setBackground(new Color(55, 71, 79));
            forceToggle.setForeground(Color.WHITE);
            forceToggle.setFocusPainted(false);
            forceToggle.setFont(new Font("Whitney", Font.BOLD, 10));
            forceToggle.setPreferredSize(new Dimension(110, 22));
            forceToggle.addActionListener(e -> {
                boolean enabled = forceToggle.isSelected();
                forceToggle.setBackground(enabled ? new Color(255, 152, 0) : new Color(55, 71, 79));
                if (plugin != null) {
                    plugin.setForceOneTickEnabled(enabled);
                }
                forcePanel.setVisible(enabled);
            });

            // Sync toggle + slider state from plugin if available
            if (plugin != null) {
                int threshold = plugin.getForceTickThresholdMs();
                forceSlider.setValue(threshold);
                forceLabel.setText("Force 1 tick threshold: " + threshold + "ms");

                boolean forceEnabled = plugin.isForceOneTickEnabled();
                forceToggle.setSelected(forceEnabled);
                forceToggle.setBackground(forceEnabled ? new Color(255, 152, 0) : new Color(55, 71, 79));
                forcePanel.setVisible(forceEnabled);
            }

            JPanel togglesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            togglesPanel.setOpaque(false);
            togglesPanel.add(forceToggle);
            togglesPanel.add(heatmapToggle);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            topPanel.add(titleLabel, BorderLayout.CENTER);
            topPanel.add(togglesPanel, BorderLayout.EAST);

            heatmapPanel.add(topPanel, BorderLayout.NORTH);
            heatmapPanel.add(heatmapVisPanel, BorderLayout.CENTER);

            System.out.println("[GEAR SWAPPER DEBUG] Heatmap section built successfully");
            return heatmapPanel;

        } catch (Exception e) {
            System.out.println("[GEAR SWAPPER DEBUG] Error building heatmap section: " + e.getMessage());
            e.printStackTrace();

            // Return a simple fallback panel
            JPanel fallback = new JPanel();
            fallback.setBackground(Theme.DANGER);
            fallback.add(new JLabel("HEATMAP ERROR"));
            return fallback;
        }
    }

    private void updateStatusLabel(JLabel label, int min, int max) {
        label.setText(String.format("Range: %d-%dms (%.0f%% of tick)", min, max, ((max - min) / 600.0) * 100));
    }

    private JPanel buildGearButton() {
        System.out.println("[Gear Swapper DEBUG] Building gear button...");

        final JPanel gearPanel = new JPanel(new BorderLayout());
        gearPanel.setOpaque(true);
        gearPanel.setBackground(new Color(45, 46, 50));
        gearPanel.setBorder(new EmptyBorder(8, 16, 8, 16));
        gearPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        final JButton gearButton = new JButton("⚔️ Visual Gear Manager v2.1");
        gearButton.setBackground(new Color(156, 39, 176)); // Purple for gear
        gearButton.setForeground(Color.WHITE);
        gearButton.setFocusPainted(false);
        gearButton.setPreferredSize(new Dimension(Integer.MAX_VALUE, 24));
        gearButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        gearButton.setFont(new Font("Whitney", Font.BOLD, 10));
        gearButton.setHorizontalAlignment(SwingConstants.CENTER);
        gearButton.setBorder(new EmptyBorder(4, 12, 4, 12));

        System.out.println("[Gear Swapper DEBUG] Gear button created, adding action listener...");

        gearButton.addActionListener(e -> {
            System.out.println("[Gear Swapper DEBUG] Gear button clicked!");
            try {
                openGearManager();
            } catch (Exception ex) {
                System.out.println("[Gear Swapper DEBUG] Exception in gear manager: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        gearPanel.add(gearButton, BorderLayout.CENTER);

        System.out.println("[Gear Swapper DEBUG] Gear button panel built successfully");
        return gearPanel;
    }

    private void openGearManager() {
        System.out.println("[Gear Swapper DEBUG] Opening gear manager...");
        System.out.println("[Gear Swapper DEBUG] Gear sets size: " + gearSets.size());
        System.out.println("[Gear Swapper DEBUG] Config manager: " + (configManager != null ? "present" : "null"));

        try {
            // Create the full gear manager panel
            SimpleGearManagerPanel gearPanel = new SimpleGearManagerPanel(gearSets, configManager);

            // Create and show the gear manager panel
            JFrame gearFrame = new JFrame("Gear Visual Manager");
            gearFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gearFrame.setSize(900, 700);
            gearFrame.setLocationRelativeTo(this);
            gearFrame.add(gearPanel);
            gearFrame.setVisible(true);

            System.out.println("[Gear Swapper DEBUG] Gear manager frame created and visible");

        } catch (Exception ex) {
            System.out.println("[Gear Swapper DEBUG] Exception in gear manager: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void setConfig(GearSwapperConfig config, ConfigManager configManager) {
        this.configManager = configManager;
        loadSavedPresets();
        loadSavedLoadouts();

        // Refresh UI after loading data
        if (loadoutsContentPanel != null) {
            refreshLoadoutPanel();
        }
    }

    private JPanel buildHeader() {
        final JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(16, 20, 16, 20)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        final JLabel title = new JLabel("⚔️ Gear Swapper");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Theme.TEXT_PRIMARY);

        final JLabel subtitle = new JLabel("Automated gear management");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(Theme.TEXT_SECONDARY);

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

    private JPanel buildPresetsSection() {
        final CollapsiblePanel presetsPanel = new CollapsiblePanel("💾 Presets");
        presetsPanel.setExpanded(true);

        presetsContentPanel = new JPanel();
        presetsContentPanel.setLayout(new BoxLayout(presetsContentPanel, BoxLayout.Y_AXIS));
        presetsContentPanel.setOpaque(false);
        presetsContentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Preset selector
        presetSelector = new JComboBox<>();
        presetSelector.setBackground(Theme.BACKGROUND);
        presetSelector.setForeground(Theme.TEXT_PRIMARY);
        presetSelector.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        presetSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        // Add renderer for better styling if possible, but basic color is fine for now
        updatePresetSelector();

        // Action buttons row - Grid for even spacing
        JPanel buttonRow = new JPanel(new GridLayout(1, 3, 5, 0));
        buttonRow.setOpaque(false);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        buttonRow.setBorder(new EmptyBorder(8, 0, 8, 0));

        JButton saveBtn = createStyledButton("Save", Theme.SUCCESS, Color.WHITE);
        saveBtn.addActionListener(e -> saveCurrentToPreset());

        JButton loadBtn = createStyledButton("Load", Theme.PRIMARY, Color.WHITE);
        loadBtn.addActionListener(e -> loadSelectedPreset());

        JButton deleteBtn = createStyledButton("Delete", Theme.DANGER, Color.WHITE);
        deleteBtn.addActionListener(e -> deleteSelectedPreset());

        buttonRow.add(saveBtn);
        buttonRow.add(loadBtn);
        buttonRow.add(deleteBtn);

        // Create new preset
        JPanel createPanel = new JPanel(new BorderLayout(5, 0));
        createPanel.setOpaque(false);
        createPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JTextField newPresetField = createStyledTextField();
        newPresetField.setText("New preset name...");
        // Add placeholder behavior
        newPresetField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (newPresetField.getText().equals("New preset name...")) {
                    newPresetField.setText("");
                    newPresetField.setForeground(Theme.TEXT_PRIMARY);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (newPresetField.getText().isEmpty()) {
                    newPresetField.setText("New preset name...");
                    newPresetField.setForeground(Theme.TEXT_MUTED);
                }
            }
        });

        JButton createBtn = createStyledButton("Create", Theme.PURPLE, Color.WHITE);
        createBtn.setPreferredSize(new Dimension(80, 30));
        createBtn.addActionListener(e -> createNewPreset(newPresetField.getText()));

        createPanel.add(newPresetField, BorderLayout.CENTER);
        createPanel.add(createBtn, BorderLayout.EAST);

        presetsContentPanel.add(presetSelector);
        presetsContentPanel.add(buttonRow);
        presetsContentPanel.add(createPanel);

        presetsPanel.setContent(presetsContentPanel);
        return presetsPanel;
    }

    private JPanel buildLoadoutsSection() {
        final CollapsiblePanel loadoutsPanel = new CollapsiblePanel("🎯 Loadouts");
        loadoutsPanel.setExpanded(true);

        loadoutsContentPanel = new JPanel();
        loadoutsContentPanel.setLayout(new BoxLayout(loadoutsContentPanel, BoxLayout.Y_AXIS));
        loadoutsContentPanel.setOpaque(false);

        refreshLoadoutPanel();

        loadoutsPanel.addContent(loadoutsContentPanel);
        return loadoutsPanel;
    }

    private JPanel buildTriggersSection() {
        final CollapsiblePanel triggersPanel = new CollapsiblePanel("⚡ Triggers");
        triggersPanel.setExpanded(false);

        JPanel triggersContentPanel = new JPanel();
        triggersContentPanel.setLayout(new BoxLayout(triggersContentPanel, BoxLayout.Y_AXIS));
        triggersContentPanel.setOpaque(false);

        // Add placeholder initially - trigger panel will be added when plugin is set
        JLabel placeholder = new JLabel("🎯 Initializing trigger system...");
        placeholder.setFont(new Font("Whitney", Font.ITALIC, 12));
        placeholder.setForeground(new Color(150, 150, 150));
        placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
        triggersContentPanel.add(placeholder);

        triggersPanel.addContent(triggersContentPanel);

        return triggersPanel;
    }

    public void refreshLoadoutPanel() {
        if (loadoutsContentPanel == null)
            return;

        loadoutsContentPanel.removeAll();
        deleteButtons.clear();
        keybindButtons.clear();

        // If no loadouts exist, don't create default ones - let users add them manually
        // This removes the unlocked loadouts limitation

        // Add existing loadouts based on actual loadoutData
        // Sort by loadout number to maintain order
        loadoutData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    JPanel card = createLoadoutCard(entry.getKey());
                    loadoutsContentPanel.add(card);
                    loadoutsContentPanel.add(Box.createVerticalStrut(8));
                });

        // Add loadout button
        JButton addBtn = new JButton("➕ Add New Loadout");
        addBtn.setBackground(new Color(76, 175, 80));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        addBtn.setFont(new Font("Whitney", Font.BOLD, 11));
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
        addBtn.addActionListener(e -> addNewLoadout());

        loadoutsContentPanel.add(addBtn);
        loadoutsContentPanel.revalidate();
        loadoutsContentPanel.repaint();
    }

    private JPanel createLoadoutCard(int loadoutNum) {
        final JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(10, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Get or create loadout data
        LoadoutData data = loadoutData.computeIfAbsent(loadoutNum, num -> new LoadoutData(loadoutNum));

        // Commands text area (shared between inline view and external editor)
        JTextArea commandsArea = new JTextArea(data.items);
        commandsArea.setBackground(Theme.BACKGROUND);
        commandsArea.setForeground(Theme.TEXT_PRIMARY); // Clean text color
        commandsArea.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        commandsArea.setFont(new Font("Consolas", Font.PLAIN, 11)); // Monospace for code/scripts
        commandsArea.setLineWrap(true);
        commandsArea.setWrapStyleWord(true);

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel titleLabel = new JLabel("Loadout " + loadoutNum);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(Theme.TEXT_LINK); // Use Link/Primary color for title
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton deleteBtn = new JButton("🗑️");
        deleteBtn.setBackground(Theme.DANGER);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setPreferredSize(new Dimension(28, 22));
        deleteBtn.setMaximumSize(new Dimension(28, 22));
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        deleteBtn.setToolTipText("Delete this loadout");
        deleteBtn.setBorder(BorderFactory.createLineBorder(Theme.DANGER.darker()));
        deleteBtn.addActionListener(e -> deleteLoadout(loadoutNum));

        JButton copyBtn = new JButton("📋");
        copyBtn.setBackground(Theme.SUCCESS);
        copyBtn.setForeground(Color.WHITE);
        copyBtn.setFocusPainted(false);
        copyBtn.setPreferredSize(new Dimension(28, 22));
        copyBtn.setMaximumSize(new Dimension(28, 22));
        copyBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        copyBtn.setToolTipText("Copy current gear to this loadout");
        copyBtn.setBorder(BorderFactory.createLineBorder(Theme.SUCCESS.darker()));
        copyBtn.addActionListener(e -> copyCurrentGearToLoadout(loadoutNum));

        // Track delete button
        deleteButtons.put(deleteBtn, loadoutNum);

        // Script editor button
        JButton scriptBtn = new JButton("📝");
        scriptBtn.setBackground(Theme.PRIMARY);
        scriptBtn.setForeground(Color.WHITE);
        scriptBtn.setFocusPainted(false);
        scriptBtn.setPreferredSize(new Dimension(32, 22));
        scriptBtn.setMaximumSize(new Dimension(32, 22));
        scriptBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        scriptBtn.setToolTipText("Open script editor for this loadout");
        scriptBtn.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY.darker()));
        scriptBtn.addActionListener(e -> openScriptEditor(loadoutNum, data, commandsArea));

        // Create button panel for right side
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(scriptBtn);
        rightButtons.add(copyBtn);
        rightButtons.add(deleteBtn);

        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(rightButtons, BorderLayout.EAST);

        // ... (rest of the method needs to be checked if I cut it off)
        // Name field
        JTextField nameField = createStyledTextField();
        nameField.setText(data.name);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        // Reset border since createStyledTextField sets padding not ideal for this
        // context?
        // Actually it's fine.

        // Save name changes
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                data.name = nameField.getText();
                saveLoadoutToConfig(loadoutNum);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                data.name = nameField.getText();
                saveLoadoutToConfig(loadoutNum);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                data.name = nameField.getText();
                saveLoadoutToConfig(loadoutNum);
            }
        });

        // Execute commands area
        JLabel executeLabel = new JLabel("Commands:");
        executeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        executeLabel.setForeground(Theme.TEXT_SECONDARY);
        executeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        executeLabel.setBorder(new EmptyBorder(4, 0, 2, 0));

        // Calculate initial height based on content
        updateTextAreaHeight(commandsArea);

        // Save command changes
        commandsArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                data.items = commandsArea.getText();
                saveLoadoutToConfig(loadoutNum);
                updateTextAreaHeight(commandsArea);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                data.items = commandsArea.getText();
                saveLoadoutToConfig(loadoutNum);
                updateTextAreaHeight(commandsArea);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                data.items = commandsArea.getText();
                saveLoadoutToConfig(loadoutNum);
                updateTextAreaHeight(commandsArea);
            }
        });

        JScrollPane commandsScroll = new JScrollPane(commandsArea);
        commandsScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        commandsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        commandsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        commandsScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        // Bottom row - Attack toggle and Keybind
        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        optionsRow.setBorder(new EmptyBorder(4, 0, 0, 0));

        // Left side - Attack toggle
        JToggleButton attackToggle = new JToggleButton("⚔️ Attack");
        attackToggle.setBackground(Theme.SUCCESS);
        attackToggle.setForeground(Color.WHITE);
        attackToggle.setFocusPainted(false);
        attackToggle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        attackToggle.setPreferredSize(new Dimension(70, 20));
        attackToggle.setMaximumSize(new Dimension(70, 20));
        attackToggle.setToolTipText("Toggle attack on/off for this loadout");
        attackToggle.setBorder(BorderFactory.createLineBorder(Theme.SUCCESS.darker()));

        // Set initial state based on data.attack
        attackToggle.setSelected(data.getAttack());
        if (data.getAttack()) {
            attackToggle.setBackground(Theme.DANGER);
            attackToggle.setBorder(BorderFactory.createLineBorder(Theme.DANGER.darker()));
        }

        // Handle attack toggle changes
        attackToggle.addActionListener(e -> {
            boolean isAttack = attackToggle.isSelected();
            data.setAttack(isAttack);
            attackToggle.setBackground(isAttack ? Theme.DANGER : Theme.SUCCESS);
            attackToggle.setBorder(
                    BorderFactory.createLineBorder(isAttack ? Theme.DANGER.darker() : Theme.SUCCESS.darker()));
            saveLoadoutToConfig(loadoutNum);
        });

        // Right side - Keybind
        JButton keybindBtn = new JButton("🔗 " + (data.keybind != null ? data.keybind.toString() : "Set Keybind"));
        keybindBtn.setBackground(Theme.SURFACE_HOVER); // Neutral/Secondary
        keybindBtn.setForeground(Theme.TEXT_PRIMARY);
        keybindBtn.setFocusPainted(false);
        keybindBtn.setPreferredSize(new Dimension(110, 22));
        keybindBtn.setMaximumSize(new Dimension(110, 22));
        keybindBtn.setFont(new Font("Segoe UI", Font.BOLD, 9));
        keybindBtn.setToolTipText("Set keybind for this loadout");
        keybindBtn.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        keybindBtn.addActionListener(e -> setKeybind(loadoutNum, keybindBtn));

        // Track keybind button
        keybindButtons.put(keybindBtn, loadoutNum);

        // Add both buttons to options row
        optionsRow.add(attackToggle, BorderLayout.WEST);
        optionsRow.add(keybindBtn, BorderLayout.EAST);

        // Assemble card with tighter spacing
        card.add(titleRow);
        card.add(Box.createVerticalStrut(6));
        card.add(nameField);
        card.add(Box.createVerticalStrut(6));
        card.add(executeLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(commandsScroll);
        card.add(Box.createVerticalStrut(6));
        card.add(optionsRow);

        return card;
    }

    private void openScriptEditor(int loadoutNum, LoadoutData data, JTextArea inlineCommandsArea) {
        if (data == null) {
            return;
        }

        JFrame frame = new JFrame("Loadout " + loadoutNum + " Script Editor");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Plain text editor with ghost autocomplete
        GhostTextArea editorArea = new GhostTextArea(data.items != null ? data.items : "");
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setFocusTraversalKeysEnabled(false);
        attachAutocomplete(editorArea);

        JScrollPane scroll = new JScrollPane(editorArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel header = new JLabel("Editing commands for " + data.name + " (Loadout " + loadoutNum + ")");
        header.setFont(new Font("Whitney", Font.BOLD, 13));

        // Toolbar with helpers
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbar.setOpaque(false);

        JLabel helperLabel = new JLabel("Select a command or condition to see examples.");
        helperLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
        helperLabel.setForeground(new Color(160, 170, 185));

        // Dropdown: Commands
        JComboBox<String> commandsDropdown = new JComboBox<>(getAvailableCommandsList());
        commandsDropdown.setFont(new Font("Whitney", Font.PLAIN, 10));
        commandsDropdown.setFocusable(false);
        commandsDropdown.addActionListener(e -> {
            String sel = (String) commandsDropdown.getSelectedItem();
            if (sel != null && !sel.startsWith("--")) {
                insertSnippetAtCaret(editorArea, sel + System.lineSeparator());
                helperLabel.setText(getCommandHelpText(sel));
            }
        });

        // Dropdown: Conditions
        JComboBox<String> conditionsDropdown = new JComboBox<>(getAvailableConditionsList());
        conditionsDropdown.setFont(new Font("Whitney", Font.PLAIN, 10));
        conditionsDropdown.setFocusable(false);
        conditionsDropdown.addActionListener(e -> {
            String sel = (String) conditionsDropdown.getSelectedItem();
            if (sel != null && !sel.startsWith("--")) {
                String condition = sel.trim();
                String snippet = "if " + condition + " {" + System.lineSeparator()
                        + "    // commands here" + System.lineSeparator()
                        + "}" + System.lineSeparator();
                insertSnippetAtCaret(editorArea, snippet);
                helperLabel.setText(getConditionHelpText(condition));
            }
        });

        toolbar.add(commandsDropdown);
        toolbar.add(conditionsDropdown);

        JButton insertIfFrozenBtn = new JButton("if frozen {}");
        insertIfFrozenBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        insertIfFrozenBtn.addActionListener(e -> insertSnippetAtCaret(editorArea,
                "if frozen { Armadyl godsword }" + System.lineSeparator()));

        JButton insertIfElseBtn = new JButton("if / else");
        insertIfElseBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        insertIfElseBtn.addActionListener(e -> insertSnippetAtCaret(editorArea,
                "if condition {\n    // commands here\n} else {\n    // alternative commands\n}"
                        + System.lineSeparator()));

        JButton validateBtn = new JButton("Validate");
        validateBtn.setFont(new Font("Whitney", Font.BOLD, 10));
        validateBtn.setBackground(new Color(76, 175, 80));
        validateBtn.setForeground(Color.WHITE);
        validateBtn.setFocusPainted(false);
        validateBtn.addActionListener(e -> validateScript("Loadout " + loadoutNum, editorArea));

        JButton suggestBtn = new JButton("Suggest");
        suggestBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        suggestBtn.setFocusPainted(false);
        suggestBtn.addActionListener(e -> showAutocompleteFromButton(editorArea));

        JButton rawTextBtn = new JButton("Raw Text");
        rawTextBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        rawTextBtn.setFocusPainted(false);
        rawTextBtn.addActionListener(e -> openRawTextEditor(frame, editorArea, data, loadoutNum, inlineCommandsArea));

        toolbar.add(commandsDropdown);
        toolbar.add(conditionsDropdown);
        toolbar.add(insertIfFrozenBtn);
        toolbar.add(insertIfElseBtn);
        toolbar.add(validateBtn);
        toolbar.add(suggestBtn);
        toolbar.add(rawTextBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        helperLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(header);
        north.add(Box.createVerticalStrut(6));
        north.add(toolbar);
        north.add(Box.createVerticalStrut(4));
        north.add(helperLabel);

        panel.add(north, BorderLayout.NORTH);

        JLabel hint = new JLabel(
                "Use multiple lines and conditions (if, &&, ||, >, <, etc.) just like in the inline box.");
        hint.setFont(new Font("Whitney", Font.PLAIN, 10));
        hint.setForeground(new Color(160, 170, 185));
        panel.add(hint, BorderLayout.SOUTH);

        // Keep data + inline area in sync while editing (no background timer)
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            private void sync() {
                String script = editorArea.getText();
                data.items = script;
                if (inlineCommandsArea != null) {
                    inlineCommandsArea.setText(script);
                    updateTextAreaHeight(inlineCommandsArea);
                }
                saveLoadoutToConfig(loadoutNum);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                sync();
            }
        });

        frame.add(panel);
        frame.setVisible(true);
    }

    private void insertSnippetAtCaret(JTextArea area, String snippet) {
        if (area == null || snippet == null) {
            return;
        }

        int pos = area.getCaretPosition();
        try {
            area.getDocument().insertString(pos, snippet, null);
        } catch (Exception e) {
            // Fallback: append at end
            area.append(snippet);
        }
    }

    private void validateScript(String scriptLabel, JTextArea editorArea) {
        if (editorArea == null) {
            return;
        }

        String text = editorArea.getText();
        if (text == null) {
            text = "";
        }

        String[] lines = text.split("\n", -1);
        java.util.List<String> messages = new java.util.ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lower = trimmed.toLowerCase();

            if (lower.startsWith("if ")) {
                // Multi-line aware validation will advance index to end of block
                i = validateIfBlock(lines, i, messages);
            } else if (lower.startsWith("else")) {
                int lineNo = i + 1;
                messages.add("Line " + lineNo + ": ERROR - 'else' must be part of an if { ... } else { ... } block.");
            } else {
                int lineNo = i + 1;
                validateCommandLine(trimmed, lineNo, messages);
            }
        }

        if (messages.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No issues found in " + scriptLabel + " script.",
                    "Validation OK",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder sb = new StringBuilder();
            for (String msg : messages) {
                sb.append(msg).append("\n");
            }
            JOptionPane.showMessageDialog(this,
                    sb.toString(),
                    "Validation results for " + scriptLabel,
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openLooperEditor(JTextArea inlineArea) {
        if (plugin == null) {
            return;
        }

        String currentScript = plugin.getLooperScript();

        JFrame frame = new JFrame("Looper Script Editor");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        // Plain text editor with ghost autocomplete
        GhostTextArea editorArea = new GhostTextArea(currentScript != null ? currentScript : "");
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setFocusTraversalKeysEnabled(false);
        attachAutocomplete(editorArea);

        JScrollPane scroll = new JScrollPane(editorArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel header = new JLabel("Editing global looper script");
        header.setFont(new Font("Whitney", Font.BOLD, 13));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbar.setOpaque(false);

        JLabel helperLabel = new JLabel("Select a command or condition to see examples.");
        helperLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
        helperLabel.setForeground(new Color(160, 170, 185));

        // Dropdown: Commands
        JComboBox<String> commandsDropdown = new JComboBox<>(getAvailableCommandsList());
        commandsDropdown.setFont(new Font("Whitney", Font.PLAIN, 10));
        commandsDropdown.setFocusable(false);
        commandsDropdown.addActionListener(e -> {
            String sel = (String) commandsDropdown.getSelectedItem();
            if (sel != null && !sel.startsWith("--")) {
                insertSnippetAtCaret(editorArea, sel + System.lineSeparator());
                helperLabel.setText(getCommandHelpText(sel));
            }
        });

        // Dropdown: Conditions
        JComboBox<String> conditionsDropdown = new JComboBox<>(getAvailableConditionsList());
        conditionsDropdown.setFont(new Font("Whitney", Font.PLAIN, 10));
        conditionsDropdown.setFocusable(false);
        conditionsDropdown.addActionListener(e -> {
            String sel = (String) conditionsDropdown.getSelectedItem();
            if (sel != null && !sel.startsWith("--")) {
                String condition = sel.trim();
                String snippet = "if " + condition + " {" + System.lineSeparator()
                        + "    // commands here" + System.lineSeparator()
                        + "}" + System.lineSeparator();
                insertSnippetAtCaret(editorArea, snippet);
                helperLabel.setText(getConditionHelpText(condition));
            }
        });

        toolbar.add(commandsDropdown);
        toolbar.add(conditionsDropdown);

        JButton insertIfFrozenBtn = new JButton("if frozen {}");
        insertIfFrozenBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        insertIfFrozenBtn.addActionListener(e -> insertSnippetAtCaret(editorArea,
                "if frozen { Armadyl godsword }" + System.lineSeparator()));

        JButton insertIfElseBtn = new JButton("if / else");
        insertIfElseBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        insertIfElseBtn.addActionListener(e -> insertSnippetAtCaret(editorArea,
                "if condition {\n    // commands here\n} else {\n    // alternative commands\n}"
                        + System.lineSeparator()));

        JButton validateBtn = new JButton("Validate");
        validateBtn.setFont(new Font("Whitney", Font.BOLD, 10));
        validateBtn.setBackground(new Color(76, 175, 80));
        validateBtn.setForeground(Color.WHITE);
        validateBtn.setFocusPainted(false);
        validateBtn.addActionListener(e -> validateScript("Looper", editorArea));

        JButton suggestBtn = new JButton("Suggest");
        suggestBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        suggestBtn.setFocusPainted(false);
        suggestBtn.addActionListener(e -> showAutocompleteFromButton(editorArea));

        JButton rawTextBtn = new JButton("Raw Text");
        rawTextBtn.setFont(new Font("Whitney", Font.PLAIN, 10));
        rawTextBtn.setFocusPainted(false);
        rawTextBtn.addActionListener(e -> openLooperRawTextEditor(frame, editorArea, inlineArea));

        toolbar.add(insertIfFrozenBtn);
        toolbar.add(insertIfElseBtn);
        toolbar.add(validateBtn);
        toolbar.add(suggestBtn);
        toolbar.add(rawTextBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        helperLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(header);
        north.add(Box.createVerticalStrut(6));
        north.add(toolbar);
        north.add(Box.createVerticalStrut(4));
        north.add(helperLabel);

        panel.add(north, BorderLayout.NORTH);

        JLabel hint = new JLabel(
                "Use multiple lines and conditions (if, &&, ||, >, <, etc.) just like in the inline box.");
        hint.setFont(new Font("Whitney", Font.PLAIN, 10));
        hint.setForeground(new Color(160, 170, 185));
        panel.add(hint, BorderLayout.SOUTH);

        // Keep looper script in sync while editing (no background timer)
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            private void sync() {
                String script = editorArea.getText();
                if (plugin != null) {
                    plugin.setLooperScript(script);
                }
                if (inlineArea != null) {
                    inlineArea.setText(script);
                    updateTextAreaHeight(inlineArea);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sync();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                sync();
            }
        });

        frame.add(panel);
        frame.setVisible(true);
    }

    /**
     * Validate an if-block that may span multiple lines, including a possible
     * else-block.
     * Returns the index of the last line that belongs to this block.
     */
    private int validateIfBlock(String[] lines, int startIndex, java.util.List<String> messages) {
        if (lines == null || startIndex < 0 || startIndex >= lines.length) {
            return startIndex;
        }

        String header = lines[startIndex] != null ? lines[startIndex] : "";
        String trimmedHeader = header.trim();
        String lowerHeader = trimmedHeader.toLowerCase();
        int headerLineNo = startIndex + 1;

        if (!lowerHeader.startsWith("if ")) {
            return startIndex;
        }

        // Extract condition from header line (between 'if' and '{' if present)
        int bracePosInHeader = trimmedHeader.indexOf('{');
        String conditionPart;
        if (bracePosInHeader != -1) {
            conditionPart = trimmedHeader.substring(2, bracePosInHeader).trim();
        } else {
            conditionPart = trimmedHeader.length() > 2 ? trimmedHeader.substring(2).trim() : "";
        }

        if (conditionPart.isEmpty()) {
            messages.add("Line " + headerLineNo + ": ERROR - empty condition in if-statement");
        } else {
            validateConditionSyntax(conditionPart, headerLineNo, messages);
        }

        // Find '{' for the THEN block, starting from the header line
        int openLine = -1;
        for (int i = startIndex; i < lines.length; i++) {
            String s = lines[i];
            if (s == null) {
                continue;
            }
            int idx = s.indexOf('{');
            if (idx != -1) {
                openLine = i;
                break;
            }
        }

        if (openLine == -1) {
            messages.add("Line " + headerLineNo + ": ERROR - missing '{' in if-statement");
            return startIndex;
        }

        // Find matching '}' for the THEN block
        int closeLine = -1;
        for (int i = openLine; i < lines.length; i++) {
            String s = lines[i];
            if (s == null) {
                continue;
            }
            int idx = s.indexOf('}');
            if (idx != -1) {
                closeLine = i;
                break;
            }
        }

        if (closeLine == -1) {
            messages.add("Line " + headerLineNo + ": ERROR - missing '}' in if-statement");
            return startIndex;
        }

        // For now we don't deeply validate the inner commands or else-block structure
        // here;
        // we just ensure braces exist somewhere after the if. Returning closeLine
        // causes
        // the outer validator loop to skip to the end of this block.
        return closeLine;
    }

    private void validateCommandLine(String trimmed, int lineNo, java.util.List<String> messages) {
        String lower = trimmed.toLowerCase();

        // Warn about unsupported words 'and'/'or' which should be &&/||
        if (lower.contains(" and ")) {
            messages.add("Line " + lineNo + ": WARNING - use '&&' instead of 'and' in conditions.");
        }
        if (lower.contains(" or ")) {
            messages.add("Line " + lineNo + ": WARNING - use '||' instead of 'or' in conditions.");
        }

        // Special handling for Tick:N commands used by the runtime scheduler
        if (lower.startsWith("tick")) {
            // Accept forms like "Tick:1" or "tick: 2"; enforce presence of ':' and numeric
            // value
            String rest = trimmed.substring(4).trim(); // after "Tick"
            if (!rest.startsWith(":")) {
                messages.add("Line " + lineNo + ": ERROR - Tick command must be in the form 'Tick:N'.");
                return;
            }

            String valuePart = rest.substring(1).trim();
            if (valuePart.isEmpty()) {
                messages.add("Line " + lineNo + ": ERROR - Tick command missing value, expected 'Tick:1'.");
                return;
            }

            try {
                int ticks = Integer.parseInt(valuePart);
                if (ticks <= 0 || ticks > 50) {
                    messages.add("Line " + lineNo + ": WARNING - Tick value should be between 1 and 50 (got " + ticks
                            + ").");
                }
            } catch (NumberFormatException e) {
                messages.add("Line " + lineNo + ": ERROR - Invalid Tick value '" + valuePart
                        + "', expected a number like 'Tick:1'.");
            }

            // Handled as a known command, no further prefix validation
            return;
        }

        // Simple no-argument commands understood by the runtime scheduler
        if (lower.equals("meleerange") || lower.equals("special") || lower.equals("attack")) {
            return; // known simple commands
        }

        // Commands with a prefix, e.g. "Item: Dragon claws", "Cast: Ice Barrage", etc.
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            if (parts.length >= 2) {
                String cmd = parts[0].trim().toLowerCase();
                if (!("item".equals(cmd) || "cast".equals(cmd) || "prayer".equals(cmd) ||
                        "special".equals(cmd) || "move".equals(cmd) || "movediag".equals(cmd) ||
                        "log".equals(cmd) || "tick".equals(cmd) || "togglepray".equals(cmd))) {
                    messages.add("Line " + lineNo + ": ERROR - unknown command prefix '" + parts[0].trim() + "'.");
                    return;
                }
            }

            // Known prefix form; further argument validation is handled at runtime
            return;
        }

        // Any other non-empty line that reaches this point is not a recognised command
        messages.add("Line " + lineNo + ": ERROR - unknown command '" + trimmed + "'.");
    }

    private void validateConditionSyntax(String condition, int lineNo, java.util.List<String> messages) {
        String lower = condition.toLowerCase();

        // Replace operators/numbers with spaces to extract variable-like tokens
        String cleaned = lower.replaceAll("[0-9<>!=&|\\s]+", " ");
        String[] tokens = cleaned.split(" ");
        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }

            if (!CONDITION_VARIABLES.contains(t)) {
                messages.add("Line " + lineNo + ": WARNING - unknown condition variable '" + t + "'.");
            }
        }
    }

    private void attachAutocomplete(JTextArea editorArea) {
        if (editorArea == null) {
            return;
        }

        java.util.List<String> baseSuggestions = buildAutocompleteBaseList();
        JPopupMenu popup = new JPopupMenu();
        JList<String> list = new JList<>();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(260, 160));
        popup.add(scroll);

        // Store on the text area so toolbar buttons can reuse the same popup
        editorArea.putClientProperty("gs-autocomplete-base", baseSuggestions);
        editorArea.putClientProperty("gs-autocomplete-popup", popup);
        editorArea.putClientProperty("gs-autocomplete-list", list);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String selected = (String) list.getSelectedValue();
                    if (selected != null) {
                        applyAutocompleteSelection(editorArea, selected);
                        popup.setVisible(false);
                    }
                }
            }
        });

        editorArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean ctrlDown = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;

                // Handle Tab as an in-editor key (no focus jumping)
                if (e.getKeyCode() == KeyEvent.VK_TAB && editorArea instanceof GhostTextArea) {
                    GhostTextArea ghostArea = (GhostTextArea) editorArea;
                    if (ghostArea.hasGhostSuffix()) {
                        // Accept the current ghost suggestion
                        acceptGhostSuggestion(ghostArea);
                    } else {
                        // No ghost: insert spaces instead of changing focus
                        try {
                            int pos = ghostArea.getCaretPosition();
                            Document doc = ghostArea.getDocument();
                            doc.insertString(pos, "    ", null);
                        } catch (Exception ex) {
                            ghostArea.replaceSelection("    ");
                        }
                    }
                    e.consume();
                    return;
                }

                if (ctrlDown && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    showAutocompletePopup(editorArea, baseSuggestions, popup, list);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && popup.isVisible()) {
                    popup.setVisible(false);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Update inline ghost suggestion after each key stroke
                updateGhostSuggestion(editorArea, baseSuggestions);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void showAutocompleteFromButton(JTextArea editorArea) {
        if (editorArea == null) {
            return;
        }

        java.util.List<String> baseSuggestions = (java.util.List<String>) editorArea
                .getClientProperty("gs-autocomplete-base");
        JPopupMenu popup = (JPopupMenu) editorArea.getClientProperty("gs-autocomplete-popup");
        JList<String> list = (JList<String>) editorArea.getClientProperty("gs-autocomplete-list");

        if (baseSuggestions == null || popup == null || list == null) {
            // Autocomplete not attached yet for some reason; attach now
            attachAutocomplete(editorArea);
            baseSuggestions = (java.util.List<String>) editorArea.getClientProperty("gs-autocomplete-base");
            popup = (JPopupMenu) editorArea.getClientProperty("gs-autocomplete-popup");
            list = (JList<String>) editorArea.getClientProperty("gs-autocomplete-list");
            if (baseSuggestions == null || popup == null || list == null) {
                return;
            }
        }

        showAutocompletePopup(editorArea, baseSuggestions, popup, list);
    }

    private void showAutocompletePopup(JTextArea editorArea,
            java.util.List<String> baseSuggestions,
            JPopupMenu popup,
            JList<String> list) {
        String text = editorArea.getText();
        int pos = editorArea.getCaretPosition();
        if (pos > text.length()) {
            pos = text.length();
        }

        int start = pos;
        while (start > 0) {
            char ch = text.charAt(start - 1);
            if (Character.isWhitespace(ch) || ch == '{' || ch == '}' || ch == ',' || ch == ':') {
                break;
            }
            start--;
        }

        String prefix = text.substring(start, pos);
        String trimmedPrefix = prefix.trim();
        if (trimmedPrefix.isEmpty()) {
            return;
        }

        String lowerPrefix = trimmedPrefix.toLowerCase();
        java.util.List<String> matches = new java.util.ArrayList<>();
        for (String s : baseSuggestions) {
            if (s.toLowerCase().startsWith(lowerPrefix)) {
                matches.add(s);
            }
        }

        if (matches.isEmpty()) {
            return;
        }

        list.setListData(matches.toArray(new String[0]));
        list.setSelectedIndex(0);

        try {
            java.awt.Rectangle r = editorArea.modelToView(pos);
            popup.show(editorArea, r.x, r.y + r.height);
        } catch (BadLocationException e) {
            popup.show(editorArea, 0, editorArea.getHeight() / 2);
        }
    }

    private java.util.List<String> buildAutocompleteBaseList() {
        java.util.List<String> list = new java.util.ArrayList<>();

        // 1) Commands from the dropdown helper so everything stays in sync
        String[] commands = getAvailableCommandsList();
        if (commands != null) {
            for (String cmd : commands) {
                if (cmd != null && !cmd.startsWith("--")) {
                    list.add(cmd);
                }
            }
        }

        // 2) Conditions and common patterns derived from CONDITION_VARIABLES
        for (String var : CONDITION_VARIABLES) {
            list.add(var);
        }

        // Helpful ready-made patterns that map to those variables
        list.add("spec>=50");
        list.add("hp<40");
        list.add("distance<=2");
        list.add("ticks_since_swap>=6");

        // 3) Common spells
        list.add("Ice Barrage");
        list.add("Ice Blitz");
        list.add("Ice Burst");
        list.add("Ice Rush");
        list.add("Blood Barrage");
        list.add("Blood Blitz");

        // 4) Popular items (subset)
        list.add("Armadyl godsword");
        list.add("Bandos godsword");
        list.add("Dragon claws");
        list.add("Abyssal whip");
        list.add("Dragon scimitar");
        list.add("Toxic blowpipe");

        return list;
    }

    private String[] getAvailableCommandsList() {
        return new String[] {
                "-- Commands --",
                "Item: Dragon claws",
                "Item: Toxic blowpipe",
                "Cast: Ice Barrage",
                "Cast: Ice Blitz",
                "Prayer: Protect from Magic",
                "Prayer: Piety",
                "Special",
                "Attack",
                "Move:1",
                "MoveDiag:1",
                "Log: Entering combo...",
                "Tick:1",
                "MeleeRange",
                "TogglePray: Augury"
        };
    }

    private String[] getAvailableConditionsList() {
        return new String[] {
                "-- Conditions --",
                "frozen",
                "target_frozen",
                "self_frozen",
                "player_frozen",
                "me_frozen",
                "has_target",
                "target_exists",
                "has_cached_target",
                "spec>=50",
                "hp<40",
                "distance<=2",
                "ticks_since_swap>=6",
                "spec",
                "spec_energy",
                "special_energy",
                "hp",
                "health",
                "hitpoints",
                "prayer",
                "distance",
                "target_distance",
                "player_frozen_ticks",
                "self_frozen_ticks",
                "target_frozen_ticks",
                "swap_ticks"
        };
    }

    private String getCommandHelpText(String command) {
        if (command == null) {
            return "";
        }

        String trimmed = command.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("item:")) {
            return "Item: <name>  → equips the given item. Example: 'Item: Dragon claws'.";
        }
        if (lower.startsWith("cast:")) {
            return "Cast: <spell>  → casts a magic spell. Example: 'Cast: Ice Barrage'.";
        }
        if (lower.startsWith("prayer:")) {
            return "Prayer: <name>  → turns on the named prayer. Example: 'Prayer: Piety'.";
        }
        if (lower.startsWith("togglepray:")) {
            return "TogglePray: <name>  → toggles a prayer on/off. Example: 'TogglePray: Augury'.";
        }
        if (lower.equals("special")) {
            return "Special  → uses your special attack with the currently equipped weapon if you have enough spec energy.";
        }
        if (lower.equals("attack")) {
            return "Attack  → issues an attack on the current target. Often used after a gear or spell swap.";
        }
        if (lower.startsWith("move:")) {
            return "Move:N  → moves your character N tiles towards / around the target. Example: 'Move:1'.";
        }
        if (lower.startsWith("moverange") || lower.startsWith("meleerange")) {
            return "MeleeRange  → positions you in melee distance from the target.";
        }
        if (lower.startsWith("log:")) {
            return "Log: <text>  → prints a debug message to chat/log. Example: 'Log: Entering combo...'.";
        }
        if (lower.startsWith("tick:")) {
            return "Tick:N  → waits N game ticks before continuing. Example: 'Tick:1' for a one-tick delay.";
        }

        return "";
    }

    private String getConditionHelpText(String condition) {
        if (condition == null) {
            return "";
        }

        String lower = condition.toLowerCase();

        if (lower.contains("frozen")) {
            return "frozen / target_frozen / self_frozen  → checks freeze status. Example: 'if frozen { ... }'.";
        }
        if (lower.contains("has_target") || lower.contains("target_exists")) {
            return "has_target / target_exists  → true when a target is selected. Example: 'if has_target { ... }'.";
        }
        if (lower.contains("cached_target")) {
            return "has_cached_target  → true when there is a remembered target from previous ticks.";
        }
        if (lower.startsWith("spec>=") || lower.equals("spec") || lower.contains("spec_energy")) {
            return "spec / spec_energy  → special attack energy (0‑100). Example: 'if spec>=50 { Special }'.";
        }
        if (lower.startsWith("hp<") || lower.equals("hp") || lower.contains("health") || lower.contains("hitpoints")) {
            return "hp / health / hitpoints  → your current HP. Example: 'if hp<40 { eat food }'.";
        }
        if (lower.startsWith("distance") || lower.contains("target_distance")) {
            return "distance / target_distance  → tiles between you and target. Example: 'if distance<=2 { ... }'.";
        }
        if (lower.contains("ticks_since_swap") || lower.contains("swap_ticks")) {
            return "ticks_since_swap / swap_ticks  → ticks since last gear swap. Example: 'if ticks_since_swap>=6 { ... }'.";
        }

        return "";
    }

    private void applyAutocompleteSelection(JTextArea editorArea, String selected) {
        if (editorArea == null || selected == null) {
            return;
        }

        String text = editorArea.getText();
        int pos = editorArea.getCaretPosition();
        if (pos > text.length()) {
            pos = text.length();
        }

        int start = pos;
        while (start > 0) {
            char ch = text.charAt(start - 1);
            if (Character.isWhitespace(ch) || ch == '{' || ch == '}' || ch == ',' || ch == ':') {
                break;
            }
            start--;
        }

        try {
            Document doc = editorArea.getDocument();
            doc.remove(start, pos - start);
            doc.insertString(start, selected, null);
        } catch (Exception e) {
            // Fallback
            editorArea.replaceRange(selected, start, pos);
        }
    }

    private void updateGhostSuggestion(JTextArea editorArea, java.util.List<String> baseSuggestions) {
        if (!(editorArea instanceof GhostTextArea)) {
            return;
        }

        GhostTextArea ghostArea = (GhostTextArea) editorArea;

        String text = editorArea.getText();
        int pos = editorArea.getCaretPosition();
        if (pos > text.length()) {
            pos = text.length();
        }

        int start = pos;
        while (start > 0) {
            char ch = text.charAt(start - 1);
            if (Character.isWhitespace(ch) || ch == '{' || ch == '}' || ch == ',' || ch == ':') {
                break;
            }
            start--;
        }

        String prefix = text.substring(start, pos);
        String trimmedPrefix = prefix.trim();
        if (trimmedPrefix.isEmpty()) {
            ghostArea.setGhostSuffix("");
            return;
        }

        String lowerPrefix = trimmedPrefix.toLowerCase();
        String best = null;
        for (String s : baseSuggestions) {
            String lower = s.toLowerCase();
            if (lower.startsWith(lowerPrefix) && s.length() > trimmedPrefix.length()) {
                best = s;
                break;
            }
        }

        if (best == null) {
            ghostArea.setGhostSuffix("");
            return;
        }

        String suffix = best.substring(trimmedPrefix.length());
        ghostArea.setGhostSuffix(suffix);
    }

    private static class GhostTextArea extends JTextArea {
        private String ghostSuffix = "";

        public GhostTextArea(String text) {
            super(text);
        }

        public void setGhostSuffix(String suffix) {
            this.ghostSuffix = suffix != null ? suffix : "";
            repaint();
        }

        public boolean hasGhostSuffix() {
            return ghostSuffix != null && !ghostSuffix.isEmpty();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (ghostSuffix == null || ghostSuffix.isEmpty()) {
                return;
            }

            try {
                int pos = getCaretPosition();
                java.awt.Rectangle r = modelToView(pos);
                if (r == null) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 80));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = r.y + fm.getAscent();
                g2.drawString(ghostSuffix, r.x, y);
                g2.dispose();
            } catch (BadLocationException e) {
                // Ignore drawing errors
            }
        }
    }

    private void copyCurrentGearToLoadout(int loadoutNum) {
        if (plugin != null) {
            plugin.copyCurrentGearToLoadout(loadoutNum);
        }
    }

    private void setKeybind(int loadoutNum, JButton button) {
        // Start inline keybind capture
        isCapturingKeybind = true;
        capturingLoadoutNum = loadoutNum;
        capturingButton = button;

        // Update button to show capturing state
        button.setText("🔗 Press any key...");
        button.setBackground(new Color(255, 152, 0)); // Orange for capturing state

        // Request focus to ensure key events are captured
        requestFocusInWindow();
    }

    private void captureKeybind(KeyEvent e) {
        if (!isCapturingKeybind)
            return;

        // Handle ESC to cancel
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            cancelKeybindCapture();
            return;
        }

        // Create keybind from key event
        Keybind keybind = new Keybind(e.getKeyCode(), e.getModifiersEx());
        LoadoutData data = loadoutData.get(capturingLoadoutNum);

        if (data != null) {
            data.keybind = keybind;
            capturingButton.setText("🔗 " + keybind.toString());
            saveLoadoutToConfig(capturingLoadoutNum);
            // Notify plugin so it can refresh hotkeys immediately
            if (plugin != null) {
                plugin.refreshHotkeys();
            }
        }

        // Reset capture state
        resetKeybindCapture();
    }

    private void cancelKeybindCapture() {
        if (capturingButton != null && capturingLoadoutNum > 0) {
            LoadoutData data = loadoutData.get(capturingLoadoutNum);
            String displayText = data != null && data.keybind != null ? "🔗 " + data.keybind.toString()
                    : "🔗 Set Keybind";
            capturingButton.setText(displayText);
            capturingButton.setBackground(new Color(76, 175, 80)); // Green
        }

        resetKeybindCapture();
    }

    private void resetKeybindCapture() {
        isCapturingKeybind = false;
        capturingLoadoutNum = -1;
        capturingButton = null;
    }

    /**
     * Update text area height based on content lines
     */
    private void updateTextAreaHeight(JTextArea textArea) {
        try {
            String text = textArea.getText();
            if (text == null || text.trim().isEmpty()) {
                textArea.setRows(2); // Minimum height
                return;
            }

            // Count lines
            String[] lines = text.split("\n");
            int lineCount = lines.length;

            // Ensure minimum of 2 lines, maximum of 10 lines
            int rows = Math.max(2, Math.min(10, lineCount));
            textArea.setRows(rows);

            // Update the scroll pane preferred size
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, textArea);
            if (scrollPane != null) {
                int height = rows * 15 + 10; // 15 pixels per line + padding
                height = Math.max(40, Math.min(160, height)); // Min 40px, max 160px
                scrollPane.setPreferredSize(new Dimension(Integer.MAX_VALUE, height));
                scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
                scrollPane.revalidate();
            }
        } catch (Exception e) {
            // Fallback to default height
            textArea.setRows(3);
        }
    }

    private void deleteLoadout(int loadoutNum) {
        LoadoutData data = loadoutData.get(loadoutNum);
        String loadoutName = data != null ? data.name : "Loadout " + loadoutNum;

        int result = JOptionPane.showConfirmDialog(this,
                "Delete " + loadoutName + "? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // Remove from memory
            loadoutData.remove(loadoutNum);

            // Remove from config
            if (configManager != null) {
                configManager.unsetConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Name");
                configManager.unsetConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Items");
                configManager.unsetConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Keybind");
            }

            // Refresh UI
            refreshLoadoutPanel();
        }
    }

    private void addNewLoadout() {
        // Find the next available loadout number
        int newLoadoutNum = 1;
        while (loadoutData.containsKey(newLoadoutNum)) {
            newLoadoutNum++;
        }

        // Create new loadout data
        LoadoutData newData = new LoadoutData(newLoadoutNum);
        loadoutData.put(newLoadoutNum, newData);
        saveLoadoutToConfig(newLoadoutNum);

        refreshLoadoutPanel();
    }

    public void saveLoadoutToConfig(int loadoutNum) {
        if (configManager == null)
            return;

        LoadoutData data = loadoutData.get(loadoutNum);
        if (data != null) {
            configManager.setConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Name", data.name);
            configManager.setConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Items", data.items);
            configManager.setConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Attack", data.attack);
            if (data.keybind != null) {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "loadout" + loadoutNum + "Keybind",
                        data.keybind.toString());
            }
        }
    }

    // Public method for plugin to get loadout data
    public LoadoutData getLoadoutData(int loadoutNum) {
        LoadoutData data = loadoutData.get(loadoutNum);
        System.out.println("[Gear Swapper UI] Plugin requested loadout " + loadoutNum + ": " +
                (data != null ? "found - " + data.name + ", items: " + data.items : "NOT FOUND"));
        return data;
    }

    private void updatePresetSelector() {
        if (presetSelector != null) {
            presetSelector.removeAllItems();
            for (String presetName : presets.keySet()) {
                presetSelector.addItem(presetName);
            }
        }
    }

    private void saveCurrentToPreset() {
        String selectedPreset = (String) presetSelector.getSelectedItem();
        if (selectedPreset == null || selectedPreset.isEmpty()) {
            return;
        }

        if (loadoutData.isEmpty()) {
            return;
        }

        // Save current loadouts to preset
        PresetData preset = presets.computeIfAbsent(selectedPreset, name -> new PresetData(name));
        preset.loadouts.clear();
        preset.loadouts.putAll(loadoutData);

        // Save preset to config
        savePresetToConfig(selectedPreset);
    }

    private void loadSelectedPreset() {
        String selectedPreset = (String) presetSelector.getSelectedItem();
        if (selectedPreset == null || selectedPreset.isEmpty()) {
            return;
        }

        PresetData preset = presets.get(selectedPreset);
        if (preset == null) {
            return;
        }

        if (preset.loadouts.isEmpty()) {
            return;
        }

        // Clear current loadouts and load from preset
        loadoutData.clear();
        loadoutData.putAll(preset.loadouts);

        // Save loaded loadouts to config
        for (Map.Entry<Integer, LoadoutData> entry : loadoutData.entrySet()) {
            saveLoadoutToConfig(entry.getKey());
        }

        // Refresh UI to show loaded data
        refreshLoadoutPanel();
    }

    private void deleteSelectedPreset() {
        String selectedPreset = (String) presetSelector.getSelectedItem();
        if (selectedPreset == null || selectedPreset.isEmpty()) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Delete preset '" + selectedPreset + "'? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // Remove from memory
            presets.remove(selectedPreset);

            // Remove from config
            deletePresetFromConfig(selectedPreset);

            // Update UI
            updatePresetSelector();
        }
    }

    private void createNewPreset(String name) {
        if (name == null || name.trim().isEmpty() || name.equals("New preset name...")) {
            return;
        }

        if (presets.containsKey(name)) {
            return;
        }

        presets.put(name, new PresetData(name));
        savePresetToConfig(name);
        updatePresetSelector();
        presetSelector.setSelectedItem(name);
    }

    private void savePresetToConfig(String presetName) {
        if (configManager == null)
            return;

        PresetData preset = presets.get(presetName);
        if (preset != null) {
            // Save preset name to list
            String presetNames = configManager.getConfiguration(GearSwapperConfig.GROUP, "presetNames");
            if (presetNames == null || presetNames.trim().isEmpty()) {
                presetNames = presetName;
            } else if (!presetNames.contains(presetName)) {
                presetNames += "," + presetName;
            }
            configManager.setConfiguration(GearSwapperConfig.GROUP, "presetNames", presetNames);

            // Save preset data
            String prefix = "preset_" + presetName + "_";
            for (Map.Entry<Integer, LoadoutData> entry : preset.loadouts.entrySet()) {
                String loadoutKey = prefix + entry.getKey();
                LoadoutData data = entry.getValue();
                configManager.setConfiguration(GearSwapperConfig.GROUP, loadoutKey + "_name", data.name);
                configManager.setConfiguration(GearSwapperConfig.GROUP, loadoutKey + "_items", data.items);
                if (data.keybind != null) {
                    configManager.setConfiguration(GearSwapperConfig.GROUP, loadoutKey + "_keybind",
                            data.keybind.toString());
                }
            }
        }
    }

    private void deletePresetFromConfig(String presetName) {
        if (configManager == null)
            return;

        // Remove from preset names list
        String presetNames = configManager.getConfiguration(GearSwapperConfig.GROUP, "presetNames");
        if (presetNames != null && presetNames.contains(presetName)) {
            String[] names = presetNames.split(",");
            StringBuilder newNames = new StringBuilder();
            for (String name : names) {
                if (!name.trim().equals(presetName) && !name.trim().isEmpty()) {
                    if (newNames.length() > 0)
                        newNames.append(",");
                    newNames.append(name.trim());
                }
            }
            if (newNames.length() > 0) {
                configManager.setConfiguration(GearSwapperConfig.GROUP, "presetNames", newNames.toString());
            } else {
                configManager.unsetConfiguration(GearSwapperConfig.GROUP, "presetNames");
            }
        }

        // Remove preset data
        // Note: ConfigManager doesn't provide a way to remove all keys matching a
        // pattern
        // For now we'll just leave the old data, it will be overwritten if the preset
        // is recreated
    }

    private void loadSavedLoadouts() {
        // Load saved loadouts from config
        if (configManager != null) {
            // Load loadout data from config
            for (int i = 1; i <= 50; i++) {
                String name = configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + i + "Name");
                String items = configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + i + "Items");
                Boolean attack = configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + i + "Attack",
                        Boolean.class);
                String keybindStr = configManager.getConfiguration(GearSwapperConfig.GROUP, "loadout" + i + "Keybind");

                if (name != null && !name.trim().isEmpty()) {
                    LoadoutData data = new LoadoutData(i);
                    data.name = name;
                    data.items = items != null ? items : "";
                    data.attack = attack != null ? attack : false;
                    // Parse keybind string back to Keybind object
                    if (keybindStr != null && !keybindStr.trim().isEmpty()) {
                        try {
                            // Simple parsing - assumes format like "ctrl+shift+1"
                            data.keybind = parseKeybind(keybindStr);
                        } catch (Exception e) {
                            // If parsing fails, leave keybind as null
                        }
                    }
                    loadoutData.put(i, data);
                    System.out.println(
                            "[Gear Swapper UI] Loaded loadout " + i + ": " + name + " (attack=" + data.attack + ")");
                }
            }
            System.out.println("[Gear Swapper UI] Total loadouts loaded: " + loadoutData.size());
        } else {
            System.out.println("[Gear Swapper UI] ConfigManager is null, cannot load loadouts");
        }
    }

    private Keybind parseKeybind(String keybindStr) {
        // Simple keybind parsing - this is a basic implementation
        // In a real scenario, you'd want more robust parsing
        int keyCode = 0;
        int modifiers = 0;

        String[] parts = keybindStr.toLowerCase().split("\\+");
        for (String part : parts) {
            switch (part.trim()) {
                case "ctrl":
                    modifiers |= KeyEvent.CTRL_DOWN_MASK;
                    break;
                case "alt":
                    modifiers |= KeyEvent.ALT_DOWN_MASK;
                    break;
                case "shift":
                    modifiers |= KeyEvent.SHIFT_DOWN_MASK;
                    break;
                case "meta":
                case "cmd":
                    modifiers |= KeyEvent.META_DOWN_MASK;
                    break;
                default:
                    // Try to parse as a key code
                    if (part.length() == 1) {
                        keyCode = KeyEvent.getExtendedKeyCodeForChar(part.charAt(0));
                    } else {
                        // Handle special keys
                        switch (part) {
                            case "enter":
                                keyCode = KeyEvent.VK_ENTER;
                                break;
                            case "space":
                                keyCode = KeyEvent.VK_SPACE;
                                break;
                            case "tab":
                                keyCode = KeyEvent.VK_TAB;
                                break;
                            case "escape":
                                keyCode = KeyEvent.VK_ESCAPE;
                                break;
                            case "delete":
                                keyCode = KeyEvent.VK_DELETE;
                                break;
                            case "backspace":
                                keyCode = KeyEvent.VK_BACK_SPACE;
                                break;
                            case "up":
                                keyCode = KeyEvent.VK_UP;
                                break;
                            case "down":
                                keyCode = KeyEvent.VK_DOWN;
                                break;
                            case "left":
                                keyCode = KeyEvent.VK_LEFT;
                                break;
                            case "right":
                                keyCode = KeyEvent.VK_RIGHT;
                                break;
                            default:
                                // Try to parse as number
                                try {
                                    keyCode = Integer.parseInt(part);
                                } catch (NumberFormatException e) {
                                    // Invalid key, skip
                                }
                                break;
                        }
                    }
                    break;
            }
        }

        return keyCode != 0 ? new Keybind(keyCode, modifiers) : null;
    }

    private void loadSavedPresets() {
        // Load saved presets from config
        if (configManager != null) {
            String presetNames = configManager.getConfiguration(GearSwapperConfig.GROUP, "presetNames");
            if (presetNames != null && !presetNames.trim().isEmpty()) {
                String[] names = presetNames.split(",");
                for (String name : names) {
                    if (!name.trim().isEmpty()) {
                        PresetData preset = new PresetData(name.trim());

                        // Load preset data
                        String prefix = "preset_" + name.trim() + "_";
                        for (int i = 1; i <= 50; i++) {
                            String loadoutName = configManager.getConfiguration(GearSwapperConfig.GROUP,
                                    prefix + i + "_name");
                            String loadoutItems = configManager.getConfiguration(GearSwapperConfig.GROUP,
                                    prefix + i + "_items");
                            String loadoutKeybind = configManager.getConfiguration(GearSwapperConfig.GROUP,
                                    prefix + i + "_keybind");

                            if (loadoutName != null && !loadoutName.trim().isEmpty()) {
                                LoadoutData data = new LoadoutData(i);
                                data.name = loadoutName;
                                data.items = loadoutItems != null ? loadoutItems : "";
                                // Parse keybind string back to Keybind object
                                if (loadoutKeybind != null && !loadoutKeybind.trim().isEmpty()) {
                                    try {
                                        data.keybind = parseKeybind(loadoutKeybind);
                                    } catch (Exception e) {
                                        // If parsing fails, leave keybind as null
                                    }
                                }
                                preset.loadouts.put(i, data);
                            }
                        }

                        presets.put(name.trim(), preset);
                    }
                }
            }
        }
    }

    // CollapsiblePanel helper class (simplified version)
    private static class CollapsiblePanel extends JPanel {
        private final JPanel headerPanel;
        private final JPanel contentPanel;
        private final JLabel titleLabel;
        private final JButton toggleButton;
        private boolean expanded = true;

        public CollapsiblePanel(String title) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            // Header
            headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(true);
            headerPanel.setBackground(Theme.SURFACE);
            headerPanel.setBorder(new EmptyBorder(10, 16, 10, 16));
            headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLabel.setForeground(Theme.TEXT_PRIMARY);

            toggleButton = new JButton("▼");
            toggleButton.setBackground(Theme.BACKGROUND); // Subtle button
            toggleButton.setForeground(Theme.TEXT_SECONDARY);
            toggleButton.setFocusPainted(false);
            toggleButton.setPreferredSize(new Dimension(30, 22));
            toggleButton.setMaximumSize(new Dimension(30, 22));
            toggleButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
            toggleButton.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            toggleButton.addActionListener(e -> toggle());

            // Hover effect for toggle
            toggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    toggleButton.setBackground(Theme.SURFACE_HOVER);
                    toggleButton.setForeground(Theme.TEXT_PRIMARY);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    toggleButton.setBackground(Theme.BACKGROUND);
                    toggleButton.setForeground(Theme.TEXT_SECONDARY);
                }
            });

            headerPanel.add(titleLabel, BorderLayout.CENTER);
            headerPanel.add(toggleButton, BorderLayout.EAST);

            // Content
            contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);

            add(headerPanel);
            add(contentPanel);
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            contentPanel.setVisible(expanded);
            toggleButton.setText(expanded ? "▼" : "▲");
        }

        public void addContent(Component component) {
            contentPanel.add(component);
        }

        public void setContent(Component component) {
            contentPanel.removeAll();
            contentPanel.add(component);
            revalidate();
            repaint();
        }

        public void addVerticalStrut(int height) {
            contentPanel.add(Box.createVerticalStrut(height));
        }

        private void toggle() {
            setExpanded(!expanded);
            revalidate();
            repaint();
        }
    }

    // Loadout data class - matches what plugin expects
    public static class LoadoutData {
        public final int loadoutNum;
        public String name = "";
        public String items = "";
        public Keybind keybind = null;
        public boolean attack = false;

        public LoadoutData(int loadoutNum) {
            this.loadoutNum = loadoutNum;
            this.name = "Loadout " + loadoutNum;
        }

        public String getName() {
            return name;
        }

        public String getItems() {
            return items;
        }

        public Keybind getKeybind() {
            return keybind;
        }

        public boolean getAttack() {
            return attack;
        }

        public void setAttack(boolean attack) {
            this.attack = attack;
        }
    }

    // Gear slot enum
    public enum GearSlot {
        HEAD("Head"),
        CAPE("Cape"),
        AMULET("Amulet"),
        WEAPON("Weapon"),
        CHEST("Chest"),
        SHIELD("Shield"),
        LEGS("Legs"),
        GLOVES("Gloves"),
        BOOTS("Boots"),
        RING("Ring"),
        AMMO("Ammo");

        private final String displayName;

        GearSlot(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Item data class
    public static class ItemData {
        public String name;
        public String wikiImageUrl;
        public String equipCommand;
        public BufferedImage cachedImage;

        public ItemData(String name, String wikiImageUrl, String equipCommand) {
            this.name = name;
            this.wikiImageUrl = wikiImageUrl;
            this.equipCommand = equipCommand;
        }

        public String getWikiImageUrl() {
            if (wikiImageUrl == null && name != null) {
                // Generate wiki URL from item name
                String formattedName = name.replace(" ", "_").replace("'", "");
                return "https://oldschool.runescape.wiki/images/" + formattedName + ".png";
            }
            return wikiImageUrl;
        }
    }

    // Gear set class
    public static class GearSet {
        public String name;
        public Map<GearSlot, ItemData> items = new HashMap<>();

        public GearSet(String name) {
            this.name = name;
        }

        public void setItem(GearSlot slot, ItemData item) {
            items.put(slot, item);
        }

        public ItemData getItem(GearSlot slot) {
            return items.get(slot);
        }

        public String generateEquipCommands() {
            StringBuilder commands = new StringBuilder();
            for (ItemData item : items.values()) {
                if (item.equipCommand != null) {
                    commands.append(item.equipCommand).append("\n");
                }
            }
            return commands.toString();
        }
    }

    // Preset data class
    public static class PresetData {
        String name;
        Map<Integer, LoadoutData> loadouts = new HashMap<>();

        public PresetData(String name) {
            this.name = name;
        }
    }

    // Documentation panel for search results
    private static class DocumentationPanel extends JPanel {
        private JPanel contentPanel;
        private JScrollPane scrollPane;
        private JTextField searchField;

        public DocumentationPanel() {
            setLayout(new BorderLayout());
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setBorder(new EmptyBorder(16, 16, 16, 16));

            // Header with search
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);
            headerPanel.setBorder(new EmptyBorder(0, 0, 16, 0));

            JLabel titleLabel = new JLabel("📚 Gear Swapper Documentation");
            titleLabel.setFont(new Font("Whitney", Font.BOLD, 16));
            titleLabel.setForeground(Color.WHITE);

            // Search section
            JPanel searchPanel = new JPanel(new BorderLayout());
            searchPanel.setOpaque(false);
            searchPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

            JLabel searchLabel = new JLabel("🔍 Search Commands:");
            searchLabel.setFont(new Font("Whitney", Font.BOLD, 12));
            searchLabel.setForeground(new Color(120, 190, 255));

            JPanel searchRow = new JPanel(new BorderLayout());
            searchRow.setOpaque(false);
            searchRow.setBorder(new EmptyBorder(8, 0, 0, 0));

            searchField = new JTextField("Search (e.g., prayer, gear, spell)...");
            searchField.setBackground(new Color(52, 53, 58));
            searchField.setForeground(new Color(160, 170, 185));
            searchField.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
            searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            searchField.setFont(new Font("Whitney", Font.PLAIN, 10));

            JButton searchBtn = new JButton("Search");
            searchBtn.setBackground(new Color(76, 175, 80));
            searchBtn.setForeground(Color.WHITE);
            searchBtn.setFocusPainted(false);
            searchBtn.setPreferredSize(new Dimension(70, 28));
            searchBtn.setMaximumSize(new Dimension(70, 28));
            searchBtn.setFont(new Font("Whitney", Font.BOLD, 10));
            searchBtn.addActionListener(e -> performSearch());

            // Add enter key support
            searchField.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        performSearch();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }

                @Override
                public void keyTyped(KeyEvent e) {
                }
            });

            JButton clearBtn = new JButton("Clear");
            clearBtn.setBackground(new Color(158, 158, 158));
            clearBtn.setForeground(Color.WHITE);
            clearBtn.setFocusPainted(false);
            clearBtn.setPreferredSize(new Dimension(60, 28));
            clearBtn.setMaximumSize(new Dimension(60, 28));
            clearBtn.setFont(new Font("Whitney", Font.BOLD, 10));
            clearBtn.addActionListener(e -> {
                searchField.setText("");
                showAllDocumentation();
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            buttonPanel.add(searchBtn);
            buttonPanel.add(Box.createHorizontalStrut(4));
            buttonPanel.add(clearBtn);

            searchRow.add(searchField, BorderLayout.CENTER);
            searchRow.add(buttonPanel, BorderLayout.EAST);

            searchPanel.add(searchLabel, BorderLayout.NORTH);
            searchPanel.add(searchRow, BorderLayout.CENTER);

            headerPanel.add(titleLabel, BorderLayout.NORTH);
            headerPanel.add(searchPanel, BorderLayout.CENTER);

            // Content area
            contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);

            // Scroll pane for content
            scrollPane = new JScrollPane(contentPanel);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            add(headerPanel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            // Show all documentation by default
            showAllDocumentation();
        }

        private void performSearch() {
            String query = searchField.getText().trim();
            if (query.isEmpty() || query.equals("Search (e.g., prayer, gear, spell)...")) {
                showAllDocumentation();
                return;
            }

            query = query.toLowerCase().trim();
            showSearchResults(query);
        }

        private void showAllDocumentation() {
            contentPanel.removeAll();

            // ════════════════════════════════════════════════════════════════════════════════
            // 🎯 GEAR SWAPPER SCRIPTING SYNTAX GUIDE
            // ════════════════════════════════════════════════════════════════════════════════

            addCommandSection(contentPanel, "🚀 Quick Start Syntax",
                    "Master these patterns to create powerful scripts:", new String[] {
                            "📦 Items: Dragon scimitar → Equips item",
                            "✨ Spells: Cast:Ice Barrage → Casts spell",
                            "🙏 Prayers: Prayer:piety → Activates prayer",
                            "⚔️ Special: Special → Uses weapon special",
                            "💬 Messages: Log:Hello → Prints to chat",
                            "🏃 Movement: Move:5 → Move 5 tiles from target"
                    });

            addCommandSection(contentPanel, "📦 Equipment Commands",
                    "Equip any item by name:", new String[] {
                            "Dragon scimitar",
                            "Armadyl godsword",
                            "Toxic blowpipe",
                            "Bandos chestplate",
                            "Amulet of fury",
                            "Barrows gloves",
                            "Dragon boots"
                    });

            addCommandSection(contentPanel, "✨ Spell Commands",
                    "Cast spells with Cast: prefix:", new String[] {
                            "Cast:Ice Barrage → Ice barrage spell",
                            "Cast:Vengeance → Vengeance spell",
                            "Cast:Teleport to house → House teleport",
                            "Cast:Varrock teleport → Varrock teleport"
                    });

            addCommandSection(contentPanel, "🙏 Prayer Commands",
                    "Activate prayers with Prayer: prefix:", new String[] {
                            "Prayer:piety → Melee prayer",
                            "Prayer:augury → Magic prayer",
                            "Prayer:rigour → Ranged prayer",
                            "Prayer:protect from magic → Magic protection",
                            "Prayer:protect from melee → Melee protection",
                            "Prayer:protect from ranged → Ranged protection"
                    });

            addCommandSection(contentPanel, "⚔️ Combat & Utility Commands",
                    "Essential combat actions:", new String[] {
                            "Special → Activate weapon special attack",
                            "Attack → Attack current target (works with Tick:N)",
                            "Log:Your message → Print custom message",
                            "Move:0 → Walk under target",
                            "Move:5 → Move 5 tiles away from target",
                            "MoveDiag:3 → Move 3 tiles diagonally away",
                            "Tick:2 → Wait 2 game ticks"
                    });

            addCommandSection(contentPanel, "🔍 Condition Variables",
                    "Use these in if statements:", new String[] {
                            "frozen → Player is frozen",
                            "target_frozen → Target is frozen",
                            "has_target → You have a target selected",
                            "hp → Current hitpoints (number)",
                            "spec → Special attack energy (0-100)",
                            "prayer → Current prayer points",
                            "target_distance → Distance to target in tiles",
                            "player_frozen_ticks → Ticks until player unfrozen",
                            "target_frozen_ticks → Ticks until target unfrozen",
                            "ticks_since_swap → Ticks since last gear swap"
                    });

            addCommandSection(contentPanel, "⚖️ Comparison Operators",
                    "Compare variables with these:", new String[] {
                            "hp < 40 → HP below 40",
                            "spec >= 50 → Special energy 50% or more",
                            "target_distance <= 3 → Target 3 tiles or closer",
                            "target_distance > 3 → Target farther than 3 tiles",
                            "player_frozen_ticks > 10 → Frozen for more than 10 ticks"
                    });

            addCommandSection(contentPanel, "🔗 Logical Operators",
                    "Combine multiple conditions:", new String[] {
                            "&& → AND (both must be true)",
                            "|| → OR (either can be true)",
                            "! → NOT (inverts condition)",
                            "frozen && target_distance > 3 → Frozen and target far",
                            "hp < 40 || spec >= 50 → Low HP OR high spec",
                            "!frozen → True when NOT frozen"
                    });

            addCommandSection(contentPanel, "📝 Conditional Statements",
                    "If-then-else logic:", new String[] {
                            "if frozen { Log: Frozen! }",
                            "if hp < 40 { Special }",
                            "if target_frozen { Armadyl godsword } else { Dragon claws }",
                            "if frozen && target_distance > 3 { Log: Danger! }"
                    });

            addCommandSection(contentPanel, "📄 Multi-line Scripts",
                    "Complex logic with multiple actions:", new String[] {
                            "if hp < 40 {",
                            "    Log: Emergency!",
                            "    Special",
                            "    Dragon claws",
                            "} else {",
                            "    Log: Safe",
                            "    Toxic blowpipe",
                            "}"
                    });

            addCommandSection(contentPanel, "💡 Pro Tips",
                    "Advanced scripting techniques:", new String[] {
                            "• Chain items: Dragon scimitar → Dragon dagger → Special",
                            "• Combine magic: Ice Barrage → Move:3 → Dragon claws",
                            "• Auto-eat: if hp < 60 { inv:eat shark }",
                            "• Prayer switching: prayer:piety → prayer:protect from magic",
                            "• Tick-perfect: Tick:1 → Move:0 → Special"
                    });

            contentPanel.revalidate();
            contentPanel.repaint();
        }

        private void showSearchResults(String query) {
            contentPanel.removeAll();

            // Prayer commands
            if (query.contains("pray")) {
                addCommandSection(contentPanel, "🙏 Prayer Commands",
                        "Activate prayers with prefix 'Prayer:'", new String[] {
                                "Prayer:piety → Melee combat prayer",
                                "Prayer:augury → Magic combat prayer",
                                "Prayer:rigour → Ranged combat prayer",
                                "Prayer:protect from magic → Magic protection",
                                "Prayer:protect from melee → Melee protection",
                                "Prayer:protect from ranged → Ranged protection"
                        });
            }

            // Gear commands
            if (query.contains("gear") || query.contains("wear") || query.contains("equip")) {
                addCommandSection(contentPanel, "📦 Equipment Commands",
                        "Equip items by name (no prefix needed):", new String[] {
                                "Dragon scimitar → Equips Dragon scimitar",
                                "Bandos chestplate → Equips Bandos chestplate",
                                "Dragon boots → Equips Dragon boots",
                                "Amulet of fury → Equips Amulet of fury",
                                "Barrows gloves → Equips Barrows gloves",
                                "Armadyl godsword → Equips Armadyl godsword"
                        });
            }

            // Spell commands
            if (query.contains("spell") || query.contains("magic")) {
                addCommandSection(contentPanel, "✨ Spell Commands",
                        "Cast spells with prefix 'Cast:'", new String[] {
                                "Cast:Ice Barrage → Casts Ice Barrage",
                                "Cast:Vengeance → Casts Vengeance",
                                "Cast:Teleport to house → House teleport",
                                "Cast:Varrock teleport → Varrock teleport",
                                "Cast:Wind Strike → Basic wind spell",
                                "Cast:Fire Bolt → Fire bolt spell"
                        });
            }

            // Inventory commands
            if (query.contains("inv") || query.contains("inventory")) {
                addCommandSection(contentPanel, "🎒 Inventory Commands",
                        "Use inventory items with prefix 'inv:'", new String[] {
                                "inv:eat shark → Eats Shark from inventory",
                                "inv:drink super restore → Drinks Super Restore",
                                "inv:drink prayer potion → Drinks Prayer potion",
                                "inv:eat karambwan → Eats Karambwan",
                                "inv:drink combat potion → Drinks Combat potion"
                        });
            }

            // Special commands
            if (query.contains("special") || query.contains("spec")) {
                addCommandSection(contentPanel, "⚔️ Special Attack Commands",
                        "Use special attacks:", new String[] {
                                "Special → Activates weapon special attack",
                                "Works with: Dragon dagger, Dragon claws, Abyssal whip",
                                "Works with: Bandos godsword, Armadyl godsword",
                                "Note: Must have weapon equipped and spec energy"
                        });
            }

            // Movement commands
            if (query.contains("move") || query.contains("walk")) {
                addCommandSection(contentPanel, "🏃 Movement Commands",
                        "Control positioning with Move commands:", new String[] {
                                "Move:0 → Walk under target (same tile)",
                                "Move:5 → Move 5 tiles directly away",
                                "MoveDiag:3 → Move 3 tiles diagonally away",
                                "Useful for: Avoiding specs, positioning"
                        });
            }

            // Condition commands
            if (query.contains("if") || query.contains("condition") || query.contains("hp")) {
                addCommandSection(contentPanel, "🔍 Conditional Logic",
                        "If statements for smart automation:", new String[] {
                                "if frozen { Log: Frozen! }",
                                "if hp < 40 { Special }",
                                "if target_frozen { Armadyl godsword }",
                                "if spec >= 50 { Dragon claws }",
                                "Combine: if hp < 40 && spec >= 50 { Special }"
                        });
            }

            // Tick commands
            if (query.contains("tick") || query.contains("delay")) {
                addCommandSection(contentPanel, "⏱️ Timing Commands",
                        "Control execution timing:", new String[] {
                                "Tick:1 → Wait 1 game tick (~0.6s)",
                                "Tick:2 → Wait 2 game ticks (~1.2s)",
                                "Use for: Perfect combos, spell timing"
                        });
            }

            // Log commands
            if (query.contains("log") || query.contains("message")) {
                addCommandSection(contentPanel, "💬 Message Commands",
                        "Print custom messages:", new String[] {
                                "Log:Hello → Prints 'Hello' to chat",
                                "Log:Low HP! → Custom alerts",
                                "Log:Switched gear → Action confirmation"
                        });
            }

            // Quick prayers
            if (query.contains("quick")) {
                addCommandSection(contentPanel, "🙏 Quick Prayer Commands",
                        "Toggle quick prayers:", new String[] {
                                "quickprayer:on → Turns on quick prayers",
                                "quickprayer:off → Turns off quick prayers",
                                "quickprayer:toggle → Toggles quick prayers"
                        });
            }

            // Run commands
            if (query.contains("run")) {
                addCommandSection(contentPanel, "🏃 Run Commands",
                        "Control run energy:", new String[] {
                                "run:on → Turns on run",
                                "run:off → Turns off run",
                                "run:toggle → Toggles run mode"
                        });
            }

            // If no specific matches, show general help
            boolean foundMatch = query.contains("pray") || query.contains("gear") || query.contains("wear") ||
                    query.contains("equip") || query.contains("spell") || query.contains("magic") ||
                    query.contains("inv") || query.contains("inventory") || query.contains("special") ||
                    query.contains("spec") || query.contains("quick") || query.contains("run") ||
                    query.contains("move") || query.contains("walk") || query.contains("if") ||
                    query.contains("condition") || query.contains("hp") || query.contains("tick") ||
                    query.contains("delay") || query.contains("log") || query.contains("message");

            if (!foundMatch) {
                addCommandSection(contentPanel, "🔍 Search Help",
                        "Try searching for:", new String[] {
                                "pray → Prayer commands",
                                "gear → Equipment commands",
                                "spell → Magic spells",
                                "inv → Inventory items",
                                "special → Special attacks",
                                "move → Movement commands",
                                "if → Conditional logic",
                                "tick → Timing controls",
                                "log → Message printing"
                        });
            }

            contentPanel.revalidate();
            contentPanel.repaint();
        }

        private void addCommandSection(JPanel contentPanel, String title, String description, String[] commands) {
            // Section header
            JPanel sectionHeader = new JPanel(new BorderLayout());
            sectionHeader.setOpaque(false);
            sectionHeader.setBorder(new EmptyBorder(16, 0, 8, 0));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Whitney", Font.BOLD, 14));
            titleLabel.setForeground(new Color(120, 190, 255));

            JLabel descLabel = new JLabel(description);
            descLabel.setFont(new Font("Whitney", Font.PLAIN, 11));
            descLabel.setForeground(new Color(170, 175, 185));

            sectionHeader.add(titleLabel, BorderLayout.NORTH);
            sectionHeader.add(descLabel, BorderLayout.CENTER);

            contentPanel.add(sectionHeader);

            // Commands list
            for (String command : commands) {
                JPanel commandPanel = new JPanel(new BorderLayout());
                commandPanel.setOpaque(false);
                commandPanel.setBorder(new EmptyBorder(4, 16, 4, 16));
                commandPanel.setBackground(new Color(52, 53, 58));

                JLabel commandLabel = new JLabel(command);
                commandLabel.setFont(new Font("Whitney", Font.PLAIN, 11));
                commandLabel.setForeground(Color.WHITE);
                commandLabel.setBorder(new EmptyBorder(8, 12, 8, 12));

                JButton copyBtn = new JButton("📋");
                copyBtn.setBackground(new Color(76, 175, 80));
                copyBtn.setForeground(Color.WHITE);
                copyBtn.setFocusPainted(false);
                copyBtn.setPreferredSize(new Dimension(30, 22));
                copyBtn.setMaximumSize(new Dimension(30, 22));
                copyBtn.setFont(new Font("Whitney", Font.BOLD, 9));
                copyBtn.setToolTipText("Copy command");
                copyBtn.addActionListener(e -> {
                    // Copy command to clipboard
                    String cmdText = command.split(" - ")[0]; // Get just the command part
                    copyToClipboard(cmdText);
                });

                commandPanel.add(commandLabel, BorderLayout.CENTER);
                commandPanel.add(copyBtn, BorderLayout.EAST);

                contentPanel.add(commandPanel);
                contentPanel.add(Box.createVerticalStrut(4));
            }
        }

        private void copyToClipboard(String text) {
            try {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(text), null);
            } catch (Exception e) {
                // Clipboard copy failed
            }
        }
    }

    // Simple Gear Manager Panel - Working version
    private class SimpleGearManagerPanel extends JPanel {
        private Map<String, GearSwapperPanel.GearSet> gearSets;
        private ConfigManager configManager;
        private GearSwapperPanel.GearSet currentGearSet;
        private Map<GearSwapperPanel.GearSlot, GearSlotPanel> slotPanels = new HashMap<>();
        private JComboBox<String> gearSetSelector;
        private JTextField newGearSetName;
        private ItemSearchPanel itemSearchPanel;

        public SimpleGearManagerPanel(Map<String, GearSwapperPanel.GearSet> gearSets, ConfigManager configManager) {
            System.out.println("[Gear Swapper DEBUG] SimpleGearManagerPanel constructor called");
            System.out.println(
                    "[Gear Swapper DEBUG] Received gear sets: " + (gearSets != null ? gearSets.size() : "null"));
            System.out.println(
                    "[Gear Swapper DEBUG] Received config manager: " + (configManager != null ? "present" : "null"));

            this.gearSets = gearSets != null ? gearSets : new HashMap<>();
            this.configManager = configManager;

            setLayout(new BorderLayout());
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setBorder(new EmptyBorder(16, 16, 16, 16));

            System.out.println("[Gear Swapper DEBUG] Building simple gear manager UI...");

            // Create main components first
            add(buildHeader(), BorderLayout.NORTH);
            add(buildMainContent(), BorderLayout.CENTER);

            // Load saved gear sets after UI is built
            loadSavedGearSets();

            System.out.println("[Gear Swapper DEBUG] SimpleGearManagerPanel construction complete");
        }

        private JPanel buildHeader() {
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.setBorder(new EmptyBorder(0, 0, 16, 0));

            JLabel titleLabel = new JLabel("⚔️ Visual Gear Manager");
            titleLabel.setFont(new Font("Whitney", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);

            JPanel gearSetControls = new JPanel(new BorderLayout());
            gearSetControls.setOpaque(false);
            gearSetControls.setBorder(new EmptyBorder(12, 0, 0, 0));

            // Gear set selector
            JPanel selectorPanel = new JPanel(new BorderLayout());
            selectorPanel.setOpaque(false);

            gearSetSelector = new JComboBox<>();
            gearSetSelector.setBackground(new Color(52, 53, 58));
            gearSetSelector.setForeground(Color.WHITE);
            gearSetSelector.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
            gearSetSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            gearSetSelector.setFont(new Font("Whitney", Font.PLAIN, 10));
            gearSetSelector.addActionListener(e -> selectGearSet());

            // New gear set controls
            JPanel newSetPanel = new JPanel(new BorderLayout());
            newSetPanel.setOpaque(false);
            newSetPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

            newGearSetName = new JTextField("New gear set name...");
            newGearSetName.setBackground(new Color(52, 53, 58));
            newGearSetName.setForeground(new Color(160, 170, 185));
            newGearSetName.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
            newGearSetName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            newGearSetName.setFont(new Font("Whitney", Font.PLAIN, 10));

            JButton createBtn = new JButton("Create");
            createBtn.setBackground(new Color(76, 175, 80));
            createBtn.setForeground(Color.WHITE);
            createBtn.setFocusPainted(false);
            createBtn.setPreferredSize(new Dimension(70, 28));
            createBtn.setMaximumSize(new Dimension(70, 28));
            createBtn.setFont(new Font("Whitney", Font.BOLD, 10));
            createBtn.addActionListener(e -> createNewGearSet());

            JButton saveBtn = new JButton("Save");
            saveBtn.setBackground(new Color(33, 150, 243));
            saveBtn.setForeground(Color.WHITE);
            saveBtn.setFocusPainted(false);
            saveBtn.setPreferredSize(new Dimension(60, 28));
            saveBtn.setMaximumSize(new Dimension(60, 28));
            saveBtn.setFont(new Font("Whitney", Font.BOLD, 10));
            saveBtn.addActionListener(e -> saveCurrentGearSet());

            JButton deleteBtn = new JButton("Delete");
            deleteBtn.setBackground(new Color(244, 67, 54));
            deleteBtn.setForeground(Color.WHITE);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setPreferredSize(new Dimension(70, 28));
            deleteBtn.setMaximumSize(new Dimension(70, 28));
            deleteBtn.setFont(new Font("Whitney", Font.BOLD, 10));
            deleteBtn.addActionListener(e -> deleteCurrentGearSet());

            JButton exportBtn = new JButton("Export Commands");
            exportBtn.setBackground(new Color(156, 39, 176));
            exportBtn.setForeground(Color.WHITE);
            exportBtn.setFocusPainted(false);
            exportBtn.setPreferredSize(new Dimension(120, 28));
            exportBtn.setMaximumSize(new Dimension(120, 28));
            exportBtn.setFont(new Font("Whitney", Font.BOLD, 10));
            exportBtn.addActionListener(e -> exportCommands());

            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            buttonPanel.add(createBtn);
            buttonPanel.add(Box.createHorizontalStrut(4));
            buttonPanel.add(saveBtn);
            buttonPanel.add(Box.createHorizontalStrut(4));
            buttonPanel.add(deleteBtn);
            buttonPanel.add(Box.createHorizontalStrut(4));
            buttonPanel.add(exportBtn);

            newSetPanel.add(newGearSetName, BorderLayout.CENTER);
            newSetPanel.add(buttonPanel, BorderLayout.EAST);

            selectorPanel.add(new JLabel("Gear Set: "), BorderLayout.WEST);
            selectorPanel.add(gearSetSelector, BorderLayout.CENTER);

            gearSetControls.add(selectorPanel, BorderLayout.NORTH);
            gearSetControls.add(newSetPanel, BorderLayout.CENTER);

            header.add(titleLabel, BorderLayout.NORTH);
            header.add(gearSetControls, BorderLayout.CENTER);

            return header;
        }

        private JPanel buildMainContent() {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setOpaque(false);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setOpaque(false);
            splitPane.setDividerSize(8);
            splitPane.setResizeWeight(0.6);

            // Left side - Gear slots
            JPanel gearSlotsPanel = buildGearSlotsPanel();
            gearSlotsPanel.setPreferredSize(new Dimension(500, 500));

            // Right side - Item search
            itemSearchPanel = new ItemSearchPanel();
            itemSearchPanel.setPreferredSize(new Dimension(350, 500));

            splitPane.setLeftComponent(gearSlotsPanel);
            splitPane.setRightComponent(itemSearchPanel);

            mainPanel.add(splitPane, BorderLayout.CENTER);
            return mainPanel;
        }

        private JPanel buildGearSlotsPanel() {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setOpaque(false);

            JLabel slotsLabel = new JLabel("Equipment Slots (Click to add items)");
            slotsLabel.setFont(new Font("Whitney", Font.BOLD, 12));
            slotsLabel.setForeground(new Color(120, 190, 255));
            slotsLabel.setBorder(new EmptyBorder(0, 0, 12, 0));

            // Create 11-slot layout (3-3-3-2 pattern like OSRS)
            JPanel slotsGrid = new JPanel(new GridBagLayout());
            slotsGrid.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.BOTH;

            // Row 1: Head, Cape, Amulet
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.HEAD, 0, 0);
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.CAPE, 1, 0);
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.AMULET, 2, 0);

            // Row 2: Weapon, Chest, Shield
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.WEAPON, 0, 1);
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.CHEST, 1, 1);
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.SHIELD, 2, 1);

            // Row 3: Legs, Gloves, Boots
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.LEGS, 0, 2);
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.GLOVES, 1, 2);
            addSlotToGrid(slotsGrid, gbc, GearSwapperPanel.GearSlot.BOOTS, 2, 2);

            // Row 4: Ring, Ammo (centered)
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 3;
            JPanel bottomRow = new JPanel(new GridLayout(1, 2, 8, 0));
            bottomRow.setOpaque(false);

            GearSlotPanel ringPanel = new GearSlotPanel(GearSwapperPanel.GearSlot.RING);
            GearSlotPanel ammoPanel = new GearSlotPanel(GearSwapperPanel.GearSlot.AMMO);

            slotPanels.put(GearSwapperPanel.GearSlot.RING, ringPanel);
            slotPanels.put(GearSwapperPanel.GearSlot.AMMO, ammoPanel);

            bottomRow.add(ringPanel);
            bottomRow.add(ammoPanel);

            slotsGrid.add(bottomRow, gbc);

            mainPanel.add(slotsLabel, BorderLayout.NORTH);
            mainPanel.add(slotsGrid, BorderLayout.CENTER);

            return mainPanel;
        }

        private void addSlotToGrid(JPanel grid, GridBagConstraints gbc, GearSwapperPanel.GearSlot slot, int x, int y) {
            gbc.gridx = x;
            gbc.gridy = y;
            gbc.gridwidth = 1;

            GearSlotPanel slotPanel = new GearSlotPanel(slot);
            slotPanels.put(slot, slotPanel);
            grid.add(slotPanel, gbc);
        }

        private void selectGearSet() {
            String selectedName = (String) gearSetSelector.getSelectedItem();
            if (selectedName != null) {
                currentGearSet = gearSets.get(selectedName);
                updateGearSlotsDisplay();
            }
        }

        private void createNewGearSet() {
            String name = newGearSetName.getText().trim();
            if (name.isEmpty() || name.equals("New gear set name...")) {
                return;
            }

            if (gearSets.containsKey(name)) {
                return;
            }

            GearSwapperPanel.GearSet newSet = new GearSwapperPanel.GearSet(name);
            gearSets.put(name, newSet);
            currentGearSet = newSet;

            updateGearSetSelector();
            updateGearSlotsDisplay();
            saveGearSetToConfig(name);
            newGearSetName.setText("");

            System.out.println("[Gear Swapper DEBUG] Created new gear set: " + name);
        }

        private void exportCommands() {
            if (currentGearSet == null)
                return;

            String commands = currentGearSet.generateEquipCommands();

            // Copy to clipboard
            try {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(commands.trim()), null);
                System.out.println("[Gear Swapper DEBUG] Commands exported to clipboard");
            } catch (Exception e) {
                System.out.println("[Gear Swapper DEBUG] Clipboard copy failed: " + e.getMessage());
            }
        }

        private void updateGearSetSelector() {
            gearSetSelector.removeAllItems();
            for (String name : gearSets.keySet()) {
                gearSetSelector.addItem(name);
            }
            if (currentGearSet != null) {
                gearSetSelector.setSelectedItem(currentGearSet.name);
            }
        }

        private void saveCurrentGearSet() {
            if (currentGearSet == null)
                return;

            // Update current gear set from slot panels
            for (Map.Entry<GearSwapperPanel.GearSlot, GearSlotPanel> entry : slotPanels.entrySet()) {
                GearSwapperPanel.GearSlot slot = entry.getKey();
                GearSlotPanel panel = entry.getValue();
                if (panel.getItem() != null) {
                    currentGearSet.setItem(slot, panel.getItem());
                }
            }

            saveGearSetToConfig(currentGearSet.name);
        }

        private void deleteCurrentGearSet() {
            if (currentGearSet == null)
                return;

            String name = currentGearSet.name;
            gearSets.remove(name);
            deleteGearSetFromConfig(name);

            currentGearSet = null;
            updateGearSetSelector();
            updateGearSlotsDisplay();
        }

        private void updateGearSlotsDisplay() {
            for (Map.Entry<GearSwapperPanel.GearSlot, GearSlotPanel> entry : slotPanels.entrySet()) {
                GearSwapperPanel.GearSlot slot = entry.getKey();
                GearSlotPanel panel = entry.getValue();

                GearSwapperPanel.ItemData item = null;
                if (currentGearSet != null) {
                    item = currentGearSet.getItem(slot);
                }
                panel.setItem(item);
            }
        }

        private void loadSavedGearSets() {
            if (configManager == null)
                return;

            String gearSetNames = configManager.getConfiguration(GearSwapperConfig.GROUP, "gearSetNames");
            if (gearSetNames != null && !gearSetNames.trim().isEmpty()) {
                String[] names = gearSetNames.split(",");
                for (String name : names) {
                    if (!name.trim().isEmpty()) {
                        GearSwapperPanel.GearSet gearSet = loadGearSetFromConfig(name.trim());
                        if (gearSet != null) {
                            gearSets.put(name.trim(), gearSet);
                        }
                    }
                }
            }

            updateGearSetSelector();
        }

        private GearSwapperPanel.GearSet loadGearSetFromConfig(String name) {
            GearSwapperPanel.GearSet gearSet = new GearSwapperPanel.GearSet(name);

            for (GearSwapperPanel.GearSlot slot : GearSwapperPanel.GearSlot.values()) {
                String itemName = configManager.getConfiguration(GearSwapperConfig.GROUP,
                        "gearset_" + name + "_" + slot.name());
                if (itemName != null && !itemName.trim().isEmpty()) {
                    String equipCommand = configManager.getConfiguration(GearSwapperConfig.GROUP,
                            "gearset_" + name + "_" + slot.name() + "_command");
                    GearSwapperPanel.ItemData item = new GearSwapperPanel.ItemData(itemName, null, equipCommand);
                    gearSet.setItem(slot, item);
                }
            }

            return gearSet;
        }

        private void saveGearSetToConfig(String name) {
            if (configManager == null)
                return;

            GearSwapperPanel.GearSet gearSet = gearSets.get(name);
            if (gearSet == null)
                return;

            // Save gear set name to list
            String gearSetNames = configManager.getConfiguration(GearSwapperConfig.GROUP, "gearSetNames");
            if (gearSetNames == null || !gearSetNames.contains(name)) {
                if (gearSetNames == null || gearSetNames.trim().isEmpty()) {
                    gearSetNames = name;
                } else {
                    gearSetNames += "," + name;
                }
                configManager.setConfiguration(GearSwapperConfig.GROUP, "gearSetNames", gearSetNames);
            }

            // Save individual items
            for (Map.Entry<GearSwapperPanel.GearSlot, GearSwapperPanel.ItemData> entry : gearSet.items.entrySet()) {
                GearSwapperPanel.GearSlot slot = entry.getKey();
                GearSwapperPanel.ItemData item = entry.getValue();

                configManager.setConfiguration(GearSwapperConfig.GROUP,
                        "gearset_" + name + "_" + slot.name(), item.name);
                configManager.setConfiguration(GearSwapperConfig.GROUP,
                        "gearset_" + name + "_" + slot.name() + "_command", item.equipCommand);
            }
        }

        private void deleteGearSetFromConfig(String name) {
            if (configManager == null)
                return;

            // Remove from gear set names list
            String gearSetNames = configManager.getConfiguration(GearSwapperConfig.GROUP, "gearSetNames");
            if (gearSetNames != null && gearSetNames.contains(name)) {
                String[] names = gearSetNames.split(",");
                StringBuilder newNames = new StringBuilder();
                for (String n : names) {
                    if (!n.trim().equals(name) && !n.trim().isEmpty()) {
                        if (newNames.length() > 0)
                            newNames.append(",");
                        newNames.append(n.trim());
                    }
                }
                if (newNames.length() > 0) {
                    configManager.setConfiguration(GearSwapperConfig.GROUP, "gearSetNames", newNames.toString());
                } else {
                    configManager.unsetConfiguration(GearSwapperConfig.GROUP, "gearSetNames");
                }
            }

            // Remove individual item configs
            for (GearSwapperPanel.GearSlot slot : GearSwapperPanel.GearSlot.values()) {
                configManager.unsetConfiguration(GearSwapperConfig.GROUP,
                        "gearset_" + name + "_" + slot.name());
                configManager.unsetConfiguration(GearSwapperConfig.GROUP,
                        "gearset_" + name + "_" + slot.name() + "_command");
            }
        }

        // Individual gear slot panel
        private class GearSlotPanel extends JPanel {
            private GearSwapperPanel.GearSlot slot;
            private GearSwapperPanel.ItemData item;
            private JLabel itemLabel;
            private JLabel itemImageLabel;

            public GearSlotPanel(GearSwapperPanel.GearSlot slot) {
                this.slot = slot;
                setLayout(new BorderLayout());
                setPreferredSize(new Dimension(100, 100));
                setMaximumSize(new Dimension(100, 100));
                setBackground(new Color(52, 53, 58));
                Border border = BorderFactory.createLineBorder(new Color(75, 77, 83));
                setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(4, 4, 4, 4)));

                // Slot name
                JLabel nameLabel = new JLabel(slot.getDisplayName());
                nameLabel.setFont(new Font("Whitney", Font.BOLD, 10));
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // Item image
                itemImageLabel = new JLabel();
                itemImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                itemImageLabel.setVerticalAlignment(SwingConstants.CENTER);
                itemImageLabel.setPreferredSize(new Dimension(60, 60));

                // Item name
                itemLabel = new JLabel("Empty");
                itemLabel.setFont(new Font("Whitney", Font.PLAIN, 8));
                itemLabel.setForeground(new Color(160, 170, 185));
                itemLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JPanel infoPanel = new JPanel(new BorderLayout());
                infoPanel.setOpaque(false);
                infoPanel.add(itemLabel, BorderLayout.CENTER);

                add(nameLabel, BorderLayout.NORTH);
                add(itemImageLabel, BorderLayout.CENTER);
                add(infoPanel, BorderLayout.SOUTH);

                // Add click listener
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectSlotForItem();
                    }
                });
            }

            public void setItem(GearSwapperPanel.ItemData item) {
                this.item = item;
                if (item != null) {
                    itemLabel.setText(item.name);
                    loadItemImage();
                } else {
                    itemLabel.setText("Empty");
                    itemImageLabel.setIcon(null);
                }
            }

            public GearSwapperPanel.ItemData getItem() {
                return item;
            }

            private void loadItemImage() {
                if (item == null)
                    return;

                // Load image in background thread
                new Thread(() -> {
                    try {
                        String imageUrl = item.getWikiImageUrl();
                        if (imageUrl != null) {
                            java.net.URL url = new java.net.URL(imageUrl);
                            BufferedImage image = javax.imageio.ImageIO.read(url);

                            // Scale image to fit
                            Image scaledImage = image.getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                            ImageIcon icon = new ImageIcon(scaledImage);

                            // Update UI on EDT
                            SwingUtilities.invokeLater(() -> {
                                itemImageLabel.setIcon(icon);
                            });
                        }
                    } catch (Exception e) {
                        // Image loading failed, use default icon
                    }
                }).start();
            }

            private void selectSlotForItem() {
                itemSearchPanel.setTargetSlot(slot);

                // Highlight selected slot
                for (GearSlotPanel panel : slotPanels.values()) {
                    if (panel == this) {
                        panel.setBackground(new Color(76, 175, 80)); // Green highlight
                    } else {
                        panel.setBackground(new Color(52, 53, 58)); // Normal
                    }
                }
            }
        }

        // Item search panel with API integration and auto-complete
        private class ItemSearchPanel extends JPanel {
            private JTextField searchField;
            private JPanel resultsPanel;
            private GearSwapperPanel.GearSlot targetSlot;
            private JScrollPane scrollPane;
            private Timer searchTimer;
            private List<String> allItems = new ArrayList<>();
            private boolean apiLoaded = false;

            public ItemSearchPanel() {
                setLayout(new BorderLayout());
                setBorder(new EmptyBorder(0, 12, 0, 0));

                // Search header
                JPanel searchHeader = new JPanel(new BorderLayout());
                searchHeader.setOpaque(false);
                searchHeader.setBorder(new EmptyBorder(0, 0, 12, 0));

                JLabel searchLabel = new JLabel("🔍 Item Search (Auto-complete)");
                searchLabel.setFont(new Font("Whitney", Font.BOLD, 12));
                searchLabel.setForeground(new Color(120, 190, 255));

                searchField = new JTextField("Search items...");
                searchField.setBackground(new Color(52, 53, 58));
                searchField.setForeground(new Color(160, 170, 185));
                searchField.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
                searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                searchField.setFont(new Font("Whitney", Font.PLAIN, 10));

                // Auto-complete with timer for real-time search
                searchTimer = new Timer(100, e -> performSearch()); // 100ms delay for instant feel
                searchTimer.setRepeats(false);

                searchField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        scheduleSearch();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        scheduleSearch();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        scheduleSearch();
                    }

                    private void scheduleSearch() {
                        searchTimer.stop();
                        searchTimer.start();
                    }
                });

                searchField.addKeyListener(new KeyListener() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            performSearch();
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                    }

                    @Override
                    public void keyTyped(KeyEvent e) {
                    }
                });

                JButton searchBtn = new JButton("Search");
                searchBtn.setBackground(new Color(76, 175, 80));
                searchBtn.setForeground(Color.WHITE);
                searchBtn.setFocusPainted(false);
                searchBtn.setPreferredSize(new Dimension(70, 28));
                searchBtn.setMaximumSize(new Dimension(70, 28));
                searchBtn.setFont(new Font("Whitney", Font.BOLD, 10));
                searchBtn.addActionListener(e -> performSearch());

                JPanel searchRow = new JPanel(new BorderLayout());
                searchRow.setOpaque(false);
                searchRow.setBorder(new EmptyBorder(8, 0, 0, 0));
                searchRow.add(searchField, BorderLayout.CENTER);
                searchRow.add(searchBtn, BorderLayout.EAST);

                searchHeader.add(searchLabel, BorderLayout.NORTH);
                searchHeader.add(searchRow, BorderLayout.CENTER);

                // Results panel
                resultsPanel = new JPanel();
                resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
                resultsPanel.setOpaque(false);

                scrollPane = new JScrollPane(resultsPanel);
                scrollPane.setOpaque(false);
                scrollPane.getViewport().setOpaque(false);
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                add(searchHeader, BorderLayout.NORTH);
                add(scrollPane, BorderLayout.CENTER);

                // Load items from API in background
                loadItemsFromAPI();

                // Show loading message
                showLoadingMessage();
            }

            private void showLoadingMessage() {
                resultsPanel.removeAll();
                JLabel loadingLabel = new JLabel("Loading OSRS items database...");
                loadingLabel.setFont(new Font("Whitney", Font.ITALIC, 11));
                loadingLabel.setForeground(new Color(160, 170, 185));
                loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
                resultsPanel.add(loadingLabel);
                resultsPanel.revalidate();
                resultsPanel.repaint();
            }

            private void loadItemsFromAPI() {
                // Load items in background thread
                new Thread(() -> {
                    try {
                        System.out.println("[Gear Swapper DEBUG] Loading OSRS items from API...");

                        // Use OSRS Wiki API with opensearch for better item search
                        String apiUrl = "https://oldschool.runescape.wiki/api.php?action=opensearch&format=json&namespace=0&limit=500";
                        List<String> apiItems = new ArrayList<>();

                        // Search for common equipment prefixes to get comprehensive coverage
                        String[] searchTerms = {
                                "dragon", "rune", "adamant", "mithril", "black", "steel", "iron", "bronze",
                                "bandos", "armadyl", "saradomin", "zamorak", "guthix", "barrows", "ancient",
                                "torva", "pernix", "virtus", "ancestral", "masori", "justiciar", "inquisitor",
                                "amulet", "ring", "necklace", "cape", "gloves", "boots", "helmet", "chest",
                                "legs", "shield", "sword", "axe", "bow", "crossbow", "staff", "wand",
                                "arrows", "bolts", "darts", "potion", "rune", "crystal", "graceful"
                        };

                        for (String searchTerm : searchTerms) {
                            try {
                                String searchUrl = apiUrl + "&search="
                                        + java.net.URLEncoder.encode(searchTerm, "UTF-8");
                                String jsonText = readUrl(searchUrl);
                                List<String> searchResults = parseOpenSearchResults(jsonText);

                                System.out.println("[Gear Swapper DEBUG] Search '" + searchTerm + "' found "
                                        + searchResults.size() + " items");
                                apiItems.addAll(searchResults);

                                // Small delay to be respectful to the API
                                Thread.sleep(100);

                            } catch (Exception e) {
                                System.out.println("[Gear Swapper DEBUG] Search for '" + searchTerm + "' failed: "
                                        + e.getMessage());
                                // Continue with other search terms
                            }
                        }

                        // Remove duplicates and filter for equipment items only
                        allItems.clear();
                        for (String item : new LinkedHashSet<>(apiItems)) {
                            if (isEquipmentItem(item)) {
                                allItems.add(item);
                            }
                        }

                        // Sort items alphabetically
                        allItems.sort(String.CASE_INSENSITIVE_ORDER);

                        System.out.println(
                                "[Gear Swapper DEBUG] Loaded " + allItems.size() + " unique equipment items from API");

                        // Update UI on EDT
                        SwingUtilities.invokeLater(() -> {
                            apiLoaded = true;
                            loadPopularItems(); // Show popular items first
                        });

                    } catch (Exception e) {
                        System.out.println("[Gear Swapper DEBUG] Failed to load API items: " + e.getMessage());
                        // Fallback to static item list
                        SwingUtilities.invokeLater(() -> {
                            apiLoaded = false;
                            loadPopularItems();
                        });
                    }
                }).start();
            }

            private List<String> parseOpenSearchResults(String json) {
                List<String> items = new ArrayList<>();

                try {
                    // OpenSearch returns JSON array with [search terms, item titles, descriptions,
                    // urls]
                    // We want the second element (item titles)
                    if (json.startsWith("[")) {
                        // Parse the JSON array manually
                        int firstBracket = json.indexOf('[');
                        int secondBracket = json.indexOf('[', firstBracket + 1);
                        int endBracket = json.indexOf(']', secondBracket);

                        if (secondBracket > 0 && endBracket > secondBracket) {
                            String titlesSection = json.substring(secondBracket + 1, endBracket);

                            // Extract quoted strings
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");
                            java.util.regex.Matcher matcher = pattern.matcher(titlesSection);

                            while (matcher.find()) {
                                String title = matcher.group(1);
                                // Skip non-item pages
                                if (!title.contains(":") && !title.contains("(") && !title.contains("Category") &&
                                        !title.contains("File") && !title.contains("Help")
                                        && !title.contains("Template") &&
                                        !title.contains("Project") && !title.contains("User") && !title.contains("Talk")
                                        &&
                                        !title.contains("Special") && !title.contains("MediaWiki") &&
                                        title.length() > 2 && title.length() < 50) {
                                    items.add(title);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Gear Swapper DEBUG] OpenSearch JSON parsing error: " + e.getMessage());
                }

                return items;
            }

            private String readUrl(String urlString) throws Exception {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "VitaLite-GearSwapper/1.0");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return response.toString();
            }

            private boolean isEquipmentItem(String itemName) {
                String lowerName = itemName.toLowerCase();

                // Equipment keywords
                String[] equipmentKeywords = {
                        "sword", "axe", "mace", "warhammer", "dagger", "scimitar", "longsword", "battleaxe",
                        "halberd", "maul", "claws", "whip", "flail", "spear", "hasta", "javelin", "dart",
                        "knife", "thrownaxe", "bow", "crossbow", "shield", "kiteshield", "defender",
                        "helmet", "helm", "coif", "hat", "hood", "mask", "cowl", "tiara", "circlet",
                        "amulet", "necklace", "stole", "scarf", "cape", "cloak", "mantle", "tunic",
                        "body", "top", "shirt", "robe", "platebody", "chainbody", "leathertop", "chestplate",
                        "platelegs", "plateskirt", "chaps", "legs", "skirt", "robe bottom", "tassets",
                        "gloves", "gauntlets", "vambraces", "bracers", "boots", "shoes", "sandals",
                        "ring", "bracelet", "bangles", "arrows", "bolts", "darts", "javelins", "throwing",
                        "staff", "wand", "orb", "book", "prayer", "holy", "unholy", "ancient", "elemental",
                        "dragon", "rune", "adamant", "mithril", "black", "steel", "iron", "bronze",
                        "bandos", "armadyl", "saradomin", "zamorak", "guthix", "ancient", "barrows",
                        "third age", "3rd age", "guthix", "saradomin", "zamorak", "void", "elite",
                        "pernix", "torva", "virtus", "ancestral", "masori", "justiciar", "inquisitor",
                        "ghrazi", "elysian", "spectral", "arcane", "blessed", "spirit", "crystal",
                        "toxic", "twisted", "elder", "kodai", "master", "occult", "torture", "anguish",
                        "fury", "glory", "dueling", "wealth", "recoil", "life", "forging", "strength",
                        "magic", "power", "accuracy", "defence", "suffering", "endurance", "vigour",
                        "shadows", "gods", "berserker", "warrior", "archers", "seers", "tyrannical",
                        "treasonous", "primordial", "pegasian", "eternal", "infinity", "mystic",
                        "splitbark", "infinity", "dragonhide", "blessed", "karil", "ahrim", "dharok",
                        "guthan", "torag", "verac", "akrisae", "slayer", "fighter", "ranger", "mage",
                        "fire", "infernal", "obsidian", "mythical", "ava's", "accumulator", "assembler",
                        "attractor", "max", "skill", "accomplishment", "team", "legend's", "fremennik",
                        "desert", "dwarven", "climbing", "spiked", "brimstone", "stone", "frog",
                        "leather", "recipe", "regen", "combat", "explorer", "karamja", "silence",
                        "dragonfire", "anti-dragon", "ancient", "toktz", "tzhaar", "bark", "snakeskin",
                        "proselyte", "initiate", "brother", "gilded", "trimmed", "heraldic", "cannon",
                        "multicannon", "graceful", "agile", "trailblazer", "league", "holiday", "event",
                        "birthday", "christmas", "easter", "halloween", "yew", "magic", "maple", "willow",
                        "oak", "teak", "mahogany", "magic", "redwood", "yew", "magic shortbow", "magic longbow",
                        "yew shortbow", "yew longbow", "maple shortbow", "maple longbow", "willow shortbow",
                        "willow longbow", "oak shortbow", "oak longbow", "teak shortbow", "teak longbow",
                        "mahogany shortbow", "mahogany longbow", "redwood shortbow", "redwood longbow"
                };

                for (String keyword : equipmentKeywords) {
                    if (lowerName.contains(keyword)) {
                        return true;
                    }
                }

                return false;
            }

            public void setTargetSlot(GearSwapperPanel.GearSlot slot) {
                this.targetSlot = slot;

                JLabel slotLabel = new JLabel("Selected: " + slot.getDisplayName());
                slotLabel.setFont(new Font("Whitney", Font.BOLD, 10));
                slotLabel.setForeground(new Color(255, 152, 0));
                slotLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

                // Remove old slot label if exists
                for (Component comp : getComponents()) {
                    if (comp instanceof JLabel && ((JLabel) comp).getText().startsWith("Selected:")) {
                        remove(comp);
                        break;
                    }
                }

                add(slotLabel, BorderLayout.SOUTH);
                revalidate();
                repaint();
            }

            private void performSearch() {
                String query = searchField.getText().trim();
                if (query.isEmpty() || query.equals("Search items...")) {
                    if (apiLoaded) {
                        loadPopularItems();
                    }
                    return;
                }

                // For very short queries (1-2 chars), show popular items that match
                if (query.length() <= 2 && apiLoaded) {
                    showQuickMatches(query);
                    return;
                }

                // For longer queries, search in real-time from OSRS Wiki API
                searchItemsFromAPI(query);
            }

            private void searchItemsFromAPI(String query) {
                // Search both our pre-loaded items AND the live API
                new Thread(() -> {
                    try {
                        // First, show results from our pre-loaded database (instant)
                        List<String> localResults = new ArrayList<>();
                        for (String item : allItems) {
                            if (item.toLowerCase().contains(query.toLowerCase())) {
                                localResults.add(item);
                            }
                        }

                        // Display local results immediately
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("[Gear Swapper DEBUG] Local search '" + query + "' found "
                                    + localResults.size() + " results");
                            displayItems(localResults.toArray(new String[0]));
                        });

                        // Then, search the live OSRS Wiki API for more comprehensive results
                        try {
                            String apiUrl = "https://oldschool.runescape.wiki/api.php?action=opensearch&format=json&namespace=0&limit=50&search="
                                    +
                                    java.net.URLEncoder.encode(query, "UTF-8");
                            String jsonText = readUrl(apiUrl);
                            List<String> apiResults = parseOpenSearchResults(jsonText);

                            // Filter API results for equipment items only
                            List<String> filteredApiResults = new ArrayList<>();
                            for (String item : apiResults) {
                                if (isEquipmentItem(item) && !localResults.contains(item)) {
                                    filteredApiResults.add(item);
                                }
                            }

                            // Combine local and API results, removing duplicates
                            List<String> combinedResults = new ArrayList<>(localResults);
                            combinedResults.addAll(filteredApiResults);

                            // Display combined results
                            SwingUtilities.invokeLater(() -> {
                                System.out.println("[Gear Swapper DEBUG] API search '" + query + "' found "
                                        + filteredApiResults.size() + " additional results");
                                System.out.println("[Gear Swapper DEBUG] Combined search '" + query + "' found "
                                        + combinedResults.size() + " total results");
                                displayItems(combinedResults.toArray(new String[0]));
                            });

                        } catch (Exception e) {
                            System.out.println(
                                    "[Gear Swapper DEBUG] API search failed for '" + query + "': " + e.getMessage());
                            // Local results are already displayed, so no action needed
                        }

                    } catch (Exception e) {
                        System.out.println("[Gear Swapper DEBUG] Search error for '" + query + "': " + e.getMessage());
                    }
                }).start();
            }

            private void showQuickMatches(String query) {
                List<String> quickMatches = new ArrayList<>();
                query = query.toLowerCase();

                // For 1-2 character queries, show items that start with that letter/sequence
                for (String item : allItems) {
                    String lowerItem = item.toLowerCase();
                    if (lowerItem.startsWith(query)) {
                        quickMatches.add(item);
                        if (quickMatches.size() >= 20)
                            break; // Limit to 20 for quick display
                    }
                }

                // If no starts-with matches, show contains matches
                if (quickMatches.isEmpty()) {
                    for (String item : allItems) {
                        if (item.toLowerCase().contains(query)) {
                            quickMatches.add(item);
                            if (quickMatches.size() >= 20)
                                break;
                        }
                    }
                }

                displayItems(quickMatches.toArray(new String[0]));
            }

            private void loadPopularItems() {
                if (!apiLoaded) {
                    // Fallback to popular items while API loads
                    String[] popularItems = {
                            "Dragon claws", "Abyssal whip", "Bandos godsword", "Armadyl godsword", "Saradomin godsword",
                            "Zamorak godsword",
                            "Dragon scimitar", "Dragon dagger", "Dragon mace", "Dragon longsword", "Dragon battleaxe",
                            "Dragon halberd",
                            "Abyssal bludgeon", "Saradomin sword", "Staff of the dead", "Trident of the seas",
                            "Trident of the swamp",
                            "Toxic blowpipe", "Magic shortbow", "Twisted bow", "Dragon hunter crossbow",
                            "Armadyl crossbow",
                            "Elder maul", "Granite maul", "Ghrazi rapier", "Scythe of vitur", "Inquisitor's mace",
                            "Bandos chestplate", "Bandos tassets", "Bandos boots", "Torva full helm", "Torva platebody",
                            "Torva platelegs",
                            "Pernix cowl", "Pernix body", "Pernix chaps", "Virtus mask", "Virtus robe top",
                            "Virtus robe legs",
                            "Dragon full helm", "Dragon chainbody", "Dragon platelegs", "Dragon boots",
                            "Dragon sq shield",
                            "Granite shield", "Blessed spirit shield", "Elysian spirit shield", "Arcane spirit shield",
                            "Spectral spirit shield",
                            "Justiciar faceguard", "Justiciar chestguard", "Justiciar legguards", "Ancestral robe top",
                            "Ancestral robe bottom",
                            "Armadyl helmet", "Armadyl chestplate", "Armadyl chainskirt", "Karil's coif",
                            "Karil's leathertop", "Karil's leatherskirt",
                            "Black d'hide body", "Black d'hide chaps", "Black d'hide vambraces", "Guthix chaps",
                            "Zamorak chaps", "Armadyl chaps",
                            "Dragonhide body", "Dragonhide chaps", "Dragonhide vambraces", "Blessed dragonhide body",
                            "Blessed dragonhide chaps",
                            "Masori mask", "Masori body", "Masori chaps", "Fremennik helm", "Fremennik shirt",
                            "Fremennik kilt",
                            "Ancestral hat", "Ancestral robe top", "Ancestral robe bottom", "Kodai wand", "Master wand",
                            "Ancient staff",
                            "Slayer's staff", "Staff of light", "Imbued god cape", "Imbued saradomin cape",
                            "Imbued guthix cape", "Imbued zamorak cape",
                            "3rd age mage hat", "3rd age robe top", "3rd age robe bottom", "Mystic hat",
                            "Mystic robe top", "Mystic robe bottom",
                            "Infinity hat", "Infinity top", "Infinity bottoms", "Splitbark helm", "Splitbark body",
                            "Splitbark gauntlets", "Splitbark boots",
                            "Amulet of fury", "Amulet of glory", "Amulet of torture", "Amulet of rancor",
                            "Amulet of the damned", "Amulet of avasice",
                            "Amulet of strength", "Amulet of magic", "Amulet of power", "Amulet of accuracy",
                            "Amulet of defence",
                            "Occult necklace", "Necklace of anguish", "Berserker necklace", "Salve amulet",
                            "Salve amulet(ei)",
                            "3rd age amulet", "Amulet of eternal glory", "Amulet of bounty", "Amulet of zealots",
                            "Fire cape", "Infernal cape", "Ardyne cloak", "Max cape", "Cape of accomplishment",
                            "Team cape",
                            "Obsidian cape", "Mythical cape", "Ava's assembler", "Ava's accumulator", "Ava's attractor",
                            "3rd age cloak", "Guthix cape", "Saradomin cape", "Zamorak cape", "Imbued god cape",
                            "Legend's cape", "Skill cape", "Fremennik cloak", "Desert cape", "Dwarven cloak",
                            "Ring of dueling", "Ring of recoil", "Ring of wealth", "Ring of forging", "Ring of life",
                            "Berserker ring", "Warrior ring", "Archers ring", "Seers ring", "Tyrannical ring",
                            "Treasonous ring",
                            "Ring of the gods", "Ring of suffering", "Ring of endurance", "Ring of vigour",
                            "Ring of shadows",
                            "3rd age ring", "Dragonstone ring", "Emerald ring", "Ruby ring", "Diamond ring",
                            "Onyx ring",
                            "Barrows gloves", "Dragon gloves", "Rune gloves", "Adamant gloves", "Mithril gloves",
                            "Black gloves",
                            "Steel gloves", "Iron gloves", "Bronze gloves", "Leather gloves", "Gloves of silence",
                            "Recipe for disaster",
                            "Regen bracelet", "Combat bracelet", "Amulet of eternal glory", "Explorer's ring",
                            "Karamja gloves",
                            "Dragon boots", "Bandos boots", "Primordial boots", "Pegasian boots", "Eternal boots",
                            "Infinity boots",
                            "Rune boots", "Adamant boots", "Mithril boots", "Black boots", "Steel boots", "Iron boots",
                            "Bronze boots",
                            "Climbing boots", "Frog leather boots", "Spiked manacles", "Boots of brimstone",
                            "Boots of stone",
                            "Dragon defender", "Avernic defender", "Rune defender", "Adamant defender",
                            "Mithril defender", "Black defender",
                            "Steel defender", "Iron defender", "Bronze defender", "Toktz-ket-xil", "Dragon kiteshield",
                            "Rune kiteshield",
                            "Adamant kiteshield", "Mithril kiteshield", "Black kiteshield", "Steel kiteshield",
                            "Iron kiteshield",
                            "Spirit shield", "Blessed spirit shield", "Arcane spirit shield", "Spectral spirit shield",
                            "Elysian spirit shield",
                            "Dragon arrows", "Rune arrows", "Adamant arrows", "Mithril arrows", "Broad arrows",
                            "Amethyst arrows",
                            "Dragon darts", "Rune darts", "Adamant darts", "Mithril darts", "Broad darts",
                            "Amethyst darts",
                            "Dragon bolts", "Rune bolts", "Adamant bolts", "Mithril bolts", "Broad bolts",
                            "Diamond bolts",
                            "Ruby bolts", "Emerald bolts", "Sapphire bolts", "Onyx bolts", "Dragonstone bolts",
                            "Toktz-xil-ul", "Toktz-mej-tal", "Toktz-ket-xil", "Toktz-zot-kal", "Toktz-ket-om",
                            "Barrows gloves", "Fighter torso", "Dragonfire shield", "Anti-dragon shield",
                            "Ancient shield",
                            "Void knight top", "Void knight robe", "Void knight gloves", "Void mage helm",
                            "Void ranger helm",
                            "Elite void top", "Elite void robe", "Void knight seal", "Void knight mace",
                            "Void knight maul",
                            "Shark", "Monkfish", "Karambwan", "Anglerfish", "Dark crab", "Manta ray",
                            "Super combat potion", "Bastion potion", "Divine ranging potion", "Divine magic potion",
                            "Super restore", "Prayer potion", "Sanfew serum", "Antidote+", "Anti-venom",
                            "Air rune", "Water rune", "Earth rune", "Fire rune", "Mind rune", "Body rune",
                            "Cosmic rune",
                            "Chaos rune", "Nature rune", "Law rune", "Death rune", "Blood rune", "Soul rune",
                            "Astral rune",
                            "Dust rune", "Lava rune", "Mist rune", "Mud rune", "Smoke rune", "Steam rune",
                            "Dragon pickaxe", "Rune pickaxe", "Adamant pickaxe", "Mithril pickaxe", "Black pickaxe",
                            "Dragon axe", "Rune axe", "Adamant axe", "Mithril axe", "Black axe", "Steel axe",
                            "Dragon harpoon", "Rune harpoon", "Adamant harpoon", "Mithril harpoon", "Infernal harpoon",
                            "Crystal harpoon", "Crystal pickaxe", "Crystal axe", "Crystal bow", "Crystal shield"
                    };
                    displayItems(popularItems);
                    return;
                }

                // Show first 50 items from API when loaded
                int maxItems = Math.min(50, allItems.size());
                String[] firstItems = allItems.subList(0, maxItems).toArray(new String[0]);
                displayItems(firstItems);
            }

            private void searchItems(String query) {
                List<String> results = new ArrayList<>();
                query = query.toLowerCase();

                // Search through all loaded items
                for (String item : allItems) {
                    if (item.toLowerCase().contains(query)) {
                        results.add(item);
                    }
                }

                // If no results from API, fallback to static list
                if (results.isEmpty() && !apiLoaded) {
                    String[] fallbackItems = {
                            "Dragon claws", "Abyssal whip", "Bandos godsword", "Armadyl godsword", "Saradomin godsword",
                            "Zamorak godsword",
                            "Dragon scimitar", "Dragon dagger", "Dragon mace", "Dragon longsword", "Dragon battleaxe",
                            "Dragon halberd",
                            "Abyssal bludgeon", "Saradomin sword", "Staff of the dead", "Trident of the seas",
                            "Trident of the swamp",
                            "Toxic blowpipe", "Magic shortbow", "Twisted bow", "Dragon hunter crossbow",
                            "Armadyl crossbow",
                            "Elder maul", "Granite maul", "Ghrazi rapier", "Scythe of vitur", "Inquisitor's mace",
                            "Bandos chestplate", "Bandos tassets", "Bandos boots", "Torva full helm", "Torva platebody",
                            "Torva platelegs",
                            "Pernix cowl", "Pernix body", "Pernix chaps", "Virtus mask", "Virtus robe top",
                            "Virtus robe legs",
                            "Dragon full helm", "Dragon chainbody", "Dragon platelegs", "Dragon boots",
                            "Dragon sq shield",
                            "Granite shield", "Blessed spirit shield", "Elysian spirit shield", "Arcane spirit shield",
                            "Spectral spirit shield",
                            "Justiciar faceguard", "Justiciar chestguard", "Justiciar legguards", "Ancestral robe top",
                            "Ancestral robe bottom",
                            "Armadyl helmet", "Armadyl chestplate", "Armadyl chainskirt", "Karil's coif",
                            "Karil's leathertop", "Karil's leatherskirt",
                            "Black d'hide body", "Black d'hide chaps", "Black d'hide vambraces", "Guthix chaps",
                            "Zamorak chaps", "Armadyl chaps",
                            "Dragonhide body", "Dragonhide chaps", "Dragonhide vambraces", "Blessed dragonhide body",
                            "Blessed dragonhide chaps",
                            "Masori mask", "Masori body", "Masori chaps", "Fremennik helm", "Fremennik shirt",
                            "Fremennik kilt",
                            "Ancestral hat", "Ancestral robe top", "Ancestral robe bottom", "Kodai wand", "Master wand",
                            "Ancient staff",
                            "Slayer's staff", "Staff of light", "Imbued god cape", "Imbued saradomin cape",
                            "Imbued guthix cape", "Imbued zamorak cape",
                            "3rd age mage hat", "3rd age robe top", "3rd age robe bottom", "Mystic hat",
                            "Mystic robe top", "Mystic robe bottom",
                            "Infinity hat", "Infinity top", "Infinity bottoms", "Splitbark helm", "Splitbark body",
                            "Splitbark gauntlets", "Splitbark boots",
                            "Amulet of fury", "Amulet of glory", "Amulet of torture", "Amulet of rancor",
                            "Amulet of the damned", "Amulet of avasice",
                            "Amulet of strength", "Amulet of magic", "Amulet of power", "Amulet of accuracy",
                            "Amulet of defence",
                            "Occult necklace", "Necklace of anguish", "Berserker necklace", "Salve amulet",
                            "Salve amulet(ei)",
                            "3rd age amulet", "Amulet of eternal glory", "Amulet of bounty", "Amulet of zealots",
                            "Fire cape", "Infernal cape", "Ardyne cloak", "Max cape", "Cape of accomplishment",
                            "Team cape",
                            "Obsidian cape", "Mythical cape", "Ava's assembler", "Ava's accumulator", "Ava's attractor",
                            "3rd age cloak", "Guthix cape", "Saradomin cape", "Zamorak cape", "Imbued god cape",
                            "Legend's cape", "Skill cape", "Fremennik cloak", "Desert cape", "Dwarven cloak",
                            "Ring of dueling", "Ring of recoil", "Ring of wealth", "Ring of forging", "Ring of life",
                            "Berserker ring", "Warrior ring", "Archers ring", "Seers ring", "Tyrannical ring",
                            "Treasonous ring",
                            "Ring of the gods", "Ring of suffering", "Ring of endurance", "Ring of vigour",
                            "Ring of shadows",
                            "3rd age ring", "Dragonstone ring", "Emerald ring", "Ruby ring", "Diamond ring",
                            "Onyx ring",
                            "Barrows gloves", "Dragon gloves", "Rune gloves", "Adamant gloves", "Mithril gloves",
                            "Black gloves",
                            "Steel gloves", "Iron gloves", "Bronze gloves", "Leather gloves", "Gloves of silence",
                            "Recipe for disaster",
                            "Regen bracelet", "Combat bracelet", "Amulet of eternal glory", "Explorer's ring",
                            "Karamja gloves",
                            "Dragon boots", "Bandos boots", "Primordial boots", "Pegasian boots", "Eternal boots",
                            "Infinity boots",
                            "Rune boots", "Adamant boots", "Mithril boots", "Black boots", "Steel boots", "Iron boots",
                            "Bronze boots",
                            "Climbing boots", "Frog leather boots", "Spiked manacles", "Boots of brimstone",
                            "Boots of stone",
                            "Dragon defender", "Avernic defender", "Rune defender", "Adamant defender",
                            "Mithril defender", "Black defender",
                            "Steel defender", "Iron defender", "Bronze defender", "Toktz-ket-xil", "Dragon kiteshield",
                            "Rune kiteshield",
                            "Adamant kiteshield", "Mithril kiteshield", "Black kiteshield", "Steel kiteshield",
                            "Iron kiteshield",
                            "Spirit shield", "Blessed spirit shield", "Arcane spirit shield", "Spectral spirit shield",
                            "Elysian spirit shield",
                            "Dragon arrows", "Rune arrows", "Adamant arrows", "Mithril arrows", "Broad arrows",
                            "Amethyst arrows",
                            "Dragon darts", "Rune darts", "Adamant darts", "Mithril darts", "Broad darts",
                            "Amethyst darts",
                            "Dragon bolts", "Rune bolts", "Adamant bolts", "Mithril bolts", "Broad bolts",
                            "Diamond bolts",
                            "Ruby bolts", "Emerald bolts", "Sapphire bolts", "Onyx bolts", "Dragonstone bolts",
                            "Toktz-xil-ul", "Toktz-mej-tal", "Toktz-ket-xil", "Toktz-zot-kal", "Toktz-ket-om",
                            "Barrows gloves", "Fighter torso", "Dragonfire shield", "Anti-dragon shield",
                            "Ancient shield",
                            "Void knight top", "Void knight robe", "Void knight gloves", "Void mage helm",
                            "Void ranger helm",
                            "Elite void top", "Elite void robe", "Void knight seal", "Void knight mace",
                            "Void knight maul",
                            "Shark", "Monkfish", "Karambwan", "Anglerfish", "Dark crab", "Manta ray",
                            "Super combat potion", "Bastion potion", "Divine ranging potion", "Divine magic potion",
                            "Super restore", "Prayer potion", "Sanfew serum", "Antidote+", "Anti-venom",
                            "Air rune", "Water rune", "Earth rune", "Fire rune", "Mind rune", "Body rune",
                            "Cosmic rune",
                            "Chaos rune", "Nature rune", "Law rune", "Death rune", "Blood rune", "Soul rune",
                            "Astral rune",
                            "Dust rune", "Lava rune", "Mist rune", "Mud rune", "Smoke rune", "Steam rune",
                            "Dragon pickaxe", "Rune pickaxe", "Adamant pickaxe", "Mithril pickaxe", "Black pickaxe",
                            "Dragon axe", "Rune axe", "Adamant axe", "Mithril axe", "Black axe", "Steel axe",
                            "Dragon harpoon", "Rune harpoon", "Adamant harpoon", "Mithril harpoon", "Infernal harpoon",
                            "Crystal harpoon", "Crystal pickaxe", "Crystal axe", "Crystal bow", "Crystal shield"
                    };

                    for (String item : fallbackItems) {
                        if (item.toLowerCase().contains(query)) {
                            results.add(item);
                        }
                    }
                }

                System.out.println("[Gear Swapper DEBUG] Search '" + query + "' found " + results.size() + " results");
                displayItems(results.toArray(new String[0]));
            }

            private void displayItems(String[] items) {
                resultsPanel.removeAll();

                for (String itemName : items) {
                    ItemResultPanel itemPanel = new ItemResultPanel(itemName);
                    resultsPanel.add(itemPanel);
                    resultsPanel.add(Box.createVerticalStrut(4));
                }

                resultsPanel.revalidate();
                resultsPanel.repaint();
            }

            // Individual item result panel
            private class ItemResultPanel extends JPanel {
                private String itemName;

                public ItemResultPanel(String itemName) {
                    this.itemName = itemName;
                    setLayout(new BorderLayout());
                    setBackground(new Color(45, 46, 50));
                    setBorder(new EmptyBorder(8, 12, 8, 12));
                    setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

                    // Item image
                    JLabel imageLabel = new JLabel();
                    imageLabel.setPreferredSize(new Dimension(40, 40));
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    loadItemImage(imageLabel);

                    // Item name
                    JLabel nameLabel = new JLabel(itemName);
                    nameLabel.setFont(new Font("Whitney", Font.BOLD, 11));
                    nameLabel.setForeground(Color.WHITE);

                    // Equip command
                    JLabel commandLabel = new JLabel("wear:" + itemName.toLowerCase());
                    commandLabel.setFont(new Font("Whitney", Font.PLAIN, 9));
                    commandLabel.setForeground(new Color(160, 170, 185));

                    JPanel infoPanel = new JPanel(new BorderLayout());
                    infoPanel.setOpaque(false);
                    infoPanel.add(nameLabel, BorderLayout.NORTH);
                    infoPanel.add(commandLabel, BorderLayout.CENTER);

                    add(imageLabel, BorderLayout.WEST);
                    add(infoPanel, BorderLayout.CENTER);

                    // Add click listener
                    addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            selectItem();
                        }
                    });

                    // Hover effect
                    addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            setBackground(new Color(76, 175, 80));
                        }

                        @Override
                        public void mouseExited(java.awt.event.MouseEvent e) {
                            setBackground(new Color(45, 46, 50));
                        }
                    });
                }

                private void loadItemImage(JLabel label) {
                    // Load image in background thread
                    new Thread(() -> {
                        try {
                            String formattedName = itemName.replace(" ", "_").replace("'", "");
                            String imageUrl = "https://oldschool.runescape.wiki/images/" + formattedName + ".png";
                            java.net.URL url = new java.net.URL(imageUrl);
                            BufferedImage image = javax.imageio.ImageIO.read(url);

                            // Scale image to fit
                            Image scaledImage = image.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                            ImageIcon icon = new ImageIcon(scaledImage);

                            // Update UI on EDT
                            SwingUtilities.invokeLater(() -> {
                                label.setIcon(icon);
                            });
                        } catch (Exception e) {
                            // Image loading failed
                        }
                    }).start();
                }

                private void selectItem() {
                    if (targetSlot == null)
                        return;

                    GearSwapperPanel.ItemData item = new GearSwapperPanel.ItemData(
                            itemName,
                            null,
                            "wear:" + itemName.toLowerCase());

                    // Update the slot panel
                    GearSlotPanel slotPanel = slotPanels.get(targetSlot);
                    if (slotPanel != null) {
                        slotPanel.setItem(item);
                    }

                    // Update current gear set
                    if (currentGearSet != null) {
                        currentGearSet.setItem(targetSlot, item);
                    }
                }
            }
        }
    }

    private void openRawTextEditor(JFrame parentFrame, JTextArea sourceArea, GearSwapperPanel.LoadoutData data,
            int loadoutNum, JTextArea inlineCommandsArea) {
        String currentScript = sourceArea != null && sourceArea.getText() != null ? sourceArea.getText() : "";

        JFrame rawFrame = new JFrame("Raw Text Editor - Loadout " + loadoutNum);
        rawFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rawFrame.setSize(700, 500);
        rawFrame.setLocationRelativeTo(parentFrame);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GhostTextArea editorArea = new GhostTextArea(currentScript);
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setFocusTraversalKeysEnabled(false);
        attachAutocomplete(editorArea);

        JScrollPane scroll = new JScrollPane(editorArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel header = new JLabel("Raw text editor for " + data.name + " (Loadout " + loadoutNum + ")");
        header.setFont(new Font("Whitney", Font.BOLD, 13));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton applyBtn = new JButton("Apply to Editor");
        applyBtn.setBackground(new Color(76, 175, 80));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setFocusPainted(false);
        applyBtn.addActionListener(e -> {
            String newText = editorArea.getText();
            data.items = newText;
            if (sourceArea != null) {
                sourceArea.setText(newText);
            }
            if (inlineCommandsArea != null) {
                inlineCommandsArea.setText(newText);
                updateTextAreaHeight(inlineCommandsArea);
            }
            saveLoadoutToConfig(loadoutNum);
            JOptionPane.showMessageDialog(rawFrame, "Changes applied to editor and data.");
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> rawFrame.dispose());

        buttonPanel.add(applyBtn);
        buttonPanel.add(closeBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(header);
        north.add(Box.createVerticalStrut(6));
        north.add(buttonPanel);

        panel.add(north, BorderLayout.NORTH);

        rawFrame.add(panel);
        rawFrame.setVisible(true);
    }

    private void openLooperRawTextEditor(JFrame parentFrame, JTextArea sourceArea, JTextArea inlineArea) {
        String currentScript = sourceArea != null && sourceArea.getText() != null ? sourceArea.getText() : "";

        JFrame rawFrame = new JFrame("Raw Text Editor - Looper Script");
        rawFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rawFrame.setSize(700, 500);
        rawFrame.setLocationRelativeTo(parentFrame);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GhostTextArea editorArea = new GhostTextArea(currentScript);
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setFocusTraversalKeysEnabled(false);

        JScrollPane scroll = new JScrollPane(editorArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel header = new JLabel("Raw text editor for global looper script");
        header.setFont(new Font("Whitney", Font.BOLD, 13));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton applyBtn = new JButton("Apply to Block Editor");
        applyBtn.setBackground(new Color(76, 175, 80));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setFocusPainted(false);
        applyBtn.addActionListener(e -> {
            String newText = editorArea.getText();
            if (plugin != null) {
                plugin.setLooperScript(newText);
            }
            if (sourceArea != null) {
                sourceArea.setText(newText);
            }
            if (inlineArea != null) {
                inlineArea.setText(newText);
                updateTextAreaHeight(inlineArea);
            }
            JOptionPane.showMessageDialog(rawFrame, "Changes applied to editor and data.");
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> rawFrame.dispose());

        buttonPanel.add(applyBtn);
        buttonPanel.add(closeBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(header);
        north.add(Box.createVerticalStrut(6));
        north.add(buttonPanel);

        panel.add(north, BorderLayout.NORTH);

        rawFrame.add(panel);
        rawFrame.setVisible(true);
    }

    // =================================================================================
    // UI Helpers
    // =================================================================================

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12) // Comfortable padding
        ));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Add subtle hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });

        return btn;
    }

    private JPanel createSectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        return panel;
    }

    private JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(Theme.PRIMARY);
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        return label;
    }

    private JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(Theme.TEXT_SECONDARY);
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(Theme.BACKGROUND);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return field;
    }
}
