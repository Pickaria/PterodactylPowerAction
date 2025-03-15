package fr.pickaria.pterodactylpoweraction.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.pickaria.messager.Messager;
import fr.pickaria.messager.components.Text;
import fr.pickaria.pterodactylpoweraction.PterodactylPowerAction;
import fr.pickaria.pterodactylpoweraction.configuration.ConfigurationDoctor;
import fr.pickaria.pterodactylpoweraction.configuration.ConfigurationLoader;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class PterodactylPowerActionCommand {

    private static final String COMMAND_NAME = "pterodactylpoweraction";
    private final ProxyServer proxy;
    private final Logger logger;
    private final ConfigurationLoader configurationLoader;
    private final Messager messager;

    public PterodactylPowerActionCommand(ProxyServer proxy, Logger logger, ConfigurationLoader configurationLoader) {
        this.proxy = proxy;
        this.logger = logger;
        this.configurationLoader = configurationLoader;
        this.messager = new Messager();
    }

    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder(COMMAND_NAME)
                .requires(source -> source.hasPermission(COMMAND_NAME + ".use"))
                .executes(this::executeHelp)
                .then(BrigadierCommand.literalArgumentBuilder("help").executes(this::executeHelp))
                .then(BrigadierCommand.literalArgumentBuilder("reload").executes(this::executeReload))
                .then(BrigadierCommand.literalArgumentBuilder("doctor").executes(this::executeDoctor))
                .build();

        return new BrigadierCommand(rootNode);
    }

    public CommandMeta getCommandMeta(CommandManager commandManager, PterodactylPowerAction pluginContainer) {
        return commandManager.metaBuilder(COMMAND_NAME)
                .aliases("ppa")
                .plugin(pluginContainer)
                .build();
    }

    private int executeHelp(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        messager.info(source, "command.usage", new Text(Component.text("/" + COMMAND_NAME + " <reload|doctor>")));
        return Command.SINGLE_SUCCESS;
    }

    private int executeReload(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        if (configurationLoader.reload()) {
            messager.info(source, "command.reload.success");
        } else {
            messager.error(source, "command.reload.error");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeDoctor(CommandContext<CommandSource> context) {
        ConfigurationDoctor doctor = new ConfigurationDoctor(proxy, logger);
        CommandSource source = context.getSource();
        messager.info(source, "command.doctor.start");
        doctor.validateConfig(configurationLoader.getConfiguration());
        return Command.SINGLE_SUCCESS;
    }
}
