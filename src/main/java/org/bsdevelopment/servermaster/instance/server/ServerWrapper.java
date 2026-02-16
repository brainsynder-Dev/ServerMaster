package org.bsdevelopment.servermaster.instance.server;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ServerWrapper {

    private final Path serverRoot;
    private final AtomicReference<Server> serverRef = new AtomicReference<>();

    public ServerWrapper(Path serverRoot) {
        this.serverRoot = Objects.requireNonNull(serverRoot, "serverRoot");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Server server = serverRef.get();
            if (server == null) return;

            var thread = server.getThread();
            if (thread == null) return;

            try {
                thread.killServer();
                Thread.sleep(750);
            } catch (Throwable ignored) {}
        }));
    }

    public Path serverRoot() {
        return serverRoot;
    }

    public Path runtimeJarPath() {
        return serverRoot.resolve("server.jar");
    }

    public Server getServer() {
        return serverRef.get();
    }

    public void setServer(Server server) {
        serverRef.set(Objects.requireNonNull(server, "server"));
    }

    public void removeServer() {
        serverRef.set(null);
    }

    public boolean isServerRunning() {
        Server s = serverRef.get();
        return s != null && s.isRunning();
    }
}
