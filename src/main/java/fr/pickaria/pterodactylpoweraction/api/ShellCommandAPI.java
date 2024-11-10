package fr.pickaria.pterodactylpoweraction.api;

import fr.pickaria.pterodactylpoweraction.Configuration;
import fr.pickaria.pterodactylpoweraction.PowerActionAPI;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
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
        Configuration.PowerCommands powerCommands = configuration.getPowerCommands(server);
        return runCommands(powerCommands.workingDirectory(), powerCommands.stop());
    }

    @Override
    public CompletableFuture<Void> start(String server) {
        logger.info("Starting server {}", server);
        Configuration.PowerCommands powerCommands = configuration.getPowerCommands(server);
        return runCommands(powerCommands.workingDirectory(), powerCommands.start());
    }

    private CompletableFuture<Void> runCommands(Optional<String> workingDirectory, String command) {
        return CompletableFuture.runAsync(() -> {
            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            workingDirectory.ifPresent((value) -> pb.directory(new File(value)));
            try {
                pb.start().onExit().get();
            } catch (IOException | ExecutionException | InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
}
