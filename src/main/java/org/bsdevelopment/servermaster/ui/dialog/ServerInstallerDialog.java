package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import org.bsdevelopment.servermaster.utils.BuildToolsArtifacts;
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
import java.util.*;
import java.util.stream.Stream;

public final class ServerInstallerDialog {

    private static final String SPIGOT_TYPE = "spigot";
    private static final String BUILDTOOLS_JAR_URL =
            "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";

    private final Stage stage;
    private final BackendApiService API = new BackendApiService();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Button installBtn;
    private final Label infoNote;
    private String selectedType;
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
        header.getStyleClass().add("app-title");

        var subtitle = new Label("Download a server jar, or compile Spigot locally with BuildTools.");
        subtitle.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setMaxWidth(Double.MAX_VALUE);

        var version = blankCombo("Server Version");
        var build = blankCombo("Server Build");

        infoNote = new Label();
        infoNote.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        infoNote.setWrapText(true);
        infoNote.setMaxWidth(Double.MAX_VALUE);

        installBtn = new Button("Install");
        installBtn.getStyleClass().addAll(Styles.ACCENT, "hero-button");
        installBtn.setMaxWidth(Double.MAX_VALUE);
        installBtn.setDisable(true);

        var typeGroup = new ToggleGroup();
        var typeGrid = new FlowPane(12, 12);
        typeGrid.setAlignment(Pos.CENTER);

        List<String> projects = new ArrayList<>(API.fetchProjects());
        if (!projects.contains(SPIGOT_TYPE)) projects.add(SPIGOT_TYPE);
        for (String project : projects) {
            typeGrid.getChildren().add(createTypeTile(project, typeGroup));
        }

        typeGroup.selectedToggleProperty().addListener((obs, previous, selected) -> {
            if (selected == null) {
                selectedType = null;
                resetSelection(version, build);
                updateInfoNote(null, null);
                return;
            }
            onTypeSelected((String) selected.getUserData(), version, build);
        });

        version.setOnAction(actionEvent -> {
            String versionVal = version.getValue();
            build.getItems().clear();

            if (selectedType == null || versionVal == null) {
                installBtn.setDisable(true);
                return;
            }

            if (SPIGOT_TYPE.equalsIgnoreCase(selectedType)) {
                build.setDisable(true);
                installBtn.setDisable(false);
                updateInfoNote(selectedType, versionVal);
                return;
            }

            try {
                build.getItems().addAll(API.extractBuildNumbers(currentBuilds = API.fetchBuilds(selectedType, versionVal)));
                build.setDisable(false);
            } catch (Exception e) {
                LogViewer.system("Failed to load builds: " + e.getMessage());
            }
            updateInfoNote(selectedType, versionVal);
        });

        build.setOnAction(actionEvent -> {
            if (!SPIGOT_TYPE.equalsIgnoreCase(selectedType)) installBtn.setDisable(false);
        });

        installBtn.setOnAction(e -> {
            String project = selectedType;
            if (project == null) return;

            stage.close();
            ServerMasterApp.focusConsole();

            try {
                if (SPIGOT_TYPE.equalsIgnoreCase(project)) {
                    runBuildTools(version.getValue());
                } else {
                    downloadJar(
                            SettingsService.get().getServerPath().resolve("instance").resolve(project),
                            project,
                            version.getValue(),
                            build.getValue(),
                            API.openDownloadConnection(currentBuilds, build.getValue())
                    );
                }
            } catch (IOException ex) {
                LogViewer.system("Install failed: " + ex.getMessage());
            }
        });

        updateInfoNote(null, null);

        var titleBox = new VBox(4, header, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        var card = new VBox(14, typeGrid, version, build, infoNote, new Separator(), installBtn);
        card.getStyleClass().add("status-card");
        card.setFillWidth(true);

        var column = new VBox(20, titleBox, card);
        column.setAlignment(Pos.CENTER);
        column.setMaxWidth(440);

        var content = new StackPane(column);
        content.setPadding(new Insets(10, 24, 24, 24));

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        FX.buildDialogScene(stage, surface, 520, 640);
    }

    private ToggleButton createTypeTile(String project, ToggleGroup group) {
        var tile = new ToggleButton();
        tile.setToggleGroup(group);
        tile.getStyleClass().add("logo-tile");
        tile.setUserData(project);

        var nameLabel = new Label(displayName(project));
        nameLabel.getStyleClass().add("logo-tile-name");
        nameLabel.setVisible(false);
        nameLabel.setMouseTransparent(true);

        Image logo = loadLogo(project);
        if (logo != null) {
            var icon = new ImageView(logo);
            icon.setFitWidth(52);
            icon.setFitHeight(52);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);

            tile.setGraphic(new StackPane(icon, nameLabel));
            tile.hoverProperty().addListener((obs, wasHover, isHover) -> nameLabel.setVisible(isHover));
        } else {
            tile.setText(displayName(project));
        }

