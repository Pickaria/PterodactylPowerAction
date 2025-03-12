package fr.pickaria.pterodactylpoweraction;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.pickaria.pterodactylpoweraction.api.PterodactylAPI;
import fr.pickaria.pterodactylpoweraction.api.ShellCommandAPI;
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
            this.configuration = new YamlConfiguration(configurationFile, logger);
        } catch (FileNotFoundException e) {
            logger.error("Error loading configuration", e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (!this.configuration.validateConfig(this.proxy)) {
            logger.error("The configuration file is not valid. Aborting plugin initialization. Please review above errors.");
            return;
        }

        initializeTranslator(
                ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.FRENCH, UTF8ResourceBundleControl.get()),
                ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.ENGLISH, UTF8ResourceBundleControl.get()),
                ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.GERMAN, UTF8ResourceBundleControl.get())
        );

        try {
            PowerActionAPI api = initializeAPI();
            ShutdownManager shutdownManager = new ShutdownManager(proxy, this, api, configuration, logger);
            ConnectionListener listener = new ConnectionListener(configuration, proxy, logger, api, shutdownManager);
            proxy.getEventManager().register(this, listener);
        } catch (NoSuchElementException e) {
            logger.error("Error loading listener", e);
        } catch (IllegalArgumentException e) {
            logger.error("Cannot load the configuration file", e);
        }
    }

    private PowerActionAPI initializeAPI() throws IllegalArgumentException {
        if (configuration.getAPIType() == APIType.PTERODACTYL) {
            return new PterodactylAPI(logger, configuration);
        }
        if (configuration.getAPIType() == APIType.SHELL) {
            return new ShellCommandAPI(logger, configuration);
        }
        throw new IllegalArgumentException("Unsupported API type: " + configuration.getAPIType());
    }

    private void initializeTranslator(ResourceBundle... bundles) {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("pickaria:power_action"));
        for (ResourceBundle bundle : bundles) {
            registry.registerAll(bundle.getLocale(), bundle, true);
        }
        registry.defaultLocale(Locale.ENGLISH);
        GlobalTranslator.translator().addSource(registry);
    }
}
