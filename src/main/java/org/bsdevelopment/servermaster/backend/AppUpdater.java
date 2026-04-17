package org.bsdevelopment.servermaster.backend;

import javafx.application.Platform;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.config.SettingsService;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

public class AppUpdater {
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    public void downloadAndRestart(String jarDownloadUrl) {
        Path currentJar = resolveCurrentJar();
        if (currentJar == null) {
            LogViewer.update("Auto-update is not supported when running outside a JAR. Download the new version manually from GitHub.");
            return;
        }

        try {
            Path updateDir = resolveUpdateDir();
            Files.createDirectories(updateDir);
            Path tempJar = updateDir.resolve("ServerMaster-update.jar");
            LogViewer.update("Downloading update...");
            HttpRequest request = HttpRequest.newBuilder(URI.create(jarDownloadUrl))
                    .header("User-Agent", "ServerMaster")
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) throw new IllegalStateException("HTTP " + response.statusCode() + " from download URL");

            try (InputStream in = response.body()) {
                Files.copy(in, tempJar, StandardCopyOption.REPLACE_EXISTING);
            }

            long bytes = Files.size(tempJar);
            if (bytes == 0) {
                Files.deleteIfExists(tempJar);
                throw new IllegalStateException("Downloaded file is empty");
            }

            LogViewer.update("Downloaded " + (bytes / 1024) + " KB — validating...");

            try (var ignored = new JarFile(tempJar.toFile())) {
                LogViewer.update("JAR validation passed");
            } catch (Exception e) {
                Files.deleteIfExists(tempJar);
                throw new IllegalStateException("Downloaded JAR is corrupted — " + e.getMessage());
            }

            String javaExe = resolveJavaExe();
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");

            if (windows) {
                applyUpdateWindows(javaExe, currentJar, tempJar, updateDir);
            } else {
                Path launchJar;
                try {
                    Files.move(tempJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
                    launchJar = currentJar;
                    LogViewer.update("JAR replaced — restarting...");
                } catch (AccessDeniedException e) {
                    LogViewer.update("No write access to install directory — launching from user directory...");
                    launchJar = tempJar;
                }

                new ProcessBuilder(javaExe, "-jar", launchJar.toAbsolutePath().toString())
                        .directory(Path.of(System.getProperty("user.dir")).toFile())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                Thread.sleep(500);
            }

            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            LogViewer.update("Update failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyUpdateWindows(String javaExe, Path currentJar, Path tempJar, Path updateDir) throws Exception {
        Path script = updateDir.resolve("servermaster-update.bat");
        String bat = "@echo off\r\n"
                + "timeout /t 2 /nobreak >nul\r\n"
                + "move /y \"" + tempJar.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"\r\n"
                + "start \"\" \"" + javaExe + "\" -jar \"" + currentJar.toAbsolutePath() + "\"\r\n"
                + "del \"%~f0\"\r\n";
        Files.writeString(script, bat);
        LogViewer.update("Applying update via script — restarting...");
        new ProcessBuilder("cmd", "/c", script.toAbsolutePath().toString())
                .inheritIO()
                .start();
    }

    private Path resolveCurrentJar() {
        try {
            var location = AppUpdater.class.getProtectionDomain().getCodeSource().getLocation();
            var path = Path.of(location.toURI());
            if (path.toString().endsWith(".jar")) return path;
        } catch (Exception ignored) {}
        return null;
    }

    private Path resolveUpdateDir() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String home = System.getProperty("user.home");

        if (windows) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home).resolve("ServerMaster");
        }

        String xdgData = System.getenv("XDG_DATA_HOME");
        return Path.of(xdgData != null ? xdgData : home + "/.local/share").resolve("servermaster");
    }

    private String resolveJavaExe() {
        var javaPath = SettingsService.get().getJavaPath();
        if (javaPath != null) return javaPath.toAbsolutePath().toString();
        return ProcessHandle.current().info().command().orElse("java");
    }
}
