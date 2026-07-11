package org.bsdevelopment.servermaster.ui;

import atlantafx.base.theme.Styles;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.components.ConfigEditorPane;
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
import java.util.ArrayList;
import java.util.List;

public final class MainWindow {
    private final Stage stage;
    private final LogViewer console;
    private final BooleanProperty serverRunning = new SimpleBooleanProperty(false);

    private final ServerSelectionPane serverSelection;
    private SplitMenuButton stopButton;
    private SplitMenuButton restartButton;
    private final Region dashboardView;
    private final Region consoleView;
    private final ConfigEditorPane configView;
    private ToggleButton consoleTab;
    private Label metaLabel;
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

        ServerOutputListener outputListener = (server, stream, line) -> Platform.runLater(() -> console.appendLine(line));
        try {
            serverSelection = new ServerSelectionPane(selection, serverRunning, outputListener, () -> {
                console.clearConsole();
                console.appendSystemMessage("Starting server...");
                if (consoleTab != null) consoleTab.setSelected(true);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dashboardView = buildDashboard();
        consoleView = buildConsole();
        configView = new ConfigEditorPane();

        var contentStack = new StackPane(dashboardView, consoleView, configView);
        contentStack.setMinHeight(0);

        var topBar = new TopBar(stage, buildTabs(), () -> console.appendSystemMessage("Closing the application in 5 seconds..."));
        ServerMasterApp.setConsoleFocusHandler(() -> {
            if (consoleTab != null) consoleTab.setSelected(true);
        });

        var surface = new WindowSurface();
        surface.setTop(topBar);
        surface.setCenter(contentStack);

        var scene = new Scene(surface, 1280, 820);
        scene.setFill(Color.TRANSPARENT);
        FX.addStyleSheet(scene);
        stage.setScene(scene);
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                serverRunning.set(ServerMasterApp.serverWrapper().isServerRunning());
            }
        });
    }

    private Region buildTabs() {
        var group = new ToggleGroup();

        var dashboardTab = new ToggleButton("Dashboard");
        dashboardTab.getStyleClass().addAll(Styles.LEFT_PILL, "nav-tab");
        dashboardTab.setToggleGroup(group);
        dashboardTab.setUserData(dashboardView);

        consoleTab = new ToggleButton("Console");
        consoleTab.getStyleClass().addAll(Styles.CENTER_PILL, "nav-tab");
        consoleTab.setToggleGroup(group);
        consoleTab.setUserData(consoleView);

        var configTab = new ToggleButton("Config");
        configTab.getStyleClass().addAll(Styles.RIGHT_PILL, "nav-tab");
        configTab.setToggleGroup(group);
        configTab.setUserData(configView);

        group.selectedToggleProperty().addListener((obs, previous, selected) -> {
            if (selected == null) {
                previous.setSelected(true);
                return;
            }
            showView((Region) selected.getUserData());
        });

        if (serverRunning.get()) consoleTab.setSelected(true);
        else dashboardTab.setSelected(true);

        var tabs = new HBox(dashboardTab, consoleTab, configTab);
        tabs.setAlignment(Pos.CENTER_LEFT);
        return tabs;
    }

    private void showView(Region target) {
        if (target == dashboardView) refreshDashboardMeta();
        if (target == configView) configView.refresh();

        for (Region view : List.of(dashboardView, consoleView, configView)) {
            boolean active = view == target;
            view.setVisible(active);
            view.setManaged(active);
        }
    }

    private Region buildDashboard() {
        var statusPill = new Label();
        statusPill.getStyleClass().add("status-pill");
        statusPill.setMaxWidth(Region.USE_PREF_SIZE);
        updateStatusPill(statusPill, serverRunning.get());
        serverRunning.addListener((obs, was, running) -> updateStatusPill(statusPill, running));

        var summary = new Label();
        summary.getStyleClass().add("card-summary");
        summary.textProperty().bind(serverSelection.summaryProperty());

        metaLabel = new Label();
        metaLabel.getStyleClass().add("card-muted");
        refreshDashboardMeta();

        var card = new VBox(14, statusPill, summary, metaLabel, new Separator(), serverSelection);
        card.getStyleClass().add("status-card");
        card.setMaxWidth(480);
        card.setFillWidth(true);

        var locked = ServerMasterApp.applicationLockedProperty();

        var installer = new Button("Server Installer");
        installer.getStyleClass().add(Styles.BUTTON_OUTLINED);
        installer.setMaxWidth(Double.MAX_VALUE);
        installer.disableProperty().bind(serverRunning.or(locked));
        installer.setOnAction(e -> {
            try {
                new ServerInstallerDialog(stage).show();
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        HBox.setHgrow(installer, Priority.ALWAYS);

        var settings = new Button("Settings");
        settings.getStyleClass().add(Styles.BUTTON_OUTLINED);
        settings.setMaxWidth(Double.MAX_VALUE);
        settings.disableProperty().bind(serverRunning.or(locked));
        settings.setOnAction(e -> new SettingsDialog(stage).show());
        HBox.setHgrow(settings, Priority.ALWAYS);

        var actions = new HBox(12, installer, settings);
        actions.setMaxWidth(480);
        actions.setAlignment(Pos.CENTER);

        var column = new VBox(16, card, actions);
        column.setAlignment(Pos.CENTER);
        column.setMaxWidth(480);

        var wrapper = new StackPane(column);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(28));
        return wrapper;
    }

    private void updateStatusPill(Label pill, boolean running) {
        pill.getStyleClass().removeAll("running", "stopped");
        pill.setText(running ? "●  RUNNING" : "●  STOPPED");
        pill.getStyleClass().add(running ? "running" : "stopped");
    }

    private void refreshDashboardMeta() {
        if (metaLabel == null) return;
        var settings = SettingsService.get();
        metaLabel.setText(settings.getMemory() + " GB RAM     ·     port " + settings.getPort());
    }

    private Region buildConsole() {
        var topButtons = new HBox(10);
        topButtons.setAlignment(Pos.CENTER_LEFT);

        var autoScrollToggle = new ToggleButton("Auto-Scroll");
        autoScrollToggle.setSelected(true);
        autoScrollToggle.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        autoScrollToggle.selectedProperty().addListener((obs, old, enabled) -> console.setAutoScroll(enabled));

        var buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, Priority.ALWAYS);

        stopButton = new SplitMenuButton();
        stopButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        stopButton.disableProperty().bind(serverRunning.not());

        restartButton = new SplitMenuButton();
        restartButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        restartButton.disableProperty().bind(serverRunning.not());

        configureStopButton();
        configureRestartButton();

        topButtons.getChildren().addAll(autoScrollToggle, buttonSpacer, restartButton, stopButton);

        var installLabel = new Label("Installing…");
        installLabel.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        var installBar = new ProgressBar();
        installBar.setMaxWidth(Double.MAX_VALUE);
        installBar.progressProperty().bind(ServerMasterApp.installProgressProperty());
        HBox.setHgrow(installBar, Priority.ALWAYS);

        var installRow = new HBox(10, installLabel, installBar);
        installRow.setAlignment(Pos.CENTER_LEFT);
        installRow.visibleProperty().bind(ServerMasterApp.installActiveProperty());
        installRow.managedProperty().bind(installRow.visibleProperty());

        var consoleBox = new VBox(10, topButtons, installRow, console);
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
            rememberCommand(cmd);

            if (cmd.equalsIgnoreCase("cls") || cmd.equalsIgnoreCase("clear")) {
                console.clearConsole();
                commandField.clear();
                return;
            }

            if (!ServerMasterApp.serverWrapper().isServerRunning()) {
                console.appendStyledLine("Unable to send command when a server is not running.", "log-fatal");
                return;
            }

            console.appendSystemMessage("Executing command: " + cmd);

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
            history = new ArrayList<>();
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
        attachRestartCallback(0);
        ServerHandlerAPI.stopServer();
    }

    private void quickRestartServer() {
        if (!serverRunning.get()) return;
        console.appendSystemMessage("Quick restart requested, force stopping server ...");

        restartPending = true;
        attachRestartCallback(3);
        ServerHandlerAPI.killServer();
    }

    private void attachRestartCallback(int delaySeconds) {
        var wrapper = ServerMasterApp.serverWrapper();
        var server = wrapper != null ? wrapper.getServer() : null;
        if (server == null || server.getThread() == null) return;

        server.getThread().setServerStopCallback((s, code) -> {
            if (!restartPending) return;

            restartPending = false;
            Platform.runLater(() -> {
                if (delaySeconds <= 0) {
                    console.appendSystemMessage("Starting server again ...");
                    serverSelection.startSelectedServer();
                } else {
                    console.appendSystemMessage("Starting server in " + delaySeconds + " seconds ...");
                    var pause = new PauseTransition(Duration.seconds(delaySeconds));
                    pause.setOnFinished(e -> serverSelection.startSelectedServer());
                    pause.play();
                }
            });
        });
    }

    private void configureStopButton() {
        var settings = SettingsService.get();
        stopButton.getItems().clear();
        stopButton.getStyleClass().removeAll(Styles.DANGER, Styles.WARNING, Styles.ACCENT);

        if (settings.isForceStopDefault()) {
            stopButton.setText("FORCE STOP");
            stopButton.getStyleClass().add(Styles.DANGER);
            stopButton.setOnAction(e -> forceStopServer());

            var alternate = new MenuItem("Stop");
            alternate.setOnAction(e -> {
                SettingsService.get().setForceStopDefault(false);
                SettingsService.save();
                configureStopButton();
            });
            stopButton.getItems().add(alternate);
        } else {
            stopButton.setText("STOP");
            stopButton.setOnAction(e -> stopServer());

            var alternate = new MenuItem("Force Stop");
            alternate.setOnAction(e -> {
                SettingsService.get().setForceStopDefault(true);
                SettingsService.save();
                configureStopButton();
            });
            stopButton.getItems().add(alternate);
        }
    }

    private void configureRestartButton() {
        var settings = SettingsService.get();
        restartButton.getItems().clear();
        restartButton.getStyleClass().removeAll(Styles.DANGER, Styles.WARNING, Styles.ACCENT);

        if (settings.isQuickRestartDefault()) {
            restartButton.setText("QUICK RESTART");
            restartButton.getStyleClass().add(Styles.WARNING);
            restartButton.setOnAction(e -> quickRestartServer());

            var alternate = new MenuItem("Restart");
            alternate.setOnAction(e -> {
                SettingsService.get().setQuickRestartDefault(false);
                SettingsService.save();
                configureRestartButton();
            });
            restartButton.getItems().add(alternate);
        } else {
            restartButton.setText("RESTART");
            restartButton.getStyleClass().add(Styles.ACCENT);
            restartButton.setOnAction(e -> restartServer());

            var alternate = new MenuItem("Quick Restart");
            alternate.setOnAction(e -> {
                SettingsService.get().setQuickRestartDefault(true);
                SettingsService.save();
                configureRestartButton();
            });
            restartButton.getItems().add(alternate);
        }
    }

    public void show() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        double width = Math.min(stage.getScene().getWidth(), bounds.getWidth());
        double height = Math.min(stage.getScene().getHeight(), bounds.getHeight());

        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);

        stage.show();
    }
}
