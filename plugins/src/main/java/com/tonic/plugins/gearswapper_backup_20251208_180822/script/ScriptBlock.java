package com.tonic.plugins.gearswapper.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a logical block in a Gear Swapper script.
 * A block can be a single command line or a multi-line if/else structure.
 *
 * This is intentionally fairly lightweight and text-oriented so we can
 * keep perfect round-tripping between script text and blocks while we
 * evolve the language.
 */
public final class ScriptBlock
{
    private final List<String> lines;
    private final boolean conditional;
    private final int startLine; // 1-based, for diagnostics
    private final int endLine;   // 1-based, inclusive

    public ScriptBlock(List<String> lines, boolean conditional, int startLine, int endLine)
    {
        if (lines == null)
        {
            this.lines = Collections.emptyList();
        }
        else
        {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }
        this.conditional = conditional;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public List<String> getLines()
    {
        return lines;
    }

    public boolean isConditional()
    {
        return conditional;
    }

    public int getStartLine()
    {
        return startLine;
    }

    public int getEndLine()
    {
        return endLine;
    }

    /**
     * Convenience: primary display text for the block (usually the first line).
     */
    public String getPrimaryText()
    {
        if (lines.isEmpty())
        {
            return "";
        }
        return lines.get(0) != null ? lines.get(0) : "";
    }
}
