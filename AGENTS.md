# AGENTS.md

AutoSprint — a Paper/Bukkit plugin that automatically toggles
sprinting based on client movement packets.

## Build & run

- Build: `mvn clean package` → output is `target/AutoSprint.jar`
  (note: `finalName` in pom.xml strips the version, so the jar is NOT
  `AutoSprint-2.0.jar`).
- Java 21 required (toolchain + runtime).
- Requires no test suite, no lint, no typecheck — none are configured.
  Do not invent `npm run lint` / `mvn test` workflows; `mvn test`
  runs but there are zero tests.

## Runtime dependencies (important)

Both Maven deps are `provided` scope — they are NOT bundled:
- `io.papermc.paper:paper-api` 1.21.4 — the server itself.
- `com.github.retrooper:packetevents-spigot` 2.12.1 — MUST be
  installed on the server; `plugin.yml` declares `depend: [packetevents]`.
The plugin will fail to load without PacketEvents.

Target: Paper 1.21.x (`api-version: 1.21`).

## Architecture

- Entry point / main class: `com.autosprint.AutoSprint`
  (`src/main/java/com/autosprint/AutoSprint.java`).
- Core logic is packet-driven, NOT a scheduler loop.
  `AutoSprintManager` extends `SimplePacketListenerAbstract` and
  listens for `PLAYER_POSITION` / `PLAYER_POSITION_AND_ROTATION`
  via PacketEvents. Sprint is decided by comparing the player's
  position delta against their yaw (forward direction), then applied
  on the main thread through `getScheduler().runTask(...)` — this
  thread hop must be preserved; `setSprinting` is not safe off-thread.
- Player state lives in three **static** maps in `AutoSprintManager`
  (`enabledPlayers`, `lastPositions`, `lastYaws`). The `/autosprint
  reload` command re-reads config but does NOT re-register the packet
  listener nor rebuild these maps — a full plugin reload
  (disable→enable) is needed if you change listener wiring.
- Eligibility (`AutoSprint.canSprint`) gates: glide / vehicle /
  riptide / sneak / block / non-survival-adventure / low food /
  not-on-ground-and-not-swimming. Keep these checks in sync if you
  touch the trigger logic.

## Config

`config.yml` keys (read via `getConfig()` at runtime):
`enabled`, `default-enabled`, `min-food`, `debug`.
`/autosprint reload` re-reads them; the `debug` subcommand also
mutates and saves `config.yml`.

## Repo hygiene

- `liball/`, `paper-server.jar`, `AutoSprint.jar`, `target/` are
  gitignored runtime/build artifacts present locally — never `git add`
  them (they were previously committed by mistake and removed).
- Only `src/`, `pom.xml`, `README.md`, `.gitignore`, `AGENTS.md` are
  tracked source/config. Keep it that way.

## Conventions

- Source is plain Paper API + PacketEvents; no Lombok, no annotation
  processing, no shaded deps — keep it dependency-light.
- Commit messages: short, Chinese OK, optional conventional prefix
  (`chore:`, `Fix:`). Match recent history.