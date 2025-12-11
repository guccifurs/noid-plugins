package com.tonic.plugins.clicktest;

import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class ClickTestPanel extends PluginPanel {

    private final Client client;
    private final ClickTestConfig config;

    private JComboBox<ClickTestConfig.ClickType> clickTypeDropdown;
    private JTextField xField;
    private JTextField yField;
    private JTextArea logArea;
    private JLabel statusLabel;
    private MouseLoggerPanel mouseLoggerPanel;
    private MouseInputBlocker mouseBlocker;
    private JToggleButton blockToggle;
    private MousePathOverlay pathOverlay;

    public ClickTestPanel(Client client, ClickTestConfig config, MousePathOverlay overlay) {
        this.client = client;
        this.config = config;
        this.pathOverlay = overlay;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(new Color(30, 30, 30));

        // Title
        JLabel title = new JLabel("🖱️ Click Tester");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(15));

        // Click Type Dropdown
        JLabel typeLabel = new JLabel("Click Type:");
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(typeLabel);

        clickTypeDropdown = new JComboBox<>(ClickTestConfig.ClickType.values());
        clickTypeDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        clickTypeDropdown.setAlignmentX(LEFT_ALIGNMENT);
        add(clickTypeDropdown);
        add(Box.createVerticalStrut(10));

        // Coordinates
        JLabel coordLabel = new JLabel("Canvas Coordinates:");
        coordLabel.setForeground(Color.WHITE);
        coordLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(coordLabel);

        JPanel coordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        coordPanel.setOpaque(false);
        coordPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        coordPanel.setAlignmentX(LEFT_ALIGNMENT);

        xField = new JTextField("400", 5);
        yField = new JTextField("300", 5);

        coordPanel.add(new JLabel("X:") {
            {
                setForeground(Color.WHITE);
            }
        });
        coordPanel.add(xField);
        coordPanel.add(new JLabel("Y:") {
            {
                setForeground(Color.WHITE);
            }
        });
        coordPanel.add(yField);

        add(coordPanel);
        add(Box.createVerticalStrut(10));

        // Use Mouse Position Button
        JButton useMouseBtn = new JButton("📍 Use Current Mouse");
        useMouseBtn.setBackground(new Color(70, 70, 70));
        useMouseBtn.setForeground(Color.WHITE);
        useMouseBtn.setFocusPainted(false);
        useMouseBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        useMouseBtn.setAlignmentX(LEFT_ALIGNMENT);
        useMouseBtn.addActionListener(e -> captureMousePosition());
        add(useMouseBtn);
        add(Box.createVerticalStrut(10));

        // Block Real Mouse Toggle
        blockToggle = new JToggleButton("🛡️ Block Real Mouse");
        blockToggle.setBackground(new Color(100, 100, 100));
        blockToggle.setForeground(Color.WHITE);
        blockToggle.setFocusPainted(false);
        blockToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        blockToggle.setAlignmentX(LEFT_ALIGNMENT);
        blockToggle.addActionListener(e -> toggleMouseBlocking());
        add(blockToggle);
        add(Box.createVerticalStrut(10));

        // Execute Button
        JButton executeBtn = new JButton("⚡ Execute Click");
        executeBtn.setBackground(new Color(76, 175, 80));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.setFocusPainted(false);
        executeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        executeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        executeBtn.setAlignmentX(LEFT_ALIGNMENT);
        executeBtn.addActionListener(e -> executeClick());
        add(executeBtn);
        add(Box.createVerticalStrut(10));

        // Status
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(new Color(150, 150, 150));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(statusLabel);
        add(Box.createVerticalStrut(15));

        // Log Area
        JLabel logLabel = new JLabel("Log:");
        logLabel.setForeground(Color.WHITE);
        logLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(logLabel);

        logArea = new JTextArea(10, 20);
        logArea.setEditable(false);
        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(new Color(150, 255, 150));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 10));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        add(scroll);

        // Description
        add(Box.createVerticalStrut(10));
        JTextArea desc = new JTextArea();
        desc.setText("Click Types:\n" +
                "• Robot: Moves actual cursor, shows indicator\n" +
                "• Canvas: Dispatches events to canvas\n" +
                "• Packet: Just sends packet (no indicator)\n" +
                "• Hybrid: Events + Packet together");
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setForeground(new Color(150, 150, 150));
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        desc.setAlignmentX(LEFT_ALIGNMENT);
        add(desc);

        // Separator
        add(Box.createVerticalStrut(15));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 65));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        add(sep);
        add(Box.createVerticalStrut(10));

        // Mouse Logger Panel
        mouseLoggerPanel = new MouseLoggerPanel(client);
        mouseLoggerPanel.setAlignmentX(LEFT_ALIGNMENT);
        mouseLoggerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
        add(mouseLoggerPanel);

        // Initialize mouse blocker with logging callback
        mouseBlocker = new MouseInputBlocker(client, this::log);

        // Hook mover's virtual position to send points to overlay
        if (mouseBlocker.getMover() != null && pathOverlay != null) {
            mouseBlocker.getMover().getVirtualPosition().setPositionListener(point -> {
                pathOverlay.addPoint(point.x, point.y);
            });
        }
    }

    private void toggleMouseBlocking() {
        if (mouseBlocker == null)
            return;

        mouseBlocker.toggle();

        if (mouseBlocker.isBlocking()) {
            blockToggle.setText("🛡️ BLOCKING (Click to Stop)");
            blockToggle.setBackground(new Color(244, 67, 54)); // Red
            log("Mouse blocking ENABLED");
        } else {
            blockToggle.setText("🛡️ Block Real Mouse");
            blockToggle.setBackground(new Color(100, 100, 100));
            log("Mouse blocking DISABLED");
        }
    }

    private void log(String type, String message) {
        log("[" + type + "] " + message);
    }

    private void captureMousePosition() {
        try {
            if (client != null) {
                net.runelite.api.Point pos = client.getMouseCanvasPosition();
                if (pos != null) {
                    xField.setText(String.valueOf(pos.getX()));
                    yField.setText(String.valueOf(pos.getY()));
                    log("Captured mouse position: " + pos.getX() + ", " + pos.getY());
                }
            }
        } catch (Exception e) {
            log("Error capturing mouse: " + e.getMessage());
        }
    }

    /**
     * Public method called by hotkey listener
     */
    public void captureMousePositionFromHotkey() {
        captureMousePosition();
    }

    private void executeClick() {
        try {
            int x = Integer.parseInt(xField.getText());
            int y = Integer.parseInt(yField.getText());
            ClickTestConfig.ClickType type = (ClickTestConfig.ClickType) clickTypeDropdown.getSelectedItem();

            log("Executing " + type + " click at (" + x + ", " + y + ")");
            statusLabel.setText("Clicking...");
            statusLabel.setForeground(Color.YELLOW);

            new Thread(() -> {
                try {
                    log("Thread started for " + type);

                    switch (type) {
                        case ROBOT:
                            executeRobotClick(x, y);
                            break;
                        case CANVAS_EVENT:
                            executeCanvasClick(x, y);
                            break;
                        case PACKET_ONLY:
                            executePacketClick(x, y);
                            break;
                        case HYBRID:
                            executeHybridClick(x, y);
                            break;
                        case TRAJECTORY:
                            executeTrajectoryClick(x, y);
                            break;
                    }

                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Done!");
                        statusLabel.setForeground(Color.GREEN);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    log("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error: " + e.getMessage());
                        statusLabel.setForeground(Color.RED);
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            log("Invalid coordinates");
            statusLabel.setText("Invalid coords");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void executeRobotClick(int canvasX, int canvasY) throws Exception {
        Canvas canvas = client.getCanvas();
        Point screenPos = canvas.getLocationOnScreen();
        int screenX = screenPos.x + canvasX;
        int screenY = screenPos.y + canvasY;

        log("Robot: Moving to screen (" + screenX + ", " + screenY + ")");

        Robot robot = new Robot();
        robot.mouseMove(screenX, screenY);
        Thread.sleep(50);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(30);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        log("Robot: Click complete - check for indicator!");
    }

    private void executeCanvasClick(int x, int y) throws Exception {
        Canvas canvas = client.getCanvas();
        long now = System.currentTimeMillis();

        log("Canvas: Dispatching move event...");

        // Move event first
        MouseEvent move = new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
                now, 0, x, y, 0, false);
        canvas.dispatchEvent(move);

        Thread.sleep(10);

        log("Canvas: Dispatching press/release/click...");

        // Press
        MouseEvent press = new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED,
                now + 10, 0, x, y, 1, false, MouseEvent.BUTTON1);
        canvas.dispatchEvent(press);

        Thread.sleep(30);

        // Release
        MouseEvent release = new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED,
                now + 40, 0, x, y, 1, false, MouseEvent.BUTTON1);
        canvas.dispatchEvent(release);

        // Click
        MouseEvent click = new MouseEvent(canvas, MouseEvent.MOUSE_CLICKED,
                now + 40, 0, x, y, 1, false, MouseEvent.BUTTON1);
        canvas.dispatchEvent(click);

        log("Canvas: Events dispatched - check for indicator!");
    }

    private void executePacketClick(int x, int y) throws Exception {
        log("Packet: No actual click, just logging...");
        log("Packet: In real use, this would call widgetActionPacket()");
        log("Packet: Position (" + x + ", " + y + ") - NO indicator expected");
    }

    private void executeHybridClick(int x, int y) throws Exception {
        log("Hybrid: Sending canvas events first...");
        executeCanvasClick(x, y);

        Thread.sleep(50);

        log("Hybrid: Then would send packet (simulated)");
        log("Hybrid: This combines visibility with action");
    }

    private void executeTrajectoryClick(int x, int y) throws Exception {
        log("Trajectory: Using VitaLite TrajectoryGenerator...");

        // Get current position as start
        net.runelite.api.Point current = client.getMouseCanvasPosition();
        int startX = current != null ? current.getX() : 0;
        int startY = current != null ? current.getY() : 0;

        // Create generator and generate movement
        com.tonic.services.mouserecorder.trajectory.TrajectoryGenerator generator = com.tonic.services.mouserecorder.trajectory.TrajectoryService
                .createGenerator();

        int trajectoryCount = com.tonic.services.mouserecorder.trajectory.TrajectoryService.getDatabase()
                .getTrajectoryCount();
        log("Trajectory: Database has " + trajectoryCount + " recorded trajectories");

        com.tonic.services.mouserecorder.MouseMovementSequence sequence = generator.generate(startX, startY, x, y);

        java.util.List<com.tonic.services.mouserecorder.MouseDataPoint> points = sequence.getPoints();
        log("Trajectory: Generated " + points.size() + " points for path");

        // Dispatch each point as a mouse move event
        java.awt.Canvas canvas = client.getCanvas();
        if (canvas == null) {
            log("Trajectory: ERROR - No canvas!");
            return;
        }

        long startTime = System.currentTimeMillis();
        long lastPointTime = startTime;

        for (int i = 0; i < points.size(); i++) {
            com.tonic.services.mouserecorder.MouseDataPoint point = points.get(i);

            // Add point to path overlay
            if (pathOverlay != null) {
                pathOverlay.addPoint(point.getX(), point.getY());
            }

            // Calculate delay from timestamps
            long pointTime = point.getTimestampMillis();
            long delay = i > 0 ? (pointTime - points.get(i - 1).getTimestampMillis()) : 0;
            if (delay > 0 && delay < 500) {
                Thread.sleep(delay);
            }

            // Dispatch move event
            java.awt.event.MouseEvent move = new java.awt.event.MouseEvent(
                    canvas, java.awt.event.MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
            canvas.dispatchEvent(move);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log("Trajectory: Path completed in " + totalTime + "ms");

        // Final click at destination
        Thread.sleep(30);
        long now = System.currentTimeMillis();

        java.awt.event.MouseEvent press = new java.awt.event.MouseEvent(
                canvas, java.awt.event.MouseEvent.MOUSE_PRESSED,
                now, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1);
        canvas.dispatchEvent(press);

        Thread.sleep(40);

        java.awt.event.MouseEvent release = new java.awt.event.MouseEvent(
                canvas, java.awt.event.MouseEvent.MOUSE_RELEASED,
                now + 40, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1);
        canvas.dispatchEvent(release);

        java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(
                canvas, java.awt.event.MouseEvent.MOUSE_CLICKED,
                now + 40, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1);
        canvas.dispatchEvent(click);

        log("Trajectory: Click dispatched!");
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + System.currentTimeMillis() % 100000 + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
