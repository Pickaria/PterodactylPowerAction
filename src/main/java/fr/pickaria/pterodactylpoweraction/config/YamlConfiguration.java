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
    public @NotNull String getPterodactylApiKey() throws NoSuchElementException, ClassCastException {
        return getConfigurationString("pterodactyl_api_key");
    }

    @Override
    public @NotNull String getPterodactylClientApiBaseURL() throws NoSuchElementException, ClassCastException {
        return removeTrailingSlash(getConfigurationString("pterodactyl_client_api_base_url"));
    }

    @Override
    public String getPterodactylServerIdentifier(String serverName) throws IllegalArgumentException, NoSuchElementException {
        Object configuration = getServerConfiguration(serverName);
        if (configuration instanceof String) {
            return (String) configuration;
        }
        throw new IllegalArgumentException("'servers." + serverName + "' must be of type String");
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
    public @NotNull PowerCommands getPowerCommands(String serverName) throws NoSuchElementException {
        Map<String, Object> serverConfiguration = (Map<String, Object>) getServerConfiguration(serverName);

        if (!serverConfiguration.containsKey("start")) {
            throw new NoSuchElementException("'servers." + serverName + ".start' is missing from the configuration file");
        }

        if (!serverConfiguration.containsKey("stop")) {
            throw new NoSuchElementException("'servers." + serverName + ".stop' is missing from the configuration file");
        }

        Optional<String> workingDirectory = Optional.ofNullable((String) serverConfiguration.get("working_directory"));
        String startCommands = (String) serverConfiguration.get("start");
        String stopCommands = (String) serverConfiguration.get("stop");

        return new PowerCommands(workingDirectory, startCommands, stopCommands);
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
            String apiKey = getPterodactylApiKey();
            if (!apiKey.startsWith("ptlc_")) {
                logger.error("Invalid API key. Please create an API Key from your account's page.");
                isValid = false;
            }
        }

        // Validate waiting server configuration
        String waitingServerName = getWaitingServerName();
        Optional<RegisteredServer> registeredServer = proxy.getServer(waitingServerName);

        if (registeredServer.isEmpty()) {
            logger.error("Waiting server '{}' not configured in 'velocity.toml'.", waitingServerName);
            isValid = false;
        } else if (!PingUtils.isReachable(registeredServer.get())) {
            logger.error("Waiting server '{}' is not reachable.", waitingServerName);
            isValid = false;
        }

        // Warn if waiting server is misconfigured in the plugin's own config
        if (config.containsKey("servers")) {
            Map<String, Object> servers = (Map<String, Object>) config.get("servers");
            if (servers.containsKey(waitingServerName)) {
                logger.warn("Waiting server '{}' should not be configured in the plugin's configuration.", waitingServerName);
            }
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

    private static @NotNull String removeTrailingSlash(@NotNull String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
