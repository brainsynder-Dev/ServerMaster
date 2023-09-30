package org.bsdevelopment.server;

/**
 * The {@code ServerWrapper} class is responsible for managing a server instance
 * and ensuring that the server is started, stopped, or killed gracefully.
 * It follows the Singleton design pattern, allowing only one instance of the class
 * to exist throughout the application's lifecycle.
 */
public class ServerWrapper {

    // The singleton instance of ServerWrapper
    private static ServerWrapper INSTANCE;

    // Manager responsible for handling server JAR files
    private final ServerJarManager jarManager;

    // The server instance managed by this wrapper
    private Server server = null;

    /**
     * Constructs a new {@code ServerWrapper} instance.
     *
     * @param jarManager The manager for handling server JAR files.
     * @throws IllegalStateException If an attempt is made to create multiple instances
     *                               of {@code ServerWrapper}, which is not allowed due
     *                               to the Singleton design pattern.
     */
    public ServerWrapper(ServerJarManager jarManager) {
        if (INSTANCE != null) {
            throw new IllegalStateException("ServerWrapper is a singleton!");
        }
        INSTANCE = this;

        this.jarManager = jarManager;

        // Register a shutdown hook to gracefully handle server shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            API.killServer();
            try {
                // Sleep briefly to ensure server cleanup
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }));
    }

    /**
     * Removes the reference to the currently managed server instance without stopping it.
     */
    public void removeServer() {
        server = null;
    }

    /**
     * Sets the server instance to be managed by this wrapper.
     *
     * @param server The server instance to manage.
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Retrieves the singleton instance of {@code ServerWrapper}.
     *
     * @return The singleton instance of {@code ServerWrapper}.
     */
    public static ServerWrapper getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieves the server instance currently managed by this wrapper.
     *
     * @return The server instance currently managed by this wrapper.
     */
    public Server getServer() {
        return server;
    }

    /**
     * Retrieves the manager responsible for handling server JAR files.
     *
     * @return The {@code ServerJarManager} instance associated with this wrapper.
     */
    public ServerJarManager getJarManager() {
        return jarManager;
    }
}
