package fr.pickaria.pterodactylpoweraction.configuration;

import fr.pickaria.pterodactylpoweraction.Configuration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class YamlConfiguration implements Configuration {
    private final Map<String, Object> config;
    private final Logger logger;

    private static final int DEFAULT_SHUTDOWN_AFTER_DURATION = 3_600; // in seconds
    private static final int DEFAULT_MAXIMUM_PING_DURATION = 60; // in seconds
    private static final boolean DEFAULT_REDIRECT_TO_WAITING_SERVER_ON_KICK = false;

    public YamlConfiguration(File file, Logger logger) throws IOException {
        this.logger = logger;

        Yaml yaml = new Yaml();
        try (InputStream is = new FileInputStream(file)) {
            this.config = yaml.load(is);
        }
    }

    @Override
    public Map<String, Object> getRawConfig() {
        return config;
    }

    @Override
    public APIType getAPIType() throws IllegalArgumentException {
        String type = (String) config.get("type");
        return APIType.valueOf(type.toUpperCase());
    }

    @Override
    public ShutdownBehaviour getShutdownBehaviour() {
        try {
            String shutdownBehaviour = getConfigurationString("shutdown_behaviour");
            return ShutdownBehaviour.valueOf(shutdownBehaviour.toUpperCase());
        } catch (NoSuchElementException ignored) {
            return ShutdownBehaviour.SHUTDOWN_ALL;
        } catch (IllegalArgumentException | ClassCastException ignored) {
            logger.warn("Setting 'shutdown_behaviour' does not have a valid value.");
            return ShutdownBehaviour.SHUTDOWN_ALL;
        }
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
    public Set<String> getAllServers() {
        return getServerMap().keySet();
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

    private Map<String, Object> getServerMap() {
        return (Map<String, Object>) config.get("servers");
    }

    private @NotNull Object getServerConfiguration(String serverName) throws NoSuchElementException {
        Map<String, Object> servers = getServerMap();
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
