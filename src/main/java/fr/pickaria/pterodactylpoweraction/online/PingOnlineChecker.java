package fr.pickaria.pterodactylpoweraction.online;

import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.OnlineChecker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PingOnlineChecker implements OnlineChecker {
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(1);
    private static final PingOptions PING_OPTIONS = PingOptions.builder().timeout(PING_TIMEOUT).build();
    private final RegisteredServer server;
    private final Configuration configuration;

    public PingOnlineChecker(RegisteredServer server, Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> waitForRunning() {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();

            while (Instant.now().isBefore(start.plus(configuration.getMaximumPingDuration()))) {
                try {
                    // Block and wait for the ping to complete
                    server.ping(PING_OPTIONS).get();
                    return null;
                } catch (InterruptedException | ExecutionException e) {
                    // Ping failed or interrupted, wait for a bit before retrying
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            // Max ping duration exceeded without a successful ping
            throw new CompletionException(new TimeoutException("Max ping duration exceeded"));
        });
    }

    @Override
    public boolean isRunningNow() {
        try {
            return server.ping(PING_OPTIONS).handle((ping, throwable) -> throwable == null).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }
}
