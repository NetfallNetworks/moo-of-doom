# Temporary-Behavior Framework & Companion Cow Fixes — Design Document

**Date:** 2026-07-14
**Minecraft Version:** 1.21.11
**Mod Loader:** NeoForge
**Mod ID:** `mooofdoom`
**Issues:** [#19](https://github.com/NetfallNetworks/moo-of-doom/issues/19),
[#20](https://github.com/NetfallNetworks/moo-of-doom/issues/20),
[#21](https://github.com/NetfallNetworks/moo-of-doom/issues/21),
[#22](https://github.com/NetfallNetworks/moo-of-doom/issues/22)

## Overview

Code review of `a480b71` found that every "temporary" combat behavior (Rebellion, Guardian,
protector conversion) adds AI goals it can never remove — there is no `removeGoal` call in the
codebase — so cows that rebel once attack players forever, repeat triggers stack duplicate goals,
and `OpCowManager` itself stacks duplicate combat goals on every chunk reload. Two further bugs
break the Mythic cow morph (companion cow leaks on logout; companion cow is not excluded from
OP activation), and two handlers do redundant per-cow entity scans. This design replaces ad-hoc
goal mutation with a small polymorphic framework that makes the leak *unrepresentable*, fixes the
companion-cow lifecycle, and hoists the redundant scans.

---

## Root Cause (issue #19 family)

Goal selectors are runtime-only state: they are never serialized, while entity tags persist.
The current code treats `addGoal` as something to undo later — but no cleanup path stores the
`Goal` instance, so undo is structurally impossible. Every fix that keeps the add/remove model
must remember cleanup in *every* expiry path (effect expiry, death, chunk unload, logout),
which is exactly the bug class that produced #19.

**Design principle:** goals are installed idempotently and gate *themselves* on persistent
entity state (tags). Deactivation strips the tag; the goal instantly goes inert. Nothing is
ever removed, so nothing can leak.

---

## PR-A — Temporary-Behavior Framework (closes #19)

New package `com.github.netfallnetworks.mooofdoom.cow.behavior`:

### `GatedGoal` (decorator)

Wraps any vanilla `Goal`, delegating all methods. `canUse()` and `canContinueToUse()` first
check a `BooleanSupplier` gate; when the gate is false the goal is inert. Carries a String
`behaviorId` so installers can detect an existing install by scanning the selector. Modeled on
vanilla's `WrappedGoal` delegation shape.

### `TemporaryBehavior` (abstract, template method)

Owns the whole lifecycle; the invariant-bearing methods are `final` so subclasses cannot
get them wrong:

| Method | Final? | Behavior |
|--------|--------|----------|
| `ensureInstalled(Mob)` | yes | Scans `goalSelector`/`targetSelector` `getAvailableGoals()` for a `GatedGoal` with this behavior's id; installs `createGoals()` (each wrapped in `GatedGoal`) only if absent. Stateless idempotence — self-heals after chunk reload (goals evaporate, scan fails, reinstall). |
| `activate(Mob)` | yes | `ensureInstalled` + add the behavior tag. |
| `deactivate(Mob)` | yes | Strip the tag. Goals go inert immediately; no removal. |
| `isActive(Mob)` | yes | Tag check. |
| `tag()` | abstract | The persistent entity tag, e.g. `MooOfDoom_Rebel`. |
| `createGoals(Mob)` | abstract | The goal set + selector/priority for this behavior. |

Subclasses are declaration-only:

- **`RebellionBehavior`** — `MeleeAttackGoal` + `NearestAttackableTargetGoal<Player>`,
  tag `MooOfDoom_Rebel`.
- **`GuardianBehavior`** — `MeleeAttackGoal` + `NearestAttackableTargetGoal<Monster>`,
  tag `MooOfDoom_Guardian`.
- **`ProtectorBehavior`** — `MeleeAttackGoal` + `NearestAttackableTargetGoal<Monster>`,
  tag `MooOfDoom_Protector`.

### Handler migration

`RebellionHandler`, `GuardianHandler`, and `MobConversionHandler` replace all direct
`addGoal` calls with `XBehavior.INSTANCE.activate(...)` / `.deactivate(...)`. The handlers lose
the ability to touch goal selectors entirely.

`MobConversionHandler`'s static `protectors` map (leaks on chunk unload, issue #19 bonus
finding) is **deleted**. Protector expiry moves to entity persistent data
(`MooOfDoom_ProtectorUntil` = game-time tick deadline written at conversion). The tick handler
deactivates when `level.getGameTime()` passes the deadline. Persistent data survives chunk
unload/reload, so protectors expire correctly even if they were unloaded for the whole window.

### `OpCowManager` hardening

`addCombatGoals` migrates to the same `ensureInstalled` mechanism (a permanent behavior gated
on `MooOfDoom` OP tag), fixing duplicate combat-goal stacking on every reload
(`OpCowManager.java:88-93`).

### Architecture invariant test

Plain JUnit test (`BehaviorArchitectureTest`) walks `src/main/java` sources and **fails if
`addGoal(` appears outside `TemporaryBehavior`** (allowlist: the framework itself). This runs in
existing CI on every PR — the enforcement is a build break, not a convention. Source-scanning
keeps the test free of Minecraft classloading, per repo test convention.

---

## PR-B — Redundant Scan Hoisting (closes #22, stacked on PR-A)

Behavior-identical perf fixes in the two handlers PR-A already touches:

- **`GuardianHandler.onPlayerTick`** — the `getEntitiesOfClass(Monster.class, ...)` query at
  lines 86-87 depends only on `player`; hoist the query and the closest-monster computation
  above the `for (Cow cow : nearbyCows)` loop. 30 cows × 5 zombies: 1 scan instead of 30.
- **`RebellionHandler.onPlayerTick` cleanup branch** — compute the set of nearby players that
  still have Rebellion once per pass, instead of a fresh `getEntitiesOfClass(Player.class, ...)`
  scan per rebel-tagged cow (currently O(players × cows × players) every 20 ticks).

Opens as **draft** with a bold "merge PR-A first" banner as the first body line; marked ready
when PR-A merges.

---

## PR-C — Companion Cow Lifecycle (closes #20 and #21, parallel lane)

Three changes in the morph subsystem:

1. **Logout cleanup (#20):** register `PlayerEvent.PlayerLoggedOutEvent`; if
   `CowMorphHandler.isMorphed(player)`, call `endMorph(player)`. Covers disconnects and client
   crashes during the 30-second morph window.
2. **Re-morph safety (#20):** `startMorph` discards any existing companion for that player
   before spawning a new one, instead of silently overwriting the map entry.
3. **Join-event guard + orphan sweep (#21):** in `OpCowManager.onEntityJoinLevel`, cows tagged
   `MooOfDoom_Companion` are never OP-activated. If a companion cow joins the level *unmanaged*
   (not present in `CowMorphHandler`'s live morph map — i.e. an orphan persisted by a crash or
   from before this fix), it is discarded. Companions are pure runtime decoration; a persisted
   one is always garbage.

PR-C touches a different region of `OpCowManager` than PR-A (top-of-method guard vs
`addCombatGoals`); whichever lands second rebases trivially.

---

## Testing

- **Unit (CI-gated):** `BehaviorArchitectureTest` source-scan invariant. `GatedGoal` /
  `TemporaryBehavior` reference Minecraft classes and cannot be unit-tested without booting
  MC (repo convention: plain JUnit, no NeoForge boot); any pure logic extracted during
  implementation gets tests per convention (seeded `Random` only).
- **Build gate:** full `./gradlew build --no-configuration-cache` green locally before every
  push, per CLAUDE.md.
- **QA review:** independent reviewer subagent per PR; HIGH findings fixed in-PR, never filed
  as follow-ups.
- **Playtest 3 (manual, post-merge):** minimal checklist —
  1. Trigger Rebellion, wait 2 min → cows genuinely stop attacking (not just retarget).
  2. Trigger Rebellion, save/quit/rejoin mid-window → no duplicate aggression, expiry still works.
  3. Convert a protector, unload its chunk past the 30 s window, return → mob is passive.
  4. Morph (Mythic), log out mid-morph, log back in → no ghost cow.
  5. `ALL_COWS` mode + morph → companion cow does not glow, resize, or explode.
  6. Save/reload an OP cow repeatedly → combat behavior unchanged (no goal stacking).

---

## Delivery

| PR | Issues | Branch | Depends on |
|----|--------|--------|------------|
| PR-A | #19 | `fix/temporary-behavior-framework` | — |
| PR-B | #22 | `fix/effect-handler-scan-hoisting` | PR-A (draft until merged) |
| PR-C | #20, #21 | `fix/companion-cow-lifecycle` | — (parallel) |

Worktrees under `code/worktrees/mod-op-cows/`; implementation via implementer/QA subagents;
Matt merges. Follow-up implementation plan: `2026-07-14-behavior-fixes-implementation.md`.
