# ProjectX CLI

`projectx-cli` is a command-line tool that lets AI agents (and developers) interact with a running ProjectX client in real time. It communicates with the embedded Agent Server plugin over HTTP on localhost, providing access to the game's widget system, inventory, NPCs, objects, banking, walking, dialogues, login, and script lifecycle management.

The CLI lives in the ProjectX client repo at `./projectx-cli`. This document is a reference copy for Hub developers.

## Prerequisites

1. The ProjectX client must be running
2. The **Agent Server** plugin must be enabled in the plugin list
3. `curl` must be available on the system (standard on Linux/macOS)

## Quick Start

```bash
# Check if the server is reachable and the player is logged in
./projectx-cli state

# See what's in the inventory
./projectx-cli inventory

# Find nearby NPCs
./projectx-cli npcs --name Guard --distance 15

# Attack the nearest guard
./projectx-cli npcs interact "Guard" "Attack"

# Walk somewhere
./projectx-cli walk 3222 3218
```

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `PROJECTX_HOST` | `127.0.0.1` | Agent Server host |
| `PROJECTX_PORT` | `8081` | Agent Server port (must match plugin config) |
| `PROJECTX_TIMEOUT` | `30` | Request timeout in seconds |

## Command Reference

### Game State

```bash
./projectx-cli state                       # Player info, position, animation
./projectx-cli skills                      # All skill levels and XP
./projectx-cli skills --name Attack        # Single skill
```

### Widget Inspection

```bash
./projectx-cli widgets list                # All visible widget interfaces
./projectx-cli widgets search "bank"       # Search widgets by keyword
./projectx-cli widgets describe 134 0      # Widget tree under a specific widget
./projectx-cli widgets click 134 42        # Click widget by ID
./projectx-cli widgets click --text "Toggle"  # Click by text
```

### Inventory

```bash
./projectx-cli inventory                               # List items
./projectx-cli inventory interact "Lobster" "Eat"      # Use/eat/equip
./projectx-cli inventory drop "Logs" --all             # Drop items
```

### NPCs

```bash
./projectx-cli npcs --name "Banker" --distance 10      # Query nearby
./projectx-cli npcs interact "Banker" "Bank"           # Interact
```

### Objects

```bash
./projectx-cli objects --name "Oak tree" --distance 10  # Query nearby
./projectx-cli objects interact "Oak tree" "Chop down"  # Interact
```

### Ground Items

```bash
./projectx-cli ground-items --name "Dragon bones"       # Query nearby
./projectx-cli ground-items pickup "Dragon bones"        # Pick up
```

### Walking

```bash
./projectx-cli walk 3222 3218              # Walk to coordinates
./projectx-cli walk 3222 3218 1            # With plane (upstairs)
```

### Banking

```bash
./projectx-cli bank                        # Bank status
./projectx-cli bank open                   # Open nearest bank
./projectx-cli bank close                  # Close bank
./projectx-cli bank deposit-all            # Deposit everything
./projectx-cli bank deposit "Logs"         # Deposit specific item
./projectx-cli bank withdraw "Pure essence" 28  # Withdraw items
```

### Dialogue

```bash
./projectx-cli dialogue                    # Dialogue state
./projectx-cli dialogue continue           # Click continue
./projectx-cli dialogue select "Buy sword" # Select option
```

### Login

`POST /login` **blocks by default** until login succeeds or fails, returning a definitive result. No polling needed.

```bash
./projectx-cli login                       # Login status + error detection
./projectx-cli login now                   # Trigger login (blocks until result)
./projectx-cli login now --world 360       # Login to specific world
./projectx-cli login wait --timeout 60     # CLI-side poll (alternative to blocking POST)
```

**HTTP API parameters** for `POST /login`:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `world` | number | - | Target world (omit for profile default) |
| `wait` | boolean | `true` | Block until login resolves. Set `false` for fire-and-forget. |
| `timeout` | number | `30` | Max seconds to wait (max 120) |

**Success:**
```json
{"success": true, "message": "Login successful", "currentWorld": 360}
```

**Failure (non-member on members world):**
```json
{"success": false, "message": "Login failed: Non-member account cannot login to members world", "loginIndex": 34, "loginError": "Non-member account cannot login to members world"}
```

**Timeout:**
```json
{"success": false, "message": "Login timed out after 30s", "currentWorld": 360}
```

Login error detection via `loginIndex` / `loginError` (on `GET /login` status and in failure responses):

| loginIndex | loginError |
|------------|------------|
| 3 | Authentication failed - invalid credentials |
| 4 | Invalid credentials |
| 14 | Account is banned |
| 24 | Disconnected from server |
| 34 | Non-member account cannot login to members world |

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Login successful (or already logged in) |
| 400 | Login rejected (no profile, not on login screen) |
| 401 | Login failed (auth failure, banned, non-member, or timeout) |
| 409 | Login attempt already in progress |

**Auto-dismiss on retry:** If login fails (e.g., non-member on members world), the game error dialog is automatically dismissed on the next login call. You can immediately retry with a different world — no manual intervention needed:

```bash
./projectx-cli login now --world 461    # fails: non-member
./projectx-cli login now --world 383    # error dismissed, retries on F2P world
```

### Script Lifecycle

```bash
./projectx-cli scripts                     # List all plugins
./projectx-cli scripts start --class "com.hub.MyPlugin"  # Start by class
./projectx-cli scripts start --name "Example"             # Start by name
./projectx-cli scripts stop --class "com.hub.MyPlugin"    # Stop
./projectx-cli scripts status --class "com.hub.MyPlugin"  # Status + runtime
./projectx-cli scripts results --class "com.hub.MyPlugin" # Get test results
```

## Hub Testing Workflow

The typical flow for automated Hub testing:

```bash
# 1. Wait for Agent Server to be ready
# 2. Login
./projectx-cli login wait --timeout 60 --world 360

# 3. Start the script under test
./projectx-cli scripts start --class "net.runelite.client.plugins.projectx.myplugin.MyPlugin"

# 4. Poll status while running
./projectx-cli scripts status --class "net.runelite.client.plugins.projectx.myplugin.MyPlugin"

# 5. Retrieve results
./projectx-cli scripts results --class "net.runelite.client.plugins.projectx.myplugin.MyPlugin"

# 6. Stop the script
./projectx-cli scripts stop --class "net.runelite.client.plugins.projectx.myplugin.MyPlugin"
```

See `docs/SCRIPT_LIFECYCLE_API.md` for the full HTTP API and Java-side result submission.

## Full HTTP API

For the complete HTTP endpoint reference, handler architecture, and server internals, see `docs/AGENT_SERVER.md` in the ProjectX client repo.
