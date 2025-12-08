package com.tonic.installer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Noid Auto-Installer
 * 
 * Double-click to install Noid plugins on vanilla RuneLite.
 * - Finds RuneLite installation automatically
 * - Downloads NoidHijack.jar
 * - Patches config.json
 */
public class NoidInstaller {

    private static final String HIJACK_JAR_NAME = "NoidHijack.jar";
    private static final String HIJACK_DOWNLOAD_URL = "https://github.com/guccifurs/noid-plugins/releases/latest/download/NoidHijack.jar";
    private static final String MAIN_CLASS = "com.tonic.noid.NoidHijack";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                runInstaller();
            } catch (Exception e) {
                showError("Installation failed: " + e.getMessage());
            }
        });
    }

    private static void runInstaller() throws Exception {
        // Find RuneLite directory
        Path runeliteDir = findRuneLiteDir();
        if (runeliteDir == null) {
            showError("Could not find RuneLite installation.\n\n" +
                    "Please make sure RuneLite is installed and has been run at least once.");
            return;
        }

        Path configPath = runeliteDir.resolve("config.json");
        if (!Files.exists(configPath)) {
            showError("config.json not found in RuneLite directory.\n\n" +
                    "Please run RuneLite at least once before installing.");
            return;
        }

        // Confirm installation
        int result = JOptionPane.showConfirmDialog(null,
                "This will install Noid plugins on RuneLite.\n\n" +
                        "RuneLite directory: " + runeliteDir + "\n\n" +
                        "Continue?",
                "Noid Installer",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // Show progress
        JDialog progress = createProgressDialog("Installing Noid...");
        progress.setVisible(true);

        new Thread(() -> {
            try {
                // Step 1: Download NoidHijack.jar
                updateProgress(progress, "Downloading NoidHijack.jar...");
                Path hijackJar = runeliteDir.resolve(HIJACK_JAR_NAME);
                downloadFile(HIJACK_DOWNLOAD_URL, hijackJar);

                // Step 2: Backup config.json
                updateProgress(progress, "Backing up config.json...");
                Path backupPath = runeliteDir.resolve("config.json.backup");
                Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

                // Step 3: Patch config.json
                updateProgress(progress, "Patching config.json...");
                patchConfig(configPath);

                // Done!
                SwingUtilities.invokeLater(() -> {
                    progress.dispose();
                    JOptionPane.showMessageDialog(null,
                            "Noid installed successfully!\n\n" +
                                    "You can now launch RuneLite normally.\n" +
                                    "Look for the Noid panel in the sidebar to log in.",
                            "Installation Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progress.dispose();
                    showError("Installation failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private static Path findRuneLiteDir() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        java.util.List<Path> possiblePaths = new java.util.ArrayList<>();

        if (os.contains("win")) {
            possiblePaths.add(Paths.get(userHome, "AppData", "Local", "RuneLite"));
            possiblePaths.add(Paths.get(userHome, ".runelite"));
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                possiblePaths.add(Paths.get(localAppData, "RuneLite"));
            }
        } else if (os.contains("mac")) {
            possiblePaths.add(Paths.get(userHome, ".runelite"));
            possiblePaths.add(Paths.get(userHome, "Library", "Application Support", "RuneLite"));
        } else {
            // Linux - check multiple locations
            possiblePaths.add(Paths.get(userHome, ".runelite"));

            // Snap RuneLite
            possiblePaths.add(Paths.get(userHome, "snap", "runelite", "current", ".runelite"));
            possiblePaths.add(Paths.get(userHome, "snap", "runelite", "common"));

            // Flatpak
            possiblePaths.add(Paths.get(userHome, ".var", "app", "net.runelite.RuneLite", "data", "runelite"));

            // Check for Steam Proton RuneLite
            Path steamPath = Paths.get(userHome, "snap", "steam", "common", ".local", "share", "Steam",
                    "steamapps", "compatdata");
            if (Files.exists(steamPath)) {
                try {
                    Files.list(steamPath).forEach(compatDir -> {
                        Path rlPath = compatDir.resolve("pfx/drive_c/users/steamuser/AppData/Local/RuneLite");
                        if (Files.exists(rlPath)) {
                            possiblePaths.add(rlPath);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }

        // Find first path that has config.json
        for (Path path : possiblePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                Path configPath = path.resolve("config.json");
                if (Files.exists(configPath)) {
                    return path;
                }
            }
        }

        // If no config.json found, return first existing directory
        for (Path path : possiblePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                return path;
            }
        }

        return null;
    }

    private static void downloadFile(String urlStr, Path destination) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "NoidInstaller/1.0");
        conn.setInstanceFollowRedirects(true);

        // Handle redirects manually for GitHub releases
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "NoidInstaller/1.0");
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

    private static void patchConfig(Path configPath) throws IOException {
        String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);

        // Check if already patched
        if (content.contains(HIJACK_JAR_NAME)) {
            return; // Already installed
        }

        // Add NoidHijack.jar to classPath
        // Pattern: "classPath": ["RuneLite.jar"]
        // Becomes: "classPath": ["RuneLite.jar", "NoidHijack.jar"]
        content = content.replaceFirst(
                "(\"classPath\"\\s*:\\s*\\[\\s*\"RuneLite\\.jar\")",
                "$1, \"" + HIJACK_JAR_NAME + "\"");

        // Change mainClass
        // Pattern: "mainClass": "net.runelite.launcher.Launcher"
        // Becomes: "mainClass": "com.tonic.noid.NoidHijack"
        content = content.replaceFirst(
                "\"mainClass\"\\s*:\\s*\"[^\"]+\"",
                "\"mainClass\": \"" + MAIN_CLASS + "\"");

        Files.write(configPath, content.getBytes(StandardCharsets.UTF_8));
    }

    private static JDialog createProgressDialog(String message) {
        JDialog dialog = new JDialog((Frame) null, "Noid Installer", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(message);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.SOUTH);

        dialog.add(panel);
        return dialog;
    }

    private static void updateProgress(JDialog dialog, String message) {
        SwingUtilities.invokeLater(() -> {
            Component[] components = ((JPanel) dialog.getContentPane().getComponent(0)).getComponents();
            for (Component c : components) {
                if (c instanceof JLabel) {
                    ((JLabel) c).setText(message);
                }
            }
        });
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Noid Installer - Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
