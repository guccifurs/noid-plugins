package com.tonic.noid;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;

/**
 * NoidUpdater - Checks for and downloads updates on startup
 */
public class NoidUpdater {

    // Your server endpoints
    private static final String VERSION_URL = "https://raw.githubusercontent.com/guccifurs/noid-plugins/main/noid-version.txt";
    private static final String DOWNLOAD_URL = "https://github.com/guccifurs/noid-plugins/releases/latest/download/NoidHijack.jar";

    private static final String CURRENT_VERSION = "1.0.0";

    /**
     * Check for updates and download if available
     * 
     * @return true if update was downloaded and restart is needed
     */
    public static boolean checkAndUpdate() {
        try {
            System.out.println("[Noid] Checking for updates...");

            // Get latest version from server
            String latestVersion = fetchLatestVersion();
            if (latestVersion == null) {
                System.out.println("[Noid] Could not check for updates");
                return false;
            }

            System.out.println("[Noid] Current: " + CURRENT_VERSION + ", Latest: " + latestVersion);

            // Compare versions
            if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                System.out.println("[Noid] Update available: " + latestVersion);

                // Ask user if they want to update
                int result = JOptionPane.showConfirmDialog(null,
                        "A new version of Noid is available!\n\n" +
                                "Current: " + CURRENT_VERSION + "\n" +
                                "Latest: " + latestVersion + "\n\n" +
                                "Download and install update?",
                        "Noid Update Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (result == JOptionPane.YES_OPTION) {
                    return downloadUpdate();
                }
            } else {
                System.out.println("[Noid] Already up to date");
            }

        } catch (Exception e) {
            System.err.println("[Noid] Update check failed: " + e.getMessage());
        }
        return false;
    }

    private static String fetchLatestVersion() {
        try {
            URL url = new URL(VERSION_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "NoidUpdater/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return reader.readLine().trim();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");

            for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

                if (latestPart > currentPart)
                    return true;
                if (latestPart < currentPart)
                    return false;
            }
        } catch (Exception e) {
            // If parsing fails, compare as strings
            return !latest.equals(current);
        }
        return false;
    }

    private static boolean downloadUpdate() {
        JDialog progress = createProgressDialog("Downloading update...");
        progress.setVisible(true);

        try {
            // Find where NoidHijack.jar is located
            Path currentJar = Paths.get(NoidUpdater.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            if (!currentJar.toString().endsWith(".jar")) {
                // Running from IDE, skip update
                progress.dispose();
                JOptionPane.showMessageDialog(null, "Cannot update in development mode.");
                return false;
            }

            // Download to temp file
            Path tempFile = currentJar.resolveSibling("NoidHijack.jar.update");
            downloadFile(DOWNLOAD_URL, tempFile);

            // Create update script that will replace the JAR after JVM exits
            createUpdateScript(currentJar, tempFile);

            progress.dispose();

            JOptionPane.showMessageDialog(null,
                    "Update downloaded!\n\n" +
                            "RuneLite will now restart to apply the update.",
                    "Update Ready",
                    JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (Exception e) {
            progress.dispose();
            JOptionPane.showMessageDialog(null,
                    "Update failed: " + e.getMessage(),
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static void downloadFile(String urlStr, Path destination) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "NoidUpdater/1.0");
        conn.setInstanceFollowRedirects(true);

        // Handle GitHub redirects
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "NoidUpdater/1.0");
        }

        try (InputStream in = conn.getInputStream();
                OutputStream out = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private static void createUpdateScript(Path currentJar, Path newJar) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Path scriptPath;
        String scriptContent;

        if (os.contains("win")) {
            scriptPath = currentJar.resolveSibling("noid_update.bat");
            scriptContent = "@echo off\n" +
                    "timeout /t 2 /nobreak > nul\n" +
                    "del \"" + currentJar + "\"\n" +
                    "move \"" + newJar + "\" \"" + currentJar + "\"\n" +
                    "del \"%~f0\"\n";
        } else {
            scriptPath = currentJar.resolveSibling("noid_update.sh");
            scriptContent = "#!/bin/bash\n" +
                    "sleep 2\n" +
                    "rm \"" + currentJar + "\"\n" +
                    "mv \"" + newJar + "\" \"" + currentJar + "\"\n" +
                    "rm \"$0\"\n";
        }

        Files.write(scriptPath, scriptContent.getBytes());

        // Make script executable on Unix
        if (!os.contains("win")) {
            scriptPath.toFile().setExecutable(true);
        }

        // Run the script
        Runtime.getRuntime().exec(new String[] { scriptPath.toString() });
    }

    private static JDialog createProgressDialog(String message) {
        JDialog dialog = new JDialog((java.awt.Frame) null, "Noid Updater", false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new java.awt.BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(message);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, java.awt.BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, java.awt.BorderLayout.SOUTH);

        dialog.add(panel);
        return dialog;
    }
}
