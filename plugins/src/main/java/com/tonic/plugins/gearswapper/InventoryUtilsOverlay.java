package com.tonic.plugins.gearswapper;

import com.tonic.Logger;
import net.runelite.api.Client;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Clickable preset buttons overlay for quick loadout/prayer activation.
 * - Empty buttons show "+" and open config popup on click
 * - Configured buttons show icons and execute action on click
 * - Right-click opens edit popup
 */
public class InventoryUtilsOverlay extends Overlay implements MouseListener {
    private static final int BASE_BUTTON_SIZE = 36;
    private static final int BUTTON_PADDING = 4;
    private static final Color PANEL_BG = new Color(30, 30, 30, 180);
    private static final Color BUTTON_BG = new Color(50, 50, 50, 230);
    private static final Color BUTTON_BORDER = new Color(100, 100, 100);
    private static final Color BUTTON_HOVER = new Color(80, 80, 80, 250);
    private static final Color LOADOUT_COLOR = new Color(255, 200, 100);
    private static final Color PRAYER_COLOR = new Color(200, 150, 255);
    private static final Color PLUS_COLOR = new Color(120, 200, 120);

    private final Client client;
    private final GearSwapperPlugin plugin;
    private final GearSwapperConfig config;
    private final MouseManager mouseManager;
    private final InventoryUtilsButtonManager buttonManager;

    private volatile List<ButtonInfo> buttons = new ArrayList<>();
    private int hoveredButton = -1;
    private int overlayWidth = 0;
    private int overlayHeight = 0;

    private static class ButtonInfo {
        int index;
        int x, y, size;
        ButtonPreset preset;

        ButtonInfo(int index, int x, int y, int size, ButtonPreset preset) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.size = size;
            this.preset = preset;
        }

