package org.bsdevelopment.serverapp;

import atlantafx.base.theme.Dracula;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * The {@code ServerMasterApplication} class is the entry point for the ServerMaster application.
 * It initializes the JavaFX application, loads the user interface, and sets up the application window.
 */
public class ServerMasterApplication extends Application {

    /**
     * Starts the ServerMaster application by initializing the JavaFX stage and setting up the user interface.
     *
     * @param stage The primary stage for the application.
     * @throws IOException If there is an error loading the FXML file.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Set the application's user agent stylesheet to the Dracula theme
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());

        // Load the FXML file for the user interface
        FXMLLoader fxmlLoader = new FXMLLoader(ServerMasterApplication.class.getResource("hello-view.fxml"));

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
}
