package org.bsdevelopment.serverapp.server;

import org.bsdevelopment.serverapp.server.error.ServerJarNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@code Server} class represents a game server instance. It manages the server's name,
 * version, type, and its execution via a {@code ServerThread}. It also provides methods for
 * starting and checking the server's status.
 */
public class Server {

    // Logger for logging events and errors
    private final Logger log = Logger.getLogger("");

    // The name of the server
    private final String name;

    // The version of the server (e.g., "1.18.2")
    private final String serverVersion;

    // The type of the server (e.g., "spigot")
    private final String serverType;

    // The thread responsible for managing the server process
    private ServerThread thread;

    // Flag indicating whether the server is running
    private boolean running;

    /**
     * Constructs a new {@code Server} instance.
     *
     * @param name          The name of the server.
     * @param serverVersion The version of the server (e.g., "1.18.2").
     * @param serverType    The type of the server (e.g., "spigot").
     * @param thread        The {@code ServerThread} responsible for managing the server process.
     * @param running       A flag indicating whether the server is running.
     */
    public Server(String name, String serverVersion, String serverType, ServerThread thread, boolean running) {
        this.name = name;
        this.serverVersion = serverVersion;
        this.serverType = serverType;
        this.thread = thread;
        this.running = running;
    }

    /**
     * Starts the server with the provided callback to handle server thread events.
     *
     * @param callback The callback to handle server thread events.
     * @throws ServerJarNotFoundException If the server JAR is not found in the repository.
     * @throws IOException                If an I/O error occurs while updating server properties.
     */
    public void start(ServerThreadCallback callback) throws ServerJarNotFoundException, IOException {
        // Set the current server instance in the ServerWrapper
        ServerWrapper.getInstance().setServer(this);

        // Get the server JAR based on server type and version
        ServerJar jar = ServerWrapper.getInstance().getJarManager().getJar(serverType, serverVersion);

        // Get the absolute path to the server JAR file
        String path = ServerWrapper.getInstance().getJarManager().getFile(jar).getAbsolutePath();

        // Retrieve Java options for running the server
        List<String> options = getJavaOptions(path);

        // Load or create the 'server.properties' file and update 'level-name'
        Properties prop = new Properties();
        File serverProp = new File(ServerWrapper.getInstance().getJarManager().getRepo(), "server.properties");

        if (!serverProp.createNewFile()) {
            try {
                prop.load(new FileInputStream(serverProp));
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to fetch 'server.properties' file");
                throw new RuntimeException(e);
            }
        }

        System.out.println("[ServerMaster] Updating level-name to 'World_" + jar.getVersion() + "'");
        prop.setProperty("level-name", "World_" + jar.getVersion());
        prop.store(new FileOutputStream(serverProp), null);

        // Create a ProcessBuilder for running the server
        ProcessBuilder pb = new ProcessBuilder(options);
        pb.directory(ServerWrapper.getInstance().getJarManager().getRepo());

        // Set the server to running, create a new server thread, and provide a callback
        running = true;
        thread = new ServerThread(this, pb, (server, statusCode) -> {
            running = false;
            callback.call(server, statusCode);
            ServerWrapper.getInstance().removeServer();
        });
    }

    /**
     * Retrieves the name of the server.
     *
     * @return The server's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Retrieves the server's associated thread.
     *
     * @return The server thread responsible for managing the server process.
     */
    public ServerThread getThread() {
        return this.thread;
    }

    /**
     * Checks if the server is running.
     *
     * @return {@code true} if the server is running, {@code false} otherwise.
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Retrieves a list of Java options for running the server.
     *
     * @param path The absolute path to the server JAR file.
     * @return A list of Java options.
     */
    private List<String> getJavaOptions(String path) {
        List<String> options = new ArrayList<>();
        options.add("java");
        options.add("-DIReallyKnowWhatIAmDoingISwear");
        options.add("-Djline.terminal=jline.UnsupportedTerminal"); // Remove warning on Windows
        options.add("-DserverName=" + name); // For use in the Bukkit plugin
        options.add("-jar");
        options.add(path);
        options.add("-Xms" + "1024M");
        options.add("-Xmx" + "1024M");
        options.add("--port");
        options.add("25565");
        options.add("-o");
        options.add("false");
        options.add("nogui");
        return options;
    }
}
