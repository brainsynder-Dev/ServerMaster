package org.bsdevelopment.servermaster.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.components.ServerSelection;
import org.bsdevelopment.servermaster.components.ServerSelectionPane;
import org.bsdevelopment.servermaster.components.TopBar;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.instance.server.thread.ServerOutputListener;
import org.bsdevelopment.servermaster.ui.dialog.ServerInstallerDialog;
import org.bsdevelopment.servermaster.ui.dialog.SettingsDialog;
import org.bsdevelopment.servermaster.ui.window.WindowSurface;
import org.bsdevelopment.servermaster.utils.FX;

import java.io.IOException;
import java.util.List;

public final class MainWindow {
    private static final double SIDEBAR_WIDTH = 280;
    private final Stage stage;
    private final LogViewer console;
    private final BooleanProperty serverRunning = new SimpleBooleanProperty(false);
    private ServerSelectionPane serverSelection;
    private Button stopButton;
    private Button restartButton;
    private Button forceStopButton;
    private volatile boolean restartPending;
    private int historyIndex = -1;
    private String historyDraft = "";

    public MainWindow(ServerSelection selection) {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("ServerMaster");

        serverRunning.set(ServerMasterApp.serverWrapper().isServerRunning());

        console = new LogViewer();
        LogViewer.registerActive(console);

        var content = new BorderPane();
        content.setPadding(Insets.EMPTY);
        content.setMinHeight(0);

        try {
            content.setLeft(buildSidebar(selection));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        content.setCenter(buildConsolePane());

        var topBar = new TopBar(stage, SIDEBAR_WIDTH, () -> console.appendSystemMessage("Closing the application in 5 seconds..."));

        var surface = new WindowSurface();
        surface.setTop(topBar);
        surface.setCenter(content);

        var scene = new Scene(surface, 1280, 720);
        scene.setFill(Color.TRANSPARENT);
        FX.addStyleSheet(scene);
        stage.setScene(scene);
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                serverRunning.set(ServerMasterApp.serverWrapper().isServerRunning());
            }
        });
    }

    private Region buildSidebar(ServerSelection selection) throws IOException {
        var header = new Label("ServerMaster");
        header.getStyleClass().addAll(Styles.TITLE_3);

        ServerOutputListener outputListener = (server, stream, line) -> Platform.runLater(() -> console.appendLine(line));
        serverSelection = new ServerSelectionPane(selection, serverRunning, outputListener,
                () -> {
                    console.clearConsole();
                    console.appendSystemMessage("Starting server...");
                }
        );

        var locked = ServerMasterApp.applicationLockedProperty();
        var installer = new Button("Server Installer");
        installer.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        installer.setMaxWidth(Double.MAX_VALUE);
        installer.disableProperty().bind(serverRunning.or(locked));
        installer.setOnAction(e -> {
            try {
                new ServerInstallerDialog(stage).show();
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });

        var settings = new Button("Settings");
        settings.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        settings.setMaxWidth(Double.MAX_VALUE);
        settings.disableProperty().bind(serverRunning.or(locked));
        settings.setOnAction(e -> new SettingsDialog(stage).show());

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        var box = new VBox(10, header, serverSelection, new Separator(), spacer, installer, settings);
        box.setPadding(new Insets(14));
        box.setPrefWidth(280);
        box.setAlignment(Pos.TOP_CENTER);
        box.getStyleClass().addAll(Styles.BG_SUBTLE);
        box.setMaxHeight(Double.MAX_VALUE);

        return box;
    }

    private Region buildConsolePane() {
        var topButtons = new HBox(10);
        topButtons.setAlignment(Pos.TOP_RIGHT);

        stopButton = new Button("STOP");
        stopButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);

        restartButton = new Button("RESTART");
        restartButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);

        forceStopButton = new Button("FORCE STOP");
        forceStopButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);

        stopButton.disableProperty().bind(serverRunning.not());
        restartButton.disableProperty().bind(serverRunning.not());
        forceStopButton.disableProperty().bind(serverRunning.not());

        stopButton.setOnAction(e -> stopServer());
        forceStopButton.setOnAction(e -> forceStopServer());
        restartButton.setOnAction(e -> restartServer());

        topButtons.getChildren().addAll(stopButton, restartButton, forceStopButton);

        var consoleBox = new VBox(10, topButtons, console);
        consoleBox.setPadding(new Insets(14));
        consoleBox.setMinHeight(0);
        VBox.setVgrow(console, Priority.ALWAYS);

        var commandField = new TextField();
        commandField.setPromptText("Type your command...");
        HBox.setHgrow(commandField, Priority.ALWAYS);

        var send = new Button("Send");
        send.getStyleClass().addAll(Styles.BUTTON_OUTLINED);

        Runnable sendCommand = () -> {
            String cmd = commandField.getText();
            if (cmd == null) return;

            cmd = cmd.trim();
            if (cmd.isBlank()) return;

            if (!ServerMasterApp.serverWrapper().isServerRunning()) {
                console.appendStyledLine("Unable to send command when a server is not running.", "log-fatal");
                return;
            }

            console.appendSystemMessage("Executing command: " + cmd);

            rememberCommand(cmd);

            historyIndex = -1;
            historyDraft = "";

            commandField.clear();
            ServerHandlerAPI.sendServerCommand(cmd);
        };

        commandField.setOnAction(e -> sendCommand.run());
        send.setOnAction(e -> sendCommand.run());

        commandField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.UP) {
                if (navigateHistory(commandField, -1)) e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                if (navigateHistory(commandField, +1)) e.consume();
            }
        });

        var bottomBar = new HBox(10, FX.inputGroup(commandField, send));
        bottomBar.setPadding(new Insets(0, 14, 14, 14));

        var wrapper = new VBox(0, consoleBox, bottomBar);
        wrapper.setMinHeight(0);
        VBox.setVgrow(consoleBox, Priority.ALWAYS);

        return wrapper;
    }

    private boolean navigateHistory(TextField field, int direction) {
        List<String> history = SettingsService.get().getRecentCommands();
        if (history == null || history.isEmpty()) return false;

        if (historyIndex == -1) {
            historyDraft = field.getText() == null ? "" : field.getText();
        }

        int size = history.size();

        if (direction < 0) {
            if (historyIndex == -1) historyIndex = size - 1;
            else if (historyIndex > 0) historyIndex--;
        } else {
            if (historyIndex == -1) return false;
            else if (historyIndex < size - 1) historyIndex++;
            else historyIndex = -1;
        }

        if (historyIndex == -1) {
            field.setText(historyDraft);
            field.positionCaret(field.getText().length());
        } else {
            String cmd = history.get(historyIndex);
            field.setText(cmd);
            field.positionCaret(cmd.length());
        }

        return true;
    }

    private void rememberCommand(String cmd) {
        var settings = SettingsService.get();

        List<String> history = settings.getRecentCommands();
        if (history == null) {
            history = new java.util.ArrayList<>();
            settings.setRecentCommands(history);
        }

        history.removeIf(s -> s != null && s.equalsIgnoreCase(cmd));
        history.add(cmd);

        while (history.size() > 10) {
            history.remove(0);
        }

        SettingsService.save();
    }

    private void stopServer() {
        if (!serverRunning.get()) return;
        console.appendSystemMessage("Sending /stop ...");
        ServerHandlerAPI.stopServer();
    }

    private void forceStopServer() {
        if (!serverRunning.get()) return;
        console.appendSystemMessage("Force stopping server ...");
        restartPending = false;
        ServerHandlerAPI.killServer();
    }

    private void restartServer() {
        if (!serverRunning.get()) return;
        console.appendSystemMessage("Restart requested ...");

        restartPending = true;
        attachRestartCallback();
        ServerHandlerAPI.stopServer();
    }

    private void attachRestartCallback() {
        var wrapper = ServerMasterApp.serverWrapper();
        var server = wrapper != null ? wrapper.getServer() : null;
        if (server == null || server.getThread() == null) return;

        server.getThread().setServerStopCallback((s, code) -> {
            if (!restartPending) return;

            restartPending = false;
            Platform.runLater(() -> {
                console.appendSystemMessage("Starting server again ...");
                serverSelection.startSelectedServer();
            });
        });
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }
}
