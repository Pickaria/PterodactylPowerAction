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

## In-game command

Reload the configuration: `/pterodactylpoweraction reload`

## Localization

The plugin's messages are automatically translated based on the client's language and currently support the following
languages:

- German
- English
- French

## Waiting/Limbo servers

Here is a small list of recommended lightweights servers software to use as waiting server:

- [Limbo](https://www.spigotmc.org/resources/82468/)
- [NanoLimbo](https://www.spigotmc.org/resources/86198/)
- [Quozul/PicoLimbo](https://github.com/Quozul/PicoLimbo) - Experimental, does not work with Velocity yet.

Note that the waiting server does not have to be a limbo server specifically, it can be any server as long as it is
always accessible. If you have a dedicated lobby server in your network, you can use that, no need for a dedicated limbo
server!

## Motivations

I am running Minecraft servers on dedicated hardware at home, and I wanted to save energy costs by stopping empty
servers.

My Limbo server is running on a low power ARM Single Board Computer to further save costs.
