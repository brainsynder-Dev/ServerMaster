/*
 * Copyright © 2026
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.servermaster.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class BackendApiService {
    private static final String BASE_URL = "https://dev.bsdevelopment.org/api/servermaster";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BackendApiService() {
        this.httpClient = createUnsafeHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<String> fetchProjects() throws IOException, InterruptedException {
        JsonNode root = sendRequest(BASE_URL + "/projects");

        List<String> projects = new LinkedList<>();
        if (root.isArray()) {
            for (JsonNode node : root) projects.add(node.asText());
        }

        return projects;
    }

    public List<String> fetchVersions(String project) throws IOException, InterruptedException {
        Objects.requireNonNull(project, "Project cannot be null");

        JsonNode root = sendRequest(BASE_URL + "/" + project);

        List<String> versions = new LinkedList<>();
        JsonNode versionArray = root.get("versions");

        if (versionArray != null && versionArray.isArray()) {
            for (JsonNode node : versionArray) versions.add(node.asText());
        }

        return versions;
    }

    public List<BuildInfo> fetchBuilds(String project, String version)
            throws IOException, InterruptedException {

        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");

        JsonNode root = sendRequest(BASE_URL + "/" + project + "/" + version);

        List<BuildInfo> builds = new ArrayList<>();
        JsonNode buildArray = root.get("builds");

        if (buildArray != null && buildArray.isArray()) {
            for (JsonNode node : buildArray) {
                int buildNumber = node.get("build").asInt();
                String downloadUrl = node.get("downloadUrl").asText();
                builds.add(new BuildInfo(buildNumber, downloadUrl));
            }
        }

        return builds;
    }

    private JsonNode sendRequest(String url)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "ServerMaster")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch data from API: " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private HttpClient createUnsafeHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());

            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm(null);

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParams)
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create unsafe SSL HttpClient", e);
        }
    }

    public HttpURLConnection openDownloadConnection(List<BuildInfo> builds, String build) throws IOException {
        Objects.requireNonNull(build, "Build cannot be null");

        BuildInfo buildInfo = findBuildByNumber(builds, build);
        if (buildInfo == null) {
            throw new IllegalArgumentException("Unknown build number: " + build);
        }

        return (HttpURLConnection) new URL(buildInfo.downloadUrl()).openConnection();
    }

    public BuildInfo findBuildByNumber(List<BuildInfo> builds, String buildNumber) {
        if (builds == null || builds.isEmpty() || buildNumber == null) return null;

        return builds.stream().filter(build -> String.valueOf(build.build()).equals(buildNumber)).findFirst().orElse(null);
    }

    public List<String> extractBuildNumbers(List<BuildInfo> builds) {
        if (builds == null || builds.isEmpty()) return List.of();
        return builds.stream().map(b -> String.valueOf(b.build())).toList();
    }

    public record BuildInfo(int build, String downloadUrl) {
        @Override
        public String toString() {
            return String.valueOf(build);
        }
    }
}
