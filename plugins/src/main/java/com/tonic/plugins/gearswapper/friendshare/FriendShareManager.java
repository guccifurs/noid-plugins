package com.tonic.plugins.gearswapper.friendshare;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Manager class that coordinates all FriendShare functionality.
 * Handles lifecycle, menu entries, share sessions, and callbacks.
 */
@Singleton
public class FriendShareManager {

    private final Client client;
    private final FriendShareService service;
    private final ScreenCapture screenCapture;
    private final FriendShareOverlay overlay;
    private final OverlayManager overlayManager;

    private ScreenViewer activeViewer;
    private boolean initialized = false;
    private Frame runeliteFrame;

    @Inject
    public FriendShareManager(
            Client client,
            FriendShareService service,
            ScreenCapture screenCapture,
            FriendShareOverlay overlay,
            OverlayManager overlayManager) {
        this.client = client;
        this.service = service;
        this.screenCapture = screenCapture;
        this.overlay = overlay;
        this.overlayManager = overlayManager;
    }

    /**
     * Find the RuneLite/Noid frame.
     */
    private Frame findFrame() {
        if (runeliteFrame != null && runeliteFrame.isVisible()) {
            return runeliteFrame;
        }
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible() && frame.getTitle() != null &&
                    (frame.getTitle().contains("RuneLite") || frame.getTitle().contains("Noid"))) {
                runeliteFrame = frame;
                return frame;
            }
        }
        return null;
    }

    /**
     * Initialize the FriendShare system.
     */
    public void initialize() {
        if (initialized)
            return;

        // Set up callbacks
        service.setOnShareRequest(this::handleShareRequest);
        service.setOnShareStart(this::handleShareStart);
        service.setOnShareEnd(this::handleShareEnd);
        service.setOnFrameReceived(this::handleFrameReceived);
        service.setOnFriendsUpdated(friends -> {
            System.out.println("[FriendShare] Online friends updated: " + friends);
        });

        // Connect to server
        service.connect();

        // Add overlay
        overlayManager.add(overlay);

        initialized = true;
        System.out.println("[FriendShare] Manager initialized");
    }

    /**
     * Shutdown the FriendShare system.
     */
    public void shutdown() {
        if (!initialized)
            return;

        // Stop any active sharing
        if (service.isSharing()) {
            service.endShare();
        }

        // Stop capture
        screenCapture.shutdown();

        // Close viewer
        if (activeViewer != null) {
            activeViewer.dispose();
            activeViewer = null;
        }

        // Disconnect
        service.disconnect();

        // Remove overlay
        overlayManager.remove(overlay);

        initialized = false;
        System.out.println("[FriendShare] Manager shutdown");
    }

    /**
     * Request to share screen with a friend.
     */
    public void requestShare(String friendName) {
        if (!service.isConnected()) {
            Frame frame = findFrame();
            JOptionPane.showMessageDialog(frame,
                    "Not connected to FriendShare server",
                    "FriendShare", JOptionPane.WARNING_MESSAGE);
            return;
        }

        service.requestShare(friendName);

        Frame frame = findFrame();
        JOptionPane.showMessageDialog(frame,
                "Share request sent to " + friendName + "\n\nWaiting for them to accept...",
                "FriendShare", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * End the current share session.
     */
    public void endShare() {
        screenCapture.stopCapture();

        if (activeViewer != null) {
            activeViewer.dispose();
            activeViewer = null;
        }

        service.endShare();
    }

    /**
     * Check if a friend is online with GearSwapper.
     */
    public boolean isFriendOnlineWithPlugin(String friendName) {
        return service.getOnlineFriends().stream()
                .anyMatch(f -> f.equalsIgnoreCase(friendName));
    }

    /**
     * Get all online friends with GearSwapper.
     */
    public Set<String> getOnlineFriends() {
        return service.getOnlineFriends();
    }

    // ========== Menu Entry Handler ==========

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        // Add "Share Screen" option when right-clicking a friend
        if (!service.isConnected())
            return;

        String target = event.getTarget();

        // Look for friend name in target
        String cleanTarget = target.replaceAll("<[^>]*>", "").trim();

        if (isFriendOnlineWithPlugin(cleanTarget) && !service.isSharing()) {
            // Add our menu entry using MenuEntry builder
            MenuEntry entry = client.createMenuEntry(-1)
                    .setOption("Share Screen")
                    .setTarget("<col=00ff64>" + cleanTarget + "</col>")
                    .setType(net.runelite.api.MenuAction.RUNELITE);

            final String targetName = cleanTarget;
            entry.onClick(e -> requestShare(targetName));
        }
    }

    // ========== Callbacks ==========

    private void handleShareRequest(String fromPlayer) {
        SwingUtilities.invokeLater(() -> {
            Frame frame = findFrame();
            ShareRequestDialog.Response response = ShareRequestDialog.show(frame, fromPlayer);

            if (response == ShareRequestDialog.Response.ACCEPT) {
                service.acceptShare(fromPlayer);
            } else {
                service.declineShare(fromPlayer);
            }
        });
    }

    private void handleShareStart(String peerName) {
        if (service.isSender()) {
            // We are sharing our screen
            System.out.println("[FriendShare] Starting screen capture for " + peerName);
            screenCapture.startCapture(data -> service.sendFrame(data));

            SwingUtilities.invokeLater(() -> {
                Frame frame = findFrame();
                JOptionPane.showMessageDialog(frame,
                        "Now sharing your screen with " + peerName + "\n\nClick OK to continue.",
                        "FriendShare - Sharing", JOptionPane.INFORMATION_MESSAGE);
            });
        } else {
            // We are viewing their screen
            System.out.println("[FriendShare] Opening viewer for " + peerName);
            SwingUtilities.invokeLater(() -> {
                activeViewer = new ScreenViewer(peerName);
                activeViewer.setOnClose(this::endShare);
                activeViewer.setVisible(true);
            });
        }
    }

    private void handleShareEnd() {
        screenCapture.stopCapture();

        if (activeViewer != null) {
            SwingUtilities.invokeLater(() -> {
                activeViewer.dispose();
                activeViewer = null;
            });
        }

        SwingUtilities.invokeLater(() -> {
            Frame frame = findFrame();
            JOptionPane.showMessageDialog(frame,
                    "Screen share ended.",
                    "FriendShare", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void handleFrameReceived(byte[] frameData) {
        if (activeViewer != null) {
            activeViewer.displayFrame(frameData);
        }
    }
}
