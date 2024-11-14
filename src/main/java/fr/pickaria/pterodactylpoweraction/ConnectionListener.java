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
import fr.pickaria.messager.MessageComponent;
import fr.pickaria.messager.MessageType;
import fr.pickaria.messager.Messager;
import fr.pickaria.messager.components.Text;
import fr.pickaria.pterodactylpoweraction.component.RunCommand;
import net.kyori.adventure.text.Component;
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
    private final Messager messager;

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
        this.messager = new Messager();

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
            boolean playerAddedToWaitingList;

            // This is cached so that we don't ping the same server for every player that is waiting for it to start
            if (startingServers.containsKey(originalServerName)) {
                playerAddedToWaitingList = startingServers.get(originalServerName).addPlayer(event.getPlayer());
            } else {
                StartingServer startingServer = new StartingServer(originalServer, api, configuration, shutdownManager);
                playerAddedToWaitingList = startingServer.addPlayer(event.getPlayer());
                startingServers.put(originalServerName, startingServer);
                // TODO: Should we clear the entry from the map once the server is started?
            }

            if (playerAddedToWaitingList) {
                Component message = messager.format(MessageType.INFO, "starting.server", new Text(Component.text(originalServerName)));
                event.getPlayer().sendMessage(message);
            }
        } catch (CancellationException | InterruptedException exception) {
            // Something else bad has happened
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe()
    public void onDisconnect(DisconnectEvent event) {
        stopServer(event.getPlayer());
    }

    @Subscribe()
    public void onKicked(KickedFromServerEvent event) {
        stopServer(event.getPlayer());

        // Redirect the player to the waiting server
        if (configuration.getRedirectToWaitingServerOnKick() && event.getServer() != this.waitingServer) {
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(waitingServer, getKickReasonMessage(event)));
        }
    }

    private Component getKickReasonMessage(KickedFromServerEvent event) {
        Optional<Component> serverKickReason = event.getServerKickReason();
        String serverName = event.getServer().getServerInfo().getName();
        String serverCommand = "/server " + serverName;
        Component serverNameComponent = Component.text(serverName);
        MessageComponent goBack = new RunCommand(serverCommand, Component.translatable("go.back.command", serverNameComponent));
        return serverKickReason.map(component -> messager.format(MessageType.INFO, "kick.reason.message", new Text(serverNameComponent), new Text(component), goBack))
                .orElseGet(() -> messager.format(MessageType.INFO, "kick.generic.message", new Text(serverNameComponent), goBack));
    }

    private void stopServer(Player player) {
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isPresent()) {
            RegisteredServer currentServer = serverConnection.get().getServer();
            shutdownManager.shutdownServer(currentServer, true);
        }
    }
}
