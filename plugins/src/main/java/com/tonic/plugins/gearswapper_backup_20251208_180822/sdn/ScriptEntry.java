package com.tonic.plugins.gearswapper.sdn;

import lombok.Data;

/**
 * Represents a script entry from the Script SDN
 */
@Data
public class ScriptEntry {
    private String id;
    private String name;
    private String description;
    private String content;
    private String authorId;
    private String authorName;
    private String createdAt;
    private String updatedAt;

    // Vote count (upvote-only system)
    private int votes;
    private boolean hasVoted; // True if user has voted on this script

    /**
     * Check if the current user is the author of this script
     */
    public boolean isOwnedBy(String discordId) {
        return authorId != null && authorId.equals(discordId);
    }

    /**
     * Get a short preview of the script content
     */
    public String getContentPreview(int maxLength) {
        if (content == null)
            return "";
        if (content.length() <= maxLength)
            return content;
        return content.substring(0, maxLength) + "...";
    }
}
