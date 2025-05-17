package fr.pickaria.pterodactylpoweraction;

import java.util.concurrent.CompletableFuture;

public interface OnlineChecker {
    CompletableFuture<Void> waitForRunning();

    boolean isRunningNow();
}
