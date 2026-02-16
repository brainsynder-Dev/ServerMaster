package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.config.AppSettings;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.InstanceCatalog;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.BackendApiService;
import org.bsdevelopment.servermaster.utils.FX;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public final class ServerInstallerDialog {

    private static final String SPIGOT_TYPE = "spigot";
    private static final String BUILDTOOLS_JAR_URL =
            "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";

    private final Stage stage;
    private final BackendApiService API = new BackendApiService();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private Button installBtn;
    private List<BackendApiService.BuildInfo> currentBuilds = new ArrayList<>();

    public ServerInstallerDialog(Stage owner) throws IOException, InterruptedException {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Server Installer");

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var header = new Label("Server Installer");
        header.getStyleClass().addAll(Styles.TITLE_3);

        var type = blankCombo("Server Type");
        type.setDisable(false);
        type.getItems().addAll(API.fetchProjects());
        if (!type.getItems().contains(SPIGOT_TYPE)) type.getItems().add(SPIGOT_TYPE);

        var version = blankCombo("Server Version");
        var build = blankCombo("Server Build");

        type.setOnAction(actionEvent -> {
            var project = type.getValue();
            version.getItems().clear();
            build.setDisable(true);
            installBtn.setDisable(true);

            try {
                if (SPIGOT_TYPE.equalsIgnoreCase(project)) {
                    version.getItems().addAll(API.fetchSpigotBuildToolsVersions());
                    version.setDisable(false);
                    build.getItems().clear();
                    build.setDisable(true);
                } else {
                    version.getItems().addAll(API.fetchVersions(project));
                    version.setDisable(false);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        version.setOnAction(actionEvent -> {
            var project = type.getValue();
            var versionVal = version.getValue();
            build.getItems().clear();

            if (project == null || versionVal == null) {
                installBtn.setDisable(true);
                return;
            }

            if (SPIGOT_TYPE.equalsIgnoreCase(project)) {
                build.setDisable(true);
                installBtn.setDisable(false);
                return;
            }

            try {
                build.getItems().addAll(API.extractBuildNumbers(currentBuilds = API.fetchBuilds(project, versionVal)));
                build.setDisable(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        build.setOnAction(actionEvent -> {
            if (!SPIGOT_TYPE.equalsIgnoreCase(type.getValue())) {
                installBtn.setDisable(false);
            }
        });

        var form = new VBox(10, type, version, build);
        form.setAlignment(Pos.TOP_CENTER);

        installBtn = new Button("Install Server Jar");
        installBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        installBtn.setMaxWidth(Double.MAX_VALUE);
        installBtn.setDisable(true);

        var progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add(Styles.MEDIUM);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        installBtn.setOnAction(e -> {
            try {
                if (SPIGOT_TYPE.equalsIgnoreCase(type.getValue())) {
                    stage.close();
                    runBuildTools(version.getValue());
                    return;
                }

                handleDownload(
                        progressBar,
                        SettingsService.get().getServerPath().resolve("instance").resolve(type.getValue()),
                        type.getValue(),
                        version.getValue(),
                        build.getValue(),
                        API.openDownloadConnection(currentBuilds, build.getValue())
                );
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        var content = new VBox(12, header, form, installBtn, progressBar);
        content.setPadding(new Insets(10, 16, 0, 16));

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        var scene = new Scene(surface, 520, 320);
        scene.setFill(Color.TRANSPARENT);
        FX.addStyleSheet(scene);
        stage.setScene(scene);
    }

    private void runBuildTools(String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.isBlank()) {
            LogViewer.system("Missing Minecraft version for BuildTools.");
            return;
        }

        AppSettings config = SettingsService.get();
        Path serverRoot = SettingsService.get().getServerPath();
        Path buildToolsDir = serverRoot.resolve("buildtools");
        Path buildToolsJar = buildToolsDir.resolve("BuildTools.jar");

        var task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ServerMasterApp.lockApplication();

                Files.createDirectories(buildToolsDir);

                downloadBuildTools(buildToolsJar);
                cleanupStaleJGitLocks(buildToolsDir);

                LogViewer.system("Executing BuildTools (this can take a while)...");

                var pb = new ProcessBuilder(
                        "java",
                        "-Xms" + config.getMemory() + "G",
                        "-Xmx" + config.getMemory() + "G",
                        "-jar",
                        buildToolsJar.toAbsolutePath().toString(),
                        "--rev",
                        minecraftVersion,
                        "--remapped"
                );

                pb.directory(buildToolsDir.toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();
                ServerMasterApp.registerBuildToolsProcess(process);

                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) LogViewer.console(line);
                    }
                } finally {
                    // Ensure we always clear the tracked process reference
                    ServerMasterApp.clearBuildToolsProcess(process);
                }

                int exit = process.waitFor();
                if (exit != 0) throw new IllegalStateException("BuildTools failed (exit code " + exit + ")");

                Path spigotJar = findNewestSpigotJar(buildToolsDir)
                        .orElseThrow(() -> new IllegalStateException("BuildTools finished but no spigot-*.jar was found"));

                Path outDir = serverRoot.resolve("instance").resolve(SPIGOT_TYPE);
                Files.createDirectories(outDir);

                Path outJar = outDir.resolve("spigot-" + minecraftVersion + ".jar");
                Files.copy(spigotJar, outJar,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);

                LogViewer.system("Spigot jar built: " + outJar.getFileName());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            ServerMasterApp.unlockApplication();
            ServerMasterApp.instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
        });

        task.setOnFailed(e -> {
            ServerMasterApp.unlockApplication();
            Throwable ex = task.getException();
            LogViewer.system("BuildTools failed: " + (ex == null ? "Unknown error" : ex.getMessage()));
        });

        task.setOnCancelled(e -> ServerMasterApp.unlockApplication());

        var thread = new Thread(task, "servermaster-buildtools");
        thread.setDaemon(true);
        thread.start();
    }

    private void downloadBuildTools(Path targetJar) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BUILDTOOLS_JAR_URL))
                .header("User-Agent", "ServerMaster")
                .GET()
                .build();

        HttpResponse<Path> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(targetJar));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("Failed to download BuildTools.jar: HTTP " + resp.statusCode());
        }
    }

    private Optional<Path> findNewestSpigotJar(Path buildToolsDir) throws IOException {
        try (Stream<Path> s = Files.walk(buildToolsDir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.startsWith("spigot-") && n.endsWith(".jar");
                    })
                    .max(Comparator.comparingLong(a -> a.toFile().lastModified()));
        }
    }private static void cleanupStaleJGitLocks(Path buildToolsDir) {
        // Typical BuildTools repositories that may leave an index.lock behind when killed.
        Path[] lockFiles = new Path[] {
                buildToolsDir.resolve("CraftBukkit/.git/index.lock"),
                buildToolsDir.resolve("Spigot/.git/index.lock"),
                buildToolsDir.resolve("Bukkit/.git/index.lock"),
                buildToolsDir.resolve("BuildData/.git/index.lock")
        };

        for (Path lock : lockFiles) {
            try {
                if (Files.exists(lock)) {
                    Files.delete(lock);
                    LogViewer.system("Removed stale git lock: " + lock.getFileName());
                }
            } catch (IOException ignored) {
                // If Windows/AV still holds it, BuildTools will throw the same message again.
            }
        }
    }

    private static ComboBox<String> blankCombo(String prompt) {
        var cb = new ComboBox<String>();
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setDisable(true);
        cb.getItems().clear();
        return cb;
    }

    private void handleDownload(ProgressBar progress, Path folder, String type, String version, String build, HttpURLConnection connection) {
        installBtn.setDisable(true);
        progress.setVisible(true);
        progress.setProgress(0);

        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            installBtn.setDisable(false);
            progress.setVisible(false);
            throw new RuntimeException("Failed to create folder: " + folder, e);
        }

        var file = folder.resolve(type + "-" + version + "-" + build + ".jar");
        var fileName = file.getFileName().toString();

        var task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "ServerMaster");
                connection.connect();

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new java.io.IOException("HTTP " + code + " " + connection.getResponseMessage());
                }

                long contentLength = connection.getContentLengthLong();

                try (var in = new BufferedInputStream(connection.getInputStream());
                     var out = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

                    byte[] buf = new byte[64 * 1024];
                    long readTotal = 0;
                    int read;

                    while ((read = in.read(buf)) != -1) {
                        if (isCancelled()) break;

                        out.write(buf, 0, read);
                        readTotal += read;

                        if (contentLength > 0) {
                            updateProgress(readTotal, contentLength);
                        } else {
                            updateProgress(-1, 1);
                        }
                    }
                } finally {
                    connection.disconnect();
                }

                return null;
            }
        };

        progress.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            progress.progressProperty().unbind();
            progress.setProgress(0);
            progress.setVisible(false);
            installBtn.setDisable(false);

            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initStyle(StageStyle.TRANSPARENT);
            alert.initOwner(stage);
            alert.setHeaderText("Install Complete");
            alert.setContentText("Install has been completed for '" + fileName + "'");
            alert.show();

            ServerMasterApp.instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
        });

        task.setOnFailed(e -> {
            progress.progressProperty().unbind();
            progress.setProgress(0);
            progress.setVisible(false);
            installBtn.setDisable(false);

            Throwable ex = task.getException();

            var alert = new Alert(Alert.AlertType.ERROR);
            alert.initStyle(StageStyle.TRANSPARENT);
            alert.initOwner(stage);
            alert.setHeaderText("Install Failed");
            alert.setContentText(ex == null ? "Unknown error" : ex.getMessage());
            alert.show();
        });

        task.setOnCancelled(e -> {
            progress.progressProperty().unbind();
            progress.setProgress(0);
            progress.setVisible(false);
            installBtn.setDisable(false);
        });

        var thread = new Thread(task, "servermaster-jar-download");
        thread.setDaemon(true);
        thread.start();
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }
}
