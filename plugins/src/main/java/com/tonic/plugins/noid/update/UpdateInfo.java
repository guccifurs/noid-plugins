package com.tonic.plugins.noid.update;

import lombok.Data;

/**
 * Information about an available update
 */
@Data
public class UpdateInfo {
    private String pluginName;
    private String currentVersion;
    private String latestVersion;
    private String downloadUrl;
    private String jarName;
    private String releaseNotes;

    public boolean hasUpdate() {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }
        return compareVersions(latestVersion, currentVersion) > 0;
    }

    /**
     * Compare version strings (e.g., "1.2.3" vs "1.2.4")
     * Returns positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private int compareVersions(String v1, String v2) {
        // Remove 'v' prefix if present
        v1 = v1.startsWith("v") ? v1.substring(1) : v1;
        v2 = v2.startsWith("v") ? v2.substring(1) : v2;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }

        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            // Extract only digits
            String digits = part.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
