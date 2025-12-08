package com.tonic.plugins.gearswapper.ui.triggers;

import com.tonic.plugins.gearswapper.triggers.Trigger;
import com.tonic.plugins.gearswapper.triggers.TriggerEngine;
import com.tonic.plugins.gearswapper.triggers.TriggerEngineStats;
import com.tonic.plugins.gearswapper.triggers.TriggerType;
import net.runelite.client.ui.ColorScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main trigger management panel with enhanced error handling and thread safety
 */
public class TriggerPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(TriggerPanel.class);
    
    // UI Constants
    private static final Color BACKGROUND_COLOR = new Color(37, 38, 43);
    private static final Color TITLE_COLOR = new Color(120, 190, 255);
    private static final Color STATUS_ACTIVE_COLOR = new Color(120, 255, 120);
    private static final Color STATUS_PAUSED_COLOR = new Color(255, 200, 120);
    private static final Color BUTTON_STOP_COLOR = new Color(180, 52, 52);
    private static final Color BUTTON_START_COLOR = new Color(52, 128, 52);
    private static final Color EMPTY_STATE_COLOR = new Color(100, 110, 125);
    private static final int REFRESH_INTERVAL_MS = 1000;
    private static final int PANEL_WIDTH = 350;
    private static final int PANEL_HEIGHT = 300;
    
    // Core components
    private final TriggerEngine triggerEngine;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    
    // UI Components
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JButton toggleButton;
    private JButton addTriggerButton;
    private JButton refreshButton;
    private JLabel statsLabel;
    private JPanel triggersListPanel;
    private JScrollPane triggersScrollPane;

    public TriggerPanel(TriggerEngine triggerEngine)
    {
        // Validate input
        if (triggerEngine == null)
        {
            throw new IllegalArgumentException("TriggerEngine cannot be null");
        }
        
        this.triggerEngine = triggerEngine;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TriggerPanel-Refresh");
            t.setDaemon(true);
            return t;
        });
        
        try
        {
            initializeComponents();
            layoutComponents();
            setupEventHandlers();
            startAutoRefresh();
            refreshDisplay();
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error initializing panel: {}", e.getMessage(), e);
            dispose();
            throw new RuntimeException("Failed to initialize TriggerPanel", e);
        }
    }
    
    /**
     * Start automatic refresh of the display
     */
    private void startAutoRefresh()
    {
        scheduler.scheduleAtFixedRate(() -> {
            if (!disposed.get())
            {
                SwingUtilities.invokeLater(() -> {
                    if (!disposed.get())
                    {
                        try
                        {
                            refreshDisplay();
                        }
                        catch (Exception e)
                        {
                            logger.error("[Trigger Panel] Error during auto refresh: {}", e.getMessage(), e);
                        }
                    }
                });
            }
        }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Dispose of resources
     */
    public void dispose()
    {
        if (disposed.compareAndSet(false, true))
        {
            logger.debug("[Trigger Panel] Disposing panel");
            
            if (scheduler != null && !scheduler.isShutdown())
            {
                scheduler.shutdown();
                try
                {
                    if (!scheduler.awaitTermination(1, TimeUnit.SECONDS))
                    {
                        scheduler.shutdownNow();
                    }
                }
                catch (InterruptedException e)
                {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void initializeComponents()
    {
        try
        {
            // Title and status
            titleLabel = new JLabel("⚡ Triggers");
            titleLabel.setFont(new Font("Whitney", Font.BOLD, 14));
            titleLabel.setForeground(TITLE_COLOR);
            
            statusLabel = new JLabel("Status: Starting...");
            statusLabel.setFont(new Font("Whitney", Font.PLAIN, 11));
            statusLabel.setForeground(new Color(160, 170, 185));
            
            statsLabel = new JLabel("Events: 0 | Fired: 0/min | Active: 0");
            statsLabel.setFont(new Font("Whitney", Font.PLAIN, 9));
            statsLabel.setForeground(new Color(120, 130, 145));
            
            // Control buttons
            toggleButton = createStyledButton("Start", BUTTON_START_COLOR);
            toggleButton.setPreferredSize(new Dimension(55, 24));
            
            addTriggerButton = createStyledButton("+ New", TITLE_COLOR);
            addTriggerButton.setPreferredSize(new Dimension(75, 24));
            
            refreshButton = createStyledButton("🔄", TITLE_COLOR);
            refreshButton.setPreferredSize(new Dimension(40, 24));
            
            // Triggers list
            triggersListPanel = new JPanel();
            triggersListPanel.setLayout(new BoxLayout(triggersListPanel, BoxLayout.Y_AXIS));
            triggersListPanel.setBackground(BACKGROUND_COLOR);
            
            triggersScrollPane = new JScrollPane(triggersListPanel);
            triggersScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            triggersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            triggersScrollPane.setBackground(BACKGROUND_COLOR);
            triggersScrollPane.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
            triggersScrollPane.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error initializing components: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize components", e);
        }
    }
    
    /**
     * Create a styled button with consistent appearance
     */
    private JButton createStyledButton(String text, Color foregroundColor)
    {
        JButton button = new JButton(text);
        button.setFont(new Font("Whitney", Font.BOLD, 10));
        button.setBackground(new Color(52, 53, 58));
        button.setForeground(foregroundColor);
        button.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        return button;
    }

    private void layoutComponents()
    {
        try
        {
            setLayout(new BorderLayout());
            setBackground(BACKGROUND_COLOR);
            setBorder(new EmptyBorder(10, 10, 10, 10));
            
            // Header panel
            JPanel headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.setBackground(BACKGROUND_COLOR);
            headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
            
            // Title section
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titlePanel.setBackground(BACKGROUND_COLOR);
            titlePanel.add(titleLabel);
            
            // Controls section - moved below title
            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
            controlsPanel.setBackground(BACKGROUND_COLOR);
            controlsPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
            controlsPanel.add(toggleButton);
            controlsPanel.add(addTriggerButton);
            controlsPanel.add(refreshButton);
            
            headerPanel.add(titlePanel);
            headerPanel.add(controlsPanel);
            
            // Status panel
            JPanel statusPanel = new JPanel(new BorderLayout());
            statusPanel.setBackground(BACKGROUND_COLOR);
            statusPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
            statusPanel.add(statusLabel, BorderLayout.WEST);
            statusPanel.add(statsLabel, BorderLayout.EAST);
            
            // Add components to main panel
            add(headerPanel, BorderLayout.NORTH);
            add(statusPanel, BorderLayout.CENTER);
            add(triggersScrollPane, BorderLayout.SOUTH);
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error laying out components: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to layout components", e);
        }
    }

    private void setupEventHandlers()
    {
        try
        {
            toggleButton.addActionListener(e -> {
                try
                {
                    if (triggerEngine.isEnabled())
                    {
                        triggerEngine.stop();
                    }
                    else
                    {
                        triggerEngine.start();
                    }
                    refreshDisplay();
                }
                catch (Exception ex)
                {
                    logger.error("[Trigger Panel] Error toggling trigger engine: {}", ex.getMessage(), ex);
                    showErrorDialog("Failed to toggle trigger engine: " + ex.getMessage());
                }
            });
            
            addTriggerButton.addActionListener(e -> {
                try
                {
                    // Open trigger creation dialog
                    TriggerCreationDialog dialog = new TriggerCreationDialog(
                        (Frame) SwingUtilities.getWindowAncestor(TriggerPanel.this), 
                        triggerEngine
                    );
                    dialog.setVisible(true);
                    
                    // Refresh display after dialog closes to show new trigger
                    refreshDisplay();
                }
                catch (Exception ex)
                {
                    logger.error("[Trigger Panel] Error opening creation dialog: {}", ex.getMessage(), ex);
                    showErrorDialog("Failed to open trigger creation dialog: " + ex.getMessage());
                }
            });
            
            refreshButton.addActionListener(e -> {
                try
                {
                    refreshDisplay();
                }
                catch (Exception ex)
                {
                    logger.error("[Trigger Panel] Error refreshing display: {}", ex.getMessage(), ex);
                    showErrorDialog("Failed to refresh display: " + ex.getMessage());
                }
            });
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error setting up event handlers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to setup event handlers", e);
        }
    }
    
    /**
     * Show error dialog to user
     */
    private void showErrorDialog(String message)
    {
        SwingUtilities.invokeLater(() -> {
            try
            {
                JOptionPane.showMessageDialog(
                    this,
                    message,
                    "Trigger System Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
            catch (Exception e)
            {
                logger.error("[Trigger Panel] Error showing error dialog: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Refresh the display with current trigger engine state
     */
    public void refreshDisplay()
    {
        if (disposed.get())
        {
            return;
        }
        
        try
        {
            // Update status
            if (triggerEngine.isEnabled())
            {
                statusLabel.setText("Status: ✅ Active");
                statusLabel.setForeground(STATUS_ACTIVE_COLOR);
                toggleButton.setText("Stop");
                toggleButton.setBackground(BUTTON_STOP_COLOR);
            }
            else
            {
                statusLabel.setText("Status: ⏸ Paused");
                statusLabel.setForeground(STATUS_PAUSED_COLOR);
                toggleButton.setText("Start");
                toggleButton.setBackground(BUTTON_START_COLOR);
            }
            
            // Update stats
            TriggerEngineStats stats = triggerEngine.getStats();
            statsLabel.setText(String.format("Events: %d | Fired: %.1f/min | Active: %d", 
                                            stats.getTotalEventsProcessed(), 
                                            stats.getTriggersPerMinute(),
                                            stats.getActiveTriggerCount()));
            
            // Update triggers list
            refreshTriggersList();
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error refreshing display: {}", e.getMessage(), e);
            statusLabel.setText("Status: ❌ Error");
            statusLabel.setForeground(Color.RED);
        }
    }

    /**
     * Refresh the triggers list display
     */
    private void refreshTriggersList()
    {
        if (disposed.get() || triggersListPanel == null)
        {
            return;
        }
        
        try
        {
            triggersListPanel.removeAll();
            
            List<Trigger> triggers = triggerEngine.getActiveTriggers();
            
            if (triggers.isEmpty())
            {
                JLabel emptyLabel = new JLabel("No triggers configured");
                emptyLabel.setFont(new Font("Whitney", Font.ITALIC, 11));
                emptyLabel.setForeground(EMPTY_STATE_COLOR);
                emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
                triggersListPanel.add(emptyLabel);
            }
            else
            {
                for (Trigger trigger : triggers)
                {
                    if (trigger != null)
                    {
                        TriggerListItem item = new TriggerListItem(trigger, triggerEngine);
                        triggersListPanel.add(item);
                        triggersListPanel.add(Box.createVerticalStrut(5));
                    }
                }
            }
            
            triggersListPanel.revalidate();
            triggersListPanel.repaint();
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error refreshing triggers list: {}", e.getMessage(), e);
            
            // Show error state in the list
            triggersListPanel.removeAll();
            JLabel errorLabel = new JLabel("Error loading triggers");
            errorLabel.setFont(new Font("Whitney", Font.ITALIC, 11));
            errorLabel.setForeground(Color.RED);
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            errorLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
            triggersListPanel.add(errorLabel);
            
            triggersListPanel.revalidate();
            triggersListPanel.repaint();
        }
    }

    /**
     * Create a sample trigger for testing
     */
    public void createSampleTrigger()
    {
        if (disposed.get())
        {
            return;
        }
        
        try
        {
            // Create a sample animation trigger
            Trigger sampleTrigger = new Trigger("sample_001", "Sample Animation Trigger", TriggerType.ANIMATION);
            sampleTrigger.getConfig().setAnimationId(3242); // Dragon fire breath animation
            sampleTrigger.getConfig().setCooldownMs(5000); // 5 second cooldown
            sampleTrigger.getConfig().setOnlyInCombat(true);
            
            // Add a gear swap action
            com.tonic.plugins.gearswapper.triggers.GearSwapAction action = 
                new com.tonic.plugins.gearswapper.triggers.GearSwapAction("Anti-Fire Gear");
            sampleTrigger.addAction(action);
            
            triggerEngine.addTrigger(sampleTrigger);
            refreshDisplay();
            
            logger.info("[Trigger Panel] Created sample trigger: {}", sampleTrigger.getName());
        }
        catch (Exception e)
        {
            logger.error("[Trigger Panel] Error creating sample trigger: {}", e.getMessage(), e);
            showErrorDialog("Failed to create sample trigger: " + e.getMessage());
        }
    }
}
