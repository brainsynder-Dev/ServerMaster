package org.bsdevelopment.servermaster.server.jar;

import com.vaadin.flow.component.notification.Notification;
import org.bsdevelopment.servermaster.server.error.ServerJarNotFoundException;
import org.bsdevelopment.servermaster.server.utils.Version;

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
    private File repo;

    // A mapping of server types to lists of supported server JARs
    private final Map<String, List<ServerJar>> supportedJars = new HashMap<>();

    public void updateRepo (File repo) {
        this.supportedJars.clear();

        this.repo = repo;

        if (repo == null) {
            Notification.show("Unable to update server path, no server path found!");
            return;
        }

        // Iterate through files in the repository directory
        for (File file : Objects.requireNonNull(repo.listFiles())) {
            // Check if the file is a JAR and follows the naming convention ServerType-MCVersion.jar
            if (!file.getName().endsWith(".jar")) continue;
            if (!file.getName().contains("-")) continue;

            String[] args = file.getName().split("-");

            // Check if the JAR file name is well-formed
            if (args.length > 3) {
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

            String build = "";
            if (args.length == 3) {
                build = args[2].replace(".jar", "");
            }

            // Add the server JAR to the list and update the mapping
            serverJars.add(new ServerJar(file, type, version, build));
            this.supportedJars.put(type, serverJars);
        }
    }

    /**
     * Constructs a new {@code ServerJarManager} instance.
     *
     * @param repo The directory where server JAR files are stored.
     */
    public ServerJarManager(File repo) {
        updateRepo(repo);
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
    public LinkedList<ServerJar> getVersionBuilds(String serverType, String serverVersion) {
        Map<String, ServerJar> serverJars = new HashMap<>();
        for (ServerJar jar : supportedJars.getOrDefault(serverType.toLowerCase(), new ArrayList<>())) {
            if (jar.getServerType().equalsIgnoreCase(serverType.toLowerCase()) && jar.getVersion().equals(serverVersion)) {
                if ((jar.getBuild() == null) || jar.getBuild().isEmpty()) continue;
                serverJars.put(jar.getBuild(), jar);
            }
        }
        if (serverJars.isEmpty()) return new LinkedList<>();

        List<String> builds = new ArrayList<>(serverJars.keySet());
        Collections.sort(builds);
        Collections.reverse(builds);

        LinkedList<ServerJar> sortedJars = new LinkedList<>();
        builds.forEach(s -> sortedJars.addLast(serverJars.get(s)));

        return sortedJars;
    }
    public ServerJar getJar(String serverType, String serverVersion, String build) throws ServerJarNotFoundException {
        for (ServerJar jar : supportedJars.getOrDefault(serverType.toLowerCase(), new ArrayList<>())) {
            if (jar.getServerType().equalsIgnoreCase(serverType.toLowerCase()) && jar.getVersion().equals(serverVersion) && jar.getBuild().equals(build)) {
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
