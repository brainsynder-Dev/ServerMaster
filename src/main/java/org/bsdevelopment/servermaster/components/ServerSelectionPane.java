package org.bsdevelopment.servermaster.components;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.config.AppSettings;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.instance.server.ServerLaunchConfig;
import org.bsdevelopment.servermaster.instance.server.gamerule.GameRuleFileApplier;
import org.bsdevelopment.servermaster.instance.server.thread.ServerOutputListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ServerSelectionPane extends VBox {
    private final ComboBox<String> type;
    private final ComboBox<String> version;
    private final ComboBox<String> build;
    private final Button start;

    private final ServerSelection selection;
    private final ServerOutputListener outputListener;
    private final Runnable onStart;

    private final BooleanProperty serverRunning;
    private final BooleanProperty versionEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty buildEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty startEnabled = new SimpleBooleanProperty(false);

    public ServerSelectionPane(ServerSelection selection, BooleanProperty serverRunning, ServerOutputListener outputListener, Runnable onStart) throws IOException {
        this.selection = Objects.requireNonNull(selection, "selection");
        this.serverRunning = Objects.requireNonNull(serverRunning, "serverRunning");
        this.outputListener = Objects.requireNonNull(outputListener, "outputListener");
        this.onStart = Objects.requireNonNull(onStart, "onStart");

        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);

        type = combo("Server Type", ServerMasterApp.instanceCatalog.listServerTypes());
        version = blankCombo("Server Version");
        build = blankCombo("Server Build");
        build.setVisible(false);

        start = new Button("START SERVER");
        start.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        start.setMaxWidth(Double.MAX_VALUE);

        type.disableProperty().bind(serverRunning);
        version.disableProperty().bind(Bindings.or(serverRunning, versionEnabled.not()));
        build.disableProperty().bind(Bindings.or(serverRunning, buildEnabled.not()));
        start.disableProperty().bind(Bindings.or(serverRunning, startEnabled.not()));

        type.setOnAction(actionEvent -> {
            version.getItems().clear();
            build.getItems().clear();
            build.setVisible(false);
            buildEnabled.set(false);
            startEnabled.set(false);

            String selectedType = type.getValue();
            if (selectedType == null || selectedType.isBlank()) {
                versionEnabled.set(false);
                return;
            }

            versionEnabled.set(true);
            try {
                version.getItems().addAll(ServerMasterApp.instanceCatalog.listVersions(selectedType));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        version.setOnAction(actionEvent -> {
            build.getItems().clear();
            build.setVisible(false);
            buildEnabled.set(false);
            startEnabled.set(false);

            String selectedType = type.getValue();
            String selectedVersion = version.getValue();
            if (selectedType == null || selectedType.isBlank() || selectedVersion == null || selectedVersion.isBlank()) {
                return;
            }

            try {
                List<Integer> builds = ServerMasterApp.instanceCatalog.listBuilds(selectedType, selectedVersion);
                if (builds.isEmpty()) {
                    startEnabled.set(true);
                } else {
                    build.setVisible(true);
                    buildEnabled.set(true);
                    build.getItems().addAll(builds.stream().map(Object::toString).toList());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        build.setOnAction(actionEvent -> {
            String b = build.getValue();
            startEnabled.set(b != null && !b.isBlank());
        });

        restoreSelectionFromModel();

        type.valueProperty().addListener((obs, o, v) -> selection.setServerType(v == null ? "" : v));
        version.valueProperty().addListener((obs, o, v) -> selection.setServerVersion(v == null ? "" : v));
        build.valueProperty().addListener((obs, o, v) -> selection.setServerBuild(v == null ? "" : v));

        getChildren().addAll(type, version, build, start);
        VBox.setVgrow(start, Priority.NEVER);

        start.setOnAction(actionEvent -> startSelectedServer());
    }

    public void startSelectedServer() {
        if (serverRunning.get()) return;

        String selectedType = type.getValue();
        String selectedVersion = version.getValue();
        String selectedBuild = build.isVisible() ? build.getValue() : "";

        if (selectedType == null || selectedType.isBlank()) return;
        if (selectedVersion == null || selectedVersion.isBlank()) return;
        if (build.isVisible() && (selectedBuild == null || selectedBuild.isBlank())) return;

        onStart.run();
        serverRunning.set(true);

        AppSettings settings = SettingsService.get();
        ServerLaunchConfig config = new ServerLaunchConfig(
                settings.getJavaPath(),
                settings.getMemory(),
                settings.getPort(),
                true,
                List.of()
        );

        Path gameruleFile = Constants.WORKING_PATH.resolve("gamerules.json");
        ServerOutputListener wrapped = GameRuleFileApplier.wrap(outputListener, gameruleFile);

        ServerHandlerAPI.startServer(
                selectedType,
                selectedVersion,
                selectedBuild == null ? "" : selectedBuild,
                config,
                wrapped,
                (server, statusCode) -> Platform.runLater(() -> serverRunning.set(false))
        );
    }

    private void restoreSelectionFromModel() throws IOException {
        if (!selection.getServerType().isBlank()) {
            type.setValue(selection.getServerType());
        }

        if (!selection.getServerVersion().isBlank()) {
            versionEnabled.set(true);
            version.getItems().addAll(ServerMasterApp.instanceCatalog.listVersions(type.getValue()));
            version.setValue(selection.getServerVersion());
        }

        if (!selection.getServerBuild().isBlank()) {
            List<Integer> builds = ServerMasterApp.instanceCatalog.listBuilds(type.getValue(), version.getValue());
            if (!builds.isEmpty()) {
                build.setVisible(true);
                buildEnabled.set(true);
                build.getItems().addAll(builds.stream().map(Object::toString).toList());
                build.setValue(selection.getServerBuild());
                startEnabled.set(true);
            }
        } else {
            if (!selection.getServerType().isBlank() && !selection.getServerVersion().isBlank()) {
                List<Integer> builds = ServerMasterApp.instanceCatalog.listBuilds(type.getValue(), version.getValue());
                if (builds.isEmpty()) {
                    startEnabled.set(true);
                }
            }
        }
    }

    private static ComboBox<String> combo(String prompt, List<String> items) {
        var cb = new ComboBox<String>();
        cb.getItems().addAll(items);
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private static ComboBox<String> blankCombo(String prompt) {
        var cb = new ComboBox<String>();
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }
}
