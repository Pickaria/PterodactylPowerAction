package fr.pickaria.pterodactylpoweraction;

import java.time.Duration;
import java.util.List;

public interface Configuration {
    APIType getAPIType();

    String getPterodactylApiKey();

    String getPterodactylClientApiBaseURL();

    String getPterodactylServerIdentifier(String serverName);

    List<String> getStartCommands(String serverName);

    List<String> getStopCommands(String serverName);

    String getWaitingServerName();

    Duration getMaximumPingDuration();

    Duration getShutdownAfterDuration();
}
