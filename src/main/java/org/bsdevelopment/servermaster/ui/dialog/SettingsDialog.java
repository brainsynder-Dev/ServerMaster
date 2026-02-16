package org.bsdevelopment.servermaster.ui.dialog;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.InstanceCatalog;
import org.bsdevelopment.servermaster.ui.window.WindowButtons;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.AdvString;
import org.bsdevelopment.servermaster.utils.FX;

import java.io.File;
import java.nio.file.Path;

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

        var title = new Label("ServerMaster Settings");
        title.getStyleClass().addAll(Styles.TITLE_3);

        Path serverPath = SettingsService.get().getServerPath();
        boolean blank = false;
        if (serverPath == null) {
            blank = true;
            serverPath = Constants.WORKING_PATH;
        }

        String path = serverPath.toAbsolutePath().toString();
        if (blank) path = AdvString.beforeLast("\\.", path);

        var serverFolder = new TextField(path);
        serverFolder.setDisable(true);
        var browse = new Button("📁");
        browse.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        browse.setOnAction(actionEvent -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Server Folder");
            File dir = dc.showDialog(stage.getOwner());
            if (dir != null) {
                String selectedPath = dir.getAbsolutePath();
                serverFolder.setText(selectedPath);
                SettingsService.get().setServerPath(dir.toPath());
            }
        });

        var folderRow = new HBox(8, FX.inputGroup(serverFolder, browse));
        HBox.setHgrow(serverFolder, Priority.ALWAYS);

        long initialRamGb = SettingsService.get().getMemory();
        if (initialRamGb < 1) initialRamGb = 1;
        if (initialRamGb > Constants.MAX_GB) initialRamGb = Constants.MAX_GB;

        var ramLabel = rowLabel("Server Ram (" + initialRamGb + " GB)");
        var ram = new Slider(1, Constants.MAX_GB, initialRamGb);
        ram.setMajorTickUnit(4);
        ram.setBlockIncrement(1);
        ram.setMinorTickCount(3);
        ram.setShowTickMarks(true);
        ram.setShowTickLabels(true);
        ram.setSnapToTicks(true);

        ram.valueProperty().addListener((obs, oldV, newV) -> {
            int gb = (int) Math.round(newV.doubleValue());
            ramLabel.setText("Server Ram (" + gb + " GB)");
            SettingsService.get().setMemory(gb);
        });

        var skipStartupWindow = new CheckBox("Skip Startup Window");
        skipStartupWindow.setSelected(SettingsService.get().isSkipStartupWindow());
        skipStartupWindow.setOnAction(e -> SettingsService.get().setSkipStartupWindow(skipStartupWindow.isSelected()));

        var port = new TextField(String.valueOf(SettingsService.get().getPort()));
        port.setPrefColumnCount(6);

        var portRow = new HBox(8, skipStartupWindow, spacer(), new Label("Port Number"), port);
        portRow.setAlignment(Pos.CENTER_LEFT);

        var javaPath = new TextField((SettingsService.get().getJavaPath() == null)
                ? Constants.JAVA_MANAGER.getPrimaryInstallation().getJavaExecutable().getAbsolutePath()
                : SettingsService.get().getJavaPath().toAbsolutePath().toString());
        javaPath.setDisable(true);
        HBox.setHgrow(javaPath, Priority.ALWAYS);

        var detect = new Button("Detect Java");
        detect.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        detect.setOnAction(e -> new JavaVersionDialog(stage, () -> {
            javaPath.setText(SettingsService.get().getJavaPath().toAbsolutePath().toString());
        }).show());

        var javaRow = new HBox(8, javaPath, detect);

        var save = new Button("SAVE SETTINGS");
        save.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        save.setMaxWidth(Double.MAX_VALUE);

        final boolean[] saved = { false };

        save.setOnAction(actionEvent -> {
            try {
                SettingsService.get().setPort(Integer.parseInt(port.getText().trim()));
            } catch (NumberFormatException ignored) { }
            SettingsService.get().setJavaPath(Path.of(javaPath.getText()));
            SettingsService.save();

            ServerMasterApp.instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
            saved[0] = true;

            stage.close();
            if (onSaved != null) onSaved.run();
        });

        var content = new VBox(10, title, rowLabel("Server Folder"), folderRow, ramLabel, ram,
                portRow, rowLabel("Java Executable Path"), javaRow, save);
        content.setPadding(new Insets(10, 16, 16, 16));

        var windowButtons = new WindowButtons(stage, false);
        windowButtons.setStyle("-fx-background-color: transparent;");

        var surface = new WindowSurface();
        surface.getStyleClass().add("dialog");
        surface.setTop(windowButtons);
        surface.setCenter(content);
        BorderPane.setMargin(windowButtons, new Insets(6, 6, 0, 6));

        var scene = new Scene(surface, 600, 420);
        scene.setFill(Color.TRANSPARENT);
        FX.addStyleSheet(scene);
        stage.setScene(scene);

        if (required) {
            stage.setOnHidden(e -> {
                if (!saved[0]) Platform.exit();
            });
        }
    }

    private static Region spacer() {
        var r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static Label rowLabel(String text) {
        var l = new Label(text);
        l.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        return l;
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }
}
