package com.wmatech.java_db_mcp.es;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "elasticsearch")
public class EsRestClient {

    private final HttpClient http;
    private final String baseUrl;
    private final String authHeader;

    public EsRestClient(
            @Value("${DB_URI:}") String uri,
            @Value("${DB_HOST:localhost}") String host,
            @Value("${DB_PORT:9200}") int port,
            @Value("${DB_USER:}") String user,
            @Value("${DB_PASSWORD:}") String password,
            @Value("${ES_API_KEY:}") String apiKey) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.baseUrl = buildBaseUrl(uri, host, port);
        this.authHeader = buildAuth(user, password, apiKey);
    }

    public String request(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        HttpRequest.BodyPublisher publisher = (body == null || body.isEmpty())
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        builder.method(method, publisher);

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Elasticsearch error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    public static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String buildBaseUrl(String uri, String host, int port) {
        if (!uri.isEmpty()) {
            return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
        }
        return "http://" + host + ":" + port;
    }

    private static String buildAuth(String user, String password, String apiKey) {
        if (!apiKey.isEmpty()) {
            return "ApiKey " + apiKey;
        }
        if (!user.isEmpty()) {
            String creds = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
            return "Basic " + creds;
        }
        return null;
    }
}
