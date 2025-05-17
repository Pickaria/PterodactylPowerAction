# PterodactylPowerAction

A Velocity plugin to start and stop servers using the [Pterodactyl](https://pterodactyl.io/) client API.

## How it works

This plugin will stop a server after a given delay (1 hour by default) if it is empty after a player left or changed
server.

When a player tries to connect to a stopped server, they will be redirected to a waiting server such
as [Limbo](https://www.spigotmc.org/resources/82468/) and will be automatically redirected to the requested server once
it is started.

If the player is already connected on the network, they will simply be informed by a message that they will be
automatically redirected once the server is ready.

If the server fails to start, the player will be informed to try again.

This plugin is also able to redirect your player that has been kicked from a backend server to the waiting server, this
is toggleable in the configuration file.

## Configuration

The [configuration guide](https://github.com/Pickaria/PterodactylPowerAction/wiki/Configuration-Guide) is available on
the wiki.

## In-game Commands

The plugin's command require the player to have the `pterodactylpoweraction.use` permission.

### Reload Plugin Configuration

To reload the plugin's configuration, use the following command:

```plaintext
/pterodactylpoweraction reload
```

> [!NOTE]
> If you have added a new server to `velocity.toml`, you will need to reload this configuration as well.
> Currently, PterodactylPowerAction does not automatically reload Velocity's configuration, so you may require an
> additional plugin for this functionality.

### Shutdown Empty Servers

To manually shut down empty servers, use the following command:

```plaintext
/pterodactylpoweraction clear [delay=0]
```

This will check the player count on all servers and send stop signals to all the empty servers after the given delay. If
not specified, the delay is 0 seconds by default.

### Run Checks and Validate Configuration

To run checks and validate the configuration, use the following command:

```plaintext
/pterodactylpoweraction doctor
```

The `doctor` command is a tool for troubleshooting issues with your PterodactylPowerAction setup. It performs a
series of diagnostic checks to ensure that your configuration files are correctly set up and that all necessary
components are functioning properly. Running this command can help you identify and resolve potential problems.

## Localization

The plugin's messages are automatically translated based on the client's language. Currently, the following languages
are supported:

- German
- English
- French

## Waiting/Limbo servers

Here is a small list of recommended lightweights servers software to use as waiting server:

- [Limbo](https://www.spigotmc.org/resources/82468/)
- [NanoLimbo](https://www.spigotmc.org/resources/86198/)

I am also developing my own experimental [PicoLimbo](https://github.com/Quozul/PicoLimbo) server from scratch in Rust.
It has 0% CPU usage on idle and uses less than 10 MB of memory with minimal network footprint by only implementing the
required packets.

Note that the waiting server does not have to be a limbo server specifically, it can be any server as long as it is
always accessible. If you have a dedicated lobby server in your network, you can use that, no need for a dedicated limbo
server!

## Motivations

I am running Minecraft servers on dedicated hardware at home, I wanted to save energy costs and memory usage by stopping
empty servers. Running the waiting server on a low power ARM Single Board Computer can also further save costs.

## Contributing

Contributions are welcome! If you encounter any issues or have suggestions for improvement, please submit an issue or
pull request on GitHub. Make sure to follow the existing code style and include relevant tests.

1. Fork the repository.
2. Create a new branch `git checkout -b <branch-name>`.
3. Make changes and commit `git commit -m 'Add some feature'`.
4. Push to your fork `git push origin <branch-name>`.
5. Submit a pull request.
