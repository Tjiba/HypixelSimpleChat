# Changelog

All notable changes to **Hypixel Simple Chat**.

## [1.2.0] — 2026-08-13

### Added
- **Foraging tab** — its own page in SkyBlock settings, split into `General`, `Torrhus`, `Hunting` and `Safari`, with colored section headers.
- **Foraging messages** — tree gifts, floor drops, `PETALFALL!` / `WOODPECKER!`, Beeheemoth (spawn, progress, escape), honeyhive loot, honey tree, caught shards, escaped mobs.
- **Safari** — one-line end-of-run reward summary merged from the seven-line block, entry lines, Safari Manager dialog, unclaimed milestones.
- **SkyBlock XP** — `+2 SkyBlock XP (Bag Upgrades) (69/100)` shortened to `+2 SB XP (Bag Upgrades)`; a guard rule keeps any XP gain from being swallowed by a generic rule.
- `/hsc debug` — logs each message with its verdict, the rule that matched, and why a guard kept it as-is.

### Changed
- **Hypixel's own colors are kept.** Words copied from a message keep the color the server gave them — item rarity, player rank (including the `+` inside `[MVP+]`), SkyBlock level, and the red percentage that warns a tree gift is below the 10% threshold.
- **Compacted messages stay clickable** and keep their tooltip. Buttons such as `[PICK UP]` are re-attached instead of the reformat being abandoned.
- XP gains are shortened, never hidden by default.
- Loot share covers all three of Hypixel's wordings, including the hunting one.
- Sacks notifications also match withdrawals (`-12 items`).

### Fixed
- Colors outside Minecraft's 16-color palette were dropped when converting a message; they now survive as `§#RRGGBB`.
- The Modrinth project slug used by the update checker was wrong, so `/hsc update` always reported a connection failure.

## [1.0.0] — 2026-07-18

Initial release.

### Added
- **Per-channel formatting** — Public / Party / Guild / Officer, each with `Vanilla` or `Compact` mode, rank-colored names, toggleable Hypixel & guild ranks, custom prefixes and colors.
- **Guild Discord bridge** cleanup — short alias for the relay bot, clear Discord username, optional `V1/V2/V3` version tags and custom name color.
- **Auto-collapse** — repeated messages fold into one line with a `(xN)` counter; *smart collapse* also merges repeats that differ only by numbers.
- **Per-message / per-group cleanup** — `OFF` / `GREY` / `COMPACT` / `COMPACT_GREY` / `HIDE` across **Lobby**, **SkyBlock** and **System** categories, plus custom hidden patterns.
- **Compact reformats** — damage numbers, kill combos, mob abilities, sacks, loot share, NPC dialog, boss lines, boosters, daily/mystery rewards, rare rewards, GEXP, server routing, profile ID, and more.
- **Custom config menu** — modern glass UI (rounded corners, blurred background), sidebar navigation, live preview, tooltips and an HSV color picker; identical on both MC versions. **Mod Menu** integration.
- **Chat tabs** — All / Party / Guild above the chat input; route what you send to the selected channel, or filter the view to it.
- **Ctrl+F search** — live-filter the chat history from the chat input.
- **Right-click to copy** any chat line ("Copied!" feedback).
- **Extended chat history** — keep up to 2048 messages instead of vanilla's 100.
- Optional `[HH:MM]` timestamps.
- Update checker — `/hsc update`.

### Supported
- Minecraft **26.1.2** and **26.2** (Fabric) — one universal jar.
