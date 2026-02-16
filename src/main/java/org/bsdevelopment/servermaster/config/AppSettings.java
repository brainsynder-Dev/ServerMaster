package org.bsdevelopment.servermaster.config;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AppSettings {
    private int appConfigVersion = -1;

    private int port = 25565;

    private long memory = 4;
    private Path serverPath;
    private Path javaPath;

    private boolean skipStartupWindow = false;

    private List<String> recentCommands = new ArrayList<>();

    public boolean isInitialized() {
        return appConfigVersion != -1;
    }
}
