package org.bsdevelopment.servermaster.instance.server;

import java.nio.file.Path;
import java.util.List;

public record ServerLaunchConfig(Path javaExecutable, long ramMb, int port, boolean autoAcceptEula, List<String> additionalJvmArgs) {
    public ServerLaunchConfig {
        if (javaExecutable == null) throw new IllegalArgumentException("javaExecutable is required");
        if (ramMb <= 0) throw new IllegalArgumentException("ramMb must be > 0");
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("port must be 1-65535");

        additionalJvmArgs = (additionalJvmArgs == null) ? List.of() : List.copyOf(additionalJvmArgs);
    }
}
