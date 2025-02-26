package fr.pickaria.pterodactylpoweraction.config;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.pickaria.pterodactylpoweraction.APIType;
import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.PingUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class YamlConfiguration implements Configuration {
    private final Map<String, Object> config;
    private final Logger logger;

    private static final int DEFAULT_SHUTDOWN_AFTER_DURATION = 3_600; // in seconds
    private static final int DEFAULT_MAXIMUM_PING_DURATION = 60; // in seconds
    private static final boolean DEFAULT_REDIRECT_TO_WAITING_SERVER_ON_KICK = false;

    public YamlConfiguration(File file, Logger logger) throws FileNotFoundException {
        this.logger = logger;

        Yaml yaml = new Yaml();
        InputStream is = new FileInputStream(file);
        this.config = yaml.load(is);
    }

    @Override
    public APIType getAPIType() throws IllegalArgumentException {
        String type = (String) config.get("type");
        return APIType.valueOf(type.toUpperCase());
    }

    @Override
    public Optional<String> getPterodactylApiKey() {
        try {
            return Optional.of(getConfigurationString("pterodactyl_api_key"));
        } catch (NoSuchElementException | ClassCastException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getPterodactylClientApiBaseURL() {
        try {
            return Optional.of(removeTrailingSlash(getConfigurationString("pterodactyl_client_api_base_url")));
        } catch (NoSuchElementException | ClassCastException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getPterodactylServerIdentifier(String serverName) {
        try {
            Object configuration = getServerConfiguration(serverName);
            if (configuration instanceof String) {
                return Optional.of((String) configuration);
            }
        } catch (NoSuchElementException e) {
            // Fall through to return empty
        }
        return Optional.empty();
    }

    @Override
    public @NotNull String getWaitingServerName() throws NoSuchElementException, ClassCastException {
        return getConfigurationString("waiting_server_name");
    }

    @Override
    public Duration getMaximumPingDuration() {
        int seconds = (int) config.getOrDefault("maximum_ping_duration", DEFAULT_MAXIMUM_PING_DURATION);
        return Duration.ofSeconds(seconds);
    }

    @Override
    public Duration getShutdownAfterDuration() {
        int seconds = (int) config.getOrDefault("shutdown_after_duration", DEFAULT_SHUTDOWN_AFTER_DURATION);
        return Duration.ofSeconds(seconds);
    }

    @Override
    public boolean getRedirectToWaitingServerOnKick() {
        String key = "redirect_to_waiting_server_on_kick";
        return config.containsKey(key) ? (boolean) config.get(key) : DEFAULT_REDIRECT_TO_WAITING_SERVER_ON_KICK;
    }

    @Override
    public Optional<PowerCommands> getPowerCommands(String serverName) {
        try {
            Map<String, Object> serverConfiguration = (Map<String, Object>) getServerConfiguration(serverName);

            if (!serverConfiguration.containsKey("start")) {
                logger.error("'servers.{}.start' is missing from the configuration file", serverName);
                return Optional.empty();
            }

            if (!serverConfiguration.containsKey("stop")) {
                logger.error("'servers.{}.stop' is missing from the configuration file", serverName);
                return Optional.empty();
            }

            Optional<String> workingDirectory = Optional.ofNullable((String) serverConfiguration.get("working_directory"));
            String startCommands = (String) serverConfiguration.get("start");
            String stopCommands = (String) serverConfiguration.get("stop");

            return Optional.of(new PowerCommands(workingDirectory, startCommands, stopCommands));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean validateConfig(ProxyServer proxy) {
        boolean isValid = true;

        // Validate API-specific configuration
        if (getAPIType() == APIType.PTERODACTYL) {
            if (!config.containsKey("pterodactyl_client_api_base_url")) {
                logger.error("'pterodactyl_client_api_base_url' is missing but required when type is 'pterodactyl'.");
                isValid = false;
            }
            Optional<String> apiKeyOpt = getPterodactylApiKey();
            if (apiKeyOpt.isEmpty() || !apiKeyOpt.get().startsWith("ptlc_")) {
                logger.error("Invalid API key. Please create an API Key from your account's page.");
                isValid = false;
            }
        }

        // Validate waiting server configuration
        String waitingServerName = getWaitingServerName();
        Optional<RegisteredServer> registeredWaitingServer = proxy.getServer(waitingServerName);

        if (registeredWaitingServer.isEmpty()) {
            logger.error("Waiting server '{}' is not configured in 'velocity.toml'.", waitingServerName);
            isValid = false;
        } else if (!PingUtils.isReachable(registeredWaitingServer.get())) {
            logger.error("Waiting server '{}' is not reachable.", waitingServerName);
            isValid = false;
        }

        // Warn if waiting server is misconfigured in the plugin's own config
        if (config.containsKey("servers")) {
            Map<String, Object> servers = (Map<String, Object>) config.get("servers");
            if (servers.containsKey(waitingServerName)) {
                logger.warn("Waiting server '{}' should not be configured in the plugin's configuration.", waitingServerName);
                servers.remove(waitingServerName);
            }

            servers.forEach((key, value) -> {
                if (proxy.getServer(key).isEmpty()) {
                    logger.warn("The server '{}' is missing in 'velocity.toml'.", key);
                }

                if (getAPIType() == APIType.PTERODACTYL) {
                    String uuid = (String) value;
                    if (!this.isUUID(uuid)) {
                        logger.warn("The identifier '{}' for server '{}' must be a valid UUID. You can find the 'Server ID' under the 'Settings' tab of your server on your Pterodactyl panel.", uuid, key);
                    }
                }
            });
        }

        // Warn about missing optional configurations
        if (!config.containsKey("maximum_ping_duration")) {
            logger.warn("'maximum_ping_duration' is not provided, using default value of '{}'.", DEFAULT_MAXIMUM_PING_DURATION);
        }
        if (!config.containsKey("shutdown_after_duration")) {
            logger.warn("'shutdown_after_duration' is not provided, using default value of '{}'.", DEFAULT_SHUTDOWN_AFTER_DURATION);
        }
        if (!config.containsKey("redirect_to_waiting_server_on_kick")) {
            logger.warn("'redirect_to_waiting_server_on_kick' is not provided, using default value of '{}'.", DEFAULT_REDIRECT_TO_WAITING_SERVER_ON_KICK);
        }

        return isValid;
    }

    private @NotNull Object getServerConfiguration(String serverName) throws NoSuchElementException {
        Map<String, Object> servers = (Map<String, Object>) config.get("servers");
        if (servers.containsKey(serverName)) {
            return servers.get(serverName);
        }
        throw new NoSuchElementException("Server " + serverName + " not found in the configuration");
    }

    private @NotNull String getConfigurationString(String key) throws NoSuchElementException, ClassCastException {
        Object configValue = config.get(key);
        if (configValue == null) {
            throw new NoSuchElementException("Configuration property " + key + " not found in the configuration");
        }
        if (configValue instanceof String) {
            return (String) configValue;
        }
        throw new ClassCastException(key + " must be of type String");
    }

    private boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static @NotNull String removeTrailingSlash(@NotNull String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
