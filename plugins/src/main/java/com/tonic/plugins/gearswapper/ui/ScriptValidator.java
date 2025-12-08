package com.tonic.plugins.gearswapper.ui;

import java.util.*;

/**
 * Real-time validator for GearSwapper scripts.
 * Designed to be LENIENT - only flag truly invalid syntax, not unknown
 * commands.
 * The script runtime is the ultimate authority on what's valid.
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

    // Track if/else blocks - supports both brace {} and endif styles
    private int openIfBlocks = 0;
    private int openBraces = 0;

    /**
     * Validate the entire script and return list of errors.
     * NOTE: This validator is designed to be LENIENT.
     * Only flag true syntax errors, not unknown commands.
     */
    public List<ValidationError> validate(String script) {
        List<ValidationError> errors = new ArrayList<>();

        if (script == null || script.trim().isEmpty()) {
            return errors; // Empty script is valid
        }

        String[] lines = script.split("\n", -1);
        openIfBlocks = 0;
        openBraces = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Count braces for brace-style blocks
            for (char c : line.toCharArray()) {
                if (c == '{')
                    openBraces++;
                if (c == '}')
                    openBraces--;
            }

            List<ValidationError> lineErrors = validateLine(line, i + 1);
            errors.addAll(lineErrors);
        }

        // Check for unclosed blocks - only if NOT using brace style
        // If braces are balanced, we're using brace style so ignore endif tracking
        if (openBraces == 0 && openIfBlocks > 0) {
            // Might be using brace style with inline braces, or might be missing endif
            // Don't error, just info
        }

        if (openBraces > 0) {
            errors.add(new ValidationError(lines.length, 0,
                    "Unclosed brace - missing " + openBraces + " '}' character(s)",
                    ErrorSeverity.ERROR));
        } else if (openBraces < 0) {
            errors.add(new ValidationError(1, 0,
                    "Unmatched '}' - more closing braces than opening",
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

        // Standalone braces - valid
        if (trimmed.equals("{") || trimmed.equals("}")) {
            return errors;
        }

        // Check for if with brace on same line: if(...) {
        if ((lower.startsWith("if ") || lower.startsWith("if(")) && trimmed.endsWith("{")) {
            openIfBlocks++;
            errors.addAll(validateIfStatement(trimmed, lineNum));
            return errors;
        }

        // Check for if/else/endif keywords (without brace)
        if (lower.startsWith("if ") || lower.startsWith("if(")) {
            openIfBlocks++;
            errors.addAll(validateIfStatement(trimmed, lineNum));
            return errors;
        }

        if (lower.equals("else") || lower.equals("else {") || lower.startsWith("else ")) {
            // else is always contextually valid - don't check block count
            return errors;
        }

        if (lower.equals("endif") || lower.equals("}")) {
            openIfBlocks--;
            // Don't error on endif - might be valid with different nesting
            return errors;
        }

        if (lower.startsWith("elseif ") || lower.startsWith("elseif(")) {
            // elseif is valid in context
            return errors;
        }

        // Everything else - don't validate strictly
        // The runtime is the authority on what commands are valid
        return errors;
    }

    /**
     * Validate an if statement condition (very lenient).
     */
    private List<ValidationError> validateIfStatement(String statement, int lineNum) {
        List<ValidationError> errors = new ArrayList<>();

        // Check for parentheses
        if (!statement.contains("(")) {
            // Some if styles might not use parentheses
            return errors;
        }

        if (!statement.contains(")")) {
            errors.add(new ValidationError(lineNum, 0,
                    "if statement missing closing parenthesis ')'", ErrorSeverity.WARNING));
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
