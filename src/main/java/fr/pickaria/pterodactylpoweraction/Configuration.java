package fr.pickaria.pterodactylpoweraction;

import java.time.Duration;

public interface Configuration {
    String getPterodactylApiKey();

    String getPterodactylClientApiBaseURL();

    String getPterodactylServerIdentifier(String serverName);

    String getWaitingServerName();

    Duration getMaximumPingDuration();

    Duration getShutdownAfterDuration();
}
