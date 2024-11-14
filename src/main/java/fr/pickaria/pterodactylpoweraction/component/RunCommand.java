package fr.pickaria.pterodactylpoweraction.component;

import fr.pickaria.messager.MessageComponent;
import fr.pickaria.messager.configuration.MessageConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;

public class RunCommand implements MessageComponent {
    private final String command;
    private final Component displayComponent;

    public RunCommand(String command, Component displayComponent) {
        assert command.startsWith("/");

        this.command = command;
        this.displayComponent = displayComponent;
    }

    @Override
    public Component getComponent(MessageConfiguration messageConfiguration) {
        return accented(displayComponent, messageConfiguration)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand(this.command))
                .hoverEvent(getHoverComponent(messageConfiguration));
    }

    private Component accented(Component component, MessageConfiguration messageConfiguration) {
        return component.color(messageConfiguration.accent());
    }

    Component getHoverComponent(MessageConfiguration messageConfiguration) {
        return Component.translatable("run.command", accented(Component.text(command.trim()), messageConfiguration));
    }
}
