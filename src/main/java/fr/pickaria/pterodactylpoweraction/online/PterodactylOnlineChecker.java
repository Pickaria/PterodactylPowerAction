package fr.pickaria.pterodactylpoweraction.online;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.OnlineChecker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class PterodactylOnlineChecker implements OnlineChecker {
    private final RegisteredServer server;
    private final Configuration configuration;

    public PterodactylOnlineChecker(RegisteredServer server, Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> isOnline() {
        String serverId = configuration
                .getPterodactylServerIdentifier(server.getServerInfo().getName())
                .orElseThrow(() -> new NoSuchElementException("No Pterodactyl server id for " + server.getServerInfo().getName()));
        WebSocketCredentialsResponse.Data websocketCredentials = getWebsocketCredentials(serverId, configuration);

        URI base = URI.create(configuration.getPterodactylClientApiBaseURL().orElseThrow(() -> new IllegalStateException("No base URL")));
        String origin = base.getScheme() + "://" + base.getHost() + (base.getPort() == -1 ? "" : ":" + base.getPort());

        CompletableFuture<Void> result = new CompletableFuture<>();

        Duration timeout = configuration.getMaximumPingDuration();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        // Schedule the timeout to fire an exception if we don't complete in time.
        ScheduledFuture<?> timeoutTask = scheduler.schedule(
                () -> result.completeExceptionally(new CompletionException(new TimeoutException("Max ping duration exceeded"))),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        AtomicReference<WebSocket> webSocketReference = new AtomicReference<>();

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .header("Authorization", "Bearer " + configuration.getPterodactylApiKey().orElseThrow())
                .header("Origin", origin)
                .buildAsync(URI.create(websocketCredentials.getSocket()), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocketReference.set(webSocket);
                        requestMore(webSocket);
                        sendJson(webSocket, new Payload("auth", List.of(websocketCredentials.getToken())));
                        sendJson(webSocket, new Payload("send stats"));
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            Payload p = new Gson().fromJson(data.toString(), Payload.class);
                            if ("status".equals(p.getEvent()) && !p.getArgs().isEmpty() && "running".equalsIgnoreCase(p.getArgs().get(0))) {
                                // Completed successfully: server is running
                                if (result.complete(null)) {
                                    timeoutTask.cancel(false);
                                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        requestMore(webSocket);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable err) {
                        result.completeExceptionally(err);
                        timeoutTask.cancel(false);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
                        if (!result.isDone()) {
                            result.completeExceptionally(new CompletionException(new IllegalStateException("WebSocket closed before server reported running")));
                            timeoutTask.cancel(false);
                        }
                        return WebSocket.Listener.super.onClose(ws, status, reason);
                    }

                    private void requestMore(WebSocket ws) {
                        ws.request(1);
                    }

                    private void sendJson(WebSocket ws, Object payload) {
                        ws.sendText(new Gson().toJson(payload), true);
                    }
                })
                .whenComplete((ws, err) -> {
                    if (err != null) {
                        result.completeExceptionally(err);
                        timeoutTask.cancel(false);
                    }
                });

        result.whenComplete((ok, err) -> {
            WebSocket ws = webSocketReference.get();
            if (ws != null) ws.abort();
            scheduler.shutdown();
        });

        return result;
    }

    private WebSocketCredentialsResponse.Data getWebsocketCredentials(String serverIdentifier, Configuration configuration) throws IllegalArgumentException, NoSuchElementException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(configuration.getPterodactylClientApiBaseURL().orElseThrow() + "/servers/" + serverIdentifier + "/websocket"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + configuration.getPterodactylApiKey().orElseThrow())
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Unexpected status: " + statusCode + " – " + response.body());
            }

            WebSocketCredentialsResponse webSocketCredentials = new Gson().fromJson(response.body(), WebSocketCredentialsResponse.class);
            return webSocketCredentials.getData();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class WebSocketCredentialsResponse {
        @SerializedName("data")
        private Data data;

        public Data getData() {
            return data;
        }

        private static class Data {
            @SerializedName("token")
            private String token;

            @SerializedName("socket")
            private String socket;

            public String getToken() {
                return token;
            }

            public String getSocket() {
                return socket;
            }
        }
    }

    private static class Payload {
        Payload(String event, List<String> args) {
            this.event = event;
            this.args = args;
        }

        Payload(String event) {
            this(event, List.of());
        }

        @SerializedName("event")
        private String event;

        @SerializedName("args")
        private List<String> args;

        public String getEvent() {
            return event;
        }

        public List<String> getArgs() {
            return args;
        }
    }
}
