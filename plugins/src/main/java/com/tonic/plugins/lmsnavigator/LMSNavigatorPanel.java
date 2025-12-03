package com.tonic.plugins.lmsnavigator;

import com.tonic.Logger;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LMSNavigatorPanel extends PluginPanel
{
    @Inject
    private LMSNavigatorPlugin plugin;
    
    private JTextArea statusArea;
    private JButton stopButton;
    
    // Final location buttons
    private JButton moserButton;
    private JButton debtorsButton;
    private JButton mountainButton;
    private JButton trinityButton;
    private JButton stoneCircleButton;
    private JButton pillarsButton;
    private JButton darkWarriorsButton;
    private JButton tradingPostButton;
    private JButton hutButton;
    private JButton blankTowerButton;
    private JButton townCityButton;
    private JButton oldManorButton;
    
    public LMSNavigatorPanel()
    {
        super();
        init();
    }
    
    public void setPlugin(LMSNavigatorPlugin plugin)
    {
        this.plugin = plugin;
        Logger.norm("[LMS Navigator Panel] Plugin reference set");
    }
    
    private void init()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        // Title
        JLabel title = new JLabel("LMS Navigator");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
        add(title, BorderLayout.NORTH);
        
        // Status area
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statusArea.setForeground(Color.WHITE);
        statusArea.setFont(FontManager.getRunescapeSmallFont());
        statusArea.setRows(8);
        statusArea.setColumns(20);
        statusArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
        
        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        // Title label
        JLabel titleLabel = new JLabel("LMS Locations");
        titleLabel.setFont(FontManager.getRunescapeFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(10, 10, 5, 10));
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Location buttons panel
        JPanel locationPanel = new JPanel();
        locationPanel.setLayout(new GridLayout(0, 2, 5, 5));
        locationPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        locationPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        // Create location buttons
        moserButton = createButton("Moser Settlement");
        debtorsButton = createButton("Debtors Hideout");
        mountainButton = createButton("The Mountain");
        trinityButton = createButton("Trinity Outpost");
        stoneCircleButton = createButton("Stone Circle");
        pillarsButton = createButton("Pillars of Sacrifice");
        darkWarriorsButton = createButton("Dark Warriors' Palace");
        tradingPostButton = createButton("Trading Post");
        hutButton = createButton("Hut");
        blankTowerButton = createButton("Blank Tower");
        townCityButton = createButton("Town Center");
        oldManorButton = createButton("Old Manor");
        
        // Add action listeners for location buttons
        moserButton.addActionListener(this::onMoserClicked);
        debtorsButton.addActionListener(this::onDebtorsClicked);
        mountainButton.addActionListener(this::onMountainClicked);
        trinityButton.addActionListener(this::onTrinityClicked);
        stoneCircleButton.addActionListener(this::onStoneCircleClicked);
        pillarsButton.addActionListener(this::onPillarsClicked);
        darkWarriorsButton.addActionListener(this::onDarkWarriorsClicked);
        tradingPostButton.addActionListener(this::onTradingPostClicked);
        hutButton.addActionListener(this::onHutClicked);
        blankTowerButton.addActionListener(this::onBlankTowerClicked);
        townCityButton.addActionListener(this::onTownCityClicked);
        oldManorButton.addActionListener(this::onOldManorClicked);
        
        // Add location buttons to panel
        locationPanel.add(moserButton);
        locationPanel.add(debtorsButton);
        locationPanel.add(mountainButton);
        locationPanel.add(trinityButton);
        locationPanel.add(stoneCircleButton);
        locationPanel.add(pillarsButton);
        locationPanel.add(darkWarriorsButton);
        locationPanel.add(tradingPostButton);
        locationPanel.add(hutButton);
        locationPanel.add(blankTowerButton);
        locationPanel.add(townCityButton);
        locationPanel.add(oldManorButton);
        
        // Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 1, 5, 5));
        controlPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        controlPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        
        stopButton = createButton("Stop Navigation");
        stopButton.addActionListener(this::onStopClicked);
        controlPanel.add(stopButton);
        
        // Add panels to content
        contentPanel.add(locationPanel, BorderLayout.CENTER);
        contentPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(contentPanel, BorderLayout.SOUTH);
        
        // Initial status
        updateStatus("LMS Navigator v" + LMSNavigatorPlugin.getVersion() + "\nReady for use\n\nSelect a location to navigate");
    }
    
    private JButton createButton(String text)
    {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        return button;
    }
    
    private void onMoserClicked(ActionEvent e)
    {
        navigateToLocation("Moser Settlement", new WorldPoint(3474, 5788, 0));
    }
    
    private void onDebtorsClicked(ActionEvent e)
    {
        navigateToLocation("Debtors Hideout", new WorldPoint(3404, 5802, 0));
    }
    
    private void onMountainClicked(ActionEvent e)
    {
        navigateToLocation("The Mountain", new WorldPoint(3430, 5845, 0));
    }
    
    private void onTrinityClicked(ActionEvent e)
    {
        navigateToLocation("Trinity Outpost", new WorldPoint(3500, 5870, 0));
    }
    
    private void onStoneCircleClicked(ActionEvent e)
    {
        navigateToLocation("Stone Circle", new WorldPoint(3663, 6061, 0));
    }
    
    private void onPillarsClicked(ActionEvent e)
    {
        navigateToLocation("Pillars of Sacrifice", new WorldPoint(3594, 6164, 0));
    }
    
    private void onDarkWarriorsClicked(ActionEvent e)
    {
        navigateToLocation("Dark Warriors' Palace", new WorldPoint(3546, 6158, 0));
    }
    
    private void onTradingPostClicked(ActionEvent e)
    {
        navigateToLocation("Trading Post", new WorldPoint(3506, 6162, 0));
    }
    
    private void onHutClicked(ActionEvent e)
    {
        navigateToLocation("Hut", new WorldPoint(3498, 6073, 0));
    }
    
    private void onBlankTowerClicked(ActionEvent e)
    {
        navigateToLocation("Blank Tower", new WorldPoint(3603, 6102, 0));
    }

    private void onTownCityClicked(ActionEvent e)
    {
        navigateToLocation("Town Center", new WorldPoint(3551, 6108, 0));
    }

    private void onOldManorClicked(ActionEvent e)
    {
        navigateToLocation("Old Manor", new WorldPoint(3497, 6117, 0));
    }
    
    private void navigateToLocation(String locationName, WorldPoint target)
    {
        Logger.norm("[LMS Navigator Panel] navigateToLocation called for: " + locationName);
        
        if (plugin == null)
        {
            Logger.error("[LMS Navigator Panel] ERROR: Plugin is null!");
            updateStatus("❌ ERROR: Plugin not available");
            return;
        }
        
        Logger.norm("[LMS Navigator Panel] Plugin reference is OK, calling navigateToTemplate");
        
        if (plugin.isNavigating())
        {
            updateStatus("✅ Updating target to:\n" + locationName + "\n" + target + "\n(continuing navigation)");
        }
        else
        {
            updateStatus("✅ Walking to:\n" + locationName + "\n" + target);
        }
        
        boolean success = plugin.navigateToTemplate(target);
        Logger.norm("[LMS Navigator Panel] navigateToTemplate returned: " + success);
        
        if (success)
        {
            if (plugin.isNavigating())
            {
                updateStatus("✅ Navigation active to:\n" + locationName + "\n" + target);
            }
            else
            {
                updateStatus("✅ Navigation started to:\n" + locationName + "\n" + target);
            }
        }
        else
        {
            updateStatus("❌ Failed to start navigation");
        }
    }
    
    private void onStopClicked(ActionEvent e)
    {
        if (plugin == null)
        {
            updateStatus("❌ ERROR: Plugin not available");
            return;
        }
        
        plugin.cancelNavigation();
        updateStatus("✅ Navigation stopped");
    }
    
    private void updateStatus(String message)
    {
        SwingUtilities.invokeLater(() -> {
            statusArea.setText(message);
            statusArea.setCaretPosition(0);
            Logger.norm("[LMS Navigator Panel] " + message.replace("\n", " | "));
        });
    }
}
