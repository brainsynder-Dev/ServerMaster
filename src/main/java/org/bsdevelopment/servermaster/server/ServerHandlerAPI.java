package org.bsdevelopment.servermaster.server;

import org.bsdevelopment.servermaster.server.error.ServerJarNotFoundException;
import org.bsdevelopment.servermaster.server.thread.ServerThreadCallback;
import org.bsdevelopment.servermaster.utils.AppUtilities;

import java.io.IOException;

/**
 * The {@code API} class provides a set of static methods for managing game servers through the ServerMaster application.
 * It allows starting, sending commands to, stopping, and forcibly killing game servers.
 */
public class ServerHandlerAPI {

    /**
     * Starts a game server with the specified name, server type, and version.
     *
     * @param serverType The type of the server (e.g., "spigot").
     * @param version    The version of the server (e.g., "1.18.2").
     * @param callback   The callback to handle server thread events.
     * @return The port number on which the server is running.
     */
    public static int startServer(String serverType, String version, String build, ServerThreadCallback callback) {
        // Create a new server instance using the ServerBuilder
        Server server = new ServerBuilder((serverType + "(" + version + ")")).serverType(serverType).version(version).build(build).build();

        // Set the current server instance in the ServerWrapper
        ServerWrapper.getInstance().setServer(server);

        try {
            // Start the server with the provided callback
            server.start(callback);
        } catch (ServerJarNotFoundException | IOException e) {
            AppUtilities.logMessage("[ERROR]: Could not start server!");
            e.printStackTrace();
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