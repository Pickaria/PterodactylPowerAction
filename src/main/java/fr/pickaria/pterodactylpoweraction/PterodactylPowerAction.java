package fr.pickaria.pterodactylpoweraction;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.pickaria.pterodactylpoweraction.commands.PterodactylPowerActionCommand;
import fr.pickaria.pterodactylpoweraction.configuration.ConfigurationLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.slf4j.Logger;

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
    private final ConfigurationLoader configurationLoader;

    @Inject
    public PterodactylPowerAction(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = server;
        this.logger = logger;
        this.configurationLoader = new ConfigurationLoader(logger, dataDirectory);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.initializeCommand();

        initializeTranslator(
                ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.FRENCH, UTF8ResourceBundleControl.get()),
                ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.ENGLISH, UTF8ResourceBundleControl.get()),
                ResourceBundle.getBundle("PterodactylPowerAction.Bundle", Locale.GERMAN, UTF8ResourceBundleControl.get())
        );

        try {
            ShutdownManager shutdownManager = new ShutdownManager(proxy, this, configurationLoader, logger);
            ConnectionListener listener = new ConnectionListener(configurationLoader, proxy, logger, shutdownManager);
            proxy.getEventManager().register(this, listener);
        } catch (NoSuchElementException e) {
            logger.error("Error loading listener", e);
        } catch (IllegalArgumentException e) {
            logger.error("Cannot load the configuration file", e);
        }
    }

    private void initializeTranslator(ResourceBundle... bundles) {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("pickaria:power_action"));
        for (ResourceBundle bundle : bundles) {
            registry.registerAll(bundle.getLocale(), bundle, true);
        }
        registry.defaultLocale(Locale.ENGLISH);
        GlobalTranslator.translator().addSource(registry);
    }

    private void initializeCommand() {
        CommandManager commandManager = proxy.getCommandManager();
        PterodactylPowerActionCommand pterodactylPowerActionCommand = new PterodactylPowerActionCommand(configurationLoader);
        BrigadierCommand commandToRegister = pterodactylPowerActionCommand.createBrigadierCommand();
        commandManager.register(pterodactylPowerActionCommand.getCommandMeta(commandManager, this), commandToRegister);
    }
}
