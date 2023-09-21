package org.bsdevelopment.serverapp;

import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * The {@code ServerMasterApplication} class is the entry point for the ServerMaster application.
 * It initializes the JavaFX application, loads the user interface, and sets up the application window.
 */
public class ServerMasterApplication extends Application {
    private static Stage appStage;
    private static File propertiesFile;
    private static Properties properties;

    /**
     * Starts the ServerMaster application by initializing the JavaFX stage and setting up the user interface.
     *
     * @param stage The primary stage for the application.
     * @throws IOException If there is an error loading the FXML file.
     */
    @Override
    public void start(Stage stage) throws IOException {
        appStage = stage;

        propertiesFile = new File(new File("."), "server-master.properties");
        properties = new Properties();

        if (!propertiesFile.createNewFile()) {
            try {
                properties.load(new FileInputStream(propertiesFile));
            } catch (IOException ignored) {}
        }else{
            properties.setProperty("server-port", "25565");
            properties.setProperty("app-theme", "Dracula");
            properties.setProperty("eula", "false");

            properties.store(new FileOutputStream(propertiesFile), null);
        }

        // Load the FXML file for the user interface
        FXMLLoader fxmlLoader = new FXMLLoader(ServerMasterApplication.class.getResource("app-view.fxml"));

        // Create a new scene with the loaded FXML content
        Scene scene = new Scene(fxmlLoader.load(), 1048, 722);

        // Set the title of the application window
        stage.setTitle("Server Master");

        // Set the scene for the primary stage and maximize the window
        stage.setScene(scene);
        stage.setMaximized(true);

        // Add an application icon to the window
        stage.getIcons().add(new Image(Objects.requireNonNull(ServerMasterApplication.class.getResourceAsStream("icon.png"))));

        // Show the application window
        stage.show();

        updateTheme();
    }

    /**
     * The main method that launches the ServerMaster application.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        // Launch the JavaFX application
        launch();
    }

    public static void updateTheme () {
        String rawTheme = properties.getProperty("app-theme");
        updateTheme(rawTheme);
    }

    public static void updateTheme (String rawTheme) {
        Platform.runLater(() -> {
            if ("No Theme".equalsIgnoreCase(rawTheme)) {
                Application.setUserAgentStylesheet(null);
                return;
            }

            try {
                Class<?> clazz = Class.forName("atlantafx.base.theme." + rawTheme);
                Theme theme = (Theme) clazz.newInstance();

                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }catch (Exception ignored) {}
        });
    }

    public static Stage getAppStage() {
        return appStage;
    }

    public static File getPropertiesFile() {
        return propertiesFile;
    }

    public static Properties getProperties() {
        return properties;
    }
}
