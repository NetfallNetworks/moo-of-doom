# Multi-Loader Rearchitecture — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restructure Moo of Doom from a single-project NeoForge mod into a
`common` + `neoforge` multi-module Gradle project using Architectury Loom.
NeoForge remains the only loader — Fabric is added later in a separate effort
once this architecture is proven.

**Architecture:** Gradle multi-project with two modules. `common` holds all
game logic, items, goals, registry definitions, and tests. `neoforge` holds
only NeoForge-specific wiring: entry point, event subscriptions, config spec,
and client setup. Architectury Loom provides the Minecraft toolchain for both
modules and sets the stage for a future `fabric` module.

**Tech Stack:** Architectury Loom, Architectury API, NeoForge, Gradle 9.x,
Java 21, JUnit 5

---

## Why Derisk First

Adding Fabric to a monolithic NeoForge project involves two independent risk
categories:

1. **Build system + code separation** — Converting a single-project Gradle
   build into Architectury multi-module, moving files, abstracting config and
   events. This is where most things break.
2. **Fabric-specific wiring** — Writing Fabric event callbacks, mixins for
   missing events, `fabric.mod.json`, client initializer. This is new code, not
   refactoring.

By doing (1) first against NeoForge alone, we validate the entire architecture
without writing any Fabric code. If the NeoForge build still works and tests
pass after restructuring, adding Fabric becomes a straightforward additive
task with no refactoring risk.

---

## Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Multi-loader toolchain | Architectury Loom + API | Need it eventually for Fabric; better to prove it works now than discover issues later |
| Modules in this phase | `common` + `neoforge` only | Derisk the restructure; Fabric module is a separate effort |
| Config abstraction | `ModConfigValues` static class in common | Simplest approach; NeoForge `ModConfigSpec` syncs values on load/reload |
| Registry approach | Architectury `DeferredRegister` in common | Drop-in replacement for NeoForge `DeferredRegister`; works on both loaders |
| Event handling | Common handlers accept vanilla params; NeoForge module dispatches | Clean seam; each handler is testable without a loader |
| CI | Single `neoforge:build` task (no matrix yet) | Add matrix when Fabric module exists |

---

## Codebase Audit Summary

30 Java source files audited. Full details in the previous (superseded)
plan revision — summary:

| Category | Files | Migration |
|----------|-------|-----------|
| Pure vanilla (no NeoForge imports) | 6 | Move to `common/` unchanged |
| Mostly vanilla (`@SubscribeEvent` only) | 14 | Move logic to `common/`, strip annotations |
| Platform-specific (registries, config, entry point) | 10 | Split: abstractions in `common/`, wiring in `neoforge/` |

**NeoForge event types used (6 unique):**

| Event | Handler count | Common signature after extraction |
|-------|--------------|-----------------------------------|
| `EntityTickEvent.Post` | 8 | `onTick(Entity entity)` |
| `PlayerInteractEvent.EntityInteract` | 3 | `onInteract(Player player, Entity target, InteractionHand hand, Level level)` |
| `LivingIncomingDamageEvent` | 2 | `onDamage(LivingEntity entity, DamageSource source, float amount)` → returns modified amount or -1 to cancel |
| `LivingDeathEvent` | 2 | `onDeath(LivingEntity entity, DamageSource source)` |
| `EntityJoinLevelEvent` | 1 | `onEntityJoinLevel(Entity entity, Level level)` |
| `EntityRenderersEvent.RegisterRenderers` | 1 | stays in `neoforge/client/` |

---

### Task 0: Gradle Multi-Project Scaffolding

**Goal:** Convert to Architectury Loom multi-project with `common` + `neoforge`.

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle` (becomes root orchestrator)
- Create: `common/build.gradle`
- Create: `neoforge/build.gradle`
- Modify: `gradle.properties` (add Architectury version)

**Step 1: Update `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        maven { url "https://maven.architectury.dev/" }
        maven { url "https://maven.neoforged.net/releases/" }
        gradlePluginPortal()
    }
}

