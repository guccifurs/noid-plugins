package com.tonic.plugins.gearswapper.ui;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Enhanced autocomplete popup for GearSwapper script editor.
 * Shows suggestions AS YOU TYPE with a documentation panel showing examples.
 */
public class ScriptAutocomplete {

    private final JTextPane editor;
    private final JPopupMenu popup;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> listModel;
    private final JTextArea docPanel;

    private String currentPrefix = "";
    private int triggerOffset = 0;
    private boolean isActive = false;

    // Command documentation map
    private static final Map<String, String> DOCS = new HashMap<>();
    static {
        // Equipment commands
        DOCS.put("Item",
                "Equip/use an item by name\n\nSyntax: Item: <name>\n\nExamples:\n  Item: Dragon claws\n  Item: Armadyl godsword\n  Item: Super restore(*)");
        DOCS.put("Cast",
                "Cast a spell\n\nSyntax: Cast: <spell>\n\nExamples:\n  Cast: Ice Barrage\n  Cast: Vengeance\n  Cast: Teleblock");
        DOCS.put("Prayer",
                "Turn ON a prayer\n\nSyntax: Prayer: <name>\n\nExamples:\n  Prayer: Piety\n  Prayer: Protect from Melee\n  Prayer: Rigour");
        DOCS.put("TogglePray",
                "Toggle prayer ON/OFF\n\nSyntax: TogglePray: <name>\n\nExamples:\n  TogglePray: Augury\n  TogglePray: Steel Skin");
        DOCS.put("Special", "Enable special attack\n\nSyntax: Special\n\n(No value needed)");
        DOCS.put("Attack", "Attack current target\n\nSyntax: Attack\n\n(No value needed)");

        // Movement
        DOCS.put("Move", "Move N tiles toward target\n\nSyntax: Move:<tiles>\n\nExamples:\n  Move:1\n  Move:3");
        DOCS.put("MoveDiag", "Move diagonally\n\nSyntax: MoveDiag:<tiles>\n\nExamples:\n  MoveDiag:1");
        DOCS.put("Walk", "Walk to coordinates\n\nSyntax: Walk:<x>,<y>\n\nExamples:\n  Walk:3200,3400");
        DOCS.put("MeleeRange", "Move to melee distance\n\nSyntax: MeleeRange\n\n(No value needed)");

        // Timing
        DOCS.put("Tick", "Wait N game ticks (~600ms each)\n\nSyntax: Tick:<count>\n\nExamples:\n  Tick:1\n  Tick:3");
        DOCS.put("Wait",
                "Wait in milliseconds\nSupports range format\n\nSyntax: Wait:<ms> or Wait:<min>:<max>\n\nExamples:\n  Wait:500\n  Wait:100:500");
        DOCS.put("WaitAnim", "Wait for animation ID\n\nSyntax: WaitAnim:<animId>\n\nExamples:\n  WaitAnim:7514");
        DOCS.put("WaitIfWalking", "Wait while player is walking\n\nSyntax: WaitIfWalking\n\n(No value needed)");
        DOCS.put("WaitIfMoving", "Wait while player is moving\n\nSyntax: WaitIfMoving\n\n(No value needed)");

        // Utility
        DOCS.put("Log",
                "Print debug message\n\nSyntax: Log: <message>\n\nExamples:\n  Log: Script started\n  Log: Target found");
        DOCS.put("Npc",
                "Interact with NPC\n\nSyntax: Npc:<name>:<action>\n\nExamples:\n  Npc:Banker:Bank\n  Npc:*goblin*:Attack");
        DOCS.put("Object",
                "Interact with game object\n\nSyntax: Object:<name>:<action>\n\nExamples:\n  Object:Cave:Crawl-through\n  Object:Door:Open");
        DOCS.put("DropAll",
                "Drop matching items\n\nSyntax: DropAll:<pattern>\n\nExamples:\n  DropAll:*bones*\n  DropAll:Fish");

        // Memory/State
        DOCS.put("SetMemory",
                "Store a value in memory\n\nSyntax: SetMemory:<key>:<value>\n\nExamples:\n  SetMemory:World:UK\n  SetMemory:Phase:2");
        DOCS.put("ClearMemory", "Clear a memory variable\n\nSyntax: ClearMemory:<key>");
        DOCS.put("SetTickTimer",
                "Set tick timer countdown\n\nSyntax: SetTickTimer:<ticks>\n\nExamples:\n  SetTickTimer:40\n  SetTickTimer:10");

        // Targeting
        DOCS.put("Target", "Set current target\n\nSyntax: Target:<name>\n\nExamples:\n  Target:*player*");
        DOCS.put("ClearTarget", "Clear current target\n\nSyntax: ClearTarget");

        // Run
        DOCS.put("ToggleRun", "Toggle run on/off\n\nSyntax: ToggleRun");
        DOCS.put("RunEnergy", "Set run energy threshold\n\nSyntax: RunEnergy:<percent>");

        // AFK/Random
        DOCS.put("RandomAfkChance",
                "Random AFK chance\n\nSyntax: RandomAfkChance\n\nMay pause randomly to appear human-like");
        DOCS.put("AfkChance", "Same as RandomAfkChance\n\nSyntax: AfkChance");

        // Keywords
        DOCS.put("if",
                "Conditional block\n\nSyntax:\n  if(<condition>) {\n    commands\n  }\n\nOr:\n  if(<condition>)\n    commands\n  endif");
        DOCS.put("else", "Else branch\n\nSyntax:\n  if(cond) {\n    ...\n  } else {\n    ...\n  }");
        DOCS.put("endif", "End if block\n\nSyntax:\n  if(cond)\n    ...\n  endif");

        // Conditions
        DOCS.put("frozen", "Check if player is frozen\n\nUsage: if(frozen)");
        DOCS.put("has_target", "Check if has target\n\nUsage: if(has_target)");
        DOCS.put("incombat", "Check if in combat\n\nUsage: if(incombat)");
        DOCS.put("moving", "Check if moving\n\nUsage: if(moving)");
        DOCS.put("target_frozen", "Check if target is frozen\n\nUsage: if(target_frozen)");
        DOCS.put("inventoryfull", "Check if inventory full\n\nUsage: if(inventoryfull)");

        // Numeric
        DOCS.put("spec", "Special attack energy\n\nUsage: if(spec >= 50)");
        DOCS.put("hp", "Current hitpoints\n\nUsage: if(hp < 30)");
        DOCS.put("prayer", "Current prayer points\n\nUsage: if(prayer > 0)");
        DOCS.put("distance", "Distance to target\n\nUsage: if(distance <= 1)");
        DOCS.put("idleticks", "Ticks since last action\n\nUsage: if(IdleTicks > 5)");
        DOCS.put("ticktimer", "Tick timer countdown\n\nUsage: if(TickTimer == 0)");

        // Pattern conditions
        DOCS.put("skillbelow_",
                "Check if skill below level\n\nSyntax: skillbelow_<skill>_<level>\n\nExamples:\n  if(skillbelow_strength_99)\n  if(skillbelow_prayer_60)");
        DOCS.put("skillabove_", "Check if skill above level\n\nSyntax: skillabove_<skill>_<level>");
        DOCS.put("targetpraying_",
                "Check target's prayer\n\nSyntax: targetpraying_<type>\n\nExamples:\n  if(targetpraying_melee)\n  if(targetpraying_mage)");
        DOCS.put("animation_", "Check player animation\n\nSyntax: animation_<id>\n\nExamples:\n  if(animation_7514)");
        DOCS.put("regionid_", "Check region/area ID\n\nSyntax: regionid_<id>");
    }

