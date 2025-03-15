package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PingUtils {
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(1);
    private static final PingOptions PING_OPTIONS = PingOptions.builder().timeout(PING_TIMEOUT).build();

    /**
     * Pings a server until it starts.
     *
     * @param server          The server we want to ping
     * @param maxPingDuration The maximum duration for which we want to ping the server
     * @return A CompletableFuture that will complete once the server has successfully pinged, the future will complete
     * exceptionally if we were not able to ping the server
     */
    public static CompletableFuture<ServerPing> pingUntilUp(RegisteredServer server, Duration maxPingDuration) {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();

            while (Instant.now().isBefore(start.plus(maxPingDuration))) {
                try {
                    // Block and wait for the ping to complete
                    return server.ping(PING_OPTIONS).get();
                } catch (InterruptedException | ExecutionException e) {
                    // Ping failed or interrupted, wait for a bit before retrying
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            // Max ping duration exceeded without successful ping
            throw new CompletionException(new TimeoutException("Max ping duration exceeded"));
        });
    }

    /**
     * Check if the server is reachable right now. This is blocking.
     *
     * @param server The server we want to ping
     * @return True if the server is reachable now and False if any exception is thrown or if the server is not reachable
     */
    public static boolean isReachable(RegisteredServer server) {
        try {
            return server.ping(PING_OPTIONS).handle((ping, throwable) -> throwable == null).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    public static CompletableFuture<Integer> getPlayerCount(RegisteredServer server) {
        return server.ping(PING_OPTIONS)
                .thenApply(ping -> ping.getPlayers().map(ServerPing.Players::getOnline).orElse(0));
    }
}
