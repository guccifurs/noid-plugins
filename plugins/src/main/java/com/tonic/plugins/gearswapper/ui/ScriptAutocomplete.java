package com.tonic.plugins.gearswapper.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Autocomplete popup for GearSwapper script editor.
 * Shows suggestions based on context and allows selection via keyboard/mouse.
 */
public class ScriptAutocomplete {

    private final JTextPane editor;
    private final JPopupMenu popup;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> listModel;

    private String currentPrefix = "";
    private int triggerOffset = 0;

    public ScriptAutocomplete(JTextPane editor) {
        this.editor = editor;
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.popup = new JPopupMenu();

        setupUI();
        setupKeyBindings();
    }

    private void setupUI() {
        // Style the suggestion list
        suggestionList.setFont(new Font("Consolas", Font.PLAIN, 12));
        suggestionList.setBackground(new Color(37, 37, 38));
        suggestionList.setForeground(new Color(212, 212, 212));
        suggestionList.setSelectionBackground(new Color(62, 62, 66));
        suggestionList.setSelectionForeground(new Color(255, 255, 255));
        suggestionList.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        suggestionList.setVisibleRowCount(8);

        // Custom cell renderer for icons
        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                String text = value.toString();
                String icon = getIconForSuggestion(text);
                setText(icon + " " + text);

                setBackground(isSelected ? new Color(62, 62, 66) : new Color(37, 37, 38));
                setForeground(isSelected ? Color.WHITE : new Color(212, 212, 212));
                setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

                return this;
            }
        });

        // Double-click to select
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelected();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        scrollPane.setPreferredSize(new Dimension(250, 180));

        popup.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        popup.add(scrollPane);
    }

    private String getIconForSuggestion(String text) {
        String lower = text.toLowerCase();

        // Commands
        if (ScriptSyntaxHighlighter.COMMANDS.stream().anyMatch(c -> lower.startsWith(c.toLowerCase()))) {
            return "[C]";
        }
        // Keywords
        if (ScriptSyntaxHighlighter.KEYWORDS.contains(lower.replace(":", ""))) {
            return "[K]";
        }
        // Prayers
        if (ScriptSyntaxHighlighter.PRAYERS.stream().anyMatch(p -> p.equalsIgnoreCase(text))) {
            return "[P]";
        }
        // Spells
        if (ScriptSyntaxHighlighter.SPELLS.stream().anyMatch(s -> s.equalsIgnoreCase(text))) {
            return "[S]";
        }
        // Conditions
        if (ScriptSyntaxHighlighter.CONDITIONS.contains(lower) ||
                ScriptSyntaxHighlighter.NUMERIC_VARS.contains(lower)) {
            return "[?]";
        }

        return "   ";
    }

    private void setupKeyBindings() {
        // Tab or Enter to insert selection
        InputMap im = editor.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = editor.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "autocomplete-insert");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "autocomplete-insert-enter");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "autocomplete-cancel");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "autocomplete-up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "autocomplete-down");

        am.put("autocomplete-insert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    insertSelected();
                } else {
                    // Default tab behavior
                    try {
                        editor.getDocument().insertString(editor.getCaretPosition(), "    ", null);
                    } catch (BadLocationException ex) {
                    }
                }
            }
        });

        am.put("autocomplete-insert-enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    insertSelected();
                } else {
                    // Default enter behavior
                    try {
                        editor.getDocument().insertString(editor.getCaretPosition(), "\n", null);
                    } catch (BadLocationException ex) {
                    }
                }
            }
        });

        am.put("autocomplete-cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });

        am.put("autocomplete-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    int idx = suggestionList.getSelectedIndex();
                    if (idx > 0) {
                        suggestionList.setSelectedIndex(idx - 1);
                        suggestionList.ensureIndexIsVisible(idx - 1);
                    }
                }
            }
        });

        am.put("autocomplete-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    int idx = suggestionList.getSelectedIndex();
                    if (idx < listModel.size() - 1) {
                        suggestionList.setSelectedIndex(idx + 1);
                        suggestionList.ensureIndexIsVisible(idx + 1);
                    }
                }
            }
        });
    }

    /**
     * Trigger autocomplete at the current caret position.
     */
    public void trigger() {
        try {
            int caretPos = editor.getCaretPosition();
            String text = editor.getDocument().getText(0, caretPos);

            // Find current line
            int lineStart = text.lastIndexOf('\n') + 1;
            String currentLine = text.substring(lineStart);
            int cursorInLine = caretPos - lineStart;

            // Get suggestions
            List<String> suggestions = getSuggestions(currentLine, cursorInLine);

            if (suggestions.isEmpty()) {
                hide();
                return;
            }

            // Find trigger point (where to start replacement)
            currentPrefix = findPrefix(currentLine, cursorInLine);
            triggerOffset = caretPos - currentPrefix.length();

            // Filter suggestions by prefix
            if (!currentPrefix.isEmpty()) {
                String prefixLower = currentPrefix.toLowerCase();
                suggestions = new ArrayList<>();
                for (String s : getSuggestions(currentLine.substring(0, cursorInLine - currentPrefix.length()),
                        cursorInLine - currentPrefix.length())) {
                    if (s.toLowerCase().startsWith(prefixLower) ||
                            s.toLowerCase().contains(prefixLower)) {
                        suggestions.add(s);
                    }
                }
            }

            if (suggestions.isEmpty()) {
                hide();
                return;
            }

            // Update list
            listModel.clear();
            for (String s : suggestions) {
                listModel.addElement(s);
            }
            suggestionList.setSelectedIndex(0);

            // Position popup
            try {
                Rectangle rect = editor.modelToView2D(caretPos).getBounds();
                popup.show(editor, rect.x, rect.y + rect.height);
            } catch (Exception e) {
                // Fallback position
                popup.show(editor, 10, 10);
            }

        } catch (BadLocationException e) {
            hide();
        }
    }

    private String findPrefix(String line, int cursorPos) {
        if (cursorPos <= 0)
            return "";

        StringBuilder prefix = new StringBuilder();
        for (int i = cursorPos - 1; i >= 0; i--) {
            char c = line.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                prefix.insert(0, c);
            } else {
                break;
            }
        }
        return prefix.toString();
    }

    private List<String> getSuggestions(String line, int cursorPos) {
        List<String> suggestions = new ArrayList<>();
        String lower = line.toLowerCase().trim();

        // After "Prayer:" or "TogglePray:"
        if (lower.endsWith("prayer:") || lower.endsWith("togglepray:") ||
                lower.contains("prayer: ") || lower.contains("togglepray: ")) {
            suggestions.addAll(ScriptSyntaxHighlighter.PRAYERS);
            return suggestions;
        }

        // After "Cast:"
        if (lower.endsWith("cast:") || lower.contains("cast: ")) {
            suggestions.addAll(ScriptSyntaxHighlighter.SPELLS);
            return suggestions;
        }

        // Inside if()
        if (lower.contains("if(") || lower.contains("if (")) {
            int parenIdx = lower.lastIndexOf("(");
            if (parenIdx >= 0 && !lower.substring(parenIdx).contains(")")) {
                suggestions.addAll(ScriptSyntaxHighlighter.CONDITIONS);
                suggestions.addAll(ScriptSyntaxHighlighter.NUMERIC_VARS);
                suggestions.add("targetpraying_melee");
                suggestions.add("targetpraying_mage");
                suggestions.add("targetpraying_range");
                suggestions.add("animation_");
                suggestions.add("regionid_");
                return suggestions;
            }
        }

        // Start of line or empty - suggest commands and keywords
        if (lower.isEmpty() || !lower.contains(":")) {
            suggestions.addAll(ScriptSyntaxHighlighter.COMMANDS);
            suggestions.addAll(ScriptSyntaxHighlighter.KEYWORDS);
        }

        return suggestions;
    }

    /**
     * Insert the selected suggestion.
     */
    private void insertSelected() {
        String selected = suggestionList.getSelectedValue();
        if (selected == null) {
            hide();
            return;
        }

        try {
            StyledDocument doc = editor.getStyledDocument();
            int caretPos = editor.getCaretPosition();

            // Remove the prefix we're replacing
            if (triggerOffset < caretPos) {
                doc.remove(triggerOffset, caretPos - triggerOffset);
            }

            // Insert selected text
            doc.insertString(triggerOffset, selected, null);

            // Add colon for commands
            if (ScriptSyntaxHighlighter.COMMANDS.contains(selected)) {
                doc.insertString(triggerOffset + selected.length(), ": ", null);
            }

        } catch (BadLocationException e) {
            // Ignore
        }

        hide();
    }

    /**
     * Hide the autocomplete popup.
     */
    public void hide() {
        popup.setVisible(false);
    }

    /**
     * Check if popup is visible.
     */
    public boolean isVisible() {
        return popup.isVisible();
    }
}
