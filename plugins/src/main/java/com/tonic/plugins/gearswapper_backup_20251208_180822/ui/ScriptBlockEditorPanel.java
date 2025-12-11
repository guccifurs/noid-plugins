package com.tonic.plugins.gearswapper.ui;

import com.tonic.plugins.gearswapper.script.ScriptBlock;
import com.tonic.plugins.gearswapper.script.ScriptError;
import com.tonic.plugins.gearswapper.script.ScriptParseResult;
import com.tonic.plugins.gearswapper.script.ScriptParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple block-based view for Gear Swapper scripts.
 *
 * This is an editor-side component only: it works on top of the
 * ScriptParser/ScriptBlock layer and can be embedded in dialogs
 * from GearSwapperPanel. It always round-trips back to plain
 * script text so the runtime doesn't need to know about it.
 */
public class ScriptBlockEditorPanel extends JPanel
{
    private final List<ScriptBlock> blocks = new ArrayList<>();
    private final JPanel blocksPanel = new JPanel();
    private final JTextArea errorArea = new JTextArea();

    public ScriptBlockEditorPanel(String scriptText)
    {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        ScriptParseResult result = ScriptParser.parse(scriptText);
        blocks.addAll(result.getBlocks());

        // Error area (optional)
        errorArea.setEditable(false);
        errorArea.setForeground(new Color(255, 120, 120));
        errorArea.setBackground(UIManager.getColor("Panel.background"));
        errorArea.setFont(new Font("Consolas", Font.PLAIN, 10));
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        errorArea.setBorder(null);

        if (result.hasErrors())
        {
            StringBuilder sb = new StringBuilder();
            for (ScriptError err : result.getErrors())
            {
                sb.append(err.toString()).append('\n');
            }
            errorArea.setText(sb.toString());
        }
        else
        {
            errorArea.setVisible(false);
        }

        JScrollPane errorScroll = new JScrollPane(errorArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        errorScroll.setBorder(null);
        errorScroll.setPreferredSize(new Dimension(100, result.hasErrors() ? 64 : 0));
        add(errorScroll, BorderLayout.NORTH);

        // Blocks container
        blocksPanel.setLayout(new BoxLayout(blocksPanel, BoxLayout.Y_AXIS));
        blocksPanel.setOpaque(false);

        JScrollPane blocksScroll = new JScrollPane(blocksPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(blocksScroll, BorderLayout.CENTER);

        rebuildBlocksUI();
    }

    private void rebuildBlocksUI()
    {
        blocksPanel.removeAll();

        for (int i = 0; i < blocks.size(); i++)
        {
            ScriptBlock block = blocks.get(i);
            blocksPanel.add(createRowPanel(i, block));
        }

        blocksPanel.add(Box.createVerticalGlue());
        blocksPanel.revalidate();
        blocksPanel.repaint();
    }

    private JPanel createRowPanel(int index, ScriptBlock block)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(2, 2, 2, 2));

        // Left: controls (move up/down, delete, edit)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        controls.setOpaque(false);

        JButton upBtn = new JButton("↑");
        upBtn.setMargin(new Insets(0, 2, 0, 2));
        upBtn.setToolTipText("Move block up");
        upBtn.addActionListener(e -> moveBlock(index, index - 1));

        JButton downBtn = new JButton("↓");
        downBtn.setMargin(new Insets(0, 2, 0, 2));
        downBtn.setToolTipText("Move block down");
        downBtn.addActionListener(e -> moveBlock(index, index + 1));

        JButton editBtn = new JButton("✎");
        editBtn.setMargin(new Insets(0, 2, 0, 2));
        editBtn.setToolTipText("Edit block text");
        editBtn.addActionListener(e -> editBlock(index));

        JButton deleteBtn = new JButton("✕");
        deleteBtn.setMargin(new Insets(0, 2, 0, 2));
        deleteBtn.setToolTipText("Delete block");
        deleteBtn.addActionListener(e -> deleteBlock(index));

        controls.add(upBtn);
        controls.add(downBtn);
        controls.add(editBtn);
        controls.add(deleteBtn);

        row.add(controls, BorderLayout.WEST);

        // Center: primary text
        String primary = block.getPrimaryText();
        if (primary == null)
        {
            primary = "";
        }
        String display = primary.trim();
        if (display.isEmpty())
        {
            display = "(empty line)";
        }

        if (block.isConditional())
        {
            display = "IF: " + display;
        }

        JLabel label = new JLabel(display);
        label.setOpaque(false);
        row.add(label, BorderLayout.CENTER);

        return row;
    }

    private void moveBlock(int fromIndex, int toIndex)
    {
        if (fromIndex < 0 || fromIndex >= blocks.size())
        {
            return;
        }
        if (toIndex < 0 || toIndex >= blocks.size())
        {
            return;
        }
        if (fromIndex == toIndex)
        {
            return;
        }

        ScriptBlock b = blocks.remove(fromIndex);
        blocks.add(toIndex, b);
        rebuildBlocksUI();
    }

    private void deleteBlock(int index)
    {
        if (index < 0 || index >= blocks.size())
        {
            return;
        }
        blocks.remove(index);
        rebuildBlocksUI();
    }

    private void editBlock(int index)
    {
        if (index < 0 || index >= blocks.size())
        {
            return;
        }

        ScriptBlock block = blocks.get(index);
        StringBuilder sb = new StringBuilder();
        List<String> lines = block.getLines();
        for (int i = 0; i < lines.size(); i++)
        {
            if (i > 0)
            {
                sb.append('\n');
            }
            String line = lines.get(i);
            if (line != null)
            {
                sb.append(line);
            }
        }

        JTextArea editor = new JTextArea(sb.toString(), 6, 50);
        editor.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(editor);

        int res = JOptionPane.showConfirmDialog(
            this,
            scroll,
            "Edit Block",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

        if (res != JOptionPane.OK_OPTION)
        {
            return;
        }

        String newText = editor.getText();
        List<String> newLines = new ArrayList<>();
        if (newText != null && !newText.isEmpty())
        {
            String[] split = newText.split("\\r?\\n", -1);
            for (String s : split)
            {
                newLines.add(s);
            }
        }
        else
        {
            newLines.add("");
        }

        // Preserve conditional flag, line numbers are editor-only so we can reset them
        ScriptBlock replacement = new ScriptBlock(newLines, block.isConditional(), 1, newLines.size());
        blocks.set(index, replacement);
        rebuildBlocksUI();
    }

    public void appendBlockFromText(String text)
    {
        if (text == null)
        {
            return;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty())
        {
            return;
        }

        boolean conditional = trimmed.toLowerCase().startsWith("if ");
        List<String> lines = new ArrayList<>();
        lines.add(text);
        ScriptBlock block = new ScriptBlock(lines, conditional, 1, 1);
        blocks.add(block);
        rebuildBlocksUI();
    }

    /**
     * Build script text from the current block list.
     */
    public String buildScript()
    {
        return ScriptParser.toScript(blocks);
    }
}
