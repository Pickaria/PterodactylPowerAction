package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class PingUtils {
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(1);
    private static final PingOptions PING_OPTIONS = PingOptions.builder().timeout(PING_TIMEOUT).build();

    public static CompletableFuture<Integer> getPlayerCount(RegisteredServer server) {
        return server.ping(PING_OPTIONS)
                .thenApply(ping -> ping.getPlayers().map(ServerPing.Players::getOnline).orElse(0));
    }
}
