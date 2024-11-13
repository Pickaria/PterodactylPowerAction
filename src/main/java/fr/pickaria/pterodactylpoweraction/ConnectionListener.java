package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConnectionListener {
    private final Logger logger;
    private final PowerActionAPI api;
    private final RegisteredServer waitingServer;
    private final Configuration configuration;
    private final Map<String, StartingServer> startingServers = new HashMap<>();
    private final ShutdownManager shutdownManager;

    ConnectionListener(
            Configuration configuration,
            ProxyServer proxy,
            Logger logger,
            PowerActionAPI api,
            ShutdownManager shutdownManager
    ) {
        this.configuration = configuration;
        this.logger = logger;
        this.api = api;
        this.shutdownManager = shutdownManager;

        String waitingServerName = configuration.getWaitingServerName();
        Optional<RegisteredServer> server = proxy.getServer(waitingServerName);
        if (server.isPresent()) {
            this.waitingServer = server.get();
        } else {
            throw new RuntimeException("The configured temporary server '" + waitingServerName + "' is not configured in Velocity. Please check your velocity configuration.");
        }
    }

    @Subscribe()
    public void onServerConnected(ServerConnectedEvent event) {
        logger.debug("Player {} connected", event.getPlayer().getUsername());
        Optional<RegisteredServer> previousServer = event.getPreviousServer();
        // Check if we can shut down the previous server once the player has been redirected
        // This applies to redirection if the server is already running
        // and the automatic redirection after a server has been started
        previousServer.ifPresent(server -> shutdownManager.shutdownServer(server, false));
    }

    @Subscribe()
    public void onServerPreConnect(ServerPreConnectEvent event) {
        RegisteredServer originalServer = event.getOriginalServer();
        RegisteredServer previousServer = event.getPreviousServer();
        CompletableFuture<ServerPing> pingCompletableFuture = originalServer.ping();

        shutdownManager.cancelTask(originalServer);

        try {
            pingCompletableFuture.get();
            // Server pinged successfully, we can connect the player to this server
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(originalServer));
        } catch (ExecutionException exception) {
            boolean isAlreadyConnected = previousServer != null;
            if (isAlreadyConnected) {
                // If the player is already connected on the network, we don't want to redirect it to the waiting server
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            } else {
                // Server is not running, inform the player and redirect somewhere else
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(this.waitingServer));
            }

            String originalServerName = originalServer.getServerInfo().getName();

            // This is cached so that we don't ping the same server for every player that is waiting for it to start
            if (startingServers.containsKey(originalServerName)) {
                startingServers.get(originalServerName).addPlayer(event.getPlayer());
            } else {
                StartingServer startingServer = new StartingServer(originalServer, api, configuration, shutdownManager);
                startingServer.addPlayer(event.getPlayer());
                startingServers.put(originalServerName, startingServer);
                // TODO: Should we clear the entry from the map once the server is started?
            }

            Component message = Component.translatable("starting.server", Component.text(originalServerName, NamedTextColor.GOLD)).color(NamedTextColor.GRAY);
            event.getPlayer().sendMessage(message);
        } catch (CancellationException | InterruptedException exception) {
            // Something else bad has happened
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe()
    public void onDisconnect(DisconnectEvent event) {
        logger.debug("Player {} disconnected", event.getPlayer().getUsername());
        stopServer(event.getPlayer());
    }

    @Subscribe()
    public void onKicked(KickedFromServerEvent event) {
        logger.debug("Player {} got kicked", event.getPlayer().getUsername());
        stopServer(event.getPlayer());
    }

    private void stopServer(Player player) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isPresent()) {
            RegisteredServer currentServer = serverConnection.get().getServer();
            logger.debug("Trying to stop server {}", currentServer.getServerInfo().getName());
            shutdownManager.shutdownServer(currentServer, true);
        }
    }
}
