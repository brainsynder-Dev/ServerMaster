package org.bsdevelopment.servermaster.instance.server;

import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.ServerMasterApp;
import org.bsdevelopment.servermaster.instance.server.thread.ServerOutputListener;
import org.bsdevelopment.servermaster.instance.server.thread.ServerThreadCallback;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public final class ServerHandlerAPI {
    public static int startServer(String serverType, String version, String build,
                                  ServerLaunchConfig config, ServerOutputListener output, ServerThreadCallback onExit) {
        Objects.requireNonNull(serverType, "serverType");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(onExit, "onExit");

        var server = new ServerBuilder(serverType + "(" + version + ")").serverType(serverType).version(version)
                .build(build == null ? "" : build).build();

        try {
            Integer buildNumber = parseBuildNumber(build);
            ServerMasterApp.instanceCatalog.copyToRuntimeJar(serverType, version, buildNumber);

            server.start(config, output, onExit);
        } catch (IOException | RuntimeException e) {
            Constants.LOGGER.log(Level.SEVERE, "Could not start server", e);

            try {
                onExit.call(server, 2);
            } catch (Throwable ignored) {}

            try {
                ServerMasterApp.serverWrapper().removeServer();
            } catch (Throwable ignored) {}
        }

        return config.port();
    }

    public static void sendServerCommand(String command) {
        var server = ServerMasterApp.serverWrapper().getServer();
        if (server == null || server.getThread() == null) return;
        server.getThread().sendMessage(command);
    }

    public static void stopServer() {
        var server = ServerMasterApp.serverWrapper().getServer();
        if (server == null || server.getThread() == null) return;
        server.getThread().stopServer();
    }

    public static void killServer() {
        var server = ServerMasterApp.serverWrapper().getServer();
        if (server == null || server.getThread() == null) return;
        server.getThread().killServer();
    }

    private static Integer parseBuildNumber(String build) throws IOException {
        if (build == null) return null;
        String trimmed = build.trim();
        if (trimmed.isEmpty()) return null;

        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid build number: '" + build + "'", e);
        }
    }
}
