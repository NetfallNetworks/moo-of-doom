# CLAUDE.md

## Project Overview

Moo of Doom — a NeoForge 1.21.11 Minecraft mod that makes cows absurdly overpowered.

## Build & Test

- **Build**: `./gradlew build --no-configuration-cache`
- **Tests only**: `./gradlew test --no-configuration-cache`
- **Java**: 21 (required)
- **Gradle**: 9.2.1 (via wrapper)

### Claude Code Web (Sandbox) Builds

The sandbox proxy requires authentication that Java's `HttpClient` can't
provide automatically, causing NeoForm artifact downloads to fail with
HTTP 407. Use the proxy relay wrapper instead:

```bash
scripts/sandbox-build.sh                  # full build
scripts/sandbox-build.sh test             # tests only
scripts/sandbox-build.sh build --info     # any Gradle args
```

This starts a local proxy relay (`scripts/proxy_relay.py`) that injects
the `Proxy-Authorization` header, then runs Gradle through it.

## Pre-Push Checklist

**IMPORTANT: Always run the full build locally before every push.**

In the sandbox, use the proxy relay wrapper:

```bash
scripts/sandbox-build.sh
```

Outside the sandbox, use Gradle directly:

```bash
./gradlew build --no-configuration-cache
```

Do NOT push if the build fails. Fix all compilation errors and test failures first.

## CI Workflows

- **build.yml**: Runs on every push and PRs to main. Builds, tests, uploads JAR artifact.
- **release.yml**: Triggered via GitHub Actions UI (workflow_dispatch) or tag push. Builds, creates GitHub release with auto-generated notes and JAR.

## Project Structure

- `src/main/java/com/github/netfallnetworks/mooofdoom/` — mod source code
- `src/test/java/` — unit tests (JUnit 5)
- `src/main/templates/META-INF/neoforge.mods.toml` — mod metadata template
- `gradle.properties` — version numbers, mod metadata
- `scripts/` — build utilities (proxy relay for sandbox environments)

## Conventions

- Mod ID: `mooofdoom`
- Version: derived from git tags (`v*.*.*`), falls back to `gradle.properties`
- Tests must be deterministic — always seed `Random` instances

## Plan Documents

Plan documents live in `docs/plans/` and follow the format described in
`docs/plans/CONVENTIONS.md`. Read that file before writing or modifying plans.
