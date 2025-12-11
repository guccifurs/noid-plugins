package com.tonic.plugins.clicktest;

import net.runelite.api.Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Live mouse logger panel that tracks all mouse activity in detail
 */
public class MouseLoggerPanel extends JPanel {

    private final Client client;

    // Stats tracking
    private long lastClickTime = 0;
    private long lastMoveTime = 0;
    private int totalClicks = 0;
    private int leftClicks = 0;
    private int rightClicks = 0;
    private Point lastPosition = new Point(0, 0);
    private double totalDistance = 0;
    private final Queue<Long> clickIntervals = new LinkedList<>();
    private final Queue<Long> clickDurations = new LinkedList<>();
    private long pressTime = 0;
    private boolean isLogging = false;

    // UI Components
    private JLabel positionLabel;
    private JLabel velocityLabel;
    private JLabel clickCountLabel;
    private JLabel lastClickLabel;
    private JLabel avgIntervalLabel;
    private JLabel avgDurationLabel;
    private JLabel clickTypeLabel;
    private JLabel distanceLabel;
    private JTextArea eventLog;
    private JToggleButton toggleButton;
    private Timer updateTimer;

    private static final DecimalFormat DF = new DecimalFormat("#,##0.0");
    private static final int MAX_INTERVALS = 20;

