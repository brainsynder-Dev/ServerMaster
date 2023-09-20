package org.bsdevelopment.serverapp.server;

/**
 * The {@code ServerBuilder} class is responsible for constructing instances of the {@code Server} class
 * with various configuration options. It provides a fluent builder pattern to set server properties
 * such as name, port, folder, version, server type, and Java options.
 */
public class ServerBuilder {

    // The name of the server
    private final String name;

    // The version of the server (e.g., "1.18.2")
    private String serverVersion;

    // The type of the server (e.g., "spigot")
    private String serverType;

    /**
     * Constructs a new {@code ServerBuilder} instance.
     *
     * @param name   The name of the server.
     */
    public ServerBuilder(String name) {
        this.name = name;
    }

    /**
     * Sets the version of the server.
     *
     * @param version The server version (e.g., "1.18.2").
     * @return The {@code ServerBuilder} instance for method chaining.
     */
    public ServerBuilder version(String version) {
        this.serverVersion = version;
        return this;
    }

    /**
     * Sets the type of the server.
     *
     * @param serverType The server type (e.g., "spigot").
     * @return The {@code ServerBuilder} instance for method chaining.
     */
    public ServerBuilder serverType(String serverType) {
        this.serverType = serverType;
        return this;
    }

    /**
     * Builds and returns a configured {@code Server} instance based on the builder's settings.
     *
     * @return A configured {@code Server} instance.
     * @throws IllegalStateException If required properties such as version, server type, or Java options are not set.
     */
    public Server build() {
        if (serverVersion == null) throw new IllegalStateException("version needs to be set!");
        if (serverType == null) throw new IllegalStateException("server type needs to be set!");

        return new Server(name, serverVersion, serverType, null, false);
    }
}
