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
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.InstanceCatalog;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.BackendApiService;
import org.bsdevelopment.servermaster.utils.FX;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class ServerInstallerDialog {
    private final Stage stage;
    private final BackendApiService API = new BackendApiService();
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

        var version = blankCombo("Server Version");
        var build = blankCombo("Server Build");

        type.setOnAction(actionEvent -> {
            var project = type.getValue();
            version.getItems().clear();
            build.setDisable(true);

            try {
                version.getItems().addAll(API.fetchVersions(project));
                version.setDisable(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        version.setOnAction(actionEvent -> {
            var project = type.getValue();
            var versionVal = version.getValue();
            build.getItems().clear();

            try {
                build.getItems().addAll(API.extractBuildNumbers(currentBuilds = API.fetchBuilds(project, versionVal)));
                build.setDisable(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        build.setOnAction(actionEvent -> installBtn.setDisable(false));

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
                handleDownload(progressBar,
                        SettingsService.get().getServerPath().resolve("instance").resolve(type.getValue()),
                        type.getValue(), version.getValue(), build.getValue(),
                        API.openDownloadConnection(currentBuilds, build.getValue()));
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

                long contentLength = connection.getContentLengthLong(); // -1 if unknown

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
