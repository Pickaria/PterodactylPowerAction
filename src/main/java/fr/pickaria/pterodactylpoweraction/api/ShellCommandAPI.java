package fr.pickaria.pterodactylpoweraction.api;

import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.PowerActionAPI;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ShellCommandAPI implements PowerActionAPI {
    private final Logger logger;
    private final Configuration configuration;

    public ShellCommandAPI(Logger logger, Configuration configuration) {
        this.logger = logger;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> stop(String server) {
        logger.info("Stopping server {}", server);
        List<String> commands = configuration.getStopCommands(server);
        return runCommands(commands);
    }

    @Override
    public CompletableFuture<Void> start(String server) {
        logger.info("Starting server {}", server);
        List<String> commands = configuration.getStartCommands(server);
        return runCommands(commands);
    }

    private CompletableFuture<Void> runCommands(List<String> commands) {
        return CompletableFuture.runAsync(() -> {
            for (String command : commands) {
                logger.info("Executing command: {}", command);
                ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                try {
                    Process process = pb.start().onExit().get();
                    String finalString = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    logger.info("Command result: {}", finalString);
                } catch (IOException | ExecutionException | InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
