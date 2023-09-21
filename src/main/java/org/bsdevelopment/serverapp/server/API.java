package org.bsdevelopment.serverapp.server;

import org.bsdevelopment.serverapp.server.error.ServerJarNotFoundException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@code API} class provides a set of static methods for managing game servers through the ServerMaster application.
 * It allows starting, sending commands to, stopping, and forcibly killing game servers.
 */
public class API {

    // Logger for logging events and errors
    private static final Logger log = Logger.getLogger("");

    /**
     * Starts a game server with the specified name, server type, and version.
     *
     * @param name     The name of the server.
     * @param serverType The type of the server (e.g., "spigot").
     * @param version  The version of the server (e.g., "1.18.2").
     * @param callback The callback to handle server thread events.
     * @return The port number on which the server is running.
     */
    public static int startServer(String name, String serverType, String version, ServerThreadCallback callback) {
        // Create a new server instance using the ServerBuilder
        Server server = new ServerBuilder(name).serverType(serverType).version(version).build();

        // Set the current server instance in the ServerWrapper
        ServerWrapper.getInstance().setServer(server);

        try {
            // Start the server with the provided callback
            server.start(callback);
        } catch (ServerJarNotFoundException | IOException e) {
            log.log(Level.WARNING, "Could not start server", e);
        }

        return server.getPort();
    }

    /**
     * Sends a command to the currently running game server.
     *
     * @param command The command to send to the server.
     */
    public static void sendServerCommand(String command) {
        // Retrieve the current server instance from ServerWrapper
        Server server = ServerWrapper.getInstance().getServer();

        // Check if a server instance exists and has a running thread
        if (server == null || server.getThread() == null) return;

        // Send the command to the server thread for execution
        server.getThread().sendMessage(command);
    }

    /**
     * Stops the currently running game server gracefully.
     */
    public static void stopServer() {
        // Retrieve the current server instance from ServerWrapper
        Server server = ServerWrapper.getInstance().getServer();

        // Check if a server instance exists and has a running thread
        if (server == null || server.getThread() == null) return;

        // Stop the server thread gracefully
        server.getThread().stopServer();

        // Remove the server instance from ServerWrapper
        ServerWrapper.getInstance().removeServer();
    }

    /**
     * Forcibly kills the currently running game server.
     */
    public static void killServer() {
        // Retrieve the current server instance from ServerWrapper
        Server server = ServerWrapper.getInstance().getServer();

        // Check if a server instance exists and has a running thread
        if (server == null || server.getThread() == null) return;

        // Forcefully kill the server process
        server.getThread().killServer();

        // Remove the server instance from ServerWrapper
        ServerWrapper.getInstance().removeServer();
    }
}