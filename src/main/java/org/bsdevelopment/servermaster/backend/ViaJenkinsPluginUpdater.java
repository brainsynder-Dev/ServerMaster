package org.bsdevelopment.servermaster.backend;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.config.SettingsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ViaJenkinsPluginUpdater {
    private static final String VIAVERSION_JOB = "https://ci.viaversion.com/job/ViaVersion/";
    private static final String VIABACKWARDS_JOB = "https://ci.viaversion.com/view/ViaBackwards/job/ViaBackwards/";
    private final HttpClient client = HttpClient.newHttpClient();

    public void runOnStartup() {
        Path serverPath = SettingsService.get().getServerPath();
        if (serverPath == null) return;

        Path plugins = serverPath.resolve("plugins");

        try {
            Files.createDirectories(plugins);

            updatePlugin("ViaVersion", "viaversion", VIAVERSION_JOB, plugins);
            updatePlugin("ViaBackwards", "viabackwards", VIABACKWARDS_JOB, plugins);

        } catch (Exception e) {
            LogViewer.system("ViaVersion/ViaBackwards updater error: " + e.getMessage());
        }
    }

    private void updatePlugin(String pluginName, String nameHintLower, String jobUrl, Path pluginsDir) throws Exception {
        JsonObject jobJson = getJson(jobUrl + "api/json");
        JsonObject lastBuild = jobJson.get("lastSuccessfulBuild").asObject();
        String buildUrl = lastBuild.getString("url", null);
        if (buildUrl == null) return;

        JsonObject buildJson = getJson(buildUrl + "api/json");

        int buildNumber = buildJson.getInt("number", -1);
        if (buildNumber <= 0) return;

        JsonArray artifacts = buildJson.get("artifacts").asArray();
        if (artifacts.isEmpty()) return;

        String relativePath = artifacts.get(0).asObject().getString("relativePath", null);
        if (relativePath == null) return;

        String originalFileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);

        String finalFileName = appendBuildNumber(originalFileName, buildNumber);

        Path target = pluginsDir.resolve(finalFileName);

        if (Files.exists(target)) {
            LogViewer.system(pluginName + " is already on the latest successful build (#" + buildNumber + ").");
            deleteOld(pluginName, nameHintLower, pluginsDir, finalFileName);
            return;
        }

        String downloadUrl = buildUrl + "artifact/" + relativePath;

        LogViewer.system("Downloading " + pluginName + " (" + originalFileName + ", build #" + buildNumber + ")...");

        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        boolean removedOld = deleteOld(pluginName, nameHintLower, pluginsDir, finalFileName);
        if (removedOld) {
            LogViewer.system(pluginName + " has been updated to build #" + buildNumber + ".");
        } else {
            LogViewer.system(pluginName + " was installed (build #" + buildNumber + ").");
        }
    }

    private String appendBuildNumber(String originalFileName, int buildNumber) {
        int dot = originalFileName.toLowerCase(Locale.ROOT).lastIndexOf(".jar");
        if (dot == -1) return originalFileName + "-b" + buildNumber;

        return originalFileName.substring(0, dot) + "-b" + buildNumber + ".jar";
    }

    private JsonObject getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream in = response.body()) {
            return Json.parse(new InputStreamReader(in, StandardCharsets.UTF_8)).asObject();
        }
    }

    private boolean deleteOld(String pluginName, String nameHintLower, Path pluginsDir, String keepFile) throws IOException {
        boolean deletedAny = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : stream) {
                String file = jar.getFileName().toString();
                if (file.equals(keepFile)) continue;

                if (file.toLowerCase(Locale.ROOT).contains(nameHintLower)) {
                    Files.deleteIfExists(jar);
                    deletedAny = true;
                    continue;
                }

                if (matchesPluginName(jar, pluginName)) {
                    Files.deleteIfExists(jar);
                    deletedAny = true;
                }
            }
        }

        return deletedAny;
    }

    private boolean matchesPluginName(Path jar, String expected) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) return false;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.startsWith("name:")) {
                        String value = line.substring(5).trim();
                        return value.equalsIgnoreCase(expected);
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }
}
