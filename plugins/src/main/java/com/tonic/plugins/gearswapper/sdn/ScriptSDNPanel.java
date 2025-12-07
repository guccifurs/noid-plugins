package com.tonic.plugins.gearswapper.sdn;

import com.tonic.plugins.noid.auth.NoidUser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Script SDN Panel - Community script sharing interface
 */
public class ScriptSDNPanel extends JPanel {

    private static final Color BACKGROUND = new Color(32, 34, 37);
    private static final Color PANEL_BG = new Color(47, 49, 54);
    private static final Color CARD_BG = new Color(54, 57, 63);
    private static final Color ACCENT = new Color(88, 101, 242);
    private static final Color TEXT = new Color(220, 221, 222);
    private static final Color TEXT_MUTED = new Color(142, 146, 151);
    private static final Color SUCCESS = new Color(67, 181, 129);
    private static final Color DANGER = new Color(237, 66, 69);

    private final ScriptSDNService sdnService;
    private final Supplier<NoidUser> userSupplier;

    private JPanel scriptListPanel;
    private JPanel detailPanel;
    private JLabel statusLabel;
    private List<ScriptEntry> scripts;

    public ScriptSDNPanel(Supplier<NoidUser> userSupplier) {
        this.sdnService = new ScriptSDNService();
        this.userSupplier = userSupplier;

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initUI();
        refreshScripts();
    }

    private void initUI() {
        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Main content - split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND);

        // Left: Detail view
        detailPanel = createDetailPanel();
        JScrollPane detailScroll = new JScrollPane(detailPanel);
        detailScroll.setBorder(null);
        detailScroll.setBackground(PANEL_BG);
        detailScroll.getViewport().setBackground(PANEL_BG);
        splitPane.setLeftComponent(detailScroll);

        // Right: Script list
        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("📜 Script SDN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        JButton refreshBtn = createButton("🔄 Refresh", ACCENT);
        refreshBtn.addActionListener(e -> refreshScripts());
        buttonPanel.add(refreshBtn);

        JButton submitBtn = createButton("➕ Submit Script", SUCCESS);
        submitBtn.addActionListener(e -> showSubmitDialog());
        buttonPanel.add(submitBtn);

        header.add(buttonPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(PANEL_BG);
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel listTitle = new JLabel("📚 Available Scripts");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        listTitle.setForeground(TEXT);
        listTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        rightPanel.add(listTitle, BorderLayout.NORTH);

        scriptListPanel = new JPanel();
        scriptListPanel.setLayout(new BoxLayout(scriptListPanel, BoxLayout.Y_AXIS));
        scriptListPanel.setBackground(PANEL_BG);

        JScrollPane listScroll = new JScrollPane(scriptListPanel);
        listScroll.setBorder(null);
        listScroll.setBackground(PANEL_BG);
        listScroll.getViewport().setBackground(PANEL_BG);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightPanel.add(listScroll, BorderLayout.CENTER);

        return rightPanel;
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        showWelcomeScreen(panel);
        return panel;
    }

    private void showWelcomeScreen(JPanel panel) {
        panel.removeAll();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel welcomeIcon = new JLabel("📜", SwingConstants.CENTER);
        welcomeIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        welcomeIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createVerticalGlue());
        panel.add(welcomeIcon);
        panel.add(Box.createVerticalStrut(20));

        JLabel welcomeTitle = new JLabel("Welcome to Script SDN");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        welcomeTitle.setForeground(TEXT);
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(welcomeTitle);
        panel.add(Box.createVerticalStrut(15));

        String[] instructions = {
                "Browse community-shared scripts on the right panel",
                "Click a script to view its details and code",
                "Upvote scripts to help others find the best ones",
                "Submit your own scripts to share with the community"
        };

        for (String instruction : instructions) {
            JLabel label = new JLabel("• " + instruction);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setForeground(TEXT_MUTED);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createVerticalStrut(8));
        }

