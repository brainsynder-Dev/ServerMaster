package org.bsdevelopment.servermaster;

import atlantafx.base.theme.NordDark;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.application.Platform;
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
import java.net.URI;

public class ServerMasterApp extends Application {
    private static final URI UPDATE_MANIFEST_URL = URI.create("https://YOUR_PUBLIC_BASE/servermaster/update.json");
    public static InstanceCatalog instanceCatalog;
    private static ServerWrapper serverWrapper;

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            Window.getWindows().addListener((ListChangeListener<? super Window>) change -> {
                while (change.next()) {
                    for (var w : change.getAddedSubList()) {
                        if (w instanceof Stage s)
                            s.getIcons().add(new Image(ServerMasterApp.class.getResourceAsStream("/images/servermaster.png")));
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

    private void initiateStartup() throws IOException {
        instanceCatalog = new InstanceCatalog(SettingsService.get().getServerPath());
        serverWrapper = new ServerWrapper(SettingsService.get().getServerPath());

        var selection = new ServerSelection("", "", "");
        new MainWindow(selection).show();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ServerHandlerAPI.killServer();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }));
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