include 'common', 'neoforge'
```

**Step 2: Root `build.gradle`**

Strip mod-specific build logic. Apply Architectury plugin, define shared
repositories and Java toolchain in `subprojects {}`.

**Step 3: `common/build.gradle`**

- Architectury common module: `architectury { common(["neoforge"]) }`
- Dependencies: Minecraft (via Loom), Architectury API common
- JUnit 5 test dependencies

**Step 4: `neoforge/build.gradle`**

- Architectury NeoForge module: `architectury { neoForge() }`
- Dependencies: `common` project, NeoForge, Architectury API NeoForge
- `processResources` for `neoforge.mods.toml` template vars

**Step 5: `gradle.properties`**

Add:
```properties
architectury_version=<latest for 1.21.1>
```

Keep existing `neo_version`, `minecraft_version`, etc.

**Acceptance:** `./gradlew build --no-configuration-cache` succeeds with empty
`common/src` and `neoforge/src` directories (or with a single stub class each).

---

### Task 1: Move Pure-Vanilla Code to `common/`

**Goal:** Move files that have zero NeoForge imports.

**Files to move (unchanged):**

```
common/src/main/java/com/github/netfallnetworks/mooofdoom/
├── rarity/
│   ├── RarityTier.java
│   └── TieredRandom.java
├── cow/
│   ├── DoomAppleItem.java
│   ├── combat/
│   │   ├── ChargeGoal.java
│   │   ├── HostileTargetGoal.java
│   │   └── MilkProjectileGoal.java
│   └── utility/
│       └── BuffBucketItem.java
```

**Tests to move (unchanged):**

```
common/src/test/java/com/github/netfallnetworks/mooofdoom/
├── rarity/TieredRandomTest.java
└── cow/utility/MoocowMultiplierTest.java
```

**Acceptance:** `./gradlew common:test --no-configuration-cache` passes.

---

### Task 2: Config Abstraction

**Goal:** Decouple handler logic from `ModConfigSpec` so common code never
imports NeoForge.

**Files:**
- Create: `common/.../config/ModConfigValues.java`
- Move + modify: `ModConfig.java` → `neoforge/.../NeoForgeConfig.java`
- Modify: all handler files in `common/` (find-replace config access)

**Step 1: Create `ModConfigValues.java` in `common/`**

Plain static fields with defaults for every config value. No NeoForge imports.
This is the single source of truth that all common code reads from.

```java
public final class ModConfigValues {
    public static ActivationMode activationMode = ActivationMode.ITEM_ACTIVATED;
    public static int cowHealth = 100;
    public static double cowDamage = 15.0;
    public static boolean enableChargeAttack = true;
    // ... all 55 fields ...
    private ModConfigValues() {}
}
```

**Step 2: Move `ModConfig.java` to `neoforge/` as `NeoForgeConfig.java`**

Keep all `ModConfigSpec` logic. Add a `syncToCommon()` method that copies
every spec value into `ModConfigValues`. Call it on config load and reload.

**Step 3: Update handler references in `common/`**

Find-replace across all handler files:
- `ModConfig.COW_HEALTH.getAsInt()` → `ModConfigValues.cowHealth`
- `ModConfig.ENABLE_CHARGE.get()` → `ModConfigValues.enableChargeAttack`
- etc. for all 55 fields

**Acceptance:** `common/` module compiles with no NeoForge imports in config
access paths.

---

### Task 3: Registry Migration to Architectury

**Goal:** Replace NeoForge `DeferredRegister` with Architectury's cross-platform
equivalent so registries live in `common/`.

**Files to move + modify:**
- `ModItems.java` → `common/.../registry/ModItems.java`
- `ModEntityTypes.java` → `common/.../registry/ModEntityTypes.java`
- `ModEffects.java` → `common/.../registry/ModEffects.java`
- `ModCriteriaTriggers.java` → `common/.../registry/ModCriteriaTriggers.java`
- `ModSimpleTrigger.java` → `common/.../registry/ModSimpleTrigger.java`

**Migration pattern (same for all):**

```java
// Before (NeoForge)
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

