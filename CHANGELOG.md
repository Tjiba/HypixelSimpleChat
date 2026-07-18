# Changelog

All notable changes to **Hypixel Simple Chat**.

## [1.0.0] — unreleased

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
