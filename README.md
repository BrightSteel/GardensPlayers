# GardensPlayers

[![Build](https://github.com/BrightSteel/GardensPlayers/actions/workflows/build.yml/badge.svg)](https://github.com/BrightSteel/GardensPlayers/actions/workflows/build.yml)

The core platform plugin for [GardensMC](https://gardensmc.com). It owns player data and most of the server's gameplay systems, and it ties the rest of the Gardens suite together.

<!-- TODO: add a screenshot or GIF of the shop or quest menus here -->

## Features

- **Economy:** a custom currency system registered as a Vault economy provider, so other plugins can use it.
- **Shops:** buy and sell menus with order handling and transaction checks, plus shops built from GardensFurniture items.
- **Quests and streaks:** daily quests with per-player progress tracking, leaderboards, plus streak rewards.
- **Resource worlds:** worlds that are generated per environment and reset monthly. Old worlds are archived to S3-compatible storage with multipart uploads.
- **Warps, homes, titles and rewards**, with menus for both Java players and Bedrock players (through Floodgate).
- **Commands** are registered at runtime through Paper's Brigadier command API.

## How it's built

- **Paper 26.1 / Java 25**, built with Gradle, paperweight-userdev and Shadow.
- **MySQL through HikariCP** for player data, with a **Caffeine** cache in front of it for hot reads.
- **Unit tests** for economy, shop ordering, quest progress, streak rules, resource world resets and the S3 store, run in GitHub Actions on every push.

| Package | What's in it |
|---|---|
| `command` | Brigadier command tree |
| `menu` | Inventory menus (shops, quests, warps) |
| `database`, `cache` | Data access and caching |
| `quest`, `streak`, `reward` | Progression systems |
| `resourceworld` | World generation, monthly reset, S3 archiving |
| `shop`, `currency`, `economy` | Economy and shops |

## Part of the Gardens suite

The Gardens suite is a set of Paper plugins that run [GardensMC](https://gardensmc.com), a survival community with 736 registered players. The plugins share APIs and depend on each other:

| Plugin | Role |
|---|---|
| [GardensPlayers](https://github.com/BrightSteel/GardensPlayers) | Core platform: economy, quests, shops, warps, resource worlds, player data |
| [GardensEnchants](https://github.com/BrightSteel/gardens-enchants) | Custom enchanting system that replaces vanilla enchanting |
| [GardensFurniture](https://github.com/BrightSteel/gardens-furniture) | Framework for persistent, interactive custom blocks and furniture |
| [GardensCrops](https://github.com/BrightSteel/GardensCrops) | Farming: planters, sprinklers, watering cans, harvest golems |

```
GardensPlayers ──► GardensEnchants ──► GardensFurniture
       │                                     ▲
       └──────────► GardensCrops ────────────┘
```

## Building

Requires Java 25.

```
./gradlew build
```

The shaded jar is written to `build/libs/`. Plugins that are not published to a Maven repository (other Gardens plugins and some third-party plugins) are compiled against jars in `dependencies/`, which are not committed. See [dependencies/README.md](dependencies/README.md) for the list.

## License

[MIT](LICENSE)
