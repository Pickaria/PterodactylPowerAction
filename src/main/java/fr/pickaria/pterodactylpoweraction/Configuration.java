package fr.pickaria.pterodactylpoweraction;

import fr.pickaria.pterodactylpoweraction.configuration.APIType;
import fr.pickaria.pterodactylpoweraction.configuration.ShutdownBehaviour;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Configuration {
    Map<String, Object> getRawConfig();

    APIType getAPIType();

    ShutdownBehaviour getShutdownBehaviour();

    Optional<String> getPterodactylApiKey();

    Optional<String> getPterodactylClientApiBaseURL();

    Optional<String> getPterodactylServerIdentifier(String serverName);

    Optional<PowerCommands> getPowerCommands(String serverName);

    String getWaitingServerName();

    Duration getMaximumPingDuration();

    Duration getShutdownAfterDuration();

    boolean getRedirectToWaitingServerOnKick();

    Set<String> getAllServers();

    record PowerCommands(Optional<String> workingDirectory, String start, String stop) {
    }
}
