package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import fr.pickaria.pterodactylpoweraction.configuration.ConfigurationLoader;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ShutdownManager {
    private static ShutdownManager instance;
    private final ProxyServer proxy;
    private final PterodactylPowerAction plugin;
    private final ConfigurationLoader configurationLoader;
    private final Map<String, ScheduledTask> shutdownTasks = new HashMap<>();
    private final Logger logger;

    public ShutdownManager(ProxyServer proxy, PterodactylPowerAction plugin, ConfigurationLoader configurationLoader, Logger logger) {
        assert instance == null; // Simply to make sure we only instantiate this class once
        instance = this;

        this.proxy = proxy;
        this.plugin = plugin;
        this.configurationLoader = configurationLoader;
        this.logger = logger;
    }

    /**
     * Start a task that will shut down the server after the configured delay.
     *
     * @param server            The server we want to shut down
     * @param playerIsConnected Set this to true if the player is currently connected to the server
     */
    public void shutdownServer(RegisteredServer server, boolean playerIsConnected) {
        int playerCount = server.getPlayersConnected().size();
        if (playerIsConnected) {
            playerCount--;
        }
        if (playerCount <= 0) {
            Configuration configuration = configurationLoader.getConfiguration();
            String currentServerName = server.getServerInfo().getName();
            // Make sure we don't stop the temporary server
            if (!currentServerName.equals(configuration.getWaitingServerName())) {
                // Cancel the previous task so we don't have conflicting tasks
                cancelTask(server);

                logger.info("Scheduling a stop task for server {} in {} seconds", currentServerName, configuration.getShutdownAfterDuration().getSeconds());
                Scheduler.TaskBuilder taskBuilder = proxy.getScheduler()
                        .buildTask(plugin, () -> configurationLoader.getAPI().stop(currentServerName))
                        .delay(configuration.getShutdownAfterDuration());
                ScheduledTask scheduledTask = taskBuilder.schedule();
                shutdownTasks.put(currentServerName, scheduledTask);
            }
        }
    }

    /**
     * Cancel any shutdown task for the requested server.
     *
     * @param server The server we don't want to stop anymore
     */
    public void cancelTask(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        if (shutdownTasks.containsKey(serverName)) {
            logger.info("Cancelling shutdown for server {}", serverName);
            shutdownTasks.get(serverName).cancel();
        }
    }
}
