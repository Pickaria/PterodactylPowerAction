package fr.pickaria.pterodactylpoweraction.configuration;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.PingUtils;
import fr.pickaria.pterodactylpoweraction.api.PterodactylAPI;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ConfigurationDoctor {
    private final ProxyServer proxy;
    private final Logger logger;

    public ConfigurationDoctor(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    public void validateConfig(Configuration configuration) {
        boolean isValid = true;
        Map<String, Object> config = configuration.getRawConfig();
        APIType apiType = configuration.getAPIType();
        PterodactylAPI pterodactylAPI = new PterodactylAPI(logger, configuration);

        // Validate API-specific configuration
        if (apiType == APIType.PTERODACTYL) {
            if (!config.containsKey("pterodactyl_client_api_base_url")) {
                logger.error("'pterodactyl_client_api_base_url' is missing but required when type is 'pterodactyl'.");
                isValid = false;
            }
            Optional<String> apiKeyOpt = configuration.getPterodactylApiKey();
            if (apiKeyOpt.isEmpty() || !apiKeyOpt.get().startsWith("ptlc_")) {
                logger.error("Invalid API key. Please create an API Key from your account's page.");
                isValid = false;
            }
        }

        // Validate waiting server configuration
        String waitingServerName = configuration.getWaitingServerName();
        Optional<RegisteredServer> registeredWaitingServer = proxy.getServer(waitingServerName);

        if (registeredWaitingServer.isEmpty()) {
            logger.warn("Waiting server '{}' is not configured in 'velocity.toml'.", waitingServerName);
            isValid = false;
        } else if (!PingUtils.isReachable(registeredWaitingServer.get())) {
            logger.warn("Waiting server '{}' is not reachable. Make sure it is always running and accessible.", waitingServerName);
            isValid = false;
        }

        // Warn if waiting server is misconfigured in the plugin's own config
        if (config.containsKey("servers")) {
            Object serversObject = config.get("servers");
            if (serversObject instanceof Map) {
                Map<String, Object> servers = (Map<String, Object>) serversObject;
                if (servers.containsKey(waitingServerName)) {
                    logger.warn("Waiting server '{}' should not be configured in the plugin's configuration. It will be ignored.", waitingServerName);
                    isValid = false;
                }

                for (Map.Entry<String, Object> entry : servers.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (proxy.getServer(key).isEmpty()) {
                        logger.warn("The server '{}' is missing in 'velocity.toml'.", key);
                        isValid = false;
                    }

                    if (apiType == APIType.PTERODACTYL) {
                        if (value instanceof String uuid) {
                            if (!this.isUUID(uuid)) {
                                logger.warn("The identifier '{}' for server '{}' must be a valid UUID. You can find the 'Server ID' under the 'Settings' tab of your server on your Pterodactyl panel.", uuid, key);
                                isValid = false;
                            } else {
                                try {
                                    Boolean exists = pterodactylAPI.exists(key).get();
                                    if (!exists) {
                                        logger.warn("Server '{}' does not exist on Pterodactyl panel, you don't have access to it or your token is invalid.", key);
                                        isValid = false;
                                    }
                                } catch (ExecutionException | InterruptedException e) {
                                    logger.warn("An error occurred when trying to get the server '{}'.", key, e);
                                    isValid = false;
                                }
                            }
                        } else {
                            logger.warn("The server entry must be a string when type is 'pterodactyl'.");
                            isValid = false;
                        }
                    } else if (apiType == APIType.SHELL) {
                        if (value instanceof Map) {
                            Map<String, Object> powerCommands = (Map<String, Object>) value;
                            if (!powerCommands.containsKey("start")) {
                                logger.warn("'start' command for server '{}' is missing but required when type is 'shell'.", key);
                                isValid = false;
                            }
                            if (!powerCommands.containsKey("stop")) {
                                logger.warn("'stop' command for server '{}' is missing but required when type is 'shell'.", key);
                                isValid = false;
                            }
                            if (powerCommands.containsKey("working_directory")) {
                                String workingDirectory = (String) powerCommands.get("working_directory");
                                Path workingDirectoryPath = Paths.get(workingDirectory);
                                if (!Files.exists(workingDirectoryPath)) {
                                    logger.warn("The working directory specified for server '{}' does not exist.", key);
                                    isValid = false;
                                }
                            }
                        } else {
                            logger.warn("The server entry must be a map when type is 'shell'.");
                            isValid = false;
                        }
                    }
                }
            } else {
                logger.warn("The 'servers' property must be a map.");
                isValid = false;
            }
        }

        // Warn about missing optional configurations
        if (!config.containsKey("maximum_ping_duration")) {
            logger.warn("'maximum_ping_duration' is not provided, using the default value.");
            isValid = false;
        }
        if (!config.containsKey("shutdown_after_duration")) {
            logger.warn("'shutdown_after_duration' is not provided, using the default value.");
            isValid = false;
        }
        if (!config.containsKey("redirect_to_waiting_server_on_kick")) {
            logger.warn("'redirect_to_waiting_server_on_kick' is not provided, using the default value.");
            isValid = false;
        }

        if (isValid) {
            logger.info("Your configuration looks good!");
        }
    }

    private boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
