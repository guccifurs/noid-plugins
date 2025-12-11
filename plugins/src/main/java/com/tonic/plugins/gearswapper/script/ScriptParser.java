package com.tonic.plugins.gearswapper.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Very lightweight parser that groups a Gear Swapper script into
 * logical blocks (simple lines vs. multi-line if/else structures),
 * preserving original text so we can round-trip safely.
 *
 * This is intentionally conservative: we do not try to fully
 * understand every command yet, we mostly group text into
 * blocks suitable for a block-based editor.
 */
public final class ScriptParser
{
    private ScriptParser()
    {
    }

    public static ScriptParseResult parse(String scriptText)
    {
        List<ScriptBlock> blocks = new ArrayList<>();
        List<ScriptError> errors = new ArrayList<>();

        if (scriptText == null || scriptText.isEmpty())
        {
            return new ScriptParseResult(blocks, errors);
        }

        String[] lines = scriptText.split("\\r?\\n", -1);
        int i = 0;
        while (i < lines.length)
        {
            String line = lines[i];
            String trimmed = line == null ? "" : line.trim();
            String lower = trimmed.toLowerCase();

            // Group if/else blocks together when possible
            if (lower.startsWith("if "))
            {
                int startLineNo = i + 1; // 1-based

                // Find first '{' belonging to this if
                int openLine = -1;
                int openCol = -1;
                outerOpen:
                for (int li = i; li < lines.length; li++)
                {
                    String s = lines[li];
                    if (s == null)
                    {
                        continue;
                    }
                    int idx = s.indexOf('{');
                    if (idx != -1)
                    {
                        openLine = li;
                        openCol = idx;
                        break outerOpen;
                    }
                }

                if (openLine == -1)
                {
                    // No '{' found - treat this as a single-line block and report error
                    errors.add(new ScriptError(startLineNo,
                        "if-statement is missing '{' - treating as plain line"));
                    blocks.add(new ScriptBlock(Arrays.asList(line), false, startLineNo, startLineNo));
                    i++;
                    continue;
                }

                // Walk forward to find matching closing '}' for this if/else block
                int depth = 1;
                int endLine = -1;
                int li = openLine;
                int col = openCol + 1;

                outerBlock:
                for (; li < lines.length; li++)
                {
                    String s = lines[li];
                    if (s == null)
                    {
                        s = "";
                    }
                    int len = s.length();
                    for (int cj = col; cj < len; cj++)
                    {
                        char c = s.charAt(cj);
                        if (c == '{')
                        {
                            depth++;
                        }
                        else if (c == '}')
                        {
                            depth--;
                            if (depth == 0)
                            {
                                endLine = li;
                                break outerBlock;
                            }
                        }
                    }
                    col = 0; // new line
                }

                if (endLine == -1)
                {
                    // Unterminated block; group until end and report error
                    errors.add(new ScriptError(startLineNo,
                        "if/else block has no matching '}' - grouping until end of script"));
                    endLine = lines.length - 1;
                }

                List<String> blockLines = new ArrayList<>();
                for (int j = i; j <= endLine && j < lines.length; j++)
                {
                    blockLines.add(lines[j]);
                }
                int endLineNo = endLine + 1;
                blocks.add(new ScriptBlock(blockLines, true, startLineNo, endLineNo));
                i = endLine + 1;
            }
            else
            {
                int lineNo = i + 1;
                blocks.add(new ScriptBlock(Arrays.asList(line), false, lineNo, lineNo));
                i++;
            }
        }

        return new ScriptParseResult(blocks, errors);
    }

    /**
     * Convert a list of blocks back into a script string.
     */
    public static String toScript(List<ScriptBlock> blocks)
    {
        if (blocks == null || blocks.isEmpty())
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean firstLine = true;
        for (ScriptBlock block : blocks)
        {
            if (block == null)
            {
                continue;
            }

            for (String line : block.getLines())
            {
                if (!firstLine)
                {
                    sb.append('\n');
                }
                if (line != null)
                {
                    sb.append(line);
                }
                firstLine = false;
            }
        }

        return sb.toString();
    }
}
