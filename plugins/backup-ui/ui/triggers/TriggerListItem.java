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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Individual trigger item in the triggers list with Premium Card Design
 */
public class TriggerListItem extends JPanel {
    private final Trigger trigger;
    private final TriggerEngine triggerEngine;

    // UI Components
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel statsLabel;
    private JToggleButton enabledButton;
    private JButton deleteButton;
    private JButton editButton;
    private JPanel cardPanel;

    public TriggerListItem(Trigger trigger, TriggerEngine triggerEngine) {
        this.trigger = trigger;
        this.triggerEngine = triggerEngine;

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        updateDisplay();
    }

    private void initializeComponents() {
        // Card container
        cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(Theme.SURFACE);

        // Labels
        nameLabel = new JLabel();
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(Color.WHITE);

        typeLabel = new JLabel();
        typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));

        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statsLabel.setForeground(Theme.TEXT_MUTED);

        // Buttons
        enabledButton = new JToggleButton();
        enabledButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
        enabledButton.setPreferredSize(new Dimension(50, 20));
        enabledButton.setFocusPainted(false);
        enabledButton.setBorder(BorderFactory.createEmptyBorder());
        enabledButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        editButton = createIconButton("📝", "Edit Trigger");
        deleteButton = createIconButton("🗑️", "Delete Trigger");
    }

    private JButton createIconButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(Theme.SURFACE);
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setForeground(Theme.TEXT_PRIMARY);
            }

            public void mouseExited(MouseEvent evt) {
                btn.setForeground(Theme.TEXT_SECONDARY);
            }
        });
        return btn;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(0, 0, 8, 0)); // Spacing between items

        // Type color accent
        Color typeColor = getTypeColor(trigger.getType());

        // Card styling
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, typeColor), // Left accent stripe
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER),
                        new EmptyBorder(8, 10, 8, 10))));

        // Header (Name + Type)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.SURFACE);
        headerPanel.add(nameLabel, BorderLayout.CENTER);

        typeLabel.setForeground(typeColor);
        headerPanel.add(typeLabel, BorderLayout.EAST);

        // Info Row (Stats)
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        infoPanel.setBackground(Theme.SURFACE);
        infoPanel.add(statsLabel);

        // Actions Row (Buttons + Toggle)
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBackground(Theme.SURFACE);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftButtons.setBackground(Theme.SURFACE);
        leftButtons.add(editButton);
        leftButtons.add(Box.createHorizontalStrut(5));
        leftButtons.add(deleteButton);

        actionsPanel.add(leftButtons, BorderLayout.WEST);
        actionsPanel.add(enabledButton, BorderLayout.EAST);

        // Assemble Card
        JPanel centerContent = new JPanel(new BorderLayout());
        centerContent.setBackground(Theme.SURFACE);
        centerContent.add(headerPanel, BorderLayout.NORTH);
        centerContent.add(infoPanel, BorderLayout.CENTER);
        centerContent.add(actionsPanel, BorderLayout.SOUTH);

        cardPanel.add(centerContent, BorderLayout.CENTER);
        add(cardPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        enabledButton.addActionListener(e -> {
            boolean newState = enabledButton.isSelected();
            triggerEngine.setTriggerEnabled(trigger.getId(), newState);
            updateDisplay();
        });

        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    TriggerListItem.this,
                    "Delete trigger '" + trigger.getName() + "'?",
                    "Delete Trigger",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                if (triggerEngine.removeTrigger(trigger.getId())) {
                    refreshParentPanel();
                }
            }
        });

        editButton.addActionListener(e -> {
            TriggerEditDialog dialog = new TriggerEditDialog(
                    (Frame) SwingUtilities.getWindowAncestor(TriggerListItem.this),
                    triggerEngine,
                    trigger);
            dialog.setVisible(true);
            refreshParentPanel();
        });
    }

    private void refreshParentPanel() {
        SwingUtilities.invokeLater(() -> {
            Container parent = getParent();
            while (parent != null) {
                if (parent instanceof TriggerPanel) {
                    ((TriggerPanel) parent).refreshDisplay();
                    break;
                }
                parent = parent.getParent();
            }
        });
    }

    private void updateDisplay() {
        nameLabel.setText(trigger.getName());
        typeLabel.setText(getTypeDisplayName(trigger.getType()).toUpperCase());

        if (trigger.isEnabled()) {
            enabledButton.setText("ON");
            enabledButton.setSelected(true);
            enabledButton.setBackground(Theme.SUCCESS); // Green for ON
            enabledButton.setForeground(Color.WHITE);
        } else {
            enabledButton.setText("OFF");
            enabledButton.setSelected(false);
            enabledButton.setBackground(Theme.SURFACE_HOVER); // Neutral for OFF
            enabledButton.setForeground(Theme.TEXT_MUTED);
        }
        enabledButton.setOpaque(true);

        long lastFired = trigger.getLastFired();
        int fireCount = trigger.getFireCount();

        if (lastFired > 0) {
            String timeAgo = formatTimeAgo(System.currentTimeMillis() - lastFired);
            statsLabel.setText(String.format("Fired %d times • Last: %s", fireCount, timeAgo));
        } else {
            statsLabel.setText("Never fired");
        }
    }

    private Color getTypeColor(TriggerType type) {
        switch (type) {
            case HP:
                return Theme.DANGER;
            case ANIMATION:
                return Theme.WARNING;
            case GFX:
                return Theme.PURPLE;
            case XP:
                return Theme.PRIMARY; // Blue
            case PLAYER_SPAWNED:
                return Theme.SUCCESS;
            default:
                return Theme.TEXT_SECONDARY;
        }
    }

    private String getTypeDisplayName(TriggerType type) {
        switch (type) {
            case HP:
                return "HP";
            case ANIMATION:
                return "Anim";
            case GFX:
                return "GFX";
            case XP:
                return "XP";
            case PLAYER_SPAWNED:
                return "Player";
            default:
                return "Unknown";
        }
    }

    private String formatTimeAgo(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60)
            return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60)
            return minutes + "m ago";
        return (minutes / 60) + "h ago";
    }
}
