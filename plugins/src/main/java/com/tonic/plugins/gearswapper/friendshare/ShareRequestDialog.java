package com.tonic.plugins.gearswapper.friendshare;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog that appears when someone requests to share their screen with you.
 */
public class ShareRequestDialog extends JDialog {

    public enum Response {
        ACCEPT, DECLINE, CLOSED
    }

    private Response response = Response.CLOSED;

    public ShareRequestDialog(Frame owner, String fromPlayer) {
        super(owner, "Screen Share Request", true);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(350, 150);
        setLocationRelativeTo(owner);
        setResizable(false);

        // Dark theme
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(45, 45, 48));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Message
        JLabel messageLabel = new JLabel(
                "<html><center><b>" + fromPlayer + "</b> wants to share their screen with you.</center></html>");
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(messageLabel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        JButton acceptBtn = new JButton("Accept");
        acceptBtn.setBackground(new Color(76, 175, 80));
        acceptBtn.setForeground(Color.WHITE);
        acceptBtn.setFocusPainted(false);
        acceptBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        acceptBtn.setPreferredSize(new Dimension(100, 32));
        acceptBtn.addActionListener(e -> {
            response = Response.ACCEPT;
            dispose();
        });

        JButton declineBtn = new JButton("Decline");
        declineBtn.setBackground(new Color(97, 97, 97));
        declineBtn.setForeground(Color.WHITE);
        declineBtn.setFocusPainted(false);
        declineBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        declineBtn.setPreferredSize(new Dimension(100, 32));
        declineBtn.addActionListener(e -> {
            response = Response.DECLINE;
            dispose();
        });

        buttonPanel.add(acceptBtn);
        buttonPanel.add(declineBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    /**
     * Show the dialog and return the response.
     */
    public Response showAndWait() {
        setVisible(true);
        return response;
    }

    /**
     * Static helper to show the dialog.
     */
    public static Response show(Frame owner, String fromPlayer) {
        ShareRequestDialog dialog = new ShareRequestDialog(owner, fromPlayer);
        return dialog.showAndWait();
    }
}