    public MouseLoggerPanel(Client client) {
        this.client = client;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(25, 25, 28));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        buildUI();
        setupMouseListener();
    }

    private void buildUI() {
        // Header
        JLabel header = new JLabel("📊 Live Mouse Logger");
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(200, 200, 200));
        header.setAlignmentX(LEFT_ALIGNMENT);
        add(header);
        add(Box.createVerticalStrut(10));

        // Toggle button
        toggleButton = new JToggleButton("▶ Start Logging");
        toggleButton.setBackground(new Color(76, 175, 80));
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setFocusPainted(false);
        toggleButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        toggleButton.setAlignmentX(LEFT_ALIGNMENT);
        toggleButton.addActionListener(e -> toggleLogging());
        add(toggleButton);
        add(Box.createVerticalStrut(10));

        // Stats panel
        JPanel statsPanel = createStatsPanel();
        statsPanel.setAlignmentX(LEFT_ALIGNMENT);
        add(statsPanel);
        add(Box.createVerticalStrut(10));

        // Event log
        JLabel logLabel = new JLabel("Event Log:");
        logLabel.setForeground(new Color(150, 150, 150));
        logLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(logLabel);
        add(Box.createVerticalStrut(3));

        eventLog = new JTextArea(8, 20);
        eventLog.setEditable(false);
        eventLog.setBackground(new Color(18, 18, 20));
        eventLog.setForeground(new Color(100, 255, 100));
        eventLog.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
        eventLog.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scroll = new JScrollPane(eventLog);
        scroll.setBorder(new LineBorder(new Color(50, 50, 55), 1));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        add(scroll);
        add(Box.createVerticalStrut(10));

        // Clear button
        JButton clearBtn = new JButton("🗑 Clear Log");
        clearBtn.setBackground(new Color(60, 60, 65));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        clearBtn.setAlignmentX(LEFT_ALIGNMENT);
        clearBtn.addActionListener(e -> clearStats());
        add(clearBtn);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2, 8, 4));
        panel.setBackground(new Color(35, 35, 40));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(55, 55, 60), 1),
                new EmptyBorder(8, 10, 8, 10)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Create stat labels
        positionLabel = createStatRow(panel, "Position:", "0, 0");
        velocityLabel = createStatRow(panel, "Velocity:", "0 px/s");
        clickCountLabel = createStatRow(panel, "Clicks:", "0 (L:0 R:0)");
        lastClickLabel = createStatRow(panel, "Last Click:", "—");
        avgIntervalLabel = createStatRow(panel, "Avg Interval:", "— ms");
        avgDurationLabel = createStatRow(panel, "Avg Duration:", "— ms");
        clickTypeLabel = createStatRow(panel, "Last Type:", "—");
        distanceLabel = createStatRow(panel, "Distance:", "0 px");

        return panel;
    }

    private JLabel createStatRow(JPanel panel, String label, String value) {
        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(new Color(130, 130, 135));
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(new Color(220, 220, 225));
        valueLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 11));

        panel.add(nameLabel);
        panel.add(valueLabel);

        return valueLabel;
    }

    private void toggleLogging() {
        isLogging = !isLogging;

        if (isLogging) {
            toggleButton.setText("⏹ Stop Logging");
            toggleButton.setBackground(new Color(244, 67, 54));
            startUpdateTimer();
            logEvent("SYSTEM", "Logging started");
        } else {
            toggleButton.setText("▶ Start Logging");
            toggleButton.setBackground(new Color(76, 175, 80));
            stopUpdateTimer();
            logEvent("SYSTEM", "Logging stopped");
        }
    }

    private void startUpdateTimer() {
        if (updateTimer != null)
            updateTimer.stop();

        updateTimer = new Timer(50, e -> updateLiveStats());
        updateTimer.start();
    }

    private void stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    private void updateLiveStats() {
        if (!isLogging || client == null)
            return;

        try {
            net.runelite.api.Point pos = client.getMouseCanvasPosition();
            if (pos != null) {
                int x = pos.getX();
                int y = pos.getY();

                // Calculate velocity
                long now = System.currentTimeMillis();
                double dx = x - lastPosition.x;
                double dy = y - lastPosition.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                long dt = now - lastMoveTime;
                double velocity = dt > 0 ? (dist / dt) * 1000 : 0; // pixels per second

                if (dist > 0) {
                    totalDistance += dist;
                    lastMoveTime = now;
                }

                lastPosition = new Point(x, y);

                // Update UI
                positionLabel.setText(x + ", " + y);
                velocityLabel.setText(DF.format(velocity) + " px/s");
                distanceLabel.setText(DF.format(totalDistance) + " px");
            }
        } catch (Exception ignored) {
        }
    }

    private void setupMouseListener() {
        // Add global AWT event listener for mouse events
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!isLogging)
                return;
            if (!(event instanceof MouseEvent))
                return;

            MouseEvent me = (MouseEvent) event;

            // Only track events on RuneLite canvas
            if (client == null || client.getCanvas() == null)
                return;
            if (me.getSource() != client.getCanvas())
                return;

            handleMouseEvent(me);

        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private void handleMouseEvent(MouseEvent e) {
        long now = System.currentTimeMillis();
        String type = "";
        String details = "";

        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                pressTime = now;
                type = "PRESS";
                details = formatClickDetails(e);
                break;

            case MouseEvent.MOUSE_RELEASED:
                long duration = now - pressTime;
                type = "RELEASE";
                details = formatClickDetails(e) + " [held " + duration + "ms]";

                // Track duration
                if (clickDurations.size() >= MAX_INTERVALS)
                    clickDurations.poll();
                clickDurations.add(duration);
                updateAvgDuration();
                break;

            case MouseEvent.MOUSE_CLICKED:
                totalClicks++;
                if (SwingUtilities.isLeftMouseButton(e))
                    leftClicks++;
                if (SwingUtilities.isRightMouseButton(e))
                    rightClicks++;

                // Track interval
                if (lastClickTime > 0) {
                    long interval = now - lastClickTime;
                    if (clickIntervals.size() >= MAX_INTERVALS)
                        clickIntervals.poll();
                    clickIntervals.add(interval);
                    updateAvgInterval();
                }
                lastClickTime = now;

                type = "CLICK";
                details = formatClickDetails(e);
                updateClickStats(e);
                break;

            case MouseEvent.MOUSE_MOVED:
            case MouseEvent.MOUSE_DRAGGED:
                // Don't log every move, too spammy
                return;

            default:
                return;
        }

        if (!type.isEmpty()) {
            logEvent(type, details);
        }
    }

    private String formatClickDetails(MouseEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(e.getX()).append(", ").append(e.getY()).append(")");
        sb.append(" btn=").append(e.getButton());

        if (SwingUtilities.isLeftMouseButton(e))
            sb.append(" [LEFT]");
        else if (SwingUtilities.isRightMouseButton(e))
            sb.append(" [RIGHT]");
        else if (SwingUtilities.isMiddleMouseButton(e))
            sb.append(" [MIDDLE]");

        if (e.getClickCount() > 1)
            sb.append(" x").append(e.getClickCount());

        return sb.toString();
    }

    private void updateClickStats(MouseEvent e) {
        SwingUtilities.invokeLater(() -> {
            clickCountLabel.setText(totalClicks + " (L:" + leftClicks + " R:" + rightClicks + ")");
            lastClickLabel.setText(e.getX() + ", " + e.getY());

            if (SwingUtilities.isLeftMouseButton(e)) {
                clickTypeLabel.setText("LEFT");
                clickTypeLabel.setForeground(new Color(100, 200, 255));
            } else if (SwingUtilities.isRightMouseButton(e)) {
                clickTypeLabel.setText("RIGHT");
                clickTypeLabel.setForeground(new Color(255, 200, 100));
            } else {
                clickTypeLabel.setText("BTN " + e.getButton());
                clickTypeLabel.setForeground(new Color(200, 100, 255));
            }
        });
    }

    private void updateAvgInterval() {
        if (clickIntervals.isEmpty())
            return;

        long sum = 0;
        for (Long interval : clickIntervals)
            sum += interval;
        double avg = (double) sum / clickIntervals.size();

        SwingUtilities.invokeLater(() -> avgIntervalLabel.setText(DF.format(avg) + " ms"));
    }

    private void updateAvgDuration() {
        if (clickDurations.isEmpty())
            return;

        long sum = 0;
        for (Long dur : clickDurations)
            sum += dur;
        double avg = (double) sum / clickDurations.size();

        SwingUtilities.invokeLater(() -> avgDurationLabel.setText(DF.format(avg) + " ms"));
    }

    private void logEvent(String type, String message) {
        SwingUtilities.invokeLater(() -> {
            long ts = System.currentTimeMillis() % 100000;
            String line = String.format("[%05d] %-8s %s%n", ts, type, message);
            eventLog.append(line);
            eventLog.setCaretPosition(eventLog.getDocument().getLength());
        });
    }

    private void clearStats() {
        totalClicks = 0;
        leftClicks = 0;
        rightClicks = 0;
        totalDistance = 0;
        lastClickTime = 0;
        clickIntervals.clear();
        clickDurations.clear();
        eventLog.setText("");

        positionLabel.setText("0, 0");
        velocityLabel.setText("0 px/s");
        clickCountLabel.setText("0 (L:0 R:0)");
        lastClickLabel.setText("—");
        avgIntervalLabel.setText("— ms");
        avgDurationLabel.setText("— ms");
        clickTypeLabel.setText("—");
        clickTypeLabel.setForeground(new Color(220, 220, 225));
        distanceLabel.setText("0 px");

        logEvent("SYSTEM", "Stats cleared");
    }

    public void shutdown() {
        stopUpdateTimer();
        isLogging = false;
    }
}
