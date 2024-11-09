package fr.pickaria.pterodactylpoweraction.config;

import fr.pickaria.pterodactylpoweraction.Configuration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public class YamlConfiguration implements Configuration {
    private final Map<String, Object> config;

    public YamlConfiguration(File file) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream is = new FileInputStream(file);
        this.config = yaml.load(is);
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
        Map<String, Object> servers = (Map<String, Object>) config.get("servers");
        return (String) servers.get(serverName);
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
}