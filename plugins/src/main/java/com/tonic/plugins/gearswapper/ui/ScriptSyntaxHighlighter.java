package com.tonic.plugins.gearswapper.ui;

import javax.swing.text.*;
import java.awt.Color;
import java.util.*;
import java.util.regex.*;

/**
 * Syntax highlighter for GearSwapper scripts.
 * Provides real-time color-coded highlighting for commands, conditions,
 * keywords, and more.
 */
public class ScriptSyntaxHighlighter {

    // Color scheme
    public static final Color COMMAND_COLOR = new Color(86, 156, 214); // Blue - commands
    public static final Color KEYWORD_COLOR = new Color(197, 134, 192); // Purple - if/else/endif
    public static final Color CONDITION_COLOR = new Color(78, 201, 176); // Teal - conditions
    public static final Color OPERATOR_COLOR = new Color(206, 145, 120); // Orange - operators
    public static final Color VALUE_COLOR = new Color(214, 157, 133); // Tan - values
    public static final Color STRING_COLOR = new Color(206, 145, 120); // Orange - strings
    public static final Color COMMENT_COLOR = new Color(106, 153, 85); // Green - comments
    public static final Color NUMBER_COLOR = new Color(181, 206, 168); // Light green - numbers
    public static final Color ERROR_COLOR = new Color(244, 71, 71); // Red - errors
    public static final Color DEFAULT_COLOR = new Color(212, 212, 212); // Light gray - default

    // All recognized commands
    public static final Set<String> COMMANDS = new LinkedHashSet<>(Arrays.asList(
            "Item", "Cast", "Prayer", "TogglePray", "Special", "Attack",
            "Move", "MoveDiag", "Walk", "MeleeRange",
            "Tick", "Wait", "WaitAnim",
            "Log", "Npc", "DropAll",
            "SetMemory", "ClearMemory",
            "Target", "ClearTarget",
            "RunEnergy", "ToggleRun"));

