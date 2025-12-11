package com.tonic.plugins.gearswapper.ui.triggers;

import com.tonic.plugins.gearswapper.triggers.Trigger;
import com.tonic.plugins.gearswapper.triggers.TriggerEngine;
import com.tonic.plugins.gearswapper.triggers.TriggerType;
import com.tonic.plugins.gearswapper.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Individual trigger item in the triggers list
 */
public class TriggerListItem extends JPanel {
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

    public TriggerListItem(Trigger trigger, TriggerEngine triggerEngine) {
        this.trigger = trigger;
        this.triggerEngine = triggerEngine;

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        updateDisplay();
    }

    private void initializeComponents() {
        // Labels
        nameLabel = new JLabel();
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(Theme.TEXT_PRIMARY);

        typeLabel = new JLabel();
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        typeLabel.setForeground(Theme.TEXT_SECONDARY);

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusLabel.setForeground(Theme.TEXT_MUTED);

        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statsLabel.setForeground(Theme.TEXT_MUTED);

        // Buttons
        enabledButton = new JToggleButton();
        enabledButton.setFont(new Font("Segoe UI", Font.BOLD, 9));
        enabledButton.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        enabledButton.setPreferredSize(new Dimension(60, 22));
        enabledButton.setFocusPainted(false);
        enabledButton.setContentAreaFilled(false);
        enabledButton.setOpaque(true);

        deleteButton = new JButton("🗑️");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
        deleteButton.setBackground(Theme.BACKGROUND);
        deleteButton.setForeground(Theme.DANGER);
        deleteButton.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        deleteButton.setPreferredSize(new Dimension(30, 22));
        deleteButton.setFocusPainted(false);
        // Add hover for delete
        deleteButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deleteButton.setBackground(Theme.DANGER);
                deleteButton.setForeground(Color.WHITE);
                deleteButton.setBorder(BorderFactory.createLineBorder(Theme.DANGER.darker()));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                deleteButton.setBackground(Theme.BACKGROUND);
                deleteButton.setForeground(Theme.DANGER);
                deleteButton.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            }
        });

        editButton = new JButton("📝");
        editButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
        editButton.setBackground(Theme.BACKGROUND);
        editButton.setForeground(Theme.PRIMARY);
        editButton.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        editButton.setPreferredSize(new Dimension(30, 22));
        editButton.setFocusPainted(false);
        // Add hover for edit
        editButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                editButton.setBackground(Theme.PRIMARY);
                editButton.setForeground(Color.WHITE);
                editButton.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY.darker()));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                editButton.setBackground(Theme.BACKGROUND);
                editButton.setForeground(Theme.PRIMARY);
                editButton.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            }
        });
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        // Main content panel using BoxLayout
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.SURFACE);
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 10));

        // Name and type
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        namePanel.setBackground(Theme.SURFACE);
        namePanel.add(nameLabel);
        namePanel.add(typeLabel);

        // Edit and delete buttons panel - below name
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonsPanel.setBackground(Theme.SURFACE);
        buttonsPanel.add(editButton);
        buttonsPanel.add(Box.createHorizontalStrut(3)); // Add spacing
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(Box.createHorizontalStrut(10)); // Add spacing before enabled button
        buttonsPanel.add(enabledButton);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        statusPanel.setBackground(Theme.SURFACE);
        statusPanel.add(statusLabel);

        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        statsPanel.setBackground(Theme.SURFACE);
        statsPanel.add(statsLabel);

        // Add all panels to content with spacing
        contentPanel.add(namePanel);
        contentPanel.add(buttonsPanel);
        contentPanel.add(Box.createVerticalStrut(5)); // Spacing before status
        contentPanel.add(statusPanel);
        contentPanel.add(statsPanel);

        // Simple wrapper panel
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Theme.SURFACE);
        wrapperPanel.add(contentPanel, BorderLayout.CENTER);

        add(wrapperPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        enabledButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean newState = enabledButton.isSelected();
                triggerEngine.setTriggerEnabled(trigger.getId(), newState);
                updateDisplay();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        TriggerListItem.this,
                        "Delete trigger '" + trigger.getName() + "'?",
                        "Delete Trigger",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (result == JOptionPane.YES_OPTION) {
                    boolean removed = triggerEngine.removeTrigger(trigger.getId());
                    if (removed) {
                        // Find the parent TriggerPanel and refresh it
                        SwingUtilities.invokeLater(() -> {
                            Container parent = getParent();
                            while (parent != null && !(parent instanceof TriggerPanel)) {
                                parent = parent.getParent();
                            }

                            if (parent instanceof TriggerPanel) {
                                ((TriggerPanel) parent).refreshDisplay();
                            }
                        });

                        JOptionPane.showMessageDialog(TriggerListItem.this,
                                "Trigger deleted successfully!",
                                "Trigger Deleted",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(TriggerListItem.this,
                                "Failed to delete trigger!",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open trigger edit dialog
                TriggerEditDialog dialog = new TriggerEditDialog(
                        (Frame) SwingUtilities.getWindowAncestor(TriggerListItem.this),
                        triggerEngine,
                        trigger);
                dialog.setVisible(true);

                // Parent will refresh the list when dialog closes
            }
        });
    }

    private void updateDisplay() {
        // Update labels
        nameLabel.setText(trigger.getName());
        typeLabel.setText(getTypeDisplayName(trigger.getType()));

        // Update status
        if (trigger.isEnabled()) {
            statusLabel.setText("✅ Active");
            statusLabel.setForeground(Theme.SUCCESS);
            enabledButton.setSelected(true);
            enabledButton.setText("ON");
            enabledButton.setBackground(Theme.SUCCESS);
            enabledButton.setForeground(Color.WHITE);
            enabledButton.setBorder(BorderFactory.createLineBorder(Theme.SUCCESS.darker()));
        } else {
            statusLabel.setText("⏸ Disabled");
            statusLabel.setForeground(Theme.WARNING);
            enabledButton.setSelected(false);
            enabledButton.setText("OFF");
            enabledButton.setBackground(Theme.DANGER); // Or neutral? Usually OFF is Red or Gray
            enabledButton.setForeground(Color.WHITE);
            enabledButton.setBorder(BorderFactory.createLineBorder(Theme.DANGER.darker()));
        }

        // Update stats
        long lastFired = trigger.getLastFired();
        int fireCount = trigger.getFireCount();

        if (lastFired > 0) {
            long timeSince = System.currentTimeMillis() - lastFired;
            String timeAgo = formatTimeAgo(timeSince);
            statsLabel.setText(String.format("Fired: %d times | Last: %s", fireCount, timeAgo));
        } else {
            statsLabel.setText("Fired: 0 times | Never fired");
        }
    }

    private String getTypeDisplayName(TriggerType type) {
        switch (type) {
            case HP:
                return "❤️ HP";
            case ANIMATION:
                return "🎬 Animation";
            case XP:
                return "⭐ XP";
            case PLAYER_SPAWNED:
                return "👤 Player Spawned";
            default:
                return "❓ Unknown";
        }
    }

    private String formatTimeAgo(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (seconds < 60) {
            return seconds + "s ago";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else {
            return hours + "h ago";
        }
    }
}
