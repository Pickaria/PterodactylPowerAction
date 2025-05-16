package fr.pickaria.pterodactylpoweraction.online;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.OnlineChecker;
import fr.pickaria.pterodactylpoweraction.PingUtils;

import java.util.concurrent.CompletableFuture;

public class PingOnlineChecker implements OnlineChecker {
    private final RegisteredServer server;
    private final Configuration configuration;

    public PingOnlineChecker(RegisteredServer server, Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> isOnline() {
        return PingUtils.pingUntilUp(server, configuration.getMaximumPingDuration()).thenAccept(ping -> {
        });
    }
}
