package org.bsdevelopment.servermaster.config;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import org.bsdevelopment.servermaster.Constants;
import org.bsdevelopment.servermaster.utils.JsonFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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

                setDefault("java-path", resolveDefaultJavaPath());
                setDefault("skip-startup-window", false);
                setDefault("force-stop-default", false);
                setDefault("quick-restart-default", false);

                setDefault("developer-mode", false);
                setDefault("maven-deploy-enabled", false);
                setDefault("maven-repo-url", "");
                setDefault("maven-repo-id", "");
                setDefault("maven-username", "");
                setDefault("maven-password", "");
                setDefault("maven-deploy-artifacts", new JsonArray());

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
        settings.setForceStopDefault(file.getBoolean("force-stop-default", false));
        settings.setQuickRestartDefault(file.getBoolean("quick-restart-default", false));

        settings.setDeveloperMode(file.getBoolean("developer-mode", false));
        settings.setMavenDeployEnabled(file.getBoolean("maven-deploy-enabled", false));
        settings.setMavenRepoUrl(file.getString("maven-repo-url"));
        settings.setMavenRepoId(file.getString("maven-repo-id"));
        settings.setMavenUsername(file.getString("maven-username"));
        settings.setMavenPassword(file.getString("maven-password"));
        settings.setMavenDeployArtifacts(readStringArray("maven-deploy-artifacts"));

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
        file.set("force-stop-default", settings.isForceStopDefault());
        file.set("quick-restart-default", settings.isQuickRestartDefault());
        file.set("java-path", settings.getJavaPath() != null ? settings.getJavaPath().toString() : resolveDefaultJavaPath());

        file.set("developer-mode", settings.isDeveloperMode());
        file.set("maven-deploy-enabled", settings.isMavenDeployEnabled());
        file.set("maven-repo-url", settings.getMavenRepoUrl() != null ? settings.getMavenRepoUrl() : "");
        file.set("maven-repo-id", settings.getMavenRepoId() != null ? settings.getMavenRepoId() : "");
        file.set("maven-username", settings.getMavenUsername() != null ? settings.getMavenUsername() : "");
        file.set("maven-password", settings.getMavenPassword() != null ? settings.getMavenPassword() : "");
        file.set("maven-deploy-artifacts", writeStringArray(settings.getMavenDeployArtifacts()));

        file.set("recent-commands", writeRecentCommands(settings.getRecentCommands()));

        file.save();
    }

    public static AppSettings get() {
        if (settings == null) throw new IllegalStateException("Settings not loaded yet");
        return settings;
    }

    private static String resolveDefaultJavaPath() {
        var primary = Constants.JAVA_MANAGER.getPrimaryInstallation();
        if (primary != null) return primary.getJavaExecutable().getAbsolutePath();

        var highest = Constants.JAVA_MANAGER.getHighestInstallation();
        if (highest != null) return highest.getJavaExecutable().getAbsolutePath();

        // Last resort: use the JVM that is currently running this process
        String javaHome = System.getProperty("java.home", "");
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return Paths.get(javaHome, "bin", isWindows ? "javaw.exe" : "java").toString();
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

    private static List<String> readStringArray(String key) {
        JsonValue value = file.getValue(key);
        if (value == null || !value.isArray()) return new ArrayList<>();

        List<String> list = new ArrayList<>();
        for (JsonValue element : value.asArray()) {
            if (element != null && element.isString() && !element.asString().isBlank()) {
                list.add(element.asString());
            }
        }
        return list;
    }

    private static JsonArray writeStringArray(List<String> values) {
        JsonArray arr = new JsonArray();
        if (values == null) return arr;

        for (String value : values) {
            if (value != null && !value.isBlank()) arr.add(value);
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