        return tile;
    }

    private void onTypeSelected(String project, ComboBox<String> version, ComboBox<String> build) {
        selectedType = project;
        resetSelection(version, build);

        try {
            if (SPIGOT_TYPE.equalsIgnoreCase(project)) {
                version.getItems().addAll(API.fetchSpigotBuildToolsVersions());
            } else {
                version.getItems().addAll(API.fetchVersions(project));
            }
            version.setDisable(false);
        } catch (Exception e) {
            LogViewer.system("Failed to load versions: " + e.getMessage());
        }

        updateInfoNote(project, null);
    }

    private void resetSelection(ComboBox<String> version, ComboBox<String> build) {
        version.getItems().clear();
        version.setValue(null);
        version.setDisable(true);
        build.getItems().clear();
        build.setValue(null);
        build.setDisable(true);
        installBtn.setDisable(true);
    }

    private Image loadLogo(String project) {
        String path = "/images/logos/" + project.toLowerCase(Locale.ROOT) + ".png";
        try (var stream = getClass().getResourceAsStream(path)) {
            return stream != null ? new Image(stream) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String displayName(String project) {
        if (project == null || project.isBlank()) return "";
        return Character.toUpperCase(project.charAt(0)) + project.substring(1);
    }

    private void updateInfoNote(String type, String version) {
        if (type == null || type.isBlank()) {
            infoNote.setText("Choose a server type to begin.");
            return;
        }

        if (SPIGOT_TYPE.equalsIgnoreCase(type)) {
            StringBuilder note = new StringBuilder(
                    "Spigot is compiled locally with BuildTools — this can take several minutes and needs internet access and Git. Output is logged to the console.");
            if (SettingsService.get().isMavenDeployReady()) {
                note.append("\nThe compiled jar will also be deployed to ")
                        .append(SettingsService.get().getMavenRepoUrl())
                        .append(".");
            }
            infoNote.setText(note.toString());
            return;
        }

        infoNote.setText("Downloads a ready-built " + type + " jar into your instance/" + type + " folder.");
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
                Files.createDirectories(buildToolsDir);

                downloadBuildTools(buildToolsJar);
                cleanupStaleJGitLocks(buildToolsDir);
                normalizeGitLineEndings(buildToolsDir);

                LogViewer.system("Executing BuildTools (this can take a while)...");

                String javaExe = config.getJavaPath() != null
                        ? config.getJavaPath().toAbsolutePath().toString()
                        : "java";

                var pb = new ProcessBuilder(
                        javaExe,
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
                forceGitLineEndings(pb);

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

                if (config.isMavenDeployReady()) {
                    deployToMaven(config);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            ServerMasterApp.endInstall(true, "Spigot " + minecraftVersion + " built");
            ServerMasterApp.instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ServerMasterApp.endInstall(false, "BuildTools failed");
            LogViewer.system("BuildTools failed: " + (ex == null ? "Unknown error" : ex.getMessage()));
        });

        task.setOnCancelled(e -> ServerMasterApp.cancelInstall());

        ServerMasterApp.beginInstall(true);

        var thread = new Thread(task, "servermaster-buildtools");
        thread.setDaemon(true);
        thread.start();
    }

    private void deployToMaven(AppSettings config) throws IOException, InterruptedException {
        List<String> selectedKeys = config.getMavenDeployArtifacts();
        List<BuildToolsArtifacts.BuiltJar> toDeploy = BuildToolsArtifacts.scan().stream()
                .filter(jar -> selectedKeys.contains(jar.deployKey()))
                .toList();

        if (toDeploy.isEmpty()) {
            LogViewer.system("Maven deploy skipped: no jars selected (choose them in Settings → Developer).");
            return;
        }

        Path settingsFile = Files.createTempFile("servermaster-mvn-settings", ".xml");
        try {
            Files.writeString(settingsFile, buildMavenSettings(config));
            for (BuildToolsArtifacts.BuiltJar jar : toDeploy) {
                deploySingleJar(jar, config, settingsFile);
            }
        } finally {
            Files.deleteIfExists(settingsFile);
        }
    }

    private void deploySingleJar(BuildToolsArtifacts.BuiltJar jar, AppSettings config, Path settingsFile) throws InterruptedException {
        LogViewer.system("Deploying " + jar.fileName() + " to " + config.getMavenRepoUrl() + " ...");

        Path stagingDir;
        Path stagedJar;
        Path stagedPom = null;
        try {
            stagingDir = Files.createTempDirectory("servermaster-deploy");
            stagedJar = stagingDir.resolve(jar.fileName());
            Files.copy(jar.path(), stagedJar, StandardCopyOption.REPLACE_EXISTING);
            if (jar.pomFile() != null) {
                stagedPom = stagingDir.resolve(jar.pomFile().getFileName().toString());
                Files.copy(jar.pomFile(), stagedPom, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LogViewer.system("Maven deploy could not stage " + jar.fileName() + ": " + e.getMessage());
            return;
        }

        var command = new ArrayList<>(List.of(
                mavenExecutable(),
                "-s", settingsFile.toAbsolutePath().toString(),
                "deploy:deploy-file",
                "-Durl=" + config.getMavenRepoUrl(),
                "-DrepositoryId=" + config.getMavenRepoId(),
                "-Dpackaging=jar",
                "-Dfile=" + stagedJar.toAbsolutePath()
        ));

        if (stagedPom != null) {
            command.add("-DpomFile=" + stagedPom.toAbsolutePath());
        } else {
            command.add("-DgroupId=" + jar.groupId());
            command.add("-DartifactId=" + jar.artifactId());
            command.add("-Dversion=" + jar.version());
        }

        if (jar.classifier() != null && !jar.classifier().isBlank()) {
            command.add("-Dclassifier=" + jar.classifier());
        }

        var pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) LogViewer.console(line);
                }
            } catch (IOException e) {
                LogViewer.system("Maven deploy stream error: " + e.getMessage());
            }

            int exit = process.waitFor();
            if (exit == 0) LogViewer.system("Deployed " + jar.deployKey() + " " + jar.version() + ".");
            else LogViewer.system("Deploy failed for " + jar.fileName() + " (exit code " + exit + ").");
        } catch (IOException e) {
            LogViewer.system("Maven deploy could not start (is 'mvn' on your PATH?): " + e.getMessage());
        } finally {
            deleteQuietly(stagedPom);
            deleteQuietly(stagedJar);
            deleteQuietly(stagingDir);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static String buildMavenSettings(AppSettings config) {
        return """
                <settings>
                  <servers>
                    <server>
                      <id>%s</id>
                      <username>%s</username>
                      <password>%s</password>
                    </server>
                  </servers>
                </settings>
                """.formatted(
                escapeXml(config.getMavenRepoId()),
                escapeXml(config.getMavenUsername()),
                escapeXml(config.getMavenPassword()));
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String mavenExecutable() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    }

    /**
     * BuildTools' shell scripts break when git checks them out with CRLF line endings
     * (typically inherited from a repo cloned with autocrlf=true). Force LF for every
     * repository already present so reused checkouts stay POSIX-clean.
     */
    private static void normalizeGitLineEndings(Path buildToolsDir) {
        Path[] repos = findGitRepos(buildToolsDir);
        for (Path repo : repos) {
            runGit(repo, "config", "core.autocrlf", "false");
            runGit(repo, "config", "core.eol", "lf");
            runGit(repo, "rm", "--cached", "-r", "--quiet", ".");
            runGit(repo, "reset", "--hard", "--quiet");
        }
        if (repos.length > 0) {
            LogViewer.system("Normalized line endings for " + repos.length + " BuildTools repositories.");
        }
    }

    private static Path[] findGitRepos(Path buildToolsDir) {
        if (!Files.isDirectory(buildToolsDir)) return new Path[0];
        try (Stream<Path> stream = Files.walk(buildToolsDir, 3)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> !path.toString().contains("PortableGit"))
                    .filter(path -> Files.exists(path.resolve(".git")))
                    .toArray(Path[]::new);
        } catch (IOException e) {
            LogViewer.system("Could not scan BuildTools folder for repositories: " + e.getMessage());
            return new Path[0];
        }
    }

    private static void runGit(Path repo, String... args) {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("-C");
        command.add(repo.toAbsolutePath().toString());
        Collections.addAll(command, args);

        try {
            var builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.getInputStream().readAllBytes();
            process.waitFor();
        } catch (IOException ignored) {
            // best-effort normalization; BuildTools will report a clearer error if git is unusable
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void forceGitLineEndings(ProcessBuilder builder) {
        var env = builder.environment();
        env.put("GIT_CONFIG_COUNT", "2");
        env.put("GIT_CONFIG_KEY_0", "core.autocrlf");
        env.put("GIT_CONFIG_VALUE_0", "false");
        env.put("GIT_CONFIG_KEY_1", "core.eol");
        env.put("GIT_CONFIG_VALUE_1", "lf");
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

    private void downloadJar(Path folder, String type, String version, String build, HttpURLConnection connection) {
        var file = folder.resolve(type + "-" + version + "-" + build + ".jar");
        var fileName = file.getFileName().toString();

        LogViewer.system("Downloading " + fileName + " ...");
        ServerMasterApp.beginInstall(false);

        var task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Files.createDirectories(folder);

                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "ServerMaster");
                connection.connect();

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code + " " + connection.getResponseMessage());
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

                        if (contentLength > 0) updateProgress(readTotal, contentLength);
                        else updateProgress(-1, 1);
                    }
                } finally {
                    connection.disconnect();
                }

                return null;
            }
        };

        task.progressProperty().addListener((obs, oldV, newV) -> ServerMasterApp.reportInstallProgress(newV.doubleValue()));

        task.setOnSucceeded(e -> {
            ServerMasterApp.endInstall(true, "Installed " + fileName);
            LogViewer.system("Installed: " + fileName);
            ServerMasterApp.instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ServerMasterApp.endInstall(false, "Install failed");
            LogViewer.system("Install failed: " + (ex == null ? "Unknown error" : ex.getMessage()));
        });

        task.setOnCancelled(e -> ServerMasterApp.cancelInstall());

        var thread = new Thread(task, "servermaster-jar-download");
        thread.setDaemon(true);
        thread.start();
    }

    public void show() {
        FX.showDialog(stage);
    }
}
