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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
    public List<String> getStartCommands(String serverName) {
        return getPowerCommands(serverName).start;
    }

    @Override
    public List<String> getStopCommands(String serverName) {
        return getPowerCommands(serverName).stop;
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

    private @NotNull Object getServerConfiguration(String serverName) throws NoSuchElementException {
        Map<String, Object> servers = (Map<String, Object>) config.get("servers");
        if (servers.containsKey(serverName)) {
            return servers.get(serverName);
        }
        throw new NoSuchElementException("Server " + serverName + " not found in the configuration");
    }

    private @NotNull PowerCommands getPowerCommands(String serverName) {
        Map<String, Object> serverConfiguration = (Map<String, Object>) getServerConfiguration(serverName);

        if (!serverConfiguration.containsKey("start")) {
            throw new NoSuchElementException("'servers." + serverName + ".start' is missing from the configuration file");
        }

        if (!serverConfiguration.containsKey("stop")) {
            throw new NoSuchElementException("'servers." + serverName + ".stop' is missing from the configuration file");
        }

        List<String> startCommands = getCommands(serverConfiguration.get("start"));
        List<String> stopCommands = getCommands(serverConfiguration.get("stop"));

        return new PowerCommands(startCommands, stopCommands);
    }

    private @NotNull List<String> getCommands(Object commands) {
        if (commands instanceof List) {
            return (List<String>) commands;
        } else if (commands instanceof String) {
            List<String> list = new ArrayList<>();
            list.add((String) commands);
            return list;
        }
        throw new IllegalArgumentException("Power command type is not supported" + commands.getClass());
    }

    private record PowerCommands(List<String> start, List<String> stop) {
    }
}
