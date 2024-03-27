package org.bsdevelopment.servermaster.views.service;

import com.eclipsesource.json.Json;
import org.apache.commons.lang3.tuple.Pair;
import org.bsdevelopment.servermaster.views.data.ServerTypeBuild;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

@Service
public class ApiService {

    public CompletableFuture<LinkedList<String>> getVersions (String type) {
        String url = "https://bsdevelopment.org/api/admin/"+type+"-versions";
        String result = sendGetRequest(url, false);

        LinkedList<String> versions = new LinkedList<>();
        Json.parse(result).asObject().get("versions").asArray().forEach(jsonValue -> versions.addLast(jsonValue.asString()));

        // return CompletableFuture.failedFuture(new NullPointerException(""));
        return CompletableFuture.completedFuture(versions);
    }

    public CompletableFuture<LinkedList<ServerTypeBuild>> getBuilds (String type, String version) {
        String url = "https://bsdevelopment.org/api/admin/"+type+"-builds/"+version;
        String result = sendGetRequest(url, false);

        LinkedList<ServerTypeBuild> versions = new LinkedList<>();
        Json.parse(result).asArray().forEach(jsonValue -> {
            if (!jsonValue.isObject()) {
                versions.addLast(new ServerTypeBuild(String.valueOf(jsonValue.asInt()), ""));
                return;
            }

            versions.addLast(new ServerTypeBuild(
                    jsonValue.asObject().get("build").asString(),
                    jsonValue.asObject().get("jar").asString()
            ));
        });

        // return CompletableFuture.failedFuture(new NullPointerException(""));
        return CompletableFuture.completedFuture(versions);
    }

    public CompletableFuture<Pair<HttpURLConnection, Long>> getConnection (String rawUrl) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
            long completeFileSize = httpConnection.getContentLength();
            return CompletableFuture.completedFuture(Pair.of(httpConnection, completeFileSize));
        }catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }






    private String sendGetRequest (String rawUrl, boolean redirect) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(redirect);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (InputStream inputStream = connection.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return "";
        }
    }
}
