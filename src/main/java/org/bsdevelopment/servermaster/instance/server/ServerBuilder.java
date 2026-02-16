package org.bsdevelopment.servermaster.instance.server;

public class ServerBuilder {
    private final String name;
    private String serverType;
    private String serverVersion;
    private String serverBuild;

    public ServerBuilder(String name) {
        this.name = name;
    }

    public ServerBuilder version(String version) {
        this.serverVersion = version;
        return this;
    }

    public ServerBuilder build(String build) {
        this.serverBuild = build;
        return this;
    }

    public ServerBuilder serverType(String serverType) {
        this.serverType = serverType;
        return this;
    }

    public Server build() {
        if (serverType == null) throw new IllegalStateException("server type needs to be set!");
        if (serverVersion == null) throw new IllegalStateException("version needs to be set!");
        if (serverBuild == null) throw new IllegalStateException("server build needs to be set!");

        return new Server(name, serverType, serverVersion, serverBuild, null, false);
    }
}