    // Keywords
    public static final Set<String> KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "if", "else", "endif", "elseif"));

    // Boolean conditions
    public static final Set<String> CONDITIONS = new LinkedHashSet<>(Arrays.asList(
            // Target conditions
            "has_target", "hastarget", "target_exists", "target_frozen", "targetfrozen",
            "has_cached_target", "cached_target",
            // Player status
            "frozen", "self_frozen", "player_frozen", "me_frozen",
            "incombat", "in_combat", "combat",
            "moving", "walking", "is_moving",
            "inventoryfull", "inventory_full",
            "run_enabled", "running",
            // Special
            "spec_enabled", "special_enabled"));

    // Numeric variables for comparisons
    public static final Set<String> NUMERIC_VARS = new LinkedHashSet<>(Arrays.asList(
            "spec", "spec_energy", "special", "special_energy", "specialattack",
            "hp", "health", "hitpoints",
            "prayer", "pray",
            "distance", "target_distance", "targetdistance",
            "player_frozen_ticks", "self_frozen_ticks", "frozenticks",
            "target_frozen_ticks", "targetfrozenticks",
            "ticks_since_swap", "swap_ticks",
            "idleticks", "idle_ticks",
            "ticktimer", "tick_timer",
            "attacktimer", "attack_timer",
            "ticktimer_myattack", "myattacktimer"));

    // Pattern-based conditions (prefixes)
    public static final Set<String> CONDITION_PREFIXES = new LinkedHashSet<>(Arrays.asList(
            "regionid_", "region_",
            "varbit_",
            "animation_",
            "targetanimation_",
            "targetpraying_",
            "skillbelow_", "skillabove_",
            "hasitem_", "hasitemamount_",
            "randomchancepercent_",
            "getmemory_"));

    // Operators
    public static final Set<String> OPERATORS = new LinkedHashSet<>(Arrays.asList(
            "==", "!=", ">=", "<=", ">", "<",
            "&&", "||", "!", "and", "or", "not"));

    // Prayers for autocomplete
    public static final List<String> PRAYERS = Arrays.asList(
            "Thick Skin", "Burst of Strength", "Clarity of Thought", "Sharp Eye", "Mystic Will",
            "Rock Skin", "Superhuman Strength", "Improved Reflexes", "Rapid Restore", "Rapid Heal",
            "Protect Item", "Hawk Eye", "Mystic Lore", "Steel Skin", "Ultimate Strength",
            "Incredible Reflexes", "Protect from Magic", "Protect from Missiles", "Protect from Melee",
            "Eagle Eye", "Mystic Might", "Retribution", "Redemption", "Smite",
            "Preserve", "Chivalry", "Piety", "Rigour", "Augury");

    // Spells for autocomplete
    public static final List<String> SPELLS = Arrays.asList(
            "Ice Barrage", "Ice Blitz", "Ice Burst", "Ice Rush",
            "Blood Barrage", "Blood Blitz", "Blood Burst", "Blood Rush",
            "Smoke Barrage", "Smoke Blitz", "Smoke Burst", "Smoke Rush",
            "Shadow Barrage", "Shadow Blitz", "Shadow Burst", "Shadow Rush",
            "Entangle", "Snare", "Bind",
            "Teleblock", "Vengeance", "Vengeance Other");

    private final StyledDocument doc;
    private final Style defaultStyle;
    private final Style commandStyle;
    private final Style keywordStyle;
    private final Style conditionStyle;
    private final Style operatorStyle;
    private final Style valueStyle;
    private final Style commentStyle;
    private final Style numberStyle;
    private final Style errorStyle;

    public ScriptSyntaxHighlighter(StyledDocument doc) {
        this.doc = doc;

        // Create styles
        defaultStyle = doc.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, DEFAULT_COLOR);
        StyleConstants.setFontFamily(defaultStyle, "Consolas");
        StyleConstants.setFontSize(defaultStyle, 13);

        commandStyle = doc.addStyle("command", defaultStyle);
        StyleConstants.setForeground(commandStyle, COMMAND_COLOR);
        StyleConstants.setBold(commandStyle, true);

        keywordStyle = doc.addStyle("keyword", defaultStyle);
        StyleConstants.setForeground(keywordStyle, KEYWORD_COLOR);
        StyleConstants.setBold(keywordStyle, true);

        conditionStyle = doc.addStyle("condition", defaultStyle);
        StyleConstants.setForeground(conditionStyle, CONDITION_COLOR);

        operatorStyle = doc.addStyle("operator", defaultStyle);
        StyleConstants.setForeground(operatorStyle, OPERATOR_COLOR);
        StyleConstants.setBold(operatorStyle, true);

        valueStyle = doc.addStyle("value", defaultStyle);
        StyleConstants.setForeground(valueStyle, VALUE_COLOR);

        commentStyle = doc.addStyle("comment", defaultStyle);
        StyleConstants.setForeground(commentStyle, COMMENT_COLOR);
        StyleConstants.setItalic(commentStyle, true);

        numberStyle = doc.addStyle("number", defaultStyle);
        StyleConstants.setForeground(numberStyle, NUMBER_COLOR);

        errorStyle = doc.addStyle("error", defaultStyle);
        StyleConstants.setForeground(errorStyle, ERROR_COLOR);
        StyleConstants.setUnderline(errorStyle, true);
    }

    /**
     * Highlight the entire document.
     */
    public void highlightAll() {
        try {
            String text = doc.getText(0, doc.getLength());
            String[] lines = text.split("\n", -1);

            int offset = 0;
            for (String line : lines) {
                highlightLine(line, offset);
                offset += line.length() + 1; // +1 for newline
            }
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    /**
     * Highlight a single line at the given offset.
     */
    public void highlightLine(String line, int offset) {
        // Reset to default
        doc.setCharacterAttributes(offset, line.length(), defaultStyle, true);

        String trimmed = line.trim();

        // Comment line
        if (trimmed.startsWith("//")) {
            doc.setCharacterAttributes(offset, line.length(), commentStyle, true);
            return;
        }

        // Inline comment
        int commentIdx = line.indexOf("//");
        if (commentIdx >= 0) {
            doc.setCharacterAttributes(offset + commentIdx, line.length() - commentIdx, commentStyle, true);
            line = line.substring(0, commentIdx);
        }

        // Keywords (if, else, endif)
        String lower = trimmed.toLowerCase();
        for (String keyword : KEYWORDS) {
            if (lower.equals(keyword) || lower.startsWith(keyword + " ") || lower.startsWith(keyword + "(")) {
                int idx = line.toLowerCase().indexOf(keyword);
                if (idx >= 0) {
                    doc.setCharacterAttributes(offset + idx, keyword.length(), keywordStyle, true);
                }
            }
        }

        // Commands (Item:, Cast:, etc.)
        for (String cmd : COMMANDS) {
            Pattern p = Pattern.compile("(?i)\\b" + cmd + "\\s*:");
            Matcher m = p.matcher(line);
            while (m.find()) {
                doc.setCharacterAttributes(offset + m.start(), cmd.length(), commandStyle, true);
                // Highlight value after colon
                int colonEnd = m.end();
                int valueLen = line.length() - colonEnd;
                if (valueLen > 0) {
                    doc.setCharacterAttributes(offset + colonEnd, valueLen, valueStyle, true);
                }
            }
        }

        // Standalone commands (Special, Attack, MeleeRange)
        for (String cmd : Arrays.asList("Special", "Attack", "MeleeRange")) {
            if (trimmed.equalsIgnoreCase(cmd)) {
                int idx = line.toLowerCase().indexOf(cmd.toLowerCase());
                if (idx >= 0) {
                    doc.setCharacterAttributes(offset + idx, cmd.length(), commandStyle, true);
                }
            }
        }

        // Conditions in if statements
        Pattern condPattern = Pattern.compile("(?i)if\\s*\\((.+)\\)");
        Matcher condMatcher = condPattern.matcher(line);
        if (condMatcher.find()) {
            String conditionContent = condMatcher.group(1);
            int condStart = condMatcher.start(1);

            // Highlight known conditions
            for (String cond : CONDITIONS) {
                int idx = conditionContent.toLowerCase().indexOf(cond.toLowerCase());
                if (idx >= 0) {
                    doc.setCharacterAttributes(offset + condStart + idx, cond.length(), conditionStyle, true);
                }
            }

            // Highlight numeric variables
            for (String var : NUMERIC_VARS) {
                int idx = conditionContent.toLowerCase().indexOf(var.toLowerCase());
                if (idx >= 0) {
                    doc.setCharacterAttributes(offset + condStart + idx, var.length(), conditionStyle, true);
                }
            }

            // Highlight pattern-based conditions
            for (String prefix : CONDITION_PREFIXES) {
                Pattern prefixPattern = Pattern.compile("(?i)" + prefix + "\\w+");
                Matcher prefixMatcher = prefixPattern.matcher(conditionContent);
                while (prefixMatcher.find()) {
                    doc.setCharacterAttributes(offset + condStart + prefixMatcher.start(),
                            prefixMatcher.end() - prefixMatcher.start(), conditionStyle, true);
                }
            }

            // Highlight operators
            for (String op : OPERATORS) {
                int idx = 0;
                while ((idx = conditionContent.indexOf(op, idx)) >= 0) {
                    doc.setCharacterAttributes(offset + condStart + idx, op.length(), operatorStyle, true);
                    idx += op.length();
                }
            }

            // Highlight numbers
            Pattern numPattern = Pattern.compile("\\b\\d+\\b");
            Matcher numMatcher = numPattern.matcher(conditionContent);
            while (numMatcher.find()) {
                doc.setCharacterAttributes(offset + condStart + numMatcher.start(),
                        numMatcher.end() - numMatcher.start(), numberStyle, true);
            }
        }

        // Numbers elsewhere
        Pattern numPattern = Pattern.compile(":\\s*(\\d+)");
        Matcher numMatcher = numPattern.matcher(line);
        while (numMatcher.find()) {
            doc.setCharacterAttributes(offset + numMatcher.start(1),
                    numMatcher.end(1) - numMatcher.start(1), numberStyle, true);
        }
    }

    /**
     * Get autocomplete suggestions based on context.
     */
    public List<String> getSuggestions(String currentLine, int cursorPos) {
        List<String> suggestions = new ArrayList<>();

        String beforeCursor = currentLine.substring(0, Math.min(cursorPos, currentLine.length()));
        String lower = beforeCursor.toLowerCase().trim();

        // After "Prayer:" or "TogglePray:"
        if (lower.endsWith("prayer:") || lower.endsWith("togglepray:")) {
            suggestions.addAll(PRAYERS);
            return suggestions;
        }

        // After "Cast:"
        if (lower.endsWith("cast:")) {
            suggestions.addAll(SPELLS);
            return suggestions;
        }

        // After "if(" - suggest conditions
        if (lower.endsWith("if(") || lower.endsWith("if (")) {
            suggestions.addAll(CONDITIONS);
            suggestions.addAll(NUMERIC_VARS);
            suggestions.add("targetpraying_melee");
            suggestions.add("targetpraying_mage");
            suggestions.add("targetpraying_range");
            suggestions.add("regionid_");
            suggestions.add("animation_");
            return suggestions;
        }

        // Start of line - suggest commands
        if (lower.isEmpty() || lower.length() < 3) {
            suggestions.addAll(COMMANDS);
            suggestions.addAll(KEYWORDS);
            return suggestions;
        }

        // Partial command match
        for (String cmd : COMMANDS) {
            if (cmd.toLowerCase().startsWith(lower)) {
                suggestions.add(cmd + ":");
            }
        }
        for (String kw : KEYWORDS) {
            if (kw.toLowerCase().startsWith(lower)) {
                suggestions.add(kw);
            }
        }

        return suggestions;
    }

    /**
     * Get all command names for reference.
     */
    public static Set<String> getAllCommands() {
        return COMMANDS;
    }

    /**
     * Get all condition names for reference.
     */
    public static Set<String> getAllConditions() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(CONDITIONS);
        all.addAll(NUMERIC_VARS);
        return all;
    }
}
