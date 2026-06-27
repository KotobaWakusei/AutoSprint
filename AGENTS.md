# AGENTS.md

AutoSprint — a Paper/Bukkit plugin that automatically toggles
sprinting for enabled players while they move forward.

## Build & run

- Build: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn clean package`
  → output is `target/AutoSprint.jar` (note: `finalName` in pom.xml
  strips the version, so the jar is NOT `AutoSprint-2.0.jar`).
  The default `java` on this machine is 17; you MUST point
  `JAVA_HOME` at `java-21-openjdk` or the build fails with
  `错误: 无效的目标发行版：21`.
- Java 21 required (toolchain + runtime).
- Requires no test suite, no lint, no typecheck — none are configured.
  Do not invent `npm run lint` / `mvn test` workflows; `mvn test`
  runs but there are zero tests.

## Runtime dependencies (important)

Only one Maven dep, and it is `provided` scope (NOT bundled):
- `io.papermc.paper:paper-api` 1.21.4 — the server itself.

No third-party runtime plugin is required. Previous versions depended
on `packetevents`; it has been removed. If you re-introduce it, also
re-add the `retrooper` repo in `pom.xml` and the `depend` key
in `plugin.yml`.

Target: Paper 1.21.x (`api-version: 1.21`).

## Architecture

- Entry point / main class: `com.autosprint.AutoSprint`
  (`src/main/java/com/autosprint/AutoSprint.java`).
- Core logic is **scheduler-driven**, NOT packet-driven and NOT a
  per-packet task. `AutoSprintManager extends BukkitRunnable` and is
  scheduled via `runTaskTimer(this, 0L, intervalTicks)` in `onEnable`;
  it runs on the main thread every `interval-ticks` (default 1).
- Each tick it iterates online players in `enabledPlayers`, reads
  `player.getLocation()` on the main thread, compares the position
  delta against the player's yaw to detect forward movement, and calls
  `player.setSprinting(true)` for those who pass `AutoSprint.canSprint`.
  Do NOT reintroduce an off-thread `runTask` hop or read locations
  off-thread — `Location`/`setSprinting` are main-thread APIs.
- The `setSprinting(true)` call is made **every tick the player is
  moving forward and eligible**, without a `!player.isSprinting()`
  gate. This is intentional: the client is authoritative over its own
  sprinting flag and can roll it back, so the server reasserts it
  each tick by force. Restoring the gate will resurrect the "speed
  doesn't actually increase" bug that motivated this rewrite.
- Player state lives in **static** maps/sets in `AutoSprintManager`:
  `enabledPlayers` (active), `explicitlyDisabled` (players who ran
  `/as` to turn it off — protects against `default-enabled` reloads
  re-enabling them), `lastPositions`, `lastYaws`.
- Eligibility (`AutoSprint.canSprint`) gates: glide / vehicle /
  riptide / sneak / block / non-survival-adventure / low food.
  Notably it does **not** require on-ground — sprinting is allowed
  mid-air/jumping to avoid flicker. Keep these checks in sync if you
  touch the trigger logic.
- Airborne speed compensation: on Paper 1.21.4 the local player's
  sprint speed multiplier is dropped while `!onGround && !swimming`,
  causing the "jump = walk speed" issue. `AutoSprintManager` tracks
  an `airBoosted` set and temporarily raises `walkSpeed` to
  `air-walk-speed` (default 0.3) for players currently sprinting in
  the air; it restores `default-walk-speed` (default 0.2) when the
  boost no longer applies (grounded/swim/not moving/disabled) or on
  quit/disable. Do NOT remove the restore path — players would be
  stuck with elevated walk speed permanently.
- `/autosprint reload` re-reads config and re-applies
  `default-enabled` to online players via `enableDefault(...)` (which
  skips `explicitlyDisabled`), but does NOT rebuild state maps nor
  reschedule the task — changing `interval-ticks` or task wiring
  requires a full plugin reload (disable→enable).

## Config

`config.yml` keys (read via `getConfig()` at runtime):
`enabled`, `default-enabled`, `min-food`, `forward-threshold`,
`interval-ticks`, `air-walk-speed`, `default-walk-speed`, `debug`.
- `forward-threshold`: forward-velocity projection (in blocks/tick)
  above which a player is considered "moving forward" (default 0.05).
- `interval-ticks`: how often the task re-evaluates sprinting
  (default 1 = every tick; raising it trades responsiveness for CPU).
`/autosprint reload` re-reads them; the `debug` subcommand also
mutates and saves `config.yml`.

## Repo hygiene

- `liball/`, `paper-server.jar`, `AutoSprint.jar`, `target/` are
  gitignored runtime/build artifacts present locally — never `git add`
  them (they were previously committed by mistake and removed).
- Only `src/`, `pom.xml`, `README.md`, `.gitignore`, `AGENTS.md` are
  tracked source/config. Keep it that way.

## Conventions

- Source is plain Paper API; no Lombok, no annotation processing, no
  shaded deps — keep it dependency-light.
- Commit messages: short, Chinese OK, optional conventional prefix
  (`chore:`, `Fix:`). Match recent history.