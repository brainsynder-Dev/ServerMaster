package org.bsdevelopment.servermaster.config;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.utils.JsonFile;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SettingsService {
    private static AppSettings settings;
    private static JsonFile file;

    public static void load(File configFile) {
        file = new JsonFile(configFile) {
            @Override
            public void loadDefaults() {
                setDefault("config-version", -1);
                setDefault("dedicated-ram", 4);
                setDefault("server-path", "");
                setDefault("server-port", 25565);

                setDefault("java-path", Constants.JAVA_MANAGER.getPrimaryInstallation().getJavaExecutable().getAbsolutePath());
                setDefault("skip-startup-window", false);

                setDefault("recent-commands", new JsonArray());
            }
        };
        file.save();

        int configVersion = file.getInteger("config-version", -1);
        int port = file.getInteger("server-port", 25565);
        long memory = file.getLong("dedicated-ram", 4);
        String serverPathRaw = file.getString("server-path");
        String javaPath = file.getString("java-path");

        settings = new AppSettings();
        settings.setAppConfigVersion(configVersion);
        settings.setMemory(memory);
        settings.setPort(port);
        settings.setSkipStartupWindow(file.getBoolean("skip-startup-window", false));

        if (!javaPath.isBlank()) {
            settings.setJavaPath(Path.of(javaPath));
        }
        if (!serverPathRaw.isBlank()) {
            settings.setServerPath(Path.of(serverPathRaw));
        }

        settings.setRecentCommands(readRecentCommands());
        trimRecentCommands(settings.getRecentCommands());
    }

    public static void save() {
        if (settings == null) throw new IllegalStateException("Settings not loaded yet");

        file.set("config-version", settings.getAppConfigVersion());
        file.set("dedicated-ram", settings.getMemory());
        file.set("server-path", settings.getServerPath() != null ? settings.getServerPath().toString() : "");
        file.set("server-port", settings.getPort());
        file.set("skip-startup-window", settings.isSkipStartupWindow());
        file.set("java-path", settings.getJavaPath() != null ? settings.getJavaPath().toString() : Constants.JAVA_MANAGER.getPrimaryInstallation().getJavaExecutable().getAbsolutePath());

        file.set("recent-commands", writeRecentCommands(settings.getRecentCommands()));

        file.save();
    }

    public static AppSettings get() {
        if (settings == null) throw new IllegalStateException("Settings not loaded yet");
        return settings;
    }

    private static List<String> readRecentCommands() {
        JsonValue value = file.getValue("recent-commands");
        if (value == null || !value.isArray()) return new ArrayList<>();

        var arr = value.asArray();
        List<String> list = new ArrayList<>(arr.size());
        for (JsonValue v : arr) {
            if (v == null || !v.isString()) continue;
            String s = v.asString();
            if (s == null || s.isBlank()) continue;
            list.add(s);
        }
        return list;
    }

    private static JsonArray writeRecentCommands(List<String> commands) {
        var arr = new JsonArray();
        if (commands == null) return arr;

        for (String cmd : commands) {
            if (cmd == null || cmd.isBlank()) continue;
            arr.add(cmd);
        }
        return arr;
    }

    private static void trimRecentCommands(List<String> commands) {
        if (commands == null) return;

        while (commands.size() > 10) {
            commands.remove(0);
        }
    }
}
