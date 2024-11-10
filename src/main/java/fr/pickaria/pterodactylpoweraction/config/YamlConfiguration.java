package fr.pickaria.pterodactylpoweraction.config;

import fr.pickaria.pterodactylpoweraction.APIType;
import fr.pickaria.pterodactylpoweraction.Configuration;
import org.jetbrains.annotations.NotNull;
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

    public YamlConfiguration(File file) throws FileNotFoundException {
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
    public String getPterodactylApiKey() {
        return (String) config.get("pterodactyl_api_key");
    }

    @Override
    public String getPterodactylClientApiBaseURL() {
        return (String) config.get("pterodactyl_client_api_base_url");
    }

    @Override
    public String getPterodactylServerIdentifier(String serverName) {
        Object configuration = getServerConfiguration(serverName);
        if (configuration instanceof String) {
            return (String) configuration;
        }
        throw new IllegalArgumentException("'servers." + serverName + "' must be of type String.");
    }

    @Override
    public String getWaitingServerName() {
        return (String) config.get("waiting_server_name");
    }

    @Override
    public Duration getMaximumPingDuration() {
        int seconds = (int) config.get("maximum_ping_duration");
        return Duration.ofSeconds(seconds);
    }

    @Override
    public Duration getShutdownAfterDuration() {
        int seconds = (int) config.get("shutdown_after_duration");
        return Duration.ofSeconds(seconds);
    }

    @Override
    public @NotNull PowerCommands getPowerCommands(String serverName) {
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

    private @NotNull Object getServerConfiguration(String serverName) throws NoSuchElementException {
        Map<String, Object> servers = (Map<String, Object>) config.get("servers");
        if (servers.containsKey(serverName)) {
            return servers.get(serverName);
        }
        throw new NoSuchElementException("Server " + serverName + " not found in the configuration");
    }
}
