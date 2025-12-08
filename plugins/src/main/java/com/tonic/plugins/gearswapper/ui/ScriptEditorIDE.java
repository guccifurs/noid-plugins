package com.tonic.plugins.gearswapper.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * IDE-like script editor for GearSwapper loadouts and loops.
 * Features: syntax highlighting, autocomplete, real-time validation, line
 * numbers.
 */
public class ScriptEditorIDE extends JPanel {

    private final JTextPane editor;
    private final StyledDocument doc;
    private final ScriptSyntaxHighlighter highlighter;
    private final ScriptAutocomplete autocomplete;
    private final ScriptValidator validator;

    private final JTextArea lineNumbers;
    private final JTextArea errorPanel;
    private final JLabel statusLabel;

    private boolean isUpdating = false;
    private javax.swing.Timer highlightTimer;

    public ScriptEditorIDE() {
        this("");
    }

    public ScriptEditorIDE(String initialScript) {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(30, 30, 30));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create main editor
        editor = new JTextPane();
        doc = editor.getStyledDocument();

        // Setup components
        highlighter = new ScriptSyntaxHighlighter(doc);
        autocomplete = new ScriptAutocomplete(editor);
        validator = new ScriptValidator();

        // Style the editor
        editor.setBackground(new Color(30, 30, 30));
        editor.setCaretColor(Color.WHITE);
        editor.setFont(new Font("Consolas", Font.PLAIN, 13));
        editor.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Set default text style
        SimpleAttributeSet defaultAttrs = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultAttrs, new Color(212, 212, 212));
        StyleConstants.setFontFamily(defaultAttrs, "Consolas");
        StyleConstants.setFontSize(defaultAttrs, 13);
        editor.setCharacterAttributes(defaultAttrs, true);

        // Line numbers
        lineNumbers = new JTextArea("1");
        lineNumbers.setBackground(new Color(37, 37, 38));
        lineNumbers.setForeground(new Color(133, 133, 133));
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 13));
        lineNumbers.setEditable(false);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        lineNumbers.setPreferredSize(new Dimension(40, 0));

        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(new Color(78, 201, 176));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        // Error panel
        errorPanel = new JTextArea();
        errorPanel.setBackground(new Color(37, 37, 38));
        errorPanel.setForeground(new Color(244, 71, 71));
        errorPanel.setFont(new Font("Consolas", Font.PLAIN, 11));
        errorPanel.setEditable(false);
        errorPanel.setLineWrap(true);
        errorPanel.setWrapStyleWord(true);
        errorPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        errorPanel.setRows(2);
        errorPanel.setVisible(false);

        // Layout
        JPanel editorPanel = new JPanel(new BorderLayout(0, 0));
        editorPanel.setBackground(new Color(30, 30, 30));
        editorPanel.add(lineNumbers, BorderLayout.WEST);

        JScrollPane scrollPane = new JScrollPane(editor);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(new Color(30, 30, 30));

        editorPanel.add(scrollPane, BorderLayout.CENTER);

        // Toolbar
        JPanel toolbar = createToolbar();

        // Bottom panel with error and status
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 0));
        bottomPanel.setBackground(new Color(37, 37, 38));

        JScrollPane errorScroll = new JScrollPane(errorPanel);
        errorScroll.setBorder(null);
        errorScroll.setPreferredSize(new Dimension(0, 50));
        bottomPanel.add(errorScroll, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0, 122, 204));
        statusBar.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);

        add(toolbar, BorderLayout.NORTH);
        add(editorPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Setup event handlers
        setupEventHandlers();

        // Set initial content
        if (initialScript != null && !initialScript.isEmpty()) {
            editor.setText(initialScript);
            SwingUtilities.invokeLater(this::refreshAll);
        }

        // Debounce timer for highlighting
        highlightTimer = new javax.swing.Timer(150, e -> {
            refreshAll();
            highlightTimer.stop();
        });
        highlightTimer.setRepeats(false);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setBackground(new Color(37, 37, 38));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)));

        // Insert dropdown
        JButton insertBtn = createToolbarButton("Insert...");
        JPopupMenu insertMenu = new JPopupMenu();
        insertMenu.setBackground(new Color(37, 37, 38));

        addInsertMenuItem(insertMenu, "If...EndIf Block", "if(condition)\n    // commands\nendif");
        addInsertMenuItem(insertMenu, "If...Else...EndIf Block",
                "if(condition)\n    // if true\nelse\n    // if false\nendif");
        insertMenu.addSeparator();
        addInsertMenuItem(insertMenu, "Item Command", "Item: ");
        addInsertMenuItem(insertMenu, "Cast Command", "Cast: ");
        addInsertMenuItem(insertMenu, "Prayer Command", "Prayer: ");
        addInsertMenuItem(insertMenu, "Special Attack", "Special");
        addInsertMenuItem(insertMenu, "Attack Target", "Attack");
        insertMenu.addSeparator();
        addInsertMenuItem(insertMenu, "Wait Ticks", "Tick:1");
        addInsertMenuItem(insertMenu, "Wait Milliseconds", "Wait:500");
        addInsertMenuItem(insertMenu, "Move to Target", "Move:1");
        insertMenu.addSeparator();
        addInsertMenuItem(insertMenu, "Spec Check Template", "if(spec >= 50)\n    Special\n    Attack\nendif");
        addInsertMenuItem(insertMenu, "HP Check Template", "if(hp < 30)\n    Item: Shark\nendif");
        addInsertMenuItem(insertMenu, "Frozen Check Template",
                "if(target_frozen)\n    Prayer: Rigour\nelse\n    Prayer: Augury\nendif");

        insertBtn.addActionListener(e -> insertMenu.show(insertBtn, 0, insertBtn.getHeight()));
        toolbar.add(insertBtn);

        // Reference button
        JButton refBtn = createToolbarButton("Reference");
        refBtn.addActionListener(e -> showReferenceDialog());
        toolbar.add(refBtn);

        toolbar.add(Box.createHorizontalStrut(20));

        // Copy button
        JButton copyBtn = createToolbarButton("Copy All");
        copyBtn.addActionListener(e -> {
            editor.selectAll();
            editor.copy();
            editor.setCaretPosition(editor.getDocument().getLength());
            setStatus("Copied to clipboard", new Color(78, 201, 176));
        });
        toolbar.add(copyBtn);

        // Clear button
        JButton clearBtn = createToolbarButton("Clear");
        clearBtn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "Clear all script content?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                editor.setText("");
                refreshAll();
            }
        });
        toolbar.add(clearBtn);

        return toolbar;
    }

    private JButton createToolbarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setBackground(new Color(60, 60, 60));
        btn.setForeground(new Color(212, 212, 212));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(60, 60, 60));
            }
        });

        return btn;
    }

    private void addInsertMenuItem(JPopupMenu menu, String label, String text) {
        JMenuItem item = new JMenuItem(label);
        item.setBackground(new Color(37, 37, 38));
        item.setForeground(new Color(212, 212, 212));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.addActionListener(e -> insertText(text));
        menu.add(item);
    }

    private void insertText(String text) {
        try {
            int pos = editor.getCaretPosition();
            doc.insertString(pos, text, null);
            editor.setCaretPosition(pos + text.length());

            // Move caret to first placeholder if present
            int placeholderIdx = text.indexOf("condition");
            if (placeholderIdx >= 0) {
                editor.setCaretPosition(pos + placeholderIdx);
                editor.moveCaretPosition(pos + placeholderIdx + "condition".length());
            }

            refreshAll();
        } catch (BadLocationException ex) {
            // Ignore
        }
    }

    private void setupEventHandlers() {
        // Document listener for changes
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleRefresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleRefresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Attribute changes, ignore
            }
        });

        // Key listener for autocomplete trigger
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                // Trigger autocomplete on specific characters
                if (c == ':' || c == '(') {
                    SwingUtilities.invokeLater(() -> autocomplete.trigger());
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // Ctrl+Space for manual autocomplete
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    autocomplete.trigger();
                    e.consume();
                }
            }
        });

        // Caret listener for line highlighting
        editor.addCaretListener(e -> updateLineNumbers());
    }

    private void scheduleRefresh() {
        if (!isUpdating) {
            highlightTimer.restart();
        }
    }

    private void refreshAll() {
        if (isUpdating)
            return;
        isUpdating = true;

        try {
            // Update line numbers
            updateLineNumbers();

            // Apply syntax highlighting
            highlighter.highlightAll();

            // Validate and show errors
            validateAndShowErrors();

        } finally {
            isUpdating = false;
        }
    }

    private void updateLineNumbers() {
        String text = editor.getText();
        int lines = text.split("\n", -1).length;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i).append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    private void validateAndShowErrors() {
        String script = editor.getText();
        List<ScriptValidator.ValidationError> errors = validator.validate(script);

        if (errors.isEmpty()) {
            errorPanel.setVisible(false);
            setStatus("No errors", new Color(78, 201, 176));
        } else {
            StringBuilder sb = new StringBuilder();
            int errorCount = 0;
            int warningCount = 0;

            for (ScriptValidator.ValidationError err : errors) {
                sb.append(err.toString()).append("\n");
                if (err.severity == ScriptValidator.ErrorSeverity.ERROR) {
                    errorCount++;
                } else {
                    warningCount++;
                }
            }

            errorPanel.setText(sb.toString().trim());
            errorPanel.setVisible(true);

            if (errorCount > 0) {
                setStatus(errorCount + " error(s)" + (warningCount > 0 ? ", " + warningCount + " warning(s)" : ""),
                        new Color(244, 71, 71));
            } else {
                setStatus(warningCount + " warning(s)", new Color(255, 193, 7));
            }
        }

        revalidate();
        repaint();
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText("  " + text);
        statusLabel.getParent().setBackground(color.darker().darker());
    }

    private void showReferenceDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Command Reference", true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        JTextPane refPane = new JTextPane();
        refPane.setContentType("text/html");
        refPane.setEditable(false);
        refPane.setBackground(new Color(37, 37, 38));

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Segoe UI; color: #d4d4d4; background: #252526; padding: 10px;'>");

        // Commands section
        html.append("<h2 style='color: #569cd6;'>Commands</h2>");
        html.append("<table style='width: 100%;'>");
        for (String cmd : ScriptSyntaxHighlighter.COMMANDS) {
            html.append("<tr><td style='color: #569cd6; font-weight: bold;'>").append(cmd)
                    .append(":</td><td style='color: #ce9178;'>value</td></tr>");
        }
        html.append("</table>");

        // Conditions section
        html.append("<h2 style='color: #4ec9b0;'>Conditions</h2>");
        html.append("<p style='color: #9cdcfe;'>");
        for (String cond : ScriptSyntaxHighlighter.CONDITIONS) {
            html.append(cond).append(", ");
        }
        html.append("</p>");

        // Numeric variables
        html.append("<h2 style='color: #4ec9b0;'>Numeric Variables</h2>");
        html.append("<p style='color: #9cdcfe;'>");
        for (String var : ScriptSyntaxHighlighter.NUMERIC_VARS) {
            html.append(var).append(", ");
        }
        html.append("</p>");

        // Pattern conditions
        html.append("<h2 style='color: #4ec9b0;'>Pattern Conditions</h2>");
        html.append("<p style='color: #9cdcfe;'>");
        for (String prefix : ScriptSyntaxHighlighter.CONDITION_PREFIXES) {
            html.append(prefix).append("VALUE, ");
        }
        html.append("</p>");

        html.append("</body></html>");

        refPane.setText(html.toString());
        refPane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(refPane);
        scroll.setBorder(null);
        dialog.add(scroll);
        dialog.setVisible(true);
    }

    /**
     * Get the current script text.
     */
    public String getScript() {
        return editor.getText();
    }

    /**
     * Set the script text.
     */
    public void setScript(String script) {
        editor.setText(script != null ? script : "");
        SwingUtilities.invokeLater(this::refreshAll);
    }

    /**
     * Check if script is valid.
     */
    public boolean isScriptValid() {
        return validator.isValid(getScript());
    }

    /**
     * Get validation errors.
     */
    public List<ScriptValidator.ValidationError> getErrors() {
        return validator.validate(getScript());
    }

    /**
     * Request focus on the editor.
     */
    public void focusEditor() {
        editor.requestFocusInWindow();
    }
}