        boolean contains(int relX, int relY) {
            return relX >= x && relX < x + size && relY >= y && relY < y + size;
        }
    }

    @Inject
    public InventoryUtilsOverlay(Client client, GearSwapperPlugin plugin, GearSwapperConfig config,
            MouseManager mouseManager, InventoryUtilsButtonManager buttonManager) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.mouseManager = mouseManager;
        this.buttonManager = buttonManager;

        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        setPriority(OverlayPriority.HIGH);
        setMovable(true);
        setSnappable(true);

        mouseManager.registerMouseListener(this);
    }

    public void shutdown() {
        mouseManager.unregisterMouseListener(this);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.inventoryUtilsEnabled()) {
            return null;
        }

        // Build new button list locally, then swap atomically to avoid race conditions
        // with mouse handlers seeing empty/partial list during rebuild
        List<ButtonInfo> newButtons = new ArrayList<>();

        int presetCount = config.inventoryUtilsPresetCount();
        boolean isVertical = config.inventoryUtilsOrientation() == InventoryUtilsOrientation.VERTICAL;
        float sizeMultiplier = config.inventoryUtilsSize() / 100f;
        int buttonSize = (int) (BASE_BUTTON_SIZE * sizeMultiplier);
        int padding = (int) (BUTTON_PADDING * sizeMultiplier);

        if (isVertical) {
            overlayWidth = buttonSize + padding * 2;
            overlayHeight = (buttonSize + padding) * presetCount + padding;
        } else {
            overlayWidth = (buttonSize + padding) * presetCount + padding;
            overlayHeight = buttonSize + padding * 2;
        }

        // Draw panel background
        graphics.setColor(PANEL_BG);
        graphics.fillRoundRect(0, 0, overlayWidth, overlayHeight, 8, 8);
        graphics.setColor(BUTTON_BORDER);
        graphics.drawRoundRect(0, 0, overlayWidth, overlayHeight, 8, 8);

        // Draw each button
        for (int i = 0; i < presetCount; i++) {
            ButtonPreset preset = buttonManager.getButton(i);

            int x, y;
            if (isVertical) {
                x = padding;
                y = padding + i * (buttonSize + padding);
            } else {
                x = padding + i * (buttonSize + padding);
                y = padding;
            }

            newButtons.add(new ButtonInfo(i, x, y, buttonSize, preset));

            boolean isHovered = (hoveredButton == i);
            graphics.setColor(isHovered ? BUTTON_HOVER : BUTTON_BG);
            graphics.fillRoundRect(x, y, buttonSize, buttonSize, 6, 6);

            graphics.setColor(BUTTON_BORDER);
            graphics.drawRoundRect(x, y, buttonSize, buttonSize, 6, 6);

            // Draw content
            if (preset.isEmpty()) {
                drawPlusButton(graphics, x, y, buttonSize);
            } else if (preset.cachedIcon != null) {
                drawIconButton(graphics, x, y, buttonSize, preset.cachedIcon);
            } else if (preset.type == PresetType.PRAYER) {
                drawPrayerButton(graphics, x, y, buttonSize, preset.value, i + 1);
            } else if (preset.type == PresetType.LOADOUT) {
                drawLoadoutButton(graphics, x, y, buttonSize, preset.value, i + 1);
            }
        }

        // Atomic swap - mouse handlers see either old complete list or new complete
        // list, never empty
        buttons = newButtons;

        return new Dimension(overlayWidth, overlayHeight);
    }

    private void drawPlusButton(Graphics2D graphics, int x, int y, int size) {
        graphics.setColor(PLUS_COLOR);
        int fontSize = Math.max(16, size / 2);
        graphics.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = graphics.getFontMetrics();
        String text = "+";
        int textX = x + (size - fm.stringWidth(text)) / 2;
        int textY = y + (size + fm.getAscent()) / 2 - fm.getDescent();
        graphics.drawString(text, textX, textY);
    }

    private void drawIconButton(Graphics2D graphics, int x, int y, int size, BufferedImage icon) {
        int iconSize = (int) (size * 0.7);
        int iconX = x + (size - iconSize) / 2;
        int iconY = y + (size - iconSize) / 2;
        graphics.drawImage(icon, iconX, iconY, iconSize, iconSize, null);
    }

    private void drawPrayerButton(Graphics2D graphics, int x, int y, int size, String name, int num) {
        graphics.setColor(PRAYER_COLOR);
        int fontSize = Math.max(10, size / 3);
        graphics.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = graphics.getFontMetrics();
        String text = "P" + num;
        int textX = x + (size - fm.stringWidth(text)) / 2;
        int textY = y + (size + fm.getAscent()) / 2 - fm.getDescent();
        graphics.drawString(text, textX, textY);
    }

    private void drawLoadoutButton(Graphics2D graphics, int x, int y, int size, String name, int num) {
        graphics.setColor(LOADOUT_COLOR);
        int fontSize = Math.max(12, size / 2);
        graphics.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = graphics.getFontMetrics();
        String text = String.valueOf(num);
        int textX = x + (size - fm.stringWidth(text)) / 2;
        int textY = y + (size + fm.getAscent()) / 2 - fm.getDescent();
        graphics.drawString(text, textX, textY);
    }

    private void executePreset(ButtonPreset preset) {
        if (preset.isEmpty())
            return;

        if (preset.type == PresetType.LOADOUT) {
            Logger.norm("[InventoryUtils] Executing loadout: " + preset.value);
            plugin.executeLoadoutByName(preset.value);
        } else if (preset.type == PresetType.PRAYER) {
            Logger.norm("[InventoryUtils] Activating prayer: " + preset.value);
            plugin.activatePrayerByName(preset.value);
        }
    }

    private void openConfigDialog(int buttonIndex) {
        SwingUtilities.invokeLater(() -> {
            ButtonPreset current = buttonManager.getButton(buttonIndex);

            JDialog dialog = new JDialog((Frame) null, "Configure Button " + (buttonIndex + 1), true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(300, 200);
            dialog.setLocationRelativeTo(null);

            JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Type dropdown
            formPanel.add(new JLabel("Type:"));
            JComboBox<PresetType> typeCombo = new JComboBox<>(PresetType.values());
            typeCombo.setSelectedItem(current.type);
            formPanel.add(typeCombo);

            // Value field (changes based on type)
            formPanel.add(new JLabel("Value:"));
            JComboBox<String> valueCombo = new JComboBox<>();
            updateValueCombo(valueCombo, (PresetType) typeCombo.getSelectedItem(), current.value);
            formPanel.add(valueCombo);

            // Update value combo when type changes
            typeCombo.addActionListener(e -> {
                PresetType selected = (PresetType) typeCombo.getSelectedItem();
                updateValueCombo(valueCombo, selected, "");
            });

            // Clear button
            formPanel.add(new JLabel(""));
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(e -> {
                buttonManager.clearButton(buttonIndex);
                dialog.dispose();
            });
            formPanel.add(clearBtn);

            dialog.add(formPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dialog.dispose());

            JButton saveBtn = new JButton("Save");
            saveBtn.addActionListener(e -> {
                PresetType type = (PresetType) typeCombo.getSelectedItem();
                String value = (String) valueCombo.getSelectedItem();

                ButtonPreset newPreset = new ButtonPreset(type, value != null ? value : "");
                buttonManager.setButton(buttonIndex, newPreset);
                dialog.dispose();
            });

            buttonPanel.add(cancelBtn);
            buttonPanel.add(saveBtn);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        });
    }

    private void updateValueCombo(JComboBox<String> combo, PresetType type, String currentValue) {
        combo.removeAllItems();

        if (type == PresetType.PRAYER) {
            for (String prayer : InventoryUtilsButtonManager.getAllPrayerNames()) {
                combo.addItem(prayer);
            }
            if (currentValue != null && !currentValue.isEmpty()) {
                combo.setSelectedItem(currentValue);
            }
        } else if (type == PresetType.LOADOUT) {
            // Add actual loadout names from plugin
            java.util.List<String> loadoutNames = plugin.getConfiguredLoadoutNames();
            if (loadoutNames.isEmpty()) {
                combo.addItem("(No loadouts configured)");
            } else {
                for (String name : loadoutNames) {
                    combo.addItem(name);
                }
            }
            if (currentValue != null && !currentValue.isEmpty()) {
                combo.setSelectedItem(currentValue);
            }
        }
    }

    // MouseListener
    @Override
    public MouseEvent mouseClicked(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event) {
        if (!config.inventoryUtilsEnabled()) {
            return event;
        }

        Rectangle bounds = getBounds();
        if (bounds == null) {
            Logger.norm("[InventoryUtils] mousePressed: bounds is null");
            return event;
        }

        int mouseX = event.getX();
        int mouseY = event.getY();

        Logger.norm("[InventoryUtils] Click at screen(" + mouseX + "," + mouseY + ") bounds=" + bounds + " buttons="
                + buttons.size());

        if (!bounds.contains(mouseX, mouseY)) {
            return event;
        }

        int relX = mouseX - bounds.x;
        int relY = mouseY - bounds.y;

        Logger.norm("[InventoryUtils] Click at rel(" + relX + "," + relY + ") checking " + buttons.size() + " buttons");

        for (ButtonInfo button : buttons) {
            Logger.norm("[InventoryUtils] Button " + button.index + " at (" + button.x + "," + button.y + ") size="
                    + button.size);
            if (button.contains(relX, relY)) {
                Logger.norm("[InventoryUtils] HIT button " + button.index + "!");
                if (SwingUtilities.isRightMouseButton(event)) {
                    // Right-click: open config dialog
                    openConfigDialog(button.index);
                } else if (button.preset.isEmpty()) {
                    // Left-click on empty: open config dialog
                    openConfigDialog(button.index);
                } else {
                    // Left-click on configured: execute
                    executePreset(button.preset);
                }
                event.consume();
                return event;
            }
        }

        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent event) {
        hoveredButton = -1;
        return event;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent event) {
        if (!config.inventoryUtilsEnabled()) {
            hoveredButton = -1;
            return event;
        }

        Rectangle bounds = getBounds();
        if (bounds == null) {
            hoveredButton = -1;
            return event;
        }

        int mouseX = event.getX();
        int mouseY = event.getY();

        if (!bounds.contains(mouseX, mouseY)) {
            hoveredButton = -1;
            return event;
        }

        int relX = mouseX - bounds.x;
        int relY = mouseY - bounds.y;

        hoveredButton = -1;
        for (ButtonInfo button : buttons) {
            if (button.contains(relX, relY)) {
                hoveredButton = button.index;
                break;
            }
        }

        return event;
    }
}