// After (Architectury)
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
```

- `DeferredItem<T>` → `RegistrySupplier<T>`
- `DeferredHolder<T, T>` → `RegistrySupplier<T>`
- `REGISTER.register(modEventBus)` → `REGISTER.register()` (called from common init)

**Also move:** `MilkProjectile.java` to `common/` — its only NeoForge coupling
is the entity type supplier reference, which becomes an Architectury
`RegistrySupplier` after this task.

**Acceptance:** `./gradlew common:build --no-configuration-cache` compiles all
registry classes.

---

### Task 4: Extract Handler Logic to `common/`

**Goal:** Move all 14 event handler files to `common/`, stripping NeoForge
annotations. Create a thin dispatcher in `neoforge/`.

**Step 1: Transform each handler**

For every handler file currently using `@SubscribeEvent`:

1. Move to `common/src/main/java/.../<same-package>/`
2. Remove `@SubscribeEvent` annotation
3. Remove NeoForge event type imports
4. Change method signature to accept vanilla parameters (see audit table above)
5. Keep all game logic unchanged

Example — `AuraHandler.java`:

```java
// Before
@SubscribeEvent
public static void onCowTick(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof Cow cow)) return;
    // aura logic
}

// After
public static void onCowTick(Entity entity) {
    if (!(entity instanceof Cow cow)) return;
    // identical aura logic
}
```

**All 14 files:**

| File | Events stripped |
|------|---------------|
| `AuraHandler` | `EntityTickEvent.Post` |
| `CowMorphHandler` | `EntityTickEvent.Post` |
| `DoomAppleUseHandler` | `PlayerInteractEvent.EntityInteract` |
| `ExplosionHandler` | `EntityTickEvent.Post` |
| `GuardianHandler` | `EntityInteract` + `EntityTickEvent.Post` |
| `LootDropHandler` | `EntityTickEvent.Post` |
| `MilkingHandler` | `EntityInteract` |
| `MobConversionHandler` | `EntityTickEvent.Post` |
| `MoonJumpHandler` | `EntityTickEvent.Post` |
| `OpCowDeathHandler` | `LivingDeathEvent` |
| `OpCowManager` | `EntityJoinLevelEvent` + `LivingIncomingDamageEvent` |
| `RebellionHandler` | `LivingIncomingDamageEvent` + `LivingDeathEvent` + `EntityTickEvent.Post` |
| `SizeChangeHandler` | `EntityTickEvent.Post` |
| `CombatLootHandler` | `LivingIncomingDamageEvent` + `LivingDeathEvent` |

**Step 2: Create `neoforge/.../NeoForgeEventHandler.java`**

Single class with `@SubscribeEvent` methods that delegate to common handlers:

```java
public class NeoForgeEventHandler {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        AuraHandler.onCowTick(entity);
        CowMorphHandler.onCowTick(entity);
        ExplosionHandler.onCowTick(entity);
        // ... all tick handlers ...
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        DoomAppleUseHandler.onInteract(event.getEntity(), event.getTarget(), ...);
        // ... all interact handlers ...
    }

    // ... onDamage, onDeath, onEntityJoin ...
}
```

**Acceptance:** `common/` has zero NeoForge imports. `neoforge/` compiles and
delegates correctly.

---

### Task 5: Entry Point Split

**Goal:** Split `MooOfDoom.java` into common init + NeoForge entry point.

**Files:**
- Modify: `MooOfDoom.java` → `common/.../MooOfDoom.java` (shared constants + init)
- Create: `neoforge/.../NeoForgeMooOfDoom.java` (NeoForge `@Mod` entry point)

**`common/.../MooOfDoom.java`:**
```java
public class MooOfDoom {
    public static final String MODID = "mooofdoom";

