# Commands Guide

This document outlines the available commands for the PterodactylPowerAction plugin for Velocity, which allows server
administrators to manage Minecraft servers through in-game commands.

## Command Overview

All PterodactylPowerAction commands use the base command:

```
/pterodactylpoweraction
```

For convenience, the plugin also supports the shortened alias:

```
/ppa
```

## Permission Requirements

All commands require the following permission:

```
pterodactylpoweraction.use
```

## Available Commands

### Reload Configuration

Reloads the plugin's configuration from disk.

```
/pterodactylpoweraction reload
```

**Aliases:** `/ppa reload`

**Description:**  
This command reloads the plugin's configuration file, allowing you to apply changes without restarting the proxy server.
Any modifications to the configuration file will take effect immediately after running this command.

**Notes:**

- This command only reloads the plugin's configuration, not Velocity's configuration.
- If you've added new servers to `velocity.toml`, you'll need to reload Velocity's configuration separately.

---

### Shutdown Empty Servers

Manually shuts down servers with no players.

```
/pterodactylpoweraction clear [delay=0]
```

**Aliases:** `/ppa clear [delay=0]`

**Parameters:**

- `delay` (optional): Time in seconds to wait before shutting down servers. Defaults to 0 if not specified.

**Description:**  
This command checks the player count on all configured servers and sends stop signals to any empty servers after the
specified delay. This is useful for manually freeing up resources when servers are not in use.

**Examples:**

- `/ppa clear` - Immediately shut down all empty servers
- `/ppa clear 30` - Shut down all servers after a 30-second delay if they're still empty

---

### Run Diagnostic Checks

Validates the plugin's configuration and performs diagnostic checks.

```
/pterodactylpoweraction doctor
```

**Aliases:** `/ppa doctor`

**Description:**  
The `doctor` command is a troubleshooting tool that performs a series of diagnostic checks on your
PterodactylPowerAction setup.
Running this command can help identify and resolve potential issues with your configuration.
