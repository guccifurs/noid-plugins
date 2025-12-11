package com.tonic.plugins.noid;

import com.tonic.Logger;
import com.tonic.plugins.noid.auth.NoidAuthService;
import com.tonic.plugins.noid.auth.NoidUser;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * NoidPlugin panel showing subscription status and Discord OAuth login
 */
public class NoidPanel extends PluginPanel {

    private final NoidPlugin plugin;
    private final NoidAuthService authService;

    // UI Components
    private JPanel loginPanel;
    private JPanel userInfoPanel;
    private JLabel statusLabel;
    private JLabel userNameLabel;
    private JLabel subscriptionLabel;
    private JLabel expiryLabel;
    private JButton loginButton;
    private JButton logoutButton;

    @Inject
    public NoidPanel(NoidPlugin plugin, NoidAuthService authService) {
        this.plugin = plugin;
        this.authService = authService;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        buildPanel();
        updateUI();
    }

    private void buildPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Header
        JLabel headerLabel = new JLabel("Noid Authentication");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        mainPanel.add(headerLabel);

        // Status indicator
        statusLabel = new JLabel("● Not Connected");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        mainPanel.add(statusLabel);

        // Login panel (shown when not authenticated)
        loginPanel = createLoginPanel();
        mainPanel.add(loginPanel);

        // User info panel (shown when authenticated)
        userInfoPanel = createUserInfoPanel();
        userInfoPanel.setVisible(false);
        mainPanel.add(userInfoPanel);

        // Plugins section - wrap in left-aligned container
        mainPanel.add(Box.createVerticalStrut(20));
        JPanel pluginsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pluginsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pluginsWrapper.add(createPluginsSection());
        mainPanel.add(pluginsWrapper);

