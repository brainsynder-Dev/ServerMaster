package org.bsdevelopment.server;

import org.bsdevelopment.server.error.ServerJarNotFoundException;
import org.bsdevelopment.utils.Version;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code ServerJarManager} class is responsible for managing server JAR files stored in a repository.
 * It provides methods for retrieving server JARs by type and version, as well as maintaining a collection
 * of supported server JARs.
 */
public class ServerJarManager {

    // The directory where server JAR files are stored
    private final File repo;

    // A mapping of server types to lists of supported server JARs
    private final Map<String, List<ServerJar>> supportedJars = new HashMap<>();

    /**
     * Constructs a new {@code ServerJarManager} instance.
     *
     * @param repo The directory where server JAR files are stored.
     */
    public ServerJarManager(File repo) {
        this.repo = repo;

        // Iterate through files in the repository directory
        for (File file : Objects.requireNonNull(repo.listFiles())) {
            // Check if the file is a JAR and follows the naming convention ServerType-MCVersion.jar
            if (!file.getName().endsWith(".jar")) continue;
            if (!file.getName().contains("-")) continue;

            String[] args = file.getName().split("-");

            // Check if the JAR file name is well-formed
            if (args.length > 2) {
                System.out.println(" - Malformed jar name for '" + file.getName() +
                        "' must be in format: ServerType-MCVersion.jar (Example: spigot-1.18.2.jar)");
                continue;
            }

            String type = args[0].toLowerCase();

            // Retrieve or create a list of server JARs for the specified type
            List<ServerJar> serverJars = supportedJars.getOrDefault(type, new ArrayList<>());
            String version = args[1].replace(".jar", "");
            if (version.split("\\.").length != 3) version += ".0";
            if (version.startsWith(".")) version = "0" + version;

            // Add the server JAR to the list and update the mapping
            serverJars.add(new ServerJar(file, type, version));
            supportedJars.put(type, serverJars);
        }
    }

    public LinkedList<Version> sortVersions (List<ServerJar> jars) {
        LinkedList<Version> versions = new LinkedList<>();
        jars.stream()
                .map(serverJar -> Version.parse(serverJar.getCompareVersion()))
                .sorted()
                .forEach(version -> {
                    String string = version.toString();
                    if (string.endsWith(".0")) string = string.replace(".0", "");
                    versions.addLast(Version.parse(string));
                });
        return versions;
    }

    /**
     * Gets a server JAR file by its server type and version.
     *
     * @param serverType    The type of the server (e.g., "spigot").
     * @param serverVersion The version of the server (e.g., "1.18.2").
     * @return The corresponding {@code ServerJar} instance.
     * @throws ServerJarNotFoundException If the requested server JAR is not found.
     */
    public ServerJar getJar(String serverType, String serverVersion) throws ServerJarNotFoundException {
        for (ServerJar jar : supportedJars.getOrDefault(serverType.toLowerCase(), new ArrayList<>())) {
            if (jar.getServerType().equalsIgnoreCase(serverType.toLowerCase()) && jar.getVersion().equals(serverVersion)) {
                return jar;
            }
        }

        throw new ServerJarNotFoundException(serverType.toLowerCase(), serverVersion);
    }

    /**
     * Gets a mapping of supported server JARs grouped by server type.
     *
     * @return A mapping of server types to lists of supported server JARs.
     */
    public Map<String, List<ServerJar>> getSupportedJars() {
        return supportedJars;
    }

    /**
     * Gets the total number of supported server JARs across all server types.
     *
     * @return The total number of supported server JARs.
     */
    public int getTotalServerJars() {
        AtomicInteger total = new AtomicInteger(0);
        supportedJars.forEach((s, serverJars) -> {
            serverJars.forEach(serverJar -> total.getAndIncrement());
        });

        return total.get();
    }

    /**
     * Gets the file associated with a given server JAR.
     *
     * @param jar The {@code ServerJar} instance.
     * @return The file corresponding to the server JAR.
     */
    public File getFile(ServerJar jar) {
        return new File(repo, jar.getFile().getName());
    }

    /**
     * Gets the directory where server JAR files are stored.
     *
     * @return The server JAR repository directory.
     */
    public File getRepo() {
        return this.repo;
    }
}
