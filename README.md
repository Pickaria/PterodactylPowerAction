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

### Pterodactyl Panel

If you have a Pterodactyl Panel to manage your servers, you can use this method.

First, create a client API key which can be found under "Account Settings" then "API Credentials", the URL path should
be `/account/api`.

Configure a waiting server in your `velocity.toml` file, such as:

```toml
[servers]
limbo = "localhost:30066"
survival = "localhost:30067"

try = ["survival"]

[forced-hosts]
"localhost" = ["survival"]
```

Install the plugin on your Velocity proxy, a default configuration file will be created when the proxy is started.
Finally edit the plugin's configuration file to include your Pterodactyl credentials.

```yaml
type: "pterodactyl" # Can also be "shell", see the README for more information
# Create a new client API key which can be found under "Account Settings" then "API Credentials", the URL path should be https://example.com/account/api.
pterodactyl_api_key: "ptlc_xxx"
pterodactyl_client_api_base_url: "https://example.com/api/client"
servers:
  # "survival" is the name of the configured server in your "velocity.toml" file
  # "Server ID" should be replaced with the identifier of your server in Pterodactyl
  # The Server ID can be found under the "Debug Information" section in the "Settings" tab of your server
  survival: "Server ID"
waiting_server_name: "limbo" # "limbo" is the name of the configured server in your "velocity.toml" file
maximum_ping_duration: 60 # in seconds, defaults to 1 minute
shutdown_after_duration: 3_600 # in seconds, defaults to 1 hour
redirect_to_waiting_server_on_kick: true # defaults to false
shutdown_behaviour: "shutdown_all" # Can be "shutdown_all", "shutdown_empty" or "nothing", defaults to "shutdown_all"
```

> [!WARNING]
> If the panel is running behind a proxy such as CloudFlare DNS proxy, the plugin may not be able to start the servers
> and output errors such as:
> ```
> An error occurred while starting the server survival
> java.net.ConnectException: Connection refused
> ```

### Shell commands

If you don't have a Pterodactyl panel, and you are running servers directly from the Linux shell, you can modify the
plugin's configuration to instead run shell commands.

This has not been tested on Windows and please note that the `cd` command will not work, you will have to use the
`working_directory` setting instead.

Here is an example using docker compose to manager servers:

```yaml
type: "shell"
servers:
  survival:
    # "working_directory" can be omitted and the current working directory will be used instead
    working_directory: /path/to/docker/compose
    start: docker compose start survival
    stop: docker compose stop survival
waiting_server_name: "limbo"
maximum_ping_duration: 60
shutdown_after_duration: 3_600
redirect_to_waiting_server_on_kick: true
```

### Shutdown Behaviour

The shutdown behaviour defines what to do when the proxy shuts down. This setting can be one of the following values:

- `nothing`: does nothing and keeps all backend servers running
- `shutdown_empty`: shutdown all empty servers
- `shutdown_all`: shutdown all, including non-empty servers

If this setting is not set, it defaults to `shutdown_all`.

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
