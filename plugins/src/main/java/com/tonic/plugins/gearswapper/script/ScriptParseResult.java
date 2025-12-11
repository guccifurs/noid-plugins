package com.tonic.plugins.gearswapper.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of parsing a Gear Swapper script for editor/validation purposes.
 */
public final class ScriptParseResult
{
    private final List<ScriptBlock> blocks;
    private final List<ScriptError> errors;

    public ScriptParseResult(List<ScriptBlock> blocks, List<ScriptError> errors)
    {
        if (blocks == null)
        {
            this.blocks = Collections.emptyList();
        }
        else
        {
            this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
        }

        if (errors == null)
        {
            this.errors = Collections.emptyList();
        }
        else
        {
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        }
    }

    public List<ScriptBlock> getBlocks()
    {
        return blocks;
    }

    public List<ScriptError> getErrors()
    {
        return errors;
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }
}