    public ScriptAutocomplete(JTextPane editor) {
        this.editor = editor;
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.popup = new JPopupMenu();

        // Documentation panel
        this.docPanel = new JTextArea();
        docPanel.setFont(new Font("Consolas", Font.PLAIN, 11));
        docPanel.setBackground(new Color(45, 45, 48));
        docPanel.setForeground(new Color(180, 180, 180));
        docPanel.setEditable(false);
        docPanel.setLineWrap(true);
        docPanel.setWrapStyleWord(true);
        docPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        setupUI();
        setupKeyBindings();
        setupRealtimeTyping();
    }

    private void setupUI() {
        // Style the suggestion list
        suggestionList.setFont(new Font("Consolas", Font.PLAIN, 12));
        suggestionList.setBackground(new Color(37, 37, 38));
        suggestionList.setForeground(new Color(212, 212, 212));
        suggestionList.setSelectionBackground(new Color(0, 122, 204));
        suggestionList.setSelectionForeground(Color.WHITE);
        suggestionList.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        suggestionList.setVisibleRowCount(10);

        // CRITICAL: Make non-focusable so typing continues in editor
        suggestionList.setFocusable(false);
        popup.setFocusable(false);
        docPanel.setFocusable(false);

        // Custom cell renderer
        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                String text = value.toString();
                String icon = getIconForSuggestion(text);
                Color iconColor = getColorForIcon(icon);

                // Use HTML for colored icons
                setText("<html><span style='color:" + toHex(iconColor) + "'>" + icon + "</span> " + text + "</html>");

                setBackground(isSelected ? new Color(0, 122, 204) : new Color(37, 37, 38));
                setForeground(isSelected ? Color.WHITE : new Color(212, 212, 212));
                setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

                return this;
            }
        });

        // Selection listener to update documentation
        suggestionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDocPanel();
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

        // Left panel: suggestions
        JScrollPane suggestScroll = new JScrollPane(suggestionList);
        suggestScroll.setBorder(null);
        suggestScroll.setPreferredSize(new Dimension(200, 220));
        suggestScroll.getViewport().setBackground(new Color(37, 37, 38));

        // Right panel: documentation
        JScrollPane docScroll = new JScrollPane(docPanel);
        docScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(60, 60, 60)));
        docScroll.setPreferredSize(new Dimension(250, 220));
        docScroll.getViewport().setBackground(new Color(45, 45, 48));

        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(new Color(37, 37, 38));
        mainPanel.add(suggestScroll, BorderLayout.WEST);
        mainPanel.add(docScroll, BorderLayout.CENTER);

        popup.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        popup.add(mainPanel);
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Color getColorForIcon(String icon) {
        switch (icon) {
            case "[C]":
                return new Color(86, 156, 214); // Blue - commands
            case "[K]":
                return new Color(197, 134, 192); // Purple - keywords
            case "[P]":
                return new Color(78, 201, 176); // Teal - prayers
            case "[S]":
                return new Color(206, 145, 120); // Orange - spells
            case "[?]":
                return new Color(181, 206, 168); // Green - conditions
            default:
                return new Color(150, 150, 150);
        }
    }

    private void updateDocPanel() {
        String selected = suggestionList.getSelectedValue();
        if (selected == null) {
            docPanel.setText("Select an item to see documentation");
            return;
        }

        // Look up documentation
        String doc = DOCS.get(selected);
        if (doc == null) {
            // Try prefix match
            for (String key : DOCS.keySet()) {
                if (selected.toLowerCase().startsWith(key.toLowerCase())) {
                    doc = DOCS.get(key);
                    break;
                }
            }
        }

        if (doc != null) {
            docPanel.setText(doc);
            docPanel.setCaretPosition(0);
        } else {
            // Generate basic doc
            if (ScriptSyntaxHighlighter.COMMANDS.contains(selected)) {
                docPanel.setText(selected + "\n\nSyntax: " + selected + ": <value>");
            } else if (ScriptSyntaxHighlighter.PRAYERS.contains(selected)) {
                docPanel.setText(
                        "Prayer: " + selected + "\n\nUse with Prayer: command\n\nExample:\n  Prayer: " + selected);
            } else if (ScriptSyntaxHighlighter.SPELLS.contains(selected)) {
                docPanel.setText("Spell: " + selected + "\n\nUse with Cast: command\n\nExample:\n  Cast: " + selected);
            } else {
                docPanel.setText(selected + "\n\nNo documentation available");
            }
        }
    }

    private String getIconForSuggestion(String text) {
        String lower = text.toLowerCase();

        if (ScriptSyntaxHighlighter.COMMANDS.stream().anyMatch(c -> c.equalsIgnoreCase(text))) {
            return "[C]";
        }
        if (ScriptSyntaxHighlighter.KEYWORDS.stream().anyMatch(k -> k.equalsIgnoreCase(text))) {
            return "[K]";
        }
        if (ScriptSyntaxHighlighter.PRAYERS.stream().anyMatch(p -> p.equalsIgnoreCase(text))) {
            return "[P]";
        }
        if (ScriptSyntaxHighlighter.SPELLS.stream().anyMatch(s -> s.equalsIgnoreCase(text))) {
            return "[S]";
        }
        if (ScriptSyntaxHighlighter.CONDITIONS.contains(lower) ||
                ScriptSyntaxHighlighter.NUMERIC_VARS.contains(lower)) {
            return "[?]";
        }

        return "   ";
    }

    private void setupKeyBindings() {
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
     * Setup real-time typing detection for autocomplete.
     */
    private void setupRealtimeTyping() {
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // Delay to let the document update
                SwingUtilities.invokeLater(() -> {
                    if (e.getLength() == 1) { // Single character typed
                        triggerIfAppropriate();
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // On delete, potentially hide or update
                SwingUtilities.invokeLater(() -> triggerIfAppropriate());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    /**
     * Check if we should trigger autocomplete based on current typing.
     */
    private void triggerIfAppropriate() {
        try {
            int caretPos = editor.getCaretPosition();
            if (caretPos == 0) {
                hide();
                return;
            }

            String text = editor.getDocument().getText(0, caretPos);
            int lineStart = text.lastIndexOf('\n') + 1;
            String currentLine = text.substring(lineStart);

            // Find what user is typing
            String prefix = findPrefix(currentLine, currentLine.length());

            // Trigger if typing 2+ characters
            if (prefix.length() >= 2) {
                trigger();
            } else if (prefix.length() == 0) {
                hide();
            }
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    /**
     * Trigger autocomplete at the current caret position.
     */
    public void trigger() {
        try {
            int caretPos = editor.getCaretPosition();
            String text = editor.getDocument().getText(0, caretPos);

            int lineStart = text.lastIndexOf('\n') + 1;
            String currentLine = text.substring(lineStart);
            int cursorInLine = caretPos - lineStart;

            currentPrefix = findPrefix(currentLine, cursorInLine);
            triggerOffset = caretPos - currentPrefix.length();

            // Get all possible suggestions
            List<String> allSuggestions = getSuggestions(currentLine, cursorInLine);

            // Filter by prefix
            List<String> filtered = new ArrayList<>();
            String prefixLower = currentPrefix.toLowerCase();

            for (String s : allSuggestions) {
                if (prefixLower.isEmpty() ||
                        s.toLowerCase().startsWith(prefixLower) ||
                        s.toLowerCase().contains(prefixLower)) {
                    filtered.add(s);
                }
            }

            // Sort: exact prefix matches first
            filtered.sort((a, b) -> {
                boolean aStarts = a.toLowerCase().startsWith(prefixLower);
                boolean bStarts = b.toLowerCase().startsWith(prefixLower);
                if (aStarts && !bStarts)
                    return -1;
                if (!aStarts && bStarts)
                    return 1;
                return a.compareToIgnoreCase(b);
            });

            if (filtered.isEmpty()) {
                hide();
                return;
            }

            // Update list
            listModel.clear();
            for (String s : filtered) {
                listModel.addElement(s);
            }
            suggestionList.setSelectedIndex(0);
            updateDocPanel();

            // Position popup
            try {
                Rectangle rect = editor.modelToView2D(caretPos).getBounds();
                popup.show(editor, rect.x, rect.y + rect.height + 2);
            } catch (Exception e) {
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
                suggestions.add("skillbelow_");
                suggestions.add("skillabove_");
                suggestions.add("animation_");
                suggestions.add("regionid_");
                return suggestions;
            }
        }

        // Default: commands and keywords
        suggestions.addAll(ScriptSyntaxHighlighter.COMMANDS);
        suggestions.addAll(ScriptSyntaxHighlighter.KEYWORDS);

        return suggestions;
    }

    private void insertSelected() {
        String selected = suggestionList.getSelectedValue();
        if (selected == null) {
            hide();
            return;
        }

        try {
            StyledDocument doc = editor.getStyledDocument();
            int caretPos = editor.getCaretPosition();

            // Remove the prefix
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
        }

        hide();
    }

    public void hide() {
        popup.setVisible(false);
    }

    public boolean isVisible() {
        return popup.isVisible();
    }
}
