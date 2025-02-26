package fr.pickaria.pterodactylpoweraction.api;

import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.PowerActionAPI;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PterodactylAPI implements PowerActionAPI {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Logger logger;
    private final Configuration configuration;

    public PterodactylAPI(Logger logger, Configuration configuration) {
        this.logger = logger;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> stop(String server) {
        Optional<String> serverIdentifier = configuration.getPterodactylServerIdentifier(server);
        if (serverIdentifier.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("No unique identifier for server " + server));
        }

        String identifier = serverIdentifier.get();
        logger.info("Stopping server {}", server);
        return makeRequest(identifier, "stop");
    }

    @Override
    public CompletableFuture<Void> start(String server) {
        Optional<String> serverIdentifier = configuration.getPterodactylServerIdentifier(server);
        if (serverIdentifier.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("No unique identifier for server " + server));
        }

        String identifier = serverIdentifier.get();
        logger.info("Starting server {}", server);
        return makeRequest(identifier, "start");
    }

    private CompletableFuture<Void> makeRequest(String server, String action) {
        CompletableFuture<Void> future = new CompletableFuture<>(); // TODO: This could be simplified
        assert action.equals("start") || action.equals("stop");

        CompletableFuture.runAsync(() -> {
            String jsonBody = "{\"signal\":\"" + action + "\"}";

            HttpRequest request = null;
            try {
                request = HttpRequest.newBuilder()
                        .uri(new URI(configuration.getPterodactylClientApiBaseURL().get() + "/servers/" + server + "/power"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + configuration.getPterodactylApiKey().get())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
            } catch (URISyntaxException e) {
                future.completeExceptionally(e);
            }

            HttpResponse<Void> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (IOException | InterruptedException e) {
                future.completeExceptionally(e);
                return;
            }

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                // Response code is in the 200-299 range, complete successfully
                future.complete(null);
            } else {
                // Response code is not in the 200-299 range, complete exceptionally with IOException
                future.completeExceptionally(new IOException("Unexpected response code: " + statusCode));
            }
        });

        return future;
    }
}
