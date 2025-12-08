package com.tonic.plugins.gearswapper.ui;

import java.util.*;
import java.util.regex.*;

/**
 * Real-time validator for GearSwapper scripts.
 * Validates syntax without false positives - only flags truly invalid
 * constructs.
 */
public class ScriptValidator {

    public static class ValidationError {
        public final int line;
        public final int column;
        public final String message;
        public final ErrorSeverity severity;

        public ValidationError(int line, int column, String message, ErrorSeverity severity) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.severity = severity;
        }

        @Override
        public String toString() {
            return String.format("Line %d: %s", line, message);
        }
    }

    public enum ErrorSeverity {
        ERROR, // Definitely wrong
        WARNING, // Might be wrong
        INFO // Just a hint
    }

    // Track if/else/endif blocks
    private int openIfBlocks = 0;

    /**
     * Validate the entire script and return list of errors.
     */
    public List<ValidationError> validate(String script) {
        List<ValidationError> errors = new ArrayList<>();

        if (script == null || script.trim().isEmpty()) {
            return errors; // Empty script is valid
        }

        String[] lines = script.split("\n", -1);
        openIfBlocks = 0;

        for (int i = 0; i < lines.length; i++) {
            List<ValidationError> lineErrors = validateLine(lines[i], i + 1);
            errors.addAll(lineErrors);
        }

        // Check for unclosed if blocks
        if (openIfBlocks > 0) {
            errors.add(new ValidationError(lines.length, 0,
                    "Unclosed if block - missing " + openIfBlocks + " endif statement(s)",
                    ErrorSeverity.ERROR));
        } else if (openIfBlocks < 0) {
            errors.add(new ValidationError(1, 0,
                    "Unmatched endif - more endif than if statements",
                    ErrorSeverity.ERROR));
        }

        return errors;
    }

    /**
     * Validate a single line.
     */
    public List<ValidationError> validateLine(String line, int lineNum) {
        List<ValidationError> errors = new ArrayList<>();

        String trimmed = line.trim();

        // Empty line or comment - always valid
        if (trimmed.isEmpty() || trimmed.startsWith("//")) {
            return errors;
        }

        // Remove inline comments for validation
        int commentIdx = trimmed.indexOf("//");
        if (commentIdx > 0) {
            trimmed = trimmed.substring(0, commentIdx).trim();
        }

        String lower = trimmed.toLowerCase();

        // Check for if/else/endif keywords
        if (lower.startsWith("if ") || lower.startsWith("if(")) {
            openIfBlocks++;
            errors.addAll(validateIfStatement(trimmed, lineNum));
            return errors;
        }

        if (lower.equals("else")) {
            if (openIfBlocks <= 0) {
                errors.add(new ValidationError(lineNum, 0,
                        "'else' without matching 'if'", ErrorSeverity.ERROR));
            }
            return errors;
        }

        if (lower.equals("endif")) {
            openIfBlocks--;
            if (openIfBlocks < 0) {
                errors.add(new ValidationError(lineNum, 0,
                        "'endif' without matching 'if'", ErrorSeverity.ERROR));
            }
            return errors;
        }

        if (lower.startsWith("elseif ") || lower.startsWith("elseif(")) {
            if (openIfBlocks <= 0) {
                errors.add(new ValidationError(lineNum, 0,
                        "'elseif' without matching 'if'", ErrorSeverity.ERROR));
            } else {
                errors.addAll(validateIfStatement(trimmed.substring(4), lineNum));
            }
            return errors;
        }

        // Validate command syntax
        errors.addAll(validateCommand(trimmed, lineNum));

        return errors;
    }

    /**
     * Validate an if statement condition.
     */
    private List<ValidationError> validateIfStatement(String statement, int lineNum) {
        List<ValidationError> errors = new ArrayList<>();

        // Extract condition from parentheses
        Pattern pattern = Pattern.compile("(?i)if\\s*\\((.*)\\)\\s*$");
        Matcher matcher = pattern.matcher(statement);

        if (!matcher.find()) {
            // Check for missing parentheses
            if (!statement.contains("(")) {
                errors.add(new ValidationError(lineNum, 0,
                        "if statement missing opening parenthesis '('", ErrorSeverity.ERROR));
            } else if (!statement.contains(")")) {
                errors.add(new ValidationError(lineNum, 0,
                        "if statement missing closing parenthesis ')'", ErrorSeverity.ERROR));
            }
            return errors;
        }

        String condition = matcher.group(1).trim();

        if (condition.isEmpty()) {
            errors.add(new ValidationError(lineNum, 0,
                    "Empty condition in if statement", ErrorSeverity.ERROR));
            return errors;
        }

        // Validate condition syntax (basic checks only)
        errors.addAll(validateCondition(condition, lineNum));

        return errors;
    }

    /**
     * Validate a condition expression.
     */
    private List<ValidationError> validateCondition(String condition, int lineNum) {
        List<ValidationError> errors = new ArrayList<>();

        // Check for balanced parentheses
        int parenCount = 0;
        for (char c : condition.toCharArray()) {
            if (c == '(')
                parenCount++;
            if (c == ')')
                parenCount--;
            if (parenCount < 0) {
                errors.add(new ValidationError(lineNum, 0,
                        "Unbalanced parentheses in condition", ErrorSeverity.ERROR));
                return errors;
            }
        }
        if (parenCount != 0) {
            errors.add(new ValidationError(lineNum, 0,
                    "Unbalanced parentheses in condition", ErrorSeverity.ERROR));
        }

        // Check for common typos
        String lower = condition.toLowerCase();
        if (lower.contains(" and ") || lower.contains(" or ")) {
            // These are actually valid, just a warning
            // No error needed
        }

        // Check for invalid comparison operators
        if (condition.contains("=") && !condition.contains("==") &&
                !condition.contains("!=") && !condition.contains(">=") && !condition.contains("<=")) {
            errors.add(new ValidationError(lineNum, 0,
                    "Use '==' for comparison, not '='", ErrorSeverity.ERROR));
        }

        return errors;
    }

    /**
     * Validate a command line.
     */
    private List<ValidationError> validateCommand(String command, int lineNum) {
        List<ValidationError> errors = new ArrayList<>();

        String lower = command.toLowerCase();

        // Check for command with colon
        if (command.contains(":")) {
            String[] parts = command.split(":", 2);
            String cmdName = parts[0].trim();
            String value = parts.length > 1 ? parts[1].trim() : "";

            // Check if command is recognized
            boolean recognized = false;
            for (String known : ScriptSyntaxHighlighter.COMMANDS) {
                if (known.equalsIgnoreCase(cmdName)) {
                    recognized = true;
                    break;
                }
            }

            if (!recognized) {
                // Don't error on unknown commands - they might be valid custom commands
                // Just a warning
                errors.add(new ValidationError(lineNum, 0,
                        "Unknown command: '" + cmdName + "' (may still work if valid)",
                        ErrorSeverity.WARNING));
            }

            // Check for empty value where required
            if (value.isEmpty()) {
                Set<String> requiresValue = new HashSet<>(Arrays.asList(
                        "item", "cast", "prayer", "togglepray", "move", "movediag",
                        "tick", "wait", "waitanim", "log", "walk"));
                if (requiresValue.contains(cmdName.toLowerCase())) {
                    errors.add(new ValidationError(lineNum, 0,
                            "Command '" + cmdName + "' requires a value after ':'",
                            ErrorSeverity.ERROR));
                }
            }

            // Validate specific commands
            if (cmdName.equalsIgnoreCase("Tick") || cmdName.equalsIgnoreCase("Wait")) {
                if (!value.isEmpty() && !value.matches("\\d+")) {
                    errors.add(new ValidationError(lineNum, 0,
                            cmdName + " requires a numeric value", ErrorSeverity.ERROR));
                }
            }

            if (cmdName.equalsIgnoreCase("Move") || cmdName.equalsIgnoreCase("MoveDiag")) {
                if (!value.isEmpty() && !value.matches("-?\\d+")) {
                    errors.add(new ValidationError(lineNum, 0,
                            cmdName + " requires a numeric distance", ErrorSeverity.ERROR));
                }
            }

            return errors;
        }

        // Standalone commands (no colon)
        Set<String> standaloneCommands = new HashSet<>(Arrays.asList(
                "special", "attack", "meleerange", "endif", "else"));

        if (!standaloneCommands.contains(lower)) {
            // Check if it looks like a command missing a colon
            for (String known : ScriptSyntaxHighlighter.COMMANDS) {
                if (lower.startsWith(known.toLowerCase()) && !lower.equals(known.toLowerCase())) {
                    errors.add(new ValidationError(lineNum, 0,
                            "Command '" + known + "' requires a colon - use '" + known + ": value'",
                            ErrorSeverity.ERROR));
                    return errors;
                }
            }

            // Unknown line - could be an item name or something
            // Don't error, just warn
            // Actually, let's not warn either - it might be intentional
        }

        return errors;
    }

    /**
     * Check if a line has errors.
     */
    public boolean hasErrors(String line, int lineNum) {
        List<ValidationError> errors = validateLine(line, lineNum);
        return errors.stream().anyMatch(e -> e.severity == ErrorSeverity.ERROR);
    }

    /**
     * Check if a script is valid.
     */
    public boolean isValid(String script) {
        List<ValidationError> errors = validate(script);
        return errors.stream().noneMatch(e -> e.severity == ErrorSeverity.ERROR);
    }
}