    public static void init() {
        ModItems.init();
        ModEntityTypes.init();
        ModEffects.init();
        ModCriteriaTriggers.init();
    }
}
```

**`neoforge/.../NeoForgeMooOfDoom.java`:**
```java
@Mod(MooOfDoom.MODID)
public class NeoForgeMooOfDoom {
    public NeoForgeMooOfDoom(IEventBus modEventBus, ModContainer modContainer) {
        MooOfDoom.init();
        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        NeoForge.EVENT_BUS.register(NeoForgeEventHandler.class);
    }
}
```

---

### Task 6: Client Setup

**Files:**
- Move + modify: `ClientSetup.java` → `neoforge/.../client/NeoForgeClientSetup.java`

This file stays entirely in `neoforge/` — it uses `EntityRenderersEvent.RegisterRenderers`
which is NeoForge-specific. The renderer registration is one line; there's no
common logic to extract.

---

### Task 7: Resource Relocation

**Files:**
- Move to `common/src/main/resources/`:
  - `assets/mooofdoom/` (textures, item models, lang)
  - `data/mooofdoom/` (advancements, recipes)
- Move to `neoforge/src/main/templates/`:
  - `META-INF/neoforge.mods.toml`

Architectury Loom merges common resources into each loader JAR automatically.

---

### Task 8: CI Updates

**Files:**
- Modify: `.github/workflows/build.yml`
- Modify: `.github/workflows/release.yml`

**`build.yml` changes:**
- Build command: `./gradlew build --no-configuration-cache` (builds all modules)
- Test command: `./gradlew common:test --no-configuration-cache`
- Artifact path: `neoforge/build/libs/*.jar`

**`release.yml` changes:**
- JAR path: `neoforge/build/libs/*.jar`
- No matrix needed yet — single NeoForge JAR

When Fabric is added later, both workflows get a `matrix.loader` strategy.

---

### Task 9: Verify

1. `./gradlew clean build --no-configuration-cache` — full build succeeds
2. `./gradlew common:test --no-configuration-cache` — all unit tests pass
3. Verify the NeoForge JAR is produced at `neoforge/build/libs/`
4. Verify zero NeoForge imports in `common/src/main/java/` (grep check)
5. Manual playtest: load the NeoForge JAR, confirm all OP cow mechanics work

---

## Final Project Structure (This Phase)

```
moo-of-doom/
├── settings.gradle                      (includes common, neoforge)
├── build.gradle                         (architectury plugin, shared config)
├── gradle.properties                    (+ architectury_version)
├── common/
│   ├── build.gradle
│   └── src/
│       ├── main/java/.../mooofdoom/
│       │   ├── MooOfDoom.java           (MODID + init)
│       │   ├── config/
│       │   │   └── ModConfigValues.java (static fields, no NeoForge)
│       │   ├── registry/               (Architectury DeferredRegister)
│       │   │   ├── ModItems.java
│       │   │   ├── ModEntityTypes.java
│       │   │   ├── ModEffects.java
│       │   │   ├── ModCriteriaTriggers.java
│       │   │   └── ModSimpleTrigger.java
│       │   ├── cow/                    (all handlers, items, goals, entity)
│       │   └── rarity/                 (RarityTier, TieredRandom)
│       ├── main/resources/
│       │   ├── assets/mooofdoom/       (textures, models, lang)
│       │   └── data/mooofdoom/         (advancements, recipes)
│       └── test/java/                  (unit tests)
├── neoforge/
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../mooofdoom/neoforge/
│       │   ├── NeoForgeMooOfDoom.java   (@Mod entry point)
│       │   ├── NeoForgeConfig.java      (ModConfigSpec → syncs to ModConfigValues)
│       │   ├── NeoForgeEventHandler.java (dispatches to common handlers)
│       │   └── client/
│       │       └── NeoForgeClientSetup.java
│       └── templates/META-INF/
│           └── neoforge.mods.toml
├── .github/workflows/
│   ├── build.yml
│   └── release.yml
└── docs/
```

---

## What This Proves for Fabric

After this phase completes successfully, adding Fabric requires only:

1. Add `'fabric'` to `settings.gradle` includes
2. Create `fabric/build.gradle` with Fabric Loader + API dependencies
3. Create `fabric/.../FabricMooOfDoom.java` (`ModInitializer` → calls `MooOfDoom.init()`)
4. Create `fabric/.../FabricEventHandler.java` (Fabric callbacks → same common handlers)
5. Create `fabric/.../FabricConfig.java` (JSON config → syncs to `ModConfigValues`)
6. Create `fabric/.../client/FabricClientSetup.java` (entity renderer registration)
7. Create `fabric/src/main/resources/fabric.mod.json`
8. Add matrix build to CI

No refactoring of `common/` needed. No changes to NeoForge module. Pure additive work.
