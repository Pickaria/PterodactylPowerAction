package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ConnectionListener {
    private final Logger logger;
    private final ProxyServer proxy;
    private final PterodactylPowerAction plugin;
    private final PowerActionAPI api;
    private final RegisteredServer waitingServer;
    private final Configuration configuration;
    private final Map<String, ScheduledTask> shutdownTasks = new HashMap<>();

    ConnectionListener(
            Configuration configuration,
            Logger logger,
            ProxyServer proxy,
            PterodactylPowerAction plugin,
            PowerActionAPI api
    ) {
        this.logger = logger;
        this.proxy = proxy;
        this.plugin = plugin;
        this.configuration = configuration;
        this.api = api;

        String waitingServerName = configuration.getWaitingServerName();
        Optional<RegisteredServer> server = proxy.getServer(waitingServerName);
        if (server.isPresent()) {
            this.waitingServer = server.get();
        } else {
            throw new RuntimeException("The configured temporary server '" + waitingServerName + "' is not configured in Velocity. Please check your velocity configuration.");
        }

        logger.info("Connection listeners registered.");
    }

    @Subscribe()
    public void onServerConnected(ServerConnectedEvent event) {
        Optional<RegisteredServer> previousServer = event.getPreviousServer();
        // Check if we can shut down the previous server once the player has been redirected
        // This applies to redirection if the server is already online
        // and redirection after a server has been started
        previousServer.ifPresent(server -> shutdownServer(server, false));
    }

    @Subscribe(order = PostOrder.CUSTOM, priority = Short.MIN_VALUE)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        RegisteredServer originalServer = event.getOriginalServer();
        RegisteredServer previousServer = event.getPreviousServer();
        String originalServerName = originalServer.getServerInfo().getName();
        CompletableFuture<ServerPing> pingCompletableFuture = originalServer.ping();

        // Cancel any shutdown task for the requested server
        if (shutdownTasks.containsKey(originalServerName)) {
            shutdownTasks.get(originalServerName).cancel();
        }

        try {
            pingCompletableFuture.get();
            // Server pinged successfully, we can connect the player to this server
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(originalServer));
        } catch (ExecutionException exception) {
            // Server is not running, inform the player and redirect somewhere else
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            boolean isAlreadyConnected = previousServer != null;
            if (isAlreadyConnected) {
                // If the player is already connected, we don't want to redirect it to the waiting server
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            } else {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(this.waitingServer));
            }
            api.start(originalServerName).thenAccept((started) -> redirectPlayer(event.getPlayer(), originalServer));

            Component message = Component.translatable("starting.server", Component.text(originalServerName, NamedTextColor.GOLD)).color(NamedTextColor.GRAY);
            event.getPlayer().sendMessage(message);
        } catch (CancellationException | InterruptedException exception) {
            // Something else bad has happened
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe(order = PostOrder.CUSTOM, priority = Short.MIN_VALUE)
    public void onDisconnect(DisconnectEvent event) {
        Optional<ServerConnection> serverConnection = event.getPlayer().getCurrentServer();
        if (serverConnection.isPresent()) {
            RegisteredServer currentServer = serverConnection.get().getServer();
            shutdownServer(currentServer, true);
        }
    }

    /**
     * Start a task that will shut down the server after the configured delay.
     *
     * @param server            The server we want to shut down
     * @param playerIsConnected Set this to true if the player is currently connected to the server
     */
    private void shutdownServer(RegisteredServer server, boolean playerIsConnected) {
        int playerCount = server.getPlayersConnected().size();
        if (playerIsConnected) {
            playerCount--;
        }
        if (playerCount <= 0) {
            String currentServerName = server.getServerInfo().getName();
            // Make sure we don't stop the temporary server
            if (!currentServerName.equals(configuration.getWaitingServerName())) {
                Scheduler.TaskBuilder taskBuilder = proxy.getScheduler()
                        .buildTask(plugin, () -> api.stop(currentServerName))
                        .delay(configuration.getShutdownAfterDuration());
                ScheduledTask scheduledTask = taskBuilder.schedule();
                shutdownTasks.put(currentServerName, scheduledTask);
            }
        }
    }

    private CompletableFuture<ServerPing> pingUntilUp(RegisteredServer server, Duration maxPingDuration) {
        CompletableFuture<ServerPing> future = new CompletableFuture<>();
        Instant start = Instant.now();

        CompletableFuture.runAsync(() -> {
            while (Instant.now().isBefore(start.plus(maxPingDuration))) {
                try {
                    // Block and wait for the ping to complete
                    ServerPing serverPing = server.ping().get();
                    // Server pinged successfully, we can connect the player to this server
                    future.complete(serverPing);
                    return;
                } catch (InterruptedException | ExecutionException e) {
                    // Ping failed or interrupted, wait for a bit before retrying
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            // Max ping duration exceeded without successful ping
            future.completeExceptionally(new TimeoutException("Max ping duration exceeded"));
        });

        return future;
    }

    private void redirectPlayer(Player player, RegisteredServer server) {
        try {
            pingUntilUp(server, configuration.getMaximumPingDuration()).get();
            if (player.isActive()) {
                player.createConnectionRequest(server).connect().get();
            } else {
                // If the player is no longer online, check if we can stop the server again
                shutdownServer(server, false);
            }
        } catch (CancellationException | ExecutionException | InterruptedException exception) {
            Component message = Component.translatable("failed.to.start.server", Component.text(server.getServerInfo().getName(), NamedTextColor.GRAY)).color(NamedTextColor.RED);
            player.sendMessage(message);
        }
    }
}
