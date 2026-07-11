package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.config.AppSettings;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.InstanceCatalog;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.AdvString;
import org.bsdevelopment.servermaster.utils.BuildToolsArtifacts;
import org.bsdevelopment.servermaster.utils.FX;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SettingsDialog {
    private final Stage stage;
    private final Runnable onSaved;
    private final boolean required;

    public SettingsDialog(Stage owner) {
        this(owner, null, false);
    }

    public SettingsDialog(Stage owner, Runnable onSaved, boolean required) {
        this.onSaved = onSaved;
        this.required = required;

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("ServerMaster Settings");

        AppSettings settings = SettingsService.get();

        var title = new Label("Settings");
        title.getStyleClass().addAll(Styles.TITLE_3);

        // --- Server section ---------------------------------------------------
        Path serverPath = settings.getServerPath();
        boolean blank = serverPath == null;
        if (blank) serverPath = Constants.WORKING_PATH;

        String path = serverPath.toAbsolutePath().toString();
        if (blank) path = AdvString.beforeLast("\\.", path);

        var serverFolder = new TextField(path);
        serverFolder.setDisable(true);
        HBox.setHgrow(serverFolder, Priority.ALWAYS);

        var browse = new Button("📁");
        browse.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Server Folder");
            File dir = chooser.showDialog(stage.getOwner());
            if (dir != null) {
                serverFolder.setText(dir.getAbsolutePath());
                SettingsService.get().setServerPath(dir.toPath());
            }
        });

        var folderRow = new HBox(FX.inputGroup(serverFolder, browse));

        long initialRam = clampRam(settings.getMemory());
        var ramLabel = fieldLabel("Dedicated RAM — " + initialRam + " GB");
        var ram = new Slider(1, Constants.MAX_GB, initialRam);
        ram.setMajorTickUnit(4);
        ram.setBlockIncrement(1);
        ram.setMinorTickCount(3);
        ram.setShowTickMarks(true);
        ram.setShowTickLabels(true);
        ram.setSnapToTicks(true);
        ram.valueProperty().addListener((obs, oldV, newV) -> {
            int gb = (int) Math.round(newV.doubleValue());
            ramLabel.setText("Dedicated RAM — " + gb + " GB");
            SettingsService.get().setMemory(gb);
        });

        var port = new TextField(String.valueOf(settings.getPort()));
        port.setPrefColumnCount(6);
        var checkUpdates = new CheckBox("Check for updates on startup");
        checkUpdates.setSelected(settings.isCheckForUpdates());
        checkUpdates.setOnAction(e -> SettingsService.get().setCheckForUpdates(checkUpdates.isSelected()));

        var portRow = new HBox(10, checkUpdates, spacer(), new Label("Port"), port);
        portRow.setAlignment(Pos.CENTER_LEFT);

        var serverSection = section("Server",
                fieldLabel("Server folder"), folderRow,
                ramLabel, ram,
                portRow);

        // --- Java section -----------------------------------------------------
        var javaPath = new TextField(settings.getJavaPath() == null
                ? Constants.JAVA_MANAGER.getPrimaryInstallation().getJavaExecutable().getAbsolutePath()
                : settings.getJavaPath().toAbsolutePath().toString());
        javaPath.setDisable(true);
        HBox.setHgrow(javaPath, Priority.ALWAYS);

        var detect = new Button("Detect Java");
        detect.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        detect.setOnAction(e -> new JavaVersionDialog(stage,
                () -> javaPath.setText(SettingsService.get().getJavaPath().toAbsolutePath().toString())).show());

        var javaRow = new HBox(10, javaPath, detect);
        javaRow.setAlignment(Pos.CENTER_LEFT);

        var javaSection = section("Java", fieldLabel("Java executable"), javaRow);

        // --- Developer section (hidden unless developer-mode is set) ----------
        var mavenEnabled = new CheckBox("Deploy BuildTools jars to a Maven repository");
        mavenEnabled.setSelected(settings.isMavenDeployEnabled());

        var repoUrl = new TextField(settings.getMavenRepoUrl());
        repoUrl.setPromptText("https://repo.example.com/repository/maven-releases/");
        var repoId = new TextField(settings.getMavenRepoId());
        repoId.setPromptText("repository id (matches your settings.xml server id)");
        var username = new TextField(settings.getMavenUsername());
        username.setPromptText("username");
        var password = new PasswordField();
        password.setText(settings.getMavenPassword());
        password.setPromptText("password / token");

        List<BuildToolsArtifacts.BuiltJar> builtJars = BuildToolsArtifacts.scan();

        var jarBox = new VBox(6);
        var jarChecks = new ArrayList<CheckBox>();
        if (builtJars.isEmpty()) {
            var empty = new Label("Run the Spigot installer once to compile the jars, then reopen Settings to choose which to upload.");
            empty.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
            empty.setWrapText(true);
            jarBox.getChildren().add(empty);
        } else {
            for (BuildToolsArtifacts.BuiltJar jar : builtJars) {
                var check = new CheckBox(jar.displayName());
                check.setSelected(settings.getMavenDeployArtifacts().contains(jar.deployKey()));
                check.setUserData(jar.deployKey());
                jarChecks.add(check);
                jarBox.getChildren().add(check);
            }
        }

        var mavenFields = new VBox(8,
                fieldLabel("Repository URL"), repoUrl,
                fieldLabel("Repository ID"), repoId,
                fieldLabel("Username"), username,
                fieldLabel("Password"), password,
                fieldLabel("Jars to deploy"), jarBox);
        mavenFields.disableProperty().bind(mavenEnabled.selectedProperty().not());

        var developerSection = section("Developer — Maven Deploy", mavenEnabled, mavenFields);
        developerSection.setVisible(settings.isDeveloperMode());
        developerSection.setManaged(settings.isDeveloperMode());

        // --- Save -------------------------------------------------------------
        var save = new Button("SAVE SETTINGS");
        save.getStyleClass().addAll(Styles.ACCENT, "hero-button");
        save.setMaxWidth(Double.MAX_VALUE);

        final boolean[] saved = { false };
        save.setOnAction(e -> {
            AppSettings current = SettingsService.get();
            try {
                current.setPort(Integer.parseInt(port.getText().trim()));
            } catch (NumberFormatException ignored) { }
            current.setJavaPath(Path.of(javaPath.getText()));

            current.setMavenDeployEnabled(mavenEnabled.isSelected());
            current.setMavenRepoUrl(repoUrl.getText().trim());
            current.setMavenRepoId(repoId.getText().trim());
            current.setMavenUsername(username.getText().trim());
            current.setMavenPassword(password.getText());

            if (!builtJars.isEmpty()) {
                var selectedJars = new ArrayList<String>();
                for (CheckBox check : jarChecks) {
                    if (check.isSelected()) selectedJars.add((String) check.getUserData());
                }
                current.setMavenDeployArtifacts(selectedJars);
            }

            SettingsService.save();
            ServerMasterApp.instanceCatalog = new InstanceCatalog(current.getServerPath());
            saved[0] = true;

            stage.close();
            if (onSaved != null) onSaved.run();
        });

        var body = new VBox(18, title, serverSection, javaSection, developerSection);
        body.setPadding(new Insets(4, 18, 8, 18));

        var scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        var saveBar = new VBox(save);
        saveBar.setPadding(new Insets(8, 18, 16, 18));

        var content = new VBox(scroll, saveBar);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        FX.buildDialogScene(stage, surface, 620, 560);

        if (required) {
            stage.setOnHidden(e -> {
                if (!saved[0]) Platform.exit();
            });
        }
    }

    public void show() {
        FX.showDialog(stage);
    }

    private static long clampRam(long ramGb) {
        if (ramGb < 1) return 1;
        if (ramGb > Constants.MAX_GB) return Constants.MAX_GB;
        return ramGb;
    }

    private static VBox section(String heading, Region... rows) {
        var header = new Label(heading);
        header.getStyleClass().add("settings-section");
        header.setMaxWidth(Double.MAX_VALUE);

        var box = new VBox(8, header);
        box.getChildren().addAll(rows);
        box.getStyleClass().add("settings-card");
        return box;
    }

    private static Region spacer() {
        var region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private static Label fieldLabel(String text) {
        var label = new Label(text);
        label.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        return label;
    }
}
