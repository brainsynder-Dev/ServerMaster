package org.bsdevelopment.serverapp;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.bsdevelopment.serverapp.server.API;
import org.bsdevelopment.serverapp.server.ServerJarManager;
import org.bsdevelopment.serverapp.server.ServerWrapper;
import org.bsdevelopment.serverapp.utils.WordUtils;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * The {@code ServerController} class controls the JavaFX user interface for managing game servers
 * using the ServerMaster application. It handles user interactions, server initialization, and console logging.
 */
public class ServerController implements Initializable {

    // Logger for logging events and errors
    private final Logger log = Logger.getLogger("");

    // JavaFX UI components
    @FXML private TextArea consoleLog;
    @FXML private TextField consoleCommand;
    @FXML private ContextMenu context;

    // ServerJarManager for managing server JAR files
    private ServerJarManager jarManager;

    /**
     * Initializes the ServerMaster application UI and sets up event handlers.
     *
     * @param url            The URL of the FXML file.
     * @param resourceBundle The ResourceBundle used for localization.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up an event handler for handling Enter key press in the command input field
        consoleCommand.onKeyPressedProperty().set(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) sendCommand();
        });

        // Request focus on the command input field when the application starts
        consoleCommand.requestFocus();

        // Initialize the logger for redirecting console output to the UI
        initLogger();

        // Initialize the ServerJarManager to manage server JAR files
        File file = new File(".");
        jarManager = new ServerJarManager(file);
        new ServerWrapper(jarManager);

        // Populate the context menu with server type and version options
        jarManager.getSupportedJars().forEach((s, serverJars) -> {
            String serverType = WordUtils.capitalizeFully(s.toLowerCase());
            Menu menu = new Menu(serverType);
            context.getItems().add(menu);

            ObservableList<MenuItem> items = menu.getItems();

            List<String> versions = new ArrayList<>();
            serverJars.forEach(serverJar -> {
                String version = serverJar.getVersion().replace(".jar", "");
                if (version.split("\\.").length != 3) version += ".0";
                versions.add(version);
            });

            versions.stream()
                    .map(ModuleDescriptor.Version::parse)
                    .sorted()
                    .forEach(version -> {
                        String string = version.toString();
                        if (string.endsWith(".0")) string = string.replace(".0", "");

                        MenuItem menuItem = new MenuItem(string);
                        String finalString = string;
                        menuItem.onActionProperty().set(actionEvent -> {
                            if (ServerWrapper.getInstance().getServer() != null) return;

                            consoleLog.setText("");

                            int port = API.startServer(serverType.toLowerCase(), s, finalString, (server, statusCode) -> {
                                if (statusCode == 0) System.out.println("[ServerMaster] Stopped the server");
                                if (statusCode == 1) System.out.println("[ServerMaster] The server process was forced to stop");
                            });

                            System.out.println("[ServerMaster] Starting server on localhost:" + port);
                        });

                        items.add(menuItem);
                    });
        });

        // Add a separator to the context menu if there are supported JARs
        if (!jarManager.getSupportedJars().isEmpty()) context.getItems().add(new SeparatorMenuItem());

        // Add menu items for stopping and forcefully killing the server
        MenuItem stopServer = new MenuItem("Stop Server");
        stopServer.onActionProperty().set(actionEvent -> {
            stopServer();
        });
        context.getItems().add(stopServer);

        MenuItem killServer = new MenuItem("Force Stop Server (Emergencies ONLY)");
        killServer.onActionProperty().set(actionEvent -> {
            forceStopServer();
        });
        context.getItems().add(killServer);
    }

    /**
     * Handles sending a command from the input field to the server.
     */
    @FXML
    public void sendCommand() {
        String text = consoleCommand.getText();
        if (text == null || text.trim().isBlank()) return;
        consoleCommand.setText("");

        text = text.trim();

        // Display help information
        if ("??".equalsIgnoreCase(text)) {
            System.out.println("Master Commands:");
            System.out.println("?? welcome");
            System.out.println("   Will explain how to use the application in simple steps");
            System.out.println("?? jar");
            System.out.println("   Will explain how to name the server jar files");
            System.out.println("?? clear");
            System.out.println("   Will clear the console window of all text");
            return;
        }

        if ("?? welcome".equalsIgnoreCase(text)) {
            System.out.println();
            System.out.println("How to use the Server Master");
            System.out.println("- Make sure you have server jar files in the folder");
            System.out.println("  Also make sure you have all the server files in the same folder");
            System.out.println("  Like the plugins/world folder as well as all the configuration files");
            System.out.println("- Right Click in the center");
            System.out.println("- Hover over what type of server you want to run");
            System.out.println("- Click the version you want to run for that server type");
            System.out.println(" The Application will automatically change what world is used");
            System.out.println(" based on what version of server you are running, everything else");
            System.out.println(" is shared between all the versions (plugins & configs)");
            System.out.println();
            return;
        }

        if ("?? jar".equalsIgnoreCase(text)) {
            System.out.println("How to add server jar files:");
            System.out.println("- Simply drag and drop the selected server's jar file");
            System.out.println("- Rename the jar file into this format: ServerType-MCVersion.jar");
            System.out.println("  Example: spigot-1.18.2.jar");
            System.out.println("- Once you add a new jar file you will need to restart the application");
            return;
        }

        if ("?? clear".equalsIgnoreCase(text) || "cls".equalsIgnoreCase(text)) {
            // Clear the console log
            consoleLog.setText("");
            return;
        }

        // Send the command to the server
        API.sendServerCommand(text);
    }

    /**
     * Forcibly stops the currently running game server.
     */
    @FXML
    public void forceStopServer() {
        API.killServer();
    }

    /**
     * Stops the currently running game server gracefully.
     */
    @FXML
    public void stopServer() {
        API.stopServer();
    }

    /**
     * Initializes the logger to redirect console output to the UI.
     */
    private void initLogger() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Redirect System.out and System.err to the consoleLog TextArea
                Console console = new Console(consoleLog);
                PrintStream ps = new PrintStream(console, true);
                System.setOut(ps);
                System.setErr(ps);
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                System.out.println("[ServerMaster] Finished loading all Server Jars...");
                System.out.println("[ServerMaster] Found a total of " + jarManager.getTotalServerJars() + " different types/versions to pick from...");
                System.out.println();
                System.out.println(" *** Type '??' to get some help ***");
                System.out.println();
                consoleLog.textProperty().unbind();
            }
        };

        // Bind the consoleLog TextArea to the messageProperty of the task
        consoleLog.textProperty().bind(task.messageProperty());

        // Start the task in a separate thread
        new Thread(task).start();
    }

    /**
     * Custom OutputStream implementation for redirecting output to the consoleLog TextArea.
     */
    public static class Console extends OutputStream {

        private final TextArea output;

        Console(TextArea ta) {
            this.output = ta;
        }

        @Override
        public void write(int i) {
            // Use Platform.runLater to update the UI in the JavaFX application thread
            Platform.runLater(() -> output.appendText(String.valueOf((char) i)));
        }
    }
}
