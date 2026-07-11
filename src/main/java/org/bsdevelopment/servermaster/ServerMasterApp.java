package org.bsdevelopment.servermaster;

import atlantafx.base.theme.NordDark;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.bsdevelopment.servermaster.backend.AppUpdater;
import org.bsdevelopment.servermaster.backend.GitHubUpdateChecker;
import org.bsdevelopment.servermaster.backend.ViaJenkinsPluginUpdater;
import org.bsdevelopment.servermaster.components.ServerSelection;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.InstanceCatalog;
import org.bsdevelopment.servermaster.instance.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.instance.server.ServerWrapper;
import org.bsdevelopment.servermaster.ui.MainWindow;
import org.bsdevelopment.servermaster.ui.UpdateFoundWindow;
import org.bsdevelopment.servermaster.ui.dialog.SettingsDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerMasterApp extends Application {
    public static InstanceCatalog instanceCatalog;
    private static ServerWrapper serverWrapper;

    private static final BooleanProperty APPLICATION_LOCKED = new SimpleBooleanProperty(false);
    private static final BooleanProperty INSTALL_ACTIVE = new SimpleBooleanProperty(false);
    private static final BooleanProperty INSTALL_STATUS_VISIBLE = new SimpleBooleanProperty(false);
    private static final DoubleProperty INSTALL_PROGRESS = new SimpleDoubleProperty(0);
    private static final StringProperty INSTALL_STATUS_MESSAGE = new SimpleStringProperty("");
    private static final StringProperty INSTALL_STATE = new SimpleStringProperty("running");
    private static Runnable consoleFocusHandler = () -> {};
    private static PauseTransition installStatusHide;

    private static final Object BUILDTOOLS_LOCK = new Object();
    private static volatile Process buildToolsProcess;

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            Window.getWindows().addListener((ListChangeListener<? super Window>) change -> {
                while (change.next()) {
                    for (var w : change.getAddedSubList()) {
                        if (w instanceof Stage s) {
                            s.getIcons().add(new Image(ServerMasterApp.class.getResourceAsStream("/images/servermaster.png")));
                        }
                    }
                }
            });

            CSSFX.start();
            Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

            if (SettingsService.get().getAppConfigVersion() == -1) {
                new SettingsDialog(primaryStage, () -> {
                    SettingsService.get().setAppConfigVersion(1);
                    SettingsService.save();

                    try {
                        initiateStartup();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, true).show();
            } else {
                initiateStartup();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void stop() {
        stopBuildToolsIfRunning();
        ServerHandlerAPI.killServer();
    }

    private void initiateStartup() throws IOException {
        instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
        serverWrapper = new ServerWrapper(SettingsService.get().getServerPath());

        var selection = new ServerSelection("", "", "");
        new MainWindow(selection).show();

        if (!Constants.DEV_MODE) new Thread(() -> new ViaJenkinsPluginUpdater().runOnStartup(), "servermaster-via-updater").start();

        new Thread(() -> {
            String currentVersion = ServerMasterApp.class.getPackage().getImplementationVersion();
            new GitHubUpdateChecker().checkForUpdate(currentVersion).ifPresent(release ->
                Platform.runLater(() -> {
                    AppUpdater updater = new AppUpdater();
                    new UpdateFoundWindow(
                            release.version(),
                            () -> new Thread(() -> updater.downloadAndRestart(release.jarDownloadUrl()), "servermaster-updater").start(),
                            () -> {}
                    ).show();
                })
            );
        }, "servermaster-update-check").start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopBuildToolsIfRunning();
            ServerHandlerAPI.killServer();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }, "servermaster-shutdown"));
    }

    public static ReadOnlyBooleanProperty applicationLockedProperty() {
        return APPLICATION_LOCKED;
    }

    public static void lockApplication() {
        APPLICATION_LOCKED.set(true);
    }

    public static void unlockApplication() {
        APPLICATION_LOCKED.set(false);
    }

    public static ReadOnlyBooleanProperty installActiveProperty() {
        return INSTALL_ACTIVE;
    }

    public static DoubleProperty installProgressProperty() {
        return INSTALL_PROGRESS;
    }

    public static ReadOnlyBooleanProperty installStatusVisibleProperty() {
        return INSTALL_STATUS_VISIBLE;
    }

    public static ReadOnlyStringProperty installStatusMessageProperty() {
        return INSTALL_STATUS_MESSAGE;
    }

    public static ReadOnlyStringProperty installStateProperty() {
        return INSTALL_STATE;
    }

    public static void beginInstall(boolean indeterminate) {
        Platform.runLater(() -> {
            if (installStatusHide != null) installStatusHide.stop();
            INSTALL_PROGRESS.set(indeterminate ? -1 : 0);
            INSTALL_STATE.set("running");
            INSTALL_STATUS_MESSAGE.set("Installing…");
            INSTALL_ACTIVE.set(true);
            INSTALL_STATUS_VISIBLE.set(true);
            APPLICATION_LOCKED.set(true);
        });
    }

    public static void reportInstallProgress(double value) {
        if (Platform.isFxApplicationThread()) INSTALL_PROGRESS.set(value);
        else Platform.runLater(() -> INSTALL_PROGRESS.set(value));
    }

    public static void endInstall(boolean success, String message) {
        Platform.runLater(() -> {
            if (installStatusHide != null) installStatusHide.stop();
            INSTALL_ACTIVE.set(false);
            INSTALL_PROGRESS.set(success ? 1 : 0);
            APPLICATION_LOCKED.set(false);
            INSTALL_STATE.set(success ? "success" : "failed");
            INSTALL_STATUS_MESSAGE.set(message != null ? message
                    : (success ? "Installation complete" : "Installation failed"));
            INSTALL_STATUS_VISIBLE.set(true);

            installStatusHide = new PauseTransition(Duration.seconds(8));
            installStatusHide.setOnFinished(e -> INSTALL_STATUS_VISIBLE.set(false));
            installStatusHide.playFromStart();
        });
    }

    public static void cancelInstall() {
        Platform.runLater(() -> {
            if (installStatusHide != null) installStatusHide.stop();
            INSTALL_ACTIVE.set(false);
            INSTALL_PROGRESS.set(0);
            APPLICATION_LOCKED.set(false);
            INSTALL_STATE.set("running");
            INSTALL_STATUS_VISIBLE.set(false);
        });
    }

    public static void setConsoleFocusHandler(Runnable handler) {
        consoleFocusHandler = handler != null ? handler : () -> {};
    }

    public static void focusConsole() {
        Platform.runLater(consoleFocusHandler);
    }

    public static void registerBuildToolsProcess(Process process) {
        synchronized (BUILDTOOLS_LOCK) {
            buildToolsProcess = process;
        }
    }

    public static void clearBuildToolsProcess(Process process) {
        synchronized (BUILDTOOLS_LOCK) {
            if (buildToolsProcess == process) buildToolsProcess = null;
        }
    }

    public static void stopBuildToolsIfRunning() {
        Process process = buildToolsProcess;
        if (process == null) return;
        process.destroyForcibly();
    }

    private static File resolveConfigFile() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String home = System.getProperty("user.home");
        Path dir;
        if (windows) {
            String appData = System.getenv("APPDATA");
            dir = Path.of(appData != null ? appData : home).resolve("ServerMaster");
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            dir = Path.of(xdgData != null ? xdgData : home + "/.local/share").resolve("servermaster");
        }

        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
            return Constants.WORKING_PATH.resolve("configuration.json").toFile();
        }
        return dir.resolve("configuration.json").toFile();
    }

    public static void main(String[] args) {
        SettingsService.load(resolveConfigFile());

        try {
            if (SettingsService.get().isInitialized()) instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
            launch(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Platform.runLater(throwable::printStackTrace);
        });
    }

    public static ServerWrapper serverWrapper() {
        if (serverWrapper == null) throw new IllegalStateException("ServerWrapper has not been initialized yet.");
        return serverWrapper;
    }
}
