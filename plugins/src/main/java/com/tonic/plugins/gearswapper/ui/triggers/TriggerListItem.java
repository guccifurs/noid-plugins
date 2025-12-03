package com.tonic.plugins.gearswapper.ui.triggers;

import com.tonic.plugins.gearswapper.triggers.Trigger;
import com.tonic.plugins.gearswapper.triggers.TriggerEngine;
import com.tonic.plugins.gearswapper.triggers.TriggerType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Individual trigger item in the triggers list
 */
public class TriggerListItem extends JPanel
{
    private final Trigger trigger;
    private final TriggerEngine triggerEngine;
    
    // UI Components
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel statusLabel;
    private JLabel statsLabel;
    private JToggleButton enabledButton;
    private JButton deleteButton;
    private JButton editButton;

    public TriggerListItem(Trigger trigger, TriggerEngine triggerEngine)
    {
        this.trigger = trigger;
        this.triggerEngine = triggerEngine;
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        updateDisplay();
    }

    private void initializeComponents()
    {
        // Labels
        nameLabel = new JLabel();
        nameLabel.setFont(new Font("Whitney", Font.BOLD, 11));
        nameLabel.setForeground(new Color(255, 255, 255));
        
        typeLabel = new JLabel();
        typeLabel.setFont(new Font("Whitney", Font.PLAIN, 10));
        typeLabel.setForeground(new Color(160, 170, 185));
        
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Whitney", Font.PLAIN, 9));
        statusLabel.setForeground(new Color(120, 130, 145));
        
        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Whitney", Font.PLAIN, 9));
        statsLabel.setForeground(new Color(100, 110, 125));
        
        // Buttons
        enabledButton = new JToggleButton();
        enabledButton.setFont(new Font("Whitney", Font.BOLD, 9));
        enabledButton.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
        enabledButton.setPreferredSize(new Dimension(60, 22));
        enabledButton.setFocusPainted(false);
        enabledButton.setContentAreaFilled(false);
        enabledButton.setOpaque(true);
        
        deleteButton = new JButton("✕");
        deleteButton.setFont(new Font("Whitney", Font.BOLD, 10));
        deleteButton.setBackground(new Color(37, 38, 43));
        deleteButton.setForeground(new Color(255, 120, 120));
        deleteButton.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
        deleteButton.setPreferredSize(new Dimension(30, 22));
        deleteButton.setFocusPainted(false);
        
        editButton = new JButton("✎");
        editButton.setFont(new Font("Whitney", Font.BOLD, 10));
        editButton.setBackground(new Color(37, 38, 43));
        editButton.setForeground(new Color(120, 190, 255));
        editButton.setBorder(BorderFactory.createLineBorder(new Color(75, 77, 83)));
        editButton.setPreferredSize(new Dimension(30, 22));
        editButton.setFocusPainted(false);
    }

    private void layoutComponents()
    {
        setLayout(new BorderLayout());
        setBackground(new Color(37, 38, 43));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(75, 77, 83)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        // Main content panel using BoxLayout
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(45, 46, 51));
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 10));
        
        // Name and type
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        namePanel.setBackground(new Color(45, 46, 51));
        namePanel.add(nameLabel);
        namePanel.add(typeLabel);
        
        // Edit and delete buttons panel - below name
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonsPanel.setBackground(new Color(45, 46, 51));
        buttonsPanel.add(editButton);
        buttonsPanel.add(Box.createHorizontalStrut(3)); // Add spacing
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(Box.createHorizontalStrut(10)); // Add spacing before enabled button
        buttonsPanel.add(enabledButton);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        statusPanel.setBackground(new Color(45, 46, 51));
        statusPanel.add(statusLabel);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        statsPanel.setBackground(new Color(45, 46, 51));
        statsPanel.add(statsLabel);
        
        // Add all panels to content with spacing
        contentPanel.add(namePanel);
        contentPanel.add(buttonsPanel);
        contentPanel.add(Box.createVerticalStrut(5)); // Spacing before status
        contentPanel.add(statusPanel);
        contentPanel.add(statsPanel);
        
        // Simple wrapper panel
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(new Color(45, 46, 51));
        wrapperPanel.add(contentPanel, BorderLayout.CENTER);
        
        add(wrapperPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers()
    {
        enabledButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean newState = enabledButton.isSelected();
                triggerEngine.setTriggerEnabled(trigger.getId(), newState);
                updateDisplay();
            }
        });
        
        deleteButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int result = JOptionPane.showConfirmDialog(
                    TriggerListItem.this,
                    "Delete trigger '" + trigger.getName() + "'?",
                    "Delete Trigger",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION)
                {
                    boolean removed = triggerEngine.removeTrigger(trigger.getId());
                    if (removed)
                    {
                        // Find the parent TriggerPanel and refresh it
                        SwingUtilities.invokeLater(() -> {
                            Container parent = getParent();
                            while (parent != null && !(parent instanceof TriggerPanel))
                            {
                                parent = parent.getParent();
                            }
                            
                            if (parent instanceof TriggerPanel)
                            {
                                ((TriggerPanel) parent).refreshDisplay();
                            }
                        });
                        
                        JOptionPane.showMessageDialog(TriggerListItem.this, 
                            "Trigger deleted successfully!", 
                            "Trigger Deleted", 
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(TriggerListItem.this, 
                            "Failed to delete trigger!", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        editButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // Open trigger edit dialog
                TriggerEditDialog dialog = new TriggerEditDialog(
                    (Frame) SwingUtilities.getWindowAncestor(TriggerListItem.this), 
                    triggerEngine, 
                    trigger
                );
                dialog.setVisible(true);
                
                // Parent will refresh the list when dialog closes
            }
        });
    }

    private void updateDisplay()
    {
        // Update labels
        nameLabel.setText(trigger.getName());
        typeLabel.setText(getTypeDisplayName(trigger.getType()));
        
        // Update status
        if (trigger.isEnabled())
        {
            statusLabel.setText("✅ Active");
            statusLabel.setForeground(new Color(120, 255, 120));
            enabledButton.setSelected(true);
            enabledButton.setText("ON");
            enabledButton.setBackground(new Color(52, 128, 52));
            enabledButton.setForeground(new Color(255, 255, 255));
        }
        else
        {
            statusLabel.setText("⏸ Disabled");
            statusLabel.setForeground(new Color(255, 200, 120));
            enabledButton.setSelected(false);
            enabledButton.setText("OFF");
            enabledButton.setBackground(new Color(128, 52, 52));
            enabledButton.setForeground(new Color(255, 255, 255));
        }
        
        // Update stats
        long lastFired = trigger.getLastFired();
        int fireCount = trigger.getFireCount();
        
        if (lastFired > 0)
        {
            long timeSince = System.currentTimeMillis() - lastFired;
            String timeAgo = formatTimeAgo(timeSince);
            statsLabel.setText(String.format("Fired: %d times | Last: %s", fireCount, timeAgo));
        }
        else
        {
            statsLabel.setText("Fired: 0 times | Never fired");
        }
    }

    private String getTypeDisplayName(TriggerType type)
    {
        switch (type)
        {
            case HP: return "❤️ HP";
            case ANIMATION: return "🎬 Animation";
            case XP: return "⭐ XP";
            default: return "❓ Unknown";
        }
    }

    private String formatTimeAgo(long milliseconds)
    {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (seconds < 60)
        {
            return seconds + "s ago";
        }
        else if (minutes < 60)
        {
            return minutes + "m ago";
        }
        else
        {
            return hours + "h ago";
        }
    }
}
