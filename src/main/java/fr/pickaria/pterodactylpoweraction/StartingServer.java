package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class StartingServer implements ForwardingAudience {
    private final RegisteredServer server;
    private final PowerActionAPI api;
    private final Configuration configuration;
    private final ShutdownManager shutdownManager;
    private final Set<Player> waitingPlayers = new HashSet<>();
    private final Logger logger;
    private boolean isStarting = false;

    public StartingServer(RegisteredServer server, PowerActionAPI api, Configuration configuration, ShutdownManager shutdownManager, Logger logger) {
        this.server = server;
        this.api = api;
        this.configuration = configuration;
        this.shutdownManager = shutdownManager;
        this.logger = logger;
    }

    /**
     * Add a player then start the server if required.
     * If the server is already in a starting state, the player will be redirected alongside the other waiting players.
     *
     * @param player Player to add to the waiting room
     * @return `true` if the player has been added to the waiting list
     */
    public boolean addPlayer(Player player) {
        boolean added = waitingPlayers.add(player);

        if (!isStarting) {
            isStarting = true;
            String serverName = server.getServerInfo().getName();
            api.start(serverName).whenComplete((result, exception) -> {
                if (exception == null) {
                    pingUntilUpAndRedirectPlayers();
                } else {
                    informError(exception);
                }
            });
        }

        return added;
    }

    private void pingUntilUpAndRedirectPlayers() {
        boolean hasRedirectedAtLeastOnePlayer = false;

        try {
            PingUtils.pingUntilUp(server, configuration.getMaximumPingDuration()).get();
            for (Player player : waitingPlayers) {
                if (player.isActive()) {
                    player.createConnectionRequest(server).connect().get();
                    hasRedirectedAtLeastOnePlayer = true;
                }
            }

            if (!hasRedirectedAtLeastOnePlayer) {
                // If we haven't redirected a single player, check if we can stop the server again
                shutdownManager.shutdownServer(server, false);
            }
        } catch (CancellationException | ExecutionException | InterruptedException exception) {
            informError(exception);
        } finally {
            isStarting = false;
            waitingPlayers.clear();
        }
    }

    private void informError(Throwable throwable) {
        String serverName = server.getServerInfo().getName();
        logger.error("An error occurred while starting the server {}", serverName, throwable);
        Component message = Component.translatable("failed.to.start.server", Component.text(serverName, NamedTextColor.GRAY)).color(NamedTextColor.RED);
        sendMessage(message);
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return waitingPlayers;
    }
}
