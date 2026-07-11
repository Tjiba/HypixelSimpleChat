<div align="center">

<img src="src/main/resources/assets/simplechat/icon.png" width="128" alt="Hypixel Simple Chat icon">

# Hypixel Simple Chat

**A client-side Fabric mod that reformats Hypixel chat (lobbies + SkyBlock) into a clean, readable version — without losing anything.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2%20%7C%2026.2-brightgreen)](#)
[![Loader](https://img.shields.io/badge/Loader-Fabric-blue)](https://fabricmc.net/)
[![Language](https://img.shields.io/badge/Kotlin-2.2-purple)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

[Modrinth](https://modrinth.com/mod/hypixel-simple-chat) · [Discord](https://discord.gg/6y35KkQ7h6)

</div>

---

## What it does

Hypixel chat is noisy: level brackets, emblems, guild bridge relays, endless SkyBlock spam, repeated damage lines. Simple Chat rewrites it live on the client into something you actually want to read — every message stays clickable and hover-intact, nothing is dropped.

Turn the master switch off and your chat is **100% untouched**.

## Features

| Feature | What it does |
|---|---|
| **Compact the spam** | Damage, combos, sacks, boss, rewards… shrunk to one-liners. |
| **You choose, per message** | `OFF` · `GREY` · `COMPACT` · `HIDE` for every message type. |
| **Cleaner channels** | Guild, Party & Public with your own colors, ranks and prefixes. |
| **Guild Discord bridge** | Tidies Discord messages relayed into guild chat. |
| **Auto-collapse** | Repeated lines fold into one with a `(xN)` counter. |
| **Live preview** | Watch your chat change as you tweak the settings. |
| **Timestamps** | Optional `[HH:MM]` on every line. |

## Commands

| Command | Action |
|---|---|
| `/hsc` | Open the settings screen |
| `/hsc update` | Check for a newer version |

Settings are also reachable through **Mod Menu**, and there's a native **Discord** link in the config header.

## Requirements

- Minecraft **26.1.2** or **26.2**
- [Fabric Loader](https://fabricmc.net/) `>= 0.18.0`
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
- [Resourceful Config](https://modrinth.com/mod/resourceful-config) *(bundled in the jar)*

## Install

1. Install Fabric Loader for your Minecraft version.
2. Drop the matching jar into your `mods` folder, along with Fabric API and Fabric Language Kotlin.
   - `HypixelSimpleChat-mc26.1-1.0.0.jar` for **26.1.2**
   - `HypixelSimpleChat-mc26.2-1.0.0.jar` for **26.2**
3. Launch, join Hypixel, run `/hsc`.

## License

[MIT](LICENSE) © Tjiba