        panel.add(Box.createVerticalGlue());
        panel.revalidate();
        panel.repaint();
    }

    private void showScriptDetail(ScriptEntry script) {
        detailPanel.removeAll();
        detailPanel.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(PANEL_BG);
        content.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel title = new JLabel(script.getName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT);
        titleRow.add(title, BorderLayout.WEST);
        content.add(titleRow);
        content.add(Box.createVerticalStrut(8));

        // Author and date
        JLabel author = new JLabel("by " + script.getAuthorName() + " • " + formatDate(script.getCreatedAt()));
        author.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        author.setForeground(TEXT_MUTED);
        author.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(author);
        content.add(Box.createVerticalStrut(10));

        // Description
        if (script.getDescription() != null && !script.getDescription().isEmpty()) {
            JLabel desc = new JLabel("<html><body style='width: 300px'>" + script.getDescription() + "</body></html>");
            desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            desc.setForeground(TEXT);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(desc);
            content.add(Box.createVerticalStrut(10));
        }

        // Vote panel (upvote only)
        JPanel votePanel = createVotePanel(script);
        votePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(votePanel);
        content.add(Box.createVerticalStrut(15));

        // Script code with syntax highlighting
        JLabel codeLabel = new JLabel("📝 Script Code:");
        codeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        codeLabel.setForeground(TEXT);
        codeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(codeLabel);
        content.add(Box.createVerticalStrut(5));

        JTextArea codeArea = new JTextArea(script.getContent());
        codeArea.setEditable(false);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        codeArea.setBackground(new Color(40, 42, 46));
        codeArea.setForeground(new Color(171, 178, 191));
        codeArea.setCaretColor(TEXT);
        codeArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(new LineBorder(new Color(60, 63, 68), 1));
        codeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(codeScroll);

        // Action buttons
        NoidUser user = userSupplier.get();
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton copyBtn = createButton("📋 Copy", ACCENT);
        copyBtn.addActionListener(e -> {
            StringSelection sel = new StringSelection(script.getContent());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            setStatus("Script copied to clipboard!");
        });
        actionPanel.add(copyBtn);

        // Owner-only buttons
        if (user != null && script.isOwnedBy(user.getDiscordId())) {
            JButton editBtn = createButton("✏️ Edit", new Color(250, 166, 26));
            editBtn.addActionListener(e -> showEditDialog(script));
            actionPanel.add(editBtn);

            JButton deleteBtn = createButton("🗑️ Delete", DANGER);
            deleteBtn.addActionListener(e -> confirmDelete(script));
            actionPanel.add(deleteBtn);
        }

        content.add(Box.createVerticalStrut(15));
        content.add(actionPanel);

        detailPanel.add(content, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private JPanel createVotePanel(ScriptEntry script) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        JButton upBtn = new JButton("👍 Upvote (" + script.getVotes() + ")");
        upBtn.setBackground(script.isHasVoted() ? SUCCESS : CARD_BG);
        upBtn.setForeground(TEXT);
        upBtn.setFocusPainted(false);
        upBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        upBtn.setEnabled(!script.isHasVoted());
        upBtn.addActionListener(e -> vote(script));

        JLabel voteInfo = new JLabel(script.isHasVoted() ? "✓ You voted" : "Vote to support this script");
        voteInfo.setForeground(TEXT_MUTED);
        voteInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        panel.add(upBtn);
        panel.add(voteInfo);

        return panel;
    }

    private JPanel createScriptCard(ScriptEntry script) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Click handler
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showScriptDetail(script);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(60, 63, 69));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(CARD_BG);
            }
        });

        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        JLabel name = new JLabel(script.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        name.setForeground(TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(name);

        JLabel authorLabel = new JLabel("by " + script.getAuthorName());
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        authorLabel.setForeground(TEXT_MUTED);
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(authorLabel);

        if (script.getDescription() != null && !script.getDescription().isEmpty()) {
            JLabel desc = new JLabel(truncate(script.getDescription(), 60));
            desc.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            desc.setForeground(TEXT_MUTED);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(desc);
        }

        card.add(contentPanel, BorderLayout.CENTER);

        // Vote count on the right
        JLabel votes = new JLabel("↑" + script.getVotes());
        votes.setFont(new Font("Segoe UI", Font.BOLD, 14));
        votes.setForeground(SUCCESS);
        card.add(votes, BorderLayout.EAST);

        return card;
    }

    private void refreshScripts() {
        setStatus("Loading scripts...");
        NoidUser user = userSupplier.get();

        if (user == null) {
            setStatus("⚠️ Please log in via Noid to use Script SDN");
            return;
        }

        sdnService.fetchScripts(user).thenAccept(result -> {
            scripts = result;
            SwingUtilities.invokeLater(() -> {
                updateScriptList();
                setStatus("Loaded " + scripts.size() + " scripts");
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> setStatus("❌ Failed to load scripts"));
            return null;
        });
    }

    private void updateScriptList() {
        scriptListPanel.removeAll();

        if (scripts == null || scripts.isEmpty()) {
            JLabel emptyLabel = new JLabel("No scripts available yet. Be the first to submit!");
            emptyLabel.setForeground(TEXT_MUTED);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            scriptListPanel.add(emptyLabel);
        } else {
            for (ScriptEntry script : scripts) {
                scriptListPanel.add(createScriptCard(script));
                scriptListPanel.add(Box.createVerticalStrut(8));
            }
        }

        scriptListPanel.revalidate();
        scriptListPanel.repaint();
    }

    private void showSubmitDialog() {
        NoidUser user = userSupplier.get();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please log in via Noid first.", "Not Logged In",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Script Name:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField nameField = new JTextField(25);
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField descField = new JTextField(25);
        panel.add(descField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Script Content:"), gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        JTextArea contentArea = new JTextArea(10, 40);
        contentArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(contentArea);
        panel.add(scroll, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Submit Script", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            String content = contentArea.getText().trim();

            if (name.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and content are required.", "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            setStatus("Submitting script...");
            sdnService.submitScript(user, name, desc, content).thenAccept(script -> {
                SwingUtilities.invokeLater(() -> {
                    setStatus("✅ Script submitted!");
                    refreshScripts();
                });
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    setStatus("❌ " + ex.getCause().getMessage());
                    JOptionPane.showMessageDialog(this, ex.getCause().getMessage(), "Submit Failed",
                            JOptionPane.ERROR_MESSAGE);
                });
                return null;
            });
        }
    }

    private void showEditDialog(ScriptEntry script) {
        NoidUser user = userSupplier.get();
        if (user == null)
            return;

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Script Name:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField nameField = new JTextField(script.getName(), 25);
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField descField = new JTextField(script.getDescription(), 25);
        panel.add(descField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Script Content:"), gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        JTextArea contentArea = new JTextArea(script.getContent(), 10, 40);
        contentArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(contentArea);
        panel.add(scroll, gbc);

        int dialogResult = JOptionPane.showConfirmDialog(this, panel, "Edit Script", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (dialogResult == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            String content = contentArea.getText().trim();

            setStatus("Updating script...");
            sdnService.updateScript(user, script.getId(), name, desc, content).thenAccept(updated -> {
                SwingUtilities.invokeLater(() -> {
                    setStatus("✅ Script updated!");
                    refreshScripts();
                });
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> setStatus("❌ " + ex.getCause().getMessage()));
                return null;
            });
        }
    }

    private void confirmDelete(ScriptEntry script) {
        int dialogResult = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete \"" + script.getName() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (dialogResult == JOptionPane.YES_OPTION) {
            NoidUser user = userSupplier.get();
            if (user == null)
                return;

            setStatus("Deleting script...");
            sdnService.deleteScript(user, script.getId()).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    setStatus("✅ Script deleted!");
                    showWelcomeScreen(detailPanel);
                    refreshScripts();
                });
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> setStatus("❌ " + ex.getCause().getMessage()));
                return null;
            });
        }
    }

    private void vote(ScriptEntry script) {
        NoidUser user = userSupplier.get();
        if (user == null) {
            setStatus("⚠️ Please log in to vote");
            return;
        }

        sdnService.voteScript(user, script.getId()).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                script.setVotes(result.votes);
                script.setHasVoted(result.hasVoted);
                showScriptDetail(script);
                updateScriptList();
                setStatus("✅ Thanks for voting!");
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> setStatus("❌ " + ex.getCause().getMessage()));
            return null;
        });
    }

    // Helper methods
    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String formatDate(String isoDate) {
        if (isoDate == null)
            return "";
        return isoDate.substring(0, 10);
    }
}
