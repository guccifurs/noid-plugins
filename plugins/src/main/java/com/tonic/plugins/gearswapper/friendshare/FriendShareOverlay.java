package com.tonic.plugins.gearswapper.friendshare;

import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.Set;

/**
 * Overlay that highlights friends who are online and using GearSwapper.
 * Draws a green glow/indicator next to their names in the friend list.
 */
public class FriendShareOverlay extends Overlay {

    private static final Color GLOW_COLOR = new Color(0, 255, 100, 180);
    private static final Color DOT_COLOR = new Color(0, 255, 100);

    private final Client client;
    private final FriendShareService friendShareService;

    @Inject
    public FriendShareOverlay(Client client, FriendShareService friendShareService) {
        this.client = client;
        this.friendShareService = friendShareService;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!friendShareService.isConnected()) {
            return null;
        }

        Set<String> onlineFriends = friendShareService.getOnlineFriends();
        if (onlineFriends.isEmpty()) {
            return null;
        }

        // Draw indicator for each online friend with GearSwapper
        drawFriendListIndicators(graphics, onlineFriends);

        return null;
    }

    private void drawFriendListIndicators(Graphics2D graphics, Set<String> onlineFriends) {
        // Get friend list widget using ComponentID instead of deprecated WidgetInfo
        Widget friendList = client.getWidget(ComponentID.FRIEND_LIST_NAMES_CONTAINER);
        if (friendList == null || friendList.isHidden()) {
            return;
        }

        Widget[] children = friendList.getDynamicChildren();
        if (children == null) {
            return;
        }

        // Check each friend entry
        Friend[] friends = client.getFriendContainer().getMembers();
        if (friends == null) {
            return;
        }

        for (int i = 0; i < children.length && i < friends.length; i++) {
            Widget child = children[i];
            Friend friend = friends[i];

            if (child == null || friend == null || child.isHidden()) {
                continue;
            }

            String friendName = friend.getName();
            if (friendName == null) {
                continue;
            }

            // Check if this friend is online with GearSwapper
            boolean isOnlineWithPlugin = onlineFriends.stream()
                    .anyMatch(f -> f.equalsIgnoreCase(friendName));

            if (isOnlineWithPlugin) {
                // Draw green dot indicator
                Rectangle bounds = child.getBounds();
                if (bounds != null && bounds.width > 0) {
                    int dotX = bounds.x - 10;
                    int dotY = bounds.y + bounds.height / 2 - 3;

                    // Green dot
                    graphics.setColor(DOT_COLOR);
                    graphics.fillOval(dotX, dotY, 6, 6);

                    // Glow effect
                    graphics.setColor(GLOW_COLOR);
                    graphics.drawOval(dotX - 1, dotY - 1, 8, 8);
                }
            }
        }
    }
}
