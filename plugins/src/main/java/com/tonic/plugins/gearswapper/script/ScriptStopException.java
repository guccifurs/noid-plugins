package com.tonic.plugins.gearswapper.script;

/**
 * Exception thrown by script commands to signal that script execution
 * should stop for the current tick (e.g., Stop command, RandomAfkChance pause).
 * 
 * For Wait commands, this also carries information about remaining commands
 * to execute after the wait completes.
 */
public class ScriptStopException extends RuntimeException {
    private final String[] remainingCommands;
    private final int startIndex;

    public ScriptStopException() {
        super();
        this.remainingCommands = null;
        this.startIndex = 0;
    }

    public ScriptStopException(String message) {
        super(message);
        this.remainingCommands = null;
        this.startIndex = 0;
    }

    public ScriptStopException(String message, String[] remainingCommands, int startIndex) {
        super(message);
        this.remainingCommands = remainingCommands;
        this.startIndex = startIndex;
    }

    public String[] getRemainingCommands() {
        return remainingCommands;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public boolean hasRemainingCommands() {
        return remainingCommands != null && startIndex < remainingCommands.length;
    }
}
