package org.bsdevelopment.servermaster.instance.server;

import lombok.Getter;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.instance.server.thread.ServerOutputListener;
import org.bsdevelopment.servermaster.instance.server.thread.ServerThread;
import org.bsdevelopment.servermaster.instance.server.thread.ServerThreadCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class Server {
    @Getter private final String name;
    private final String serverType;
    private final String serverVersion;
    private final String build;
    @Getter private int port = 25565;
    @Getter private ServerThread thread;
    @Getter private boolean running;

    public Server(String name, String serverType, String serverVersion, String build, ServerThread thread, boolean running) {
        this.name = name;
        this.serverType = serverType;
        this.serverVersion = serverVersion;
        this.build = build;
        this.thread = thread;
        this.running = running;
    }

    public void start(ServerLaunchConfig config, ServerOutputListener outputListener, ServerThreadCallback callback) throws IOException {
        this.port = config.port();

        var wrapper = ServerMasterApp.serverWrapper();
        wrapper.setServer(this);

        Path serverRoot = wrapper.serverRoot();
        Path runtimeJar = wrapper.runtimeJarPath();

        if (!Files.exists(runtimeJar)) {
            throw new IOException("Missing runtime jar: " + runtimeJar + " (expected instanceCatalog.copyToRuntimeJar(...) to run first)");
        }

        updateServerProperties(serverRoot);

        List<String> options = getJavaOptions(config, runtimeJar);

        ProcessBuilder pb = new ProcessBuilder(options);
        pb.directory(serverRoot.toFile());

        running = true;
        thread = new ServerThread(this, pb, outputListener, (server, statusCode) -> {
            running = false;
            callback.call(server, statusCode);
            wrapper.removeServer();
        });
    }

    private void updateServerProperties(Path serverRoot) {
        Properties prop = new Properties();
        File serverProp = serverRoot.resolve("server.properties").toFile();

        try {
            if (!serverProp.createNewFile()) {
                try (FileInputStream fis = new FileInputStream(serverProp)) {
                    prop.load(fis);
                }
            }

            String worldName = "World_" + serverVersion;

            LogViewer.system("Updating level-name to " + worldName);
            prop.setProperty("level-name", worldName);

            try (FileOutputStream fos = new FileOutputStream(serverProp)) {
                prop.store(fos, null);
            }
        } catch (IOException e) {
            Constants.LOGGER.log(Level.WARNING, "Failed to update server.properties", e);
        }
    }

    private List<String> getJavaOptions(ServerLaunchConfig config, Path jarPath) {
        List<String> options = new ArrayList<>();
        options.add(config.javaExecutable().toAbsolutePath().toString());
        options.add("-Xms" + config.ramMb() + "G");
        options.add("-Xmx" + config.ramMb() + "G");
        options.addAll(config.additionalJvmArgs());
        options.add("-DIReallyKnowWhatIAmDoingISwear");
        if (config.autoAcceptEula()) options.add("-Dcom.mojang.eula.agree=true");
        options.add("-Ddebug.rewriteForIde=true");
        options.add("-Dpaper.disableStartupVersionCheck=true");
        options.add("-Djline.terminal=jline.UnsupportedTerminal");
        options.add("-DserverName=" + name);
        options.add("-jar");
        options.add(jarPath.toAbsolutePath().toString());
        options.add("--port");
        options.add(String.valueOf(config.port()));
        options.add("nogui");
        return options;
    }
}
