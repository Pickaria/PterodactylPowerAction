package fr.pickaria.pterodactylpoweraction;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.pickaria.pterodactylpoweraction.config.YamlConfiguration;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

@Plugin(
        id = "pterodactyl_power_action",
        name = "PterodactylPowerAction",
        version = BuildConstants.VERSION,
        authors = {"Quozul"}
)
public class PterodactylPowerAction {
    private final ProxyServer proxy;
    private final Logger logger;
    private Configuration configuration;

    @Inject
    public PterodactylPowerAction(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = server;
        this.logger = logger;

        // Create the dataDirectory if it does not exist
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Error creating data directory", e);
            }
        }

        // Load the config.yml file from the dataDirectory
        File configurationFile = dataDirectory.resolve("config.yml").toFile();
        if (!configurationFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configurationFile.toPath());
                } else {
                    throw new FileNotFoundException("config.yml not found in resources");
                }
            } catch (IOException e) {
                logger.error("Error creating default configuration", e);
            }
        }

        try {
            this.configuration = new YamlConfiguration(configurationFile);
        } catch (FileNotFoundException e) {
            logger.error("Error loading configuration", e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("pickaria:power_action"));

        ResourceBundle bundle = ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.ENGLISH, UTF8ResourceBundleControl.get());
        registry.registerAll(Locale.FRENCH, bundle, true);
        registry.registerAll(Locale.ENGLISH, bundle, true);
        GlobalTranslator.translator().addSource(registry);

        try {
            ConnectionListener listener = new ConnectionListener(configuration, logger, proxy, this);
            proxy.getEventManager().register(this, listener);
        } catch (NoSuchElementException e) {
            logger.error("Error loading listener", e);
        }
    }
}
