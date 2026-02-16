package org.bsdevelopment.servermaster;

import atlantafx.base.theme.NordDark;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.bsdevelopment.servermaster.components.ServerSelection;
import org.bsdevelopment.servermaster.config.SettingsService;
import org.bsdevelopment.servermaster.instance.InstanceCatalog;
import org.bsdevelopment.servermaster.instance.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.instance.server.ServerWrapper;
import org.bsdevelopment.servermaster.ui.MainWindow;
import org.bsdevelopment.servermaster.ui.dialog.SettingsDialog;

import java.io.IOException;

public class ServerMasterApp extends Application {
    public static InstanceCatalog instanceCatalog;
    private static ServerWrapper serverWrapper;

    private static final BooleanProperty APPLICATION_LOCKED = new SimpleBooleanProperty(false);

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

    public static void main(String[] args) {
        SettingsService.load(Constants.WORKING_PATH.resolve("configuration.json").toFile());

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
