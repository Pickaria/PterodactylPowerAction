package fr.pickaria.pterodactylpoweraction;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface Configuration {
    Map<String, Object> getRawConfig();

    APIType getAPIType();

    Optional<String> getPterodactylApiKey();

    Optional<String> getPterodactylClientApiBaseURL();

    Optional<String> getPterodactylServerIdentifier(String serverName);

    Optional<PowerCommands> getPowerCommands(String serverName);

    String getWaitingServerName();

    Duration getMaximumPingDuration();

    Duration getShutdownAfterDuration();

    boolean getRedirectToWaitingServerOnKick();

    record PowerCommands(Optional<String> workingDirectory, String start, String stop) {
    }
}
