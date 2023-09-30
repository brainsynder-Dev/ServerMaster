package org.bsdevelopment.server.error;

/**
 * The {@code ServerJarNotFoundException} is a custom exception class that is thrown when a specific server JAR
 * file for a given server type and version cannot be found in the ServerJarManager.
 */
public class ServerJarNotFoundException extends Exception {

    // The error message to describe the exception
    private final String message;

    /**
     * Constructs a new instance of {@code ServerJarNotFoundException} with the specified server type and version.
     *
     * @param serverType    The type of the server (e.g., "spigot").
     * @param serverVersion The version of the server (e.g., "1.18.2").
     */
    public ServerJarNotFoundException(String serverType, String serverVersion) {
        // Construct an error message indicating that the server JAR file was not found
        message = "A ServerJar " + serverType + " and version " + serverVersion + " could not be found!";
    }

    /**
     * Retrieves the error message describing the exception.
     *
     * @return The error message.
     */
    @Override
    public String getMessage() {
        return message;
    }
}
