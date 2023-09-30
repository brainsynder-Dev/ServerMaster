package org.bsdevelopment.server;

import java.io.File;

/**
 * The {@code ServerJar} class represents a server JAR file and its associated metadata,
 * including the file itself, server type, and version.
 */
public class ServerJar {

    // The file representing the server JAR
    private final File file;

    // The type of the server (e.g., "spigot")
    private final String serverType;

    // The version of the server (e.g., "1.18.2")
    private String version;

    /**
     * Constructs a new {@code ServerJar} instance.
     *
     * @param file       The file representing the server JAR.
     * @param serverType The type of the server (e.g., "spigot").
     * @param version    The version of the server (e.g., "1.18.2").
     */
    public ServerJar(File file, String serverType, String version) {
        this.file = file;
        this.serverType = serverType;
        this.version = version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns a string representation of the {@code ServerJar} instance.
     *
     * @return A string in the format "serverType-version" (e.g., "spigot-1.18.2").
     */
    @Override
    public String toString() {
        return serverType + "-" + version;
    }

    /**
     * Gets the file associated with the server JAR.
     *
     * @return The {@code File} representing the server JAR.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Gets the type of the server.
     *
     * @return The server type (e.g., "spigot").
     */
    public String getServerType() {
        return this.serverType;
    }

    /**
     * Gets the version of the server.
     *
     * @return The server version (e.g., "1.18.2").
     */
    public String getVersion() {
        String string = version;
        if (string.endsWith(".0")) string = string.replace(".0", "");
        return string;
    }

    public String getCompareVersion () {
        return this.version;
    }
}
