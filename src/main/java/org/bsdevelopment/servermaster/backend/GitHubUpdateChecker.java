package org.bsdevelopment.servermaster.backend;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.instance.server.utils.Version;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class GitHubUpdateChecker {
    private static final String RELEASES_API = "https://api.github.com/repos/brainsynder-Dev/ServerMaster/releases/latest";
    private final HttpClient client = HttpClient.newHttpClient();
    public record ReleaseInfo(String version, String jarDownloadUrl) {}

    public Optional<ReleaseInfo> checkForUpdate(String currentVersion) {
        if (currentVersion == null) return Optional.empty();

        try {
            LogViewer.update("Checking for updates (current: v" + currentVersion + ")");
            HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES_API)).header("Accept", "application/vnd.github+json").header("User-Agent", "ServerMaster").GET().build();

            JsonObject json;
            try (InputStream in = client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
                json = Json.parse(new InputStreamReader(in, StandardCharsets.UTF_8)).asObject();
            }

            String tagName = json.getString("tag_name", null);
            if (tagName == null) return Optional.empty();

            String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            if (Version.parse(latestVersion).compareTo(Version.parse(currentVersion)) <= 0) {
                LogViewer.update("Already up to date (latest: v" + latestVersion + ")");
                return Optional.empty();
            }

            LogViewer.update("New version available: v" + latestVersion);

            JsonArray assets = json.get("assets").asArray();
            String jarUrl = null;
            for (var asset : assets) {
                var obj = asset.asObject();
                if ("ServerMaster.jar".equals(obj.getString("name", ""))) {
                    jarUrl = obj.getString("browser_download_url", null);
                    break;
                }
            }

            if (jarUrl == null) {
                LogViewer.update("ServerMaster.jar not found in release assets");
                return Optional.empty();
            }

            return Optional.of(new ReleaseInfo(latestVersion, jarUrl));
        } catch (Exception e) {
            LogViewer.update("Update check failed: " + e.getMessage());
            return Optional.empty();
        }
    }
}
