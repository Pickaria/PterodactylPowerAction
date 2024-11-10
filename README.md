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

## Configuration

### Pterodactyl Panel

If you have a Pterodactyl Panel to manage your servers, you can use this method.

First, create a client API key which can be found under "Account Settings" then "API Credentials", the URL path should
be `/account/api`.

Configure a waiting server in your `velocity.toml` file, such as:

```toml
[servers]
limbo = "127.0.0.1:30066"
```

Install the plugin on your Velocity proxy, a default configuration file will be created when the proxy is started.
Finally edit the plugin's configuration file to include your Pterodactyl credentials.

```yaml
type: "pterodactyl" # Can also be "shell", see the README for more information
# Create a new client API key which can be found under "Account Settings" then " API Credentials", the URL path should be https://pterodactyl.test/account/api.
pterodactyl_api_key: "ptlc_xxx"
pterodactyl_client_api_base_url: "https://pterodactyl.test/api/client" # No trailing slash
servers:
  # "survival" is the name of the configured server in your "velocity.toml" file
  # "abc" should be replaced with the identifier of your server in Pterodactyl
  # this can be found in the URL such as: https://pterodactyl.test/server/:server_id
  # where ":server_id" is the identifier of the server
  survival: "abc"
waiting_server_name: "limbo" # "limbo" is the name of the configured server in your "velocity.toml" file
maximum_ping_duration: 60 # in seconds
shutdown_after_duration: 3600 # in seconds
```

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
    working_directory: /path/to/server
    start: docker compose start survival
    stop: docker compose stop survival
waiting_server_name: "limbo"
maximum_ping_duration: 60 # in seconds
shutdown_after_duration: 3600 # in seconds
```

## Motivations

I am running Minecraft servers on dedicated hardware at home, and I wanted to save energy costs by stopping empty
servers.

My Limbo server is running on a low power ARM Single Board Computer to further save costs.
