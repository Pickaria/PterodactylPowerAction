package fr.pickaria.pterodactylpoweraction;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;

import java.util.HashMap;
import java.util.Map;

public class ShutdownManager {
    private static ShutdownManager instance;
    private final ProxyServer proxy;
    private final PterodactylPowerAction plugin;
    private final PowerActionAPI api;
    private final Configuration configuration;
    private final Map<String, ScheduledTask> shutdownTasks = new HashMap<>();

    public ShutdownManager(ProxyServer proxy, PterodactylPowerAction plugin, PowerActionAPI api, Configuration configuration) {
        assert instance == null; // Simply to make sure we only instantiate this class once
        instance = this;

        this.proxy = proxy;
        this.plugin = plugin;
        this.api = api;
        this.configuration = configuration;
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

    /**
     * Cancel any shutdown task for the requested server.
     *
     * @param server The server we don't want to stop anymore
     */
    public void cancelTask(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        if (shutdownTasks.containsKey(serverName)) {
            shutdownTasks.get(serverName).cancel();
        }
    }
}
