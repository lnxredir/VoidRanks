# VoidRanks

VoidRanks is a **Paper/Spigot Minecraft plugin** that integrates with **LuckPerms** to automatically promote and demote players based on their playtime. All messages are configurable and support colored ranks.

---

## Features

* Automatic **promotion** after a configurable amount of playtime.
* Optional **demotion** if players are offline for too long.
* Fully **configurable rank display names** with colors and brackets.
* **Grace period** to prevent instant promotion/demotion.
* `/ptime` command for players to check:

  * Total playtime (hours and minutes)
  * Current rank
  * Time until next rank
* `/vranks` admin command with subcommands:

  * `reload` → reloads the config
  * `check <player>` → view a player’s playtime and rank
  * `reset <player>` → reset a player’s playtime
* Displays **final rank promotions publicly**, all other promotions privately.

---

## Installation

1. Make sure you have **LuckPerms** installed.
2. Download `VoidRanks.jar` and place it in your server's `plugins` folder.
3. Start the server to generate the default `config.yml` and `data.yml`.
4. Edit `config.yml` to configure ranks, display names, messages, and grace period.
5. Restart or use `/vranks reload` to apply config changes.

---

## Configuration

`config.yml` example:

```yaml
# O tempo é marcado em MINUTOS
# Exemplo: 'time: 60' = 1 hora.
ranks:
  - name: "Novato"
    time: 0
  - name: "Membro"
    time: 60
  - name: "Veterano"
    time: 240
  - name: "Elite"
    time: 720

# Como o rank é exibido no comando /ptime
display_names:
  Novato: "&7[Novato]&f"
  Membro: "&a[Membro]&f"
  Veterano: "&b[Veterano]&f"
  Elite: "&6[Elite]&f"

messages:
  promoted: "&aParabéns! Você foi promovido para &e{rank}&a!"
  promoted_final: "&6&l{player} alcançou o rank máximo: &e{rank}&6&l!"

# minutos extras de tolerância antes de promover/demotar, coloque 0 para desativar.
settings:
  grace_period: 5
```

**Notes:**

* `ranks` → Simple LuckPerms group names.
* `display_names` → Colored, bracketed display names for messages.
* `messages` → Fully configurable messages in Portuguese or any language.
* `grace_period` → Extra minutes before promotion/demotion applies.
---

## Commands

### Player Commands

| Command  | Description                                                 |
| -------- | ----------------------------------------------------------- |
| `/ptime` | Shows your playtime, current rank, and time until next rank |

### Admin Commands (`voidranks.admin`)

| Command                  | Description                                      |
| ------------------------ | ------------------------------------------------ |
| `/vranks reload`         | Reloads the config without restarting the server |
| `/vranks check <player>` | Shows a player’s playtime and rank               |
| `/vranks reset <player>` | Resets a player’s playtime                       |

---

## Permissions

| Permission        | Default | Description                             |
| ----------------- | ------- | --------------------------------------- |
| `voidranks.admin` | OP      | Manage VoidRanks (reload, check, reset) |
| `voidranks.ptime` | True    | Allows players to use `/ptime`          |

---

## Notes

* Ranks are counted **top to bottom** in `config.yml`. The first item is the starting rank, the last is the final rank.
* Promotions and demotions are based on **minutes played/offline**.
* The plugin **does not rename LuckPerms groups** automatically. Make sure your LuckPerms groups match the `ranks` names.
* The `/vranks reload` command updates **messages, display names, and times**, but does **not migrate renamed ranks**.