        add(mainPanel, BorderLayout.NORTH);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                new EmptyBorder(15, 15, 15, 15)));

        JLabel infoLabel = new JLabel(
                "<html><center>Login with Discord to<br>verify your subscription</center></html>");
        infoLabel.setForeground(Color.LIGHT_GRAY);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(infoLabel);

        panel.add(Box.createVerticalStrut(15));

        loginButton = new JButton("🔗 Login with Discord");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setBackground(new Color(88, 101, 242)); // Discord blue
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setFont(new Font("Arial", Font.BOLD, 12));
        loginButton.setMaximumSize(new Dimension(200, 35));

        loginButton.addActionListener(e -> startOAuthFlow());

        // Hover effect
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(new Color(71, 82, 196));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(new Color(88, 101, 242));
            }
        });

        panel.add(loginButton);

        return panel;
    }

    private JPanel createUserInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(67, 181, 129)), // Discord green
                new EmptyBorder(15, 15, 15, 15)));

        userNameLabel = new JLabel("Username");
        userNameLabel.setForeground(Color.WHITE);
        userNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(userNameLabel);

        panel.add(Box.createVerticalStrut(10));

        subscriptionLabel = new JLabel("Subscription: Active");
        subscriptionLabel.setForeground(new Color(67, 181, 129));
        subscriptionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subscriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subscriptionLabel);

        expiryLabel = new JLabel("Expires: Never");
        expiryLabel.setForeground(Color.LIGHT_GRAY);
        expiryLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        expiryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(expiryLabel);

        panel.add(Box.createVerticalStrut(15));

        logoutButton = new JButton("Logout");
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> logout());

        panel.add(logoutButton);

        return panel;
    }

    private JPanel createPluginsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sectionLabel = new JLabel("Plugin Updates");
        sectionLabel.setForeground(Color.WHITE);
        sectionLabel.setFont(new Font("Arial", Font.BOLD, 13));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionLabel.setHorizontalAlignment(SwingConstants.LEFT);
        sectionLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(sectionLabel);

        // Update status label
        updateStatusLabel = new JLabel("Click to check for updates");
        updateStatusLabel.setForeground(Color.LIGHT_GRAY);
        updateStatusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        updateStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(updateStatusLabel);

        panel.add(Box.createVerticalStrut(10));

        // Check for Updates button
        checkUpdatesButton = new JButton("🔄 Check for Updates");
        checkUpdatesButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkUpdatesButton.setHorizontalAlignment(SwingConstants.LEFT);
        checkUpdatesButton.setBackground(new Color(88, 101, 242));
        checkUpdatesButton.setForeground(Color.WHITE);
        checkUpdatesButton.setFocusPainted(false);
        checkUpdatesButton.setBorderPainted(false);
        checkUpdatesButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkUpdatesButton.setFont(new Font("Arial", Font.BOLD, 11));
        checkUpdatesButton.setPreferredSize(new Dimension(180, 30));
        checkUpdatesButton.setMaximumSize(new Dimension(180, 30));

        checkUpdatesButton.addActionListener(e -> checkForUpdates());

        // Hover effect
        checkUpdatesButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (checkUpdatesButton.isEnabled()) {
                    checkUpdatesButton.setBackground(new Color(71, 82, 196));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (checkUpdatesButton.isEnabled()) {
                    checkUpdatesButton.setBackground(new Color(88, 101, 242));
                }
            }
        });

        panel.add(checkUpdatesButton);

        return panel;
    }

    private JLabel updateStatusLabel;
    private JButton checkUpdatesButton;

    private void checkForUpdates() {
        checkUpdatesButton.setEnabled(false);
        checkUpdatesButton.setText("Checking...");
        updateStatusLabel.setText("Checking GitHub for updates...");

        new SwingWorker<java.util.List<com.tonic.plugins.noid.update.UpdateInfo>, Void>() {
            @Override
            protected java.util.List<com.tonic.plugins.noid.update.UpdateInfo> doInBackground() {
                return plugin.getPluginManager().getAvailableUpdates();
            }

            @Override
            protected void done() {
                try {
                    java.util.List<com.tonic.plugins.noid.update.UpdateInfo> updates = get();

                    if (updates.isEmpty()) {
                        updateStatusLabel.setText("✓ All plugins up to date!");
                        updateStatusLabel.setForeground(new Color(67, 181, 129));
                        checkUpdatesButton.setText("🔄 Check for Updates");
                        checkUpdatesButton.setEnabled(true);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (com.tonic.plugins.noid.update.UpdateInfo update : updates) {
                            sb.append(update.getPluginName()).append(": ").append(update.getLatestVersion())
                                    .append(" available\n");
                        }
                        updateStatusLabel.setText("<html>" + sb.toString().replace("\n", "<br>") + "</html>");
                        updateStatusLabel.setForeground(new Color(255, 200, 0));

                        checkUpdatesButton.setText("⬇ Update All (" + updates.size() + ")");
                        checkUpdatesButton.setBackground(new Color(67, 181, 129));
                        checkUpdatesButton.setEnabled(true);

                        // Change button action to install updates
                        for (java.awt.event.ActionListener al : checkUpdatesButton.getActionListeners()) {
                            checkUpdatesButton.removeActionListener(al);
                        }
                        checkUpdatesButton.addActionListener(e -> installUpdates(updates));
                    }
                } catch (Exception e) {
                    updateStatusLabel.setText("Error checking updates");
                    updateStatusLabel.setForeground(Color.RED);
                    checkUpdatesButton.setText("🔄 Check for Updates");
                    checkUpdatesButton.setEnabled(true);
                    Logger.error("[Noid] Update check error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void installUpdates(java.util.List<com.tonic.plugins.noid.update.UpdateInfo> updates) {
        checkUpdatesButton.setEnabled(false);
        checkUpdatesButton.setText("Updating...");
        updateStatusLabel.setText("Downloading and installing updates...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                boolean allSuccess = true;
                for (com.tonic.plugins.noid.update.UpdateInfo update : updates) {
                    if (!plugin.getPluginManager().downloadAndReload(update)) {
                        allSuccess = false;
                    }
                }
                return allSuccess;
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        updateStatusLabel.setText("✓ All plugins updated!");
                        updateStatusLabel.setForeground(new Color(67, 181, 129));
                    } else {
                        updateStatusLabel.setText("Some updates failed - restart may be needed");
                        updateStatusLabel.setForeground(new Color(255, 200, 0));
                    }

                    // Reset button
                    checkUpdatesButton.setText("🔄 Check for Updates");
                    checkUpdatesButton.setBackground(new Color(88, 101, 242));
                    checkUpdatesButton.setEnabled(true);
                    for (java.awt.event.ActionListener al : checkUpdatesButton.getActionListeners()) {
                        checkUpdatesButton.removeActionListener(al);
                    }
                    checkUpdatesButton.addActionListener(e -> checkForUpdates());

                } catch (Exception e) {
                    updateStatusLabel.setText("Update failed: " + e.getMessage());
                    updateStatusLabel.setForeground(Color.RED);
                    checkUpdatesButton.setText("🔄 Check for Updates");
                    checkUpdatesButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private String pendingState = null;

    private void startOAuthFlow() {
        Logger.norm("[Noid] Starting Discord OAuth flow...");
        loginButton.setEnabled(false);
        loginButton.setText("Opening browser...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return authService.getOAuthUrl();
            }

            @Override
            protected void done() {
                try {
                    String url = get();
                    if (url != null && !url.isEmpty()) {
                        // Extract state from URL for polling
                        if (url.contains("state=")) {
                            pendingState = url.substring(url.indexOf("state=") + 6);
                            if (pendingState.contains("&")) {
                                pendingState = pendingState.substring(0, pendingState.indexOf("&"));
                            }
                        }

                        LinkBrowser.browse(url);
                        loginButton.setText("Waiting for auth...");

                        // Start polling for auth completion
                        startAuthPolling();
                    } else {
                        loginButton.setText("🔗 Login with Discord");
                        loginButton.setEnabled(true);
                        JOptionPane.showMessageDialog(NoidPanel.this,
                                "Failed to get OAuth URL. Check backend is running.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    Logger.error("[Noid] OAuth error: " + e.getMessage());
                    loginButton.setText("🔗 Login with Discord");
                    loginButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void startAuthPolling() {
        // Poll every 2 seconds to check if auth completed
        Timer timer = new Timer(2000, null);
        final int[] pollCount = { 0 };

        timer.addActionListener(e -> {
            pollCount[0]++;

            // Try to get auth result from backend
            new SwingWorker<NoidUser, Void>() {
                @Override
                protected NoidUser doInBackground() {
                    // Poll all recent auths (we'll match by checking the user)
                    return authService.pollForAuthByState(pendingState);
                }

                @Override
                protected void done() {
                    try {
                        NoidUser user = get();
                        if (user != null) {
                            timer.stop();
                            plugin.setCurrentUser(user);
                            Logger.norm("[Noid] ✅ OAuth completed for: " + user.getDiscordName());
                            updateUI();
                        } else if (pollCount[0] > 60) { // 2 minutes
                            timer.stop();
                            loginButton.setText("🔗 Login with Discord");
                            loginButton.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        Logger.error("[Noid] Poll error: " + ex.getMessage());
                    }
                }
            }.execute();
        });
        timer.setRepeats(true);
        timer.start();
    }

    private void logout() {
        authService.logout();
        plugin.setAuthenticated(false);
        updateUI();
    }

    public void updateUI() {
        SwingUtilities.invokeLater(() -> {
            if (plugin.isAuthenticated()) {
                NoidUser user = plugin.getCurrentUser();

                statusLabel.setText("● Connected");
                statusLabel.setForeground(new Color(67, 181, 129));

                if (user != null) {
                    userNameLabel.setText(user.getDiscordName());
                    subscriptionLabel.setText("Subscription: " + user.getTier());

                    if (user.getExpiresAt() != null) {
                        expiryLabel.setText("Expires: " + user.getExpiresAt());
                    } else {
                        expiryLabel.setText("Expires: Never (Lifetime)");
                    }
                }

                loginPanel.setVisible(false);
                userInfoPanel.setVisible(true);
            } else {
                statusLabel.setText("● Not Connected");
                statusLabel.setForeground(Color.RED);

                loginPanel.setVisible(true);
                userInfoPanel.setVisible(false);
                loginButton.setText("🔗 Login with Discord");
                loginButton.setEnabled(true);
            }

            revalidate();
            repaint();
        });
    }
}
