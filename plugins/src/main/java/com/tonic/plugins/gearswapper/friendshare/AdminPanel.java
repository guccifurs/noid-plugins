package com.tonic.plugins.gearswapper.friendshare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin panel for viewing connected users and spectating.
 * Only accessible by admin (thenoid2).
 */
public class AdminPanel extends JFrame {

    private final SpectateService service;
    private final ScreenViewer viewer;

    private final DefaultTableModel tableModel;
    private final JTable userTable;
    private final JButton refreshButton;
    private final JButton viewButton;
    private final JButton stopButton;

    private String currentlyViewing = null;

    public AdminPanel(SpectateService service) {
        super("Admin Panel - User Spectate");
        this.service = service;
        this.viewer = new ScreenViewer("Spectate");

        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Dark theme
        getContentPane().setBackground(new Color(30, 30, 30));
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(40, 40, 40));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("Connected Users");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshUserList());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = { "Discord Name", "RSN", "Online Time", "IP Address", "Status" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setBackground(new Color(45, 45, 48));
        userTable.setForeground(Color.WHITE);
        userTable.setGridColor(new Color(60, 60, 60));
        userTable.setSelectionBackground(new Color(70, 70, 75));
        userTable.setRowHeight(28);
        userTable.getTableHeader().setBackground(new Color(50, 50, 55));
        userTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.getViewport().setBackground(new Color(45, 45, 48));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(new Color(40, 40, 40));

        viewButton = new JButton("View Selected");
        viewButton.setBackground(new Color(50, 150, 50));
        viewButton.setForeground(Color.WHITE);
        viewButton.addActionListener(e -> viewSelected());
        buttonPanel.add(viewButton);

        stopButton = new JButton("Stop Viewing");
        stopButton.setBackground(new Color(150, 50, 50));
        stopButton.setForeground(Color.WHITE);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopViewing());
        buttonPanel.add(stopButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Set up callbacks
        service.setOnUserListReceived(this::updateUserList);
        service.setOnFrameReceived(data -> {
            if (viewer != null && viewer.isVisible()) {
                viewer.displayFrame(data);
            }
        });

        // Close viewer when panel closes
        viewer.setOnClose(() -> {
            service.stopView();
            currentlyViewing = null;
            viewButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        // Initial refresh
        refreshUserList();
    }

    private void refreshUserList() {
        service.requestUserList();
    }

    private void updateUserList(List<SpectateService.UserInfo> users) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);

            long now = System.currentTimeMillis();

            for (SpectateService.UserInfo u : users) {
                long onlineMs = now - u.connectedAt;
                String onlineTime = formatDuration(onlineMs);
                String status = u.streaming ? "Streaming" : "Online";

                tableModel.addRow(new Object[] {
                        u.discordName,
                        u.rsn,
                        onlineTime,
                        u.ip,
                        status
                });
            }
        });
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private void viewSelected() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user first", "View", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String discordName = (String) tableModel.getValueAt(row, 0);
        currentlyViewing = discordName;

        service.viewUser(discordName);

        // Open viewer
        viewer.setTitle("Viewing: " + discordName);
        viewer.setVisible(true);

        viewButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    private void stopViewing() {
        service.stopView();
        currentlyViewing = null;

        viewer.setVisible(false);

        viewButton.setEnabled(true);
        stopButton.setEnabled(false);

        refreshUserList();
    }

    @Override
    public void dispose() {
        if (currentlyViewing != null) {
            service.stopView();
        }
        viewer.dispose();
        super.dispose();
    }
}
