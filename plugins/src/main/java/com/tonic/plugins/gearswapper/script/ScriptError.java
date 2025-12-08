package com.tonic.plugins.gearswapper.script;

/**
 * Simple parse/validation error used by the editor layer
 * (separate from runtime execution errors).
 */
public final class ScriptError
{
    private final int line; // 1-based
    private final String message;

    public ScriptError(int line, String message)
    {
        this.line = line;
        this.message = message != null ? message : "";
    }

    public int getLine()
    {
        return line;
    }

    public String getMessage()
    {
        return message;
    }

    @Override
    public String toString()
    {
        return "Line " + line + ": " + message;
    }
}
