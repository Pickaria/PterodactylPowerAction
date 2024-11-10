package fr.pickaria.pterodactylpoweraction;

import java.time.Duration;
import java.util.Optional;

public interface Configuration {
    APIType getAPIType();

    String getPterodactylApiKey();

    String getPterodactylClientApiBaseURL();

    String getPterodactylServerIdentifier(String serverName);

    PowerCommands getPowerCommands(String serverName);

    String getWaitingServerName();

    Duration getMaximumPingDuration();

    Duration getShutdownAfterDuration();

    record PowerCommands(Optional<String> workingDirectory, String start, String stop) {
    }
}
