# Temporary-Behavior Framework & Companion Cow Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix issues #19–#22 via a tag-gated behavior framework, companion-cow lifecycle
hardening, and scan hoisting — as 3 PRs in 2 parallel lanes.
**Architecture:** Goals are installed idempotently and gate themselves on persistent entity
tags (`GatedGoal` decorator + `TemporaryBehavior` template base with final lifecycle methods).
Deactivation strips a tag; nothing is ever removed from a goal selector, so nothing can leak.
A source-scanning JUnit test enforces the boundary in CI.
**Tech Stack:** Java 21, NeoForge 21.11.38-beta (MC 1.21.11), Gradle 9.2.1, JUnit 5.
**Design doc:** `docs/plans/2026-07-14-behavior-fixes-design.md`

## Global Constraints

- Never commit on local `main`; each PR lane works in its own worktree under
  `C:/Users/mxm58/Documents/code/worktrees/mod-op-cows/<branch-slug>` branched off `origin/main`.
- NEVER use `git stash` (shared across worktrees), `git reset --hard`, `git checkout --`,
  or any destructive git operation. No `--no-verify`.
- Full `./gradlew build --no-configuration-cache` green before every push
  (set `JAVA_HOME` to `C:/Users/mxm58/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`
  if `java` is not on PATH). Do NOT push on a red build.
- Tests must be deterministic — seed any `Random`.
- `git diff package.json` does not apply here, but DO review `git diff --stat` before staging;
  stage only files you intentionally changed.
- Commit messages use the repo's `feat:/fix:/refactor:/test:/docs:` prefixes.
- PR bodies: full issue URLs (never bare #N), a **Blast radius** section naming each changed
  method's consumers and what could regress, and end with the Claude Code attribution footer.
- API-signature caution: code below is written against NeoForge 21.11.38-beta with Parchment
  mappings. If a vanilla signature differs (e.g. `Goal.getFlags()` shape), keep the stated
  semantics and let the compiler guide the adjustment — the build gate catches drift.

---

## Lane 1, PR-A — `fix/temporary-behavior-framework` (closes #19)

### Task A1: `GatedGoal` decorator

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/GatedGoal.java`

**Interfaces:**
- Produces: `GatedGoal(String behaviorId, BooleanSupplier gate, Goal delegate)`;
  `String getBehaviorId()`. Used by `TemporaryBehavior` (A2) to install and detect goals.

- [ ] **Step 1: Write the class**

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.function.BooleanSupplier;

/**
 * Decorator that wraps any vanilla Goal and gates its activation on a predicate
 * (typically "entity has behavior tag X"). When the gate is false the goal is inert.
 * Nothing is ever removed from a goal selector — deactivation just flips the gate —
 * so goal leaks are structurally impossible. Carries a behaviorId so installers can
 * detect an existing install by scanning the selector.
 */
public class GatedGoal extends Goal {

    private final String behaviorId;
    private final BooleanSupplier gate;
    private final Goal delegate;

    public GatedGoal(String behaviorId, BooleanSupplier gate, Goal delegate) {
        this.behaviorId = behaviorId;
        this.gate = gate;
        this.delegate = delegate;
        this.setFlags(delegate.getFlags());
    }

    public String getBehaviorId() {
        return behaviorId;
    }

    @Override
    public boolean canUse() {
        return gate.getAsBoolean() && delegate.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return gate.getAsBoolean() && delegate.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return delegate.isInterruptable();
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return delegate.requiresUpdateEveryTick();
    }

    @Override
    public void tick() {
        delegate.tick();
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileJava --no-configuration-cache`
Expected: `BUILD SUCCESSFUL`. If `getFlags()`/`setFlags(...)` signatures differ in this
mapping set, mirror the delegate's flags however the current `Goal` API expresses them —
the requirement is only that the wrapper claims the same flags as its delegate.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/GatedGoal.java
git commit -m "feat: add GatedGoal decorator for tag-gated AI goals"
```

---

### Task A2: `TemporaryBehavior` template base

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/TemporaryBehavior.java`

**Interfaces:**
- Consumes: `GatedGoal` from A1.
- Produces: `void activate(Mob)`, `void deactivate(Mob)`, `boolean isActive(Mob)`,
  `void ensureInstalled(Mob)` — all final. Subclass contract: `String tag()`,
  `List<GoalSpec> createGoals(Mob)`. `GoalSpec(boolean targetSelector, int priority, Goal goal)`.

- [ ] **Step 1: Write the class**

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.List;

/**
 * Template base for tag-gated AI behaviors (Rebellion, Guardian, Protector, OP combat).
 *
 * Lifecycle invariants are implemented final so subclasses cannot get them wrong:
 * - ensureInstalled: idempotent by scanning the live selector for this behavior's
 *   GatedGoals — no external state, self-heals across chunk reload (goal selectors are
 *   never serialized; tags are).
 * - deactivate: strips the tag. The GatedGoal gate goes false and the goals are inert
 *   immediately. Nothing is ever removed, so nothing can leak (issue #19).
 *
 * Subclasses declare only tag() and createGoals(). This class is the ONLY place in the
 * mod allowed to call GoalSelector.addGoal — enforced by BehaviorArchitectureTest.
 */
public abstract class TemporaryBehavior {

    /** One goal to install: which selector, at what priority. */
    public record GoalSpec(boolean targetSelector, int priority, Goal goal) {}

    /** Persistent entity tag that marks this behavior active. */
    public abstract String tag();

    /** The goals this behavior needs. Called at most once per entity per session. */
    protected abstract List<GoalSpec> createGoals(Mob mob);

    public final boolean isActive(Mob mob) {
        return mob.getTags().contains(tag());
    }

    public final void activate(Mob mob) {
        ensureInstalled(mob);
        mob.addTag(tag());
    }

    public final void deactivate(Mob mob) {
        mob.removeTag(tag());
    }

    public final void ensureInstalled(Mob mob) {
        if (isInstalled(mob.goalSelector) || isInstalled(mob.targetSelector)) return;
        for (GoalSpec spec : createGoals(mob)) {
            GatedGoal gated = new GatedGoal(tag(), () -> isActive(mob), spec.goal());
            GoalSelector selector = spec.targetSelector() ? mob.targetSelector : mob.goalSelector;
            selector.addGoal(spec.priority(), gated);
        }
    }

    private boolean isInstalled(GoalSelector selector) {
        for (WrappedGoal wrapped : selector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof GatedGoal gated
                    && gated.getBehaviorId().equals(tag())) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileJava --no-configuration-cache`
Expected: `BUILD SUCCESSFUL`. If `getAvailableGoals()` is named differently in this mapping
set, the requirement is: iterate the selector's registered goals and unwrap to the raw Goal.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/TemporaryBehavior.java
git commit -m "feat: add TemporaryBehavior template base with final lifecycle methods"
```

---

### Task A3: Behavior subclasses

**Files:**
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/RebellionBehavior.java`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/GuardianBehavior.java`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/ProtectorBehavior.java`
- Create: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/OpCowCombatBehavior.java`

**Interfaces:**
- Consumes: `TemporaryBehavior`, `GoalSpec` from A2.
- Produces: `RebellionBehavior.INSTANCE` (tag `MooOfDoom_Rebel`),
  `GuardianBehavior.INSTANCE` (tag `MooOfDoom_Guardian`),
  `ProtectorBehavior.INSTANCE` (tag `MooOfDoom_Protector`),
  `OpCowCombatBehavior.INSTANCE` (tag `MooOfDoom` — the existing OP tag).
  Handlers in A4–A7 call `activate`/`deactivate`/`isActive` on these.

- [ ] **Step 1: Write RebellionBehavior**

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Rebel cows melee-attack players while the rebel tag is present. */
public final class RebellionBehavior extends TemporaryBehavior {

    public static final RebellionBehavior INSTANCE = new RebellionBehavior();
    public static final String TAG = "MooOfDoom_Rebel";

    private RebellionBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        return List.of(
                new GoalSpec(false, 1, new MeleeAttackGoal((PathfinderMob) mob, 1.2, true)),
                new GoalSpec(true, 1, new NearestAttackableTargetGoal<>(mob, Player.class, true)));
    }
}
```

- [ ] **Step 2: Write GuardianBehavior**

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

/** Guardian cows melee-attack monsters while the guardian tag is present. */
public final class GuardianBehavior extends TemporaryBehavior {

    public static final GuardianBehavior INSTANCE = new GuardianBehavior();
    public static final String TAG = "MooOfDoom_Guardian";

    private GuardianBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        return List.of(
                new GoalSpec(false, 1, new MeleeAttackGoal((PathfinderMob) mob, 1.2, true)),
                new GoalSpec(true, 1, new NearestAttackableTargetGoal<>(mob, Monster.class, true)));
    }
}
```

- [ ] **Step 3: Write ProtectorBehavior**

Converted protectors may be non-PathfinderMob (current code guards this), so the melee
goal is conditional:

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.List;

/** Converted "protector" mobs fight monsters while the protector tag is present. */
public final class ProtectorBehavior extends TemporaryBehavior {

    public static final ProtectorBehavior INSTANCE = new ProtectorBehavior();
    public static final String TAG = "MooOfDoom_Protector";

    private ProtectorBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        List<GoalSpec> goals = new ArrayList<>();
        if (mob instanceof PathfinderMob pathfinderMob) {
            goals.add(new GoalSpec(false, 1, new MeleeAttackGoal(pathfinderMob, 1.2, true)));
        }
        goals.add(new GoalSpec(true, 1, new NearestAttackableTargetGoal<>(mob, Monster.class, true)));
        return goals;
    }
}
```

- [ ] **Step 4: Write OpCowCombatBehavior**

Gated on the existing OP tag; goal set and config gates copied from
`OpCowManager.addCombatGoals` (which A7 deletes):

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import com.github.netfallnetworks.mooofdoom.ModConfig;
import com.github.netfallnetworks.mooofdoom.cow.combat.ChargeGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.HostileTargetGoal;
import com.github.netfallnetworks.mooofdoom.cow.combat.MilkProjectileGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;

import java.util.ArrayList;
import java.util.List;

/** OP cow combat AI (charge, milk projectile, hostile targeting), gated on the OP tag. */
public final class OpCowCombatBehavior extends TemporaryBehavior {

    public static final OpCowCombatBehavior INSTANCE = new OpCowCombatBehavior();
    public static final String TAG = "MooOfDoom"; // OpCowManager.OP_TAG

    private OpCowCombatBehavior() {}

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    protected List<GoalSpec> createGoals(Mob mob) {
        Cow cow = (Cow) mob;
        List<GoalSpec> goals = new ArrayList<>();
        goals.add(new GoalSpec(true, 1, new HostileTargetGoal(cow)));
        if (ModConfig.CHARGE_ATTACK_ENABLED.getAsBoolean()) {
            goals.add(new GoalSpec(false, 2, new ChargeGoal(cow)));
        }
        if (ModConfig.MILK_PROJECTILE_ENABLED.getAsBoolean()) {
            goals.add(new GoalSpec(false, 3, new MilkProjectileGoal(cow)));
        }
        return goals;
    }
}
```

- [ ] **Step 5: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/behavior/
git commit -m "feat: add Rebellion/Guardian/Protector/OpCowCombat behavior subclasses"
```

---

### Task A4: Migrate `RebellionHandler`

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/RebellionHandler.java`

**Interfaces:**
- Consumes: `RebellionBehavior.INSTANCE` from A3.

- [ ] **Step 1: Replace goal mutation with behavior calls**

In `onPlayerTick`, replace the hot-loop body (currently lines 89–102):

```java
            for (Cow cow : nearbyCows) {
                if (OpCowManager.isOpCow(cow)) {
                    // OP cows: just set target to the player
                    cow.setTarget(player);
                } else {
                    // Vanilla cows: activate tag-gated rebel combat AI
                    RebellionBehavior.INSTANCE.activate(cow);
                    cow.setTarget(player);
                }
            }
```

In the cleanup branch, replace the tagged-cow check and clearing (currently lines 111–127):

```java
            for (Cow cow : nearbyCows) {
                if (RebellionBehavior.INSTANCE.isActive(cow) && !OpCowManager.isOpCow(cow)) {
                    // Check if any nearby player still has rebellion
                    boolean anyRebellion = false;
                    List<Player> nearbyPlayers = cow.level().getEntitiesOfClass(Player.class,
                            cow.getBoundingBox().inflate(ModConfig.REBELLION_RANGE.getAsInt()));
                    for (Player p : nearbyPlayers) {
                        if (p.hasEffect(ModEffects.REBELLION)) {
                            anyRebellion = true;
                            break;
                        }
                    }
                    if (!anyRebellion) {
                        RebellionBehavior.INSTANCE.deactivate(cow);
                        cow.setTarget(null);
                    }
                }
            }
```

Delete the `REBEL_TAG` constant and the now-unused imports
(`MeleeAttackGoal`, `NearestAttackableTargetGoal`). Add
`import com.github.netfallnetworks.mooofdoom.cow.behavior.RebellionBehavior;`.

- [ ] **Step 2: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/RebellionHandler.java
git commit -m "fix: rebel cow AI deactivates via tag gate instead of leaking goals"
```

---

### Task A5: Migrate `GuardianHandler`

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/GuardianHandler.java`

**Interfaces:**
- Consumes: `GuardianBehavior.INSTANCE` from A3.

- [ ] **Step 1: Replace goal mutation with behavior calls**

Preserve current semantics exactly: ALL nearby cows get the guardian tag; only non-OP cows
get combat goals. In `onPlayerTick`'s hot loop, replace the tag-and-add block (currently
lines 69–77):

```java
                // Activate guardian AI (tag-gated) — OP cows keep their own combat AI
                if (!OpCowManager.isOpCow(cow)) {
                    GuardianBehavior.INSTANCE.activate(cow);
                } else if (!GuardianBehavior.INSTANCE.isActive(cow)) {
                    cow.addTag(GuardianBehavior.TAG);
                }
```

In the cleanup branch, replace the tag check and clearing (currently lines 111–127):

```java
                if (GuardianBehavior.INSTANCE.isActive(cow)) {
                    // Check if any nearby player still has guardian
                    boolean anyGuardian = false;
                    List<Player> nearbyPlayers = cow.level().getEntitiesOfClass(Player.class,
                            cow.getBoundingBox().inflate(ModConfig.GUARDIAN_RANGE.getAsInt()));
                    for (Player p : nearbyPlayers) {
                        if (p.hasEffect(ModEffects.GUARDIAN)) {
                            anyGuardian = true;
                            break;
                        }
                    }
                    if (!anyGuardian) {
                        GuardianBehavior.INSTANCE.deactivate(cow);
                        cow.setTarget(null);
                        cow.getNavigation().stop();
                    }
                }
```

Delete the `GUARDIAN_TAG` constant and unused goal imports; add the
`GuardianBehavior` import.

- [ ] **Step 2: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/GuardianHandler.java
git commit -m "fix: guardian cow AI deactivates via tag gate instead of leaking goals"
```

---

### Task A6: Migrate `MobConversionHandler` — persistent protector expiry

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/MobConversionHandler.java`

**Interfaces:**
- Consumes: `ProtectorBehavior.INSTANCE` from A3.
- Produces: persistent-data keys `MooOfDoom_ProtectorOwner` (UUID as String) and
  `MooOfDoom_ProtectorUntil` (long, absolute game time).

Deletes the static `protectors` map (leaks on chunk unload — issue #19) and the
`ProtectorData` record. Expiry moves from `mob.tickCount` (resets on reload — latent bug:
the 30 s window restarted every reload) to absolute `level.getGameTime()` in the entity's
persistent data, which survives unload/reload.

- [ ] **Step 1: Replace `applyFollowingProtector`**

```java
    private static final String PROTECTOR_OWNER_KEY = "MooOfDoom_ProtectorOwner";
    private static final String PROTECTOR_UNTIL_KEY = "MooOfDoom_ProtectorUntil";

    private static void applyFollowingProtector(Mob mob, Player player) {
        // Cancel hostility toward the player
        mob.setTarget(null);

        // Tag-gated protector combat AI (goals go inert when the tag is stripped)
        ProtectorBehavior.INSTANCE.activate(mob);

        // Persist owner + absolute expiry so the window survives chunk unload/reload
        mob.getPersistentData().putString(PROTECTOR_OWNER_KEY, player.getUUID().toString());
        mob.getPersistentData().putLong(PROTECTOR_UNTIL_KEY, mob.level().getGameTime() + 600);

        // Visual effects
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    mob.getX(), mob.getY() + 1, mob.getZ(),
                    15, 0.5, 0.5, 0.5, 0.1);
        }
        mob.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }
```

- [ ] **Step 2: Replace `onMobTick`**

```java
    /**
     * Tick handler: make protector mobs follow their owner and expire after 30s (game time).
     */
    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (!ProtectorBehavior.INSTANCE.isActive(mob)) return;

        // Check expiry against absolute game time (survives unload/reload)
        long until = mob.getPersistentData().getLongOr(PROTECTOR_UNTIL_KEY, 0L);
        if (mob.level().getGameTime() >= until || !mob.isAlive()) {
            ProtectorBehavior.INSTANCE.deactivate(mob);
            mob.setTarget(null);
            return;
        }

        // Navigate toward owner (every 20 ticks for performance)
        if (mob.tickCount % 20 == 0) {
            ServerLevel level = (ServerLevel) mob.level();
            String ownerString = mob.getPersistentData().getStringOr(PROTECTOR_OWNER_KEY, "");
            if (ownerString.isEmpty()) return;
            Player owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(ownerString));
            if (owner != null && owner.level() == mob.level()) {
                double distSq = mob.distanceToSqr(owner);
                if (distSq > 9.0) {
                    mob.getNavigation().moveTo(owner, 1.2);
                }

                // Target nearby hostile mobs threatening the owner
                List<Monster> nearbyMonsters = mob.level().getEntitiesOfClass(Monster.class,
                        owner.getBoundingBox().inflate(16));
                Monster closest = null;
                double closestDist = Double.MAX_VALUE;
                for (Monster monster : nearbyMonsters) {
                    if (monster == mob) continue;
                    if (ProtectorBehavior.INSTANCE.isActive(monster)) continue; // Don't target other protectors
                    double d = mob.distanceToSqr(monster);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = monster;
                    }
                }
                if (closest != null) {
                    mob.setTarget(closest);
                }
            }
        }
    }
```

Delete the `protectors` map, the `ProtectorData` record, and now-unused imports
(`MeleeAttackGoal`, `NearestAttackableTargetGoal`, `HashMap`, `Map`, `PathfinderMob` if
unused). Keep `UUID` and `List`. Add the `ProtectorBehavior` import.

If `getLongOr`/`getStringOr` don't exist in this mapping set, use the equivalent
Optional-returning getters with a default (`getLong(key).orElse(0L)`); the requirement is
read-with-default from the persistent-data `CompoundTag`.

- [ ] **Step 3: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/MobConversionHandler.java
git commit -m "fix: protector expiry via persistent game-time deadline, delete leaking map"
```

---

### Task A7: Migrate `OpCowManager`

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`

**Interfaces:**
- Consumes: `OpCowCombatBehavior.INSTANCE` from A3.
- Produces: `makeOpCow`/join-reload path no longer stack duplicate goals across reloads.

- [ ] **Step 1: Replace `addCombatGoals` with the behavior**

Delete the `addCombatGoals` method entirely. In `makeOpCow`, replace the
`addCombatGoals(cow)` call:

```java
    public static void makeOpCow(Cow cow) {
        cow.addTag(OP_TAG);
        boostAttributes(cow);
        cow.setGlowingTag(true);
        OpCowCombatBehavior.INSTANCE.ensureInstalled(cow);
    }
```

In `onEntityJoinLevel`, replace the already-OP branch:

```java
        // If already OP, re-apply attributes (they reset on load) and reinstall
        // combat goals idempotently (goal selectors are never serialized)
        if (isOpCow(cow)) {
            boostAttributes(cow);
            cow.setGlowingTag(true);
            OpCowCombatBehavior.INSTANCE.ensureInstalled(cow);
            return;
        }
```

Remove now-unused imports (`ChargeGoal`, `HostileTargetGoal`, `MilkProjectileGoal`); add
`import com.github.netfallnetworks.mooofdoom.cow.behavior.OpCowCombatBehavior;`.

- [ ] **Step 2: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java
git commit -m "fix: OP cow combat goals install idempotently, no reload stacking"
```

---

### Task A8: Architecture invariant test

**Files:**
- Create: `src/test/java/com/github/netfallnetworks/mooofdoom/cow/behavior/BehaviorArchitectureTest.java`

Plain JUnit source scan — no Minecraft classloading, per repo test convention.

- [ ] **Step 1: Write the test**

```java
package com.github.netfallnetworks.mooofdoom.cow.behavior;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture invariant for issue #19: goal installation must go through
 * TemporaryBehavior so AI goals are always tag-gated and idempotently installed.
 * Direct GoalSelector.addGoal calls anywhere else reintroduce the goal-leak bug class.
 */
class BehaviorArchitectureTest {

    private static final String ALLOWED_FILE = "TemporaryBehavior.java";

    @Test
    void addGoalIsOnlyCalledInTemporaryBehavior() throws IOException {
        Path srcRoot = Path.of("src", "main", "java");
        assertTrue(Files.isDirectory(srcRoot), "run from the project root; missing " + srcRoot);

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(srcRoot)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals(ALLOWED_FILE)) continue;
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains(".addGoal(")) {
                        offenders.add(file + ":" + (i + 1));
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "GoalSelector.addGoal must only be called from TemporaryBehavior "
                        + "(tag-gated, idempotent — see issue #19). Offenders:\n"
                        + String.join("\n", offenders));
    }
}
```

- [ ] **Step 2: Run the test — verify it passes**

Run: `./gradlew test --no-configuration-cache --tests '*BehaviorArchitectureTest*'`
Expected: PASS (all `addGoal` calls were migrated in A4–A7).

- [ ] **Step 3: Verify the test actually catches violations**

Temporarily add a line `// probe` + `cow.goalSelector.addGoal(9, null);` inside any method
of `OpCowManager.java`, re-run the test, confirm it FAILS naming `OpCowManager.java`, then
remove the probe lines and confirm it passes again. Do not commit the probe.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/github/netfallnetworks/mooofdoom/cow/behavior/BehaviorArchitectureTest.java
git commit -m "test: enforce addGoal-only-in-TemporaryBehavior architecture invariant"
```

---

### Task A9: Full build, push, PR

- [ ] **Step 1: Full build**

Run: `./gradlew build --no-configuration-cache`
Expected: `BUILD SUCCESSFUL`, all tests green.

- [ ] **Step 2: Review the diff, push, open PR**

```bash
git diff origin/main --stat   # sanity: only intended files
git push -u origin fix/temporary-behavior-framework
```

Open a PR titled `fix: tag-gated behavior framework — temporary AI goals can no longer leak`.
Body must include: `Closes https://github.com/NetfallNetworks/moo-of-doom/issues/19`, a
summary of the framework, and a **Blast radius** section covering: `makeOpCow` (callers:
`DoomAppleUseHandler`, `MobConversionHandler.convertToCow`, `OpCowManager` join event),
`applyFollowingProtector` (caller: `applyHostileConversion`), rebel/guardian tag names
unchanged, protector expiry semantics changed from tickCount-relative to game-time-absolute
(window no longer restarts on reload). Poll `gh pr checks` after pushing.

---

## Lane 1, PR-B — `fix/effect-handler-scan-hoisting` (closes #22, stacked on PR-A)

Branch from `fix/temporary-behavior-framework` (NOT origin/main). Open as **DRAFT** with
first body line: `**Merge https://github.com/NetfallNetworks/moo-of-doom/pull/<PR-A> first.**`

### Task B1: Hoist the Guardian monster query

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/GuardianHandler.java`

The monster *query* depends only on `player` and hoists out of the cow loop. The
closest-monster *selection* is per-cow (distance from the cow) and stays inside.

- [ ] **Step 1: Hoist the query**

In the `hasGuardian` branch, immediately after `nearbyCows` is fetched, add:

```java
            // One query for all cows — the box depends only on the player (issue #22)
            List<Monster> nearbyMonsters = player.level().getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(range));
```

Inside the cow loop, delete the per-cow `nearbyMonsters` query (the two lines building it),
keeping the per-cow closest-monster selection loop that consumes the hoisted list.

- [ ] **Step 2: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/GuardianHandler.java
git commit -m "fix: hoist guardian monster query out of the per-cow loop"
```

---

### Task B2: Single rebellious-player query in Rebellion cleanup

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/RebellionHandler.java`

Replaces the per-cow `getEntitiesOfClass(Player.class, ...)` scan with one query per pass.
Semantics preserved: a cow stays rebellious if any player with the Rebellion effect is
within `REBELLION_RANGE` of that cow (AABB check, as `getEntitiesOfClass` uses).

- [ ] **Step 1: Rewrite the cleanup branch**

Replace the entire `else` branch of `onPlayerTick` with:

```java
        } else {
            // Clean up rebel cows near this player that no longer need to be aggressive
            // We do a wider range check to catch cows that were made rebels
            int range = ModConfig.REBELLION_RANGE.getAsInt();
            int searchRange = range + 8;
            AABB searchBox = player.getBoundingBox().inflate(searchRange);
            List<Cow> nearbyCows = player.level().getEntitiesOfClass(Cow.class, searchBox);
            if (nearbyCows.isEmpty()) return;

            // One query per pass instead of one per cow (issue #22): every player with
            // Rebellion who could be within `range` of any cow in the search box
            List<Player> rebelliousPlayers = player.level().getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(searchRange + range),
                    p -> p.hasEffect(ModEffects.REBELLION));

            for (Cow cow : nearbyCows) {
                if (RebellionBehavior.INSTANCE.isActive(cow) && !OpCowManager.isOpCow(cow)) {
                    AABB cowRange = cow.getBoundingBox().inflate(range);
                    boolean anyRebellion = false;
                    for (Player p : rebelliousPlayers) {
                        if (cowRange.intersects(p.getBoundingBox())) {
                            anyRebellion = true;
                            break;
                        }
                    }
                    if (!anyRebellion) {
                        RebellionBehavior.INSTANCE.deactivate(cow);
                        cow.setTarget(null);
                    }
                }
            }
        }
```

- [ ] **Step 2: Full build, push, draft PR**

Run: `./gradlew build --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/effects/RebellionHandler.java
git commit -m "fix: single rebellious-player query per cleanup pass"
git push -u origin fix/effect-handler-scan-hoisting
gh pr create --draft --base main --title "fix: hoist redundant entity scans in Guardian/Rebellion handlers"
```

PR body first line: bold merge-PR-A-first banner. `Closes
https://github.com/NetfallNetworks/moo-of-doom/issues/22`. Blast radius: `onPlayerTick` in
both handlers (no callers — event subscribers); behavior-identical except one fewer/wider
AABB query; regression risk = cleanup-range semantics (covered by preserved AABB checks).

---

## Lane 2, PR-C — `fix/companion-cow-lifecycle` (closes #20 and #21)

Branch from `origin/main`, parallel with Lane 1.

### Task C1: Morph lifecycle hardening in `CowMorphHandler`

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/CowMorphHandler.java`

**Interfaces:**
- Produces: `public static final String COMPANION_TAG = "MooOfDoom_Companion"` and
  `public static boolean isManagedCompanion(int entityId)` — consumed by C2.

**ORDERING CONSTRAINT (do not skip):** `level.addFreshEntity(cow)` synchronously fires
`EntityJoinLevelEvent`, which C2's guard consults via `isManagedCompanion`. The
`morphedPlayers.put(...)` MUST therefore happen BEFORE `addFreshEntity`, or the guard will
discard every legitimate fresh companion. Entity IDs are assigned at construction, so the
ID is valid before spawning.

- [ ] **Step 1: Rewrite `startMorph` and add the accessor**

```java
    public static final String COMPANION_TAG = "MooOfDoom_Companion";

    public static void startMorph(Player player, int durationTicks) {
        if (!(player.level() instanceof ServerLevel level)) return;

        // Discard any previous companion (e.g. orphan from a re-morph) before spawning
        Integer existingId = morphedPlayers.remove(player.getUUID());
        if (existingId != null) {
            Entity existing = level.getEntity(existingId);
            if (existing != null) {
                existing.discard();
            }
        }

        // Spawn companion cow at player position
        Cow cow = new Cow(EntityType.COW, level);
        cow.setPos(player.position());
        cow.setYRot(player.getYRot());
        cow.setInvulnerable(true);
        cow.setNoAi(true);
        cow.setSilent(true);
        cow.addTag(COMPANION_TAG);

        // Register BEFORE addFreshEntity: spawning fires EntityJoinLevelEvent synchronously,
        // and OpCowManager's companion guard discards companions it can't find here.
        morphedPlayers.put(player.getUUID(), cow.getId());
        level.addFreshEntity(cow);

        // Particle effects
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1, player.getZ(),
                50, 0.5, 1.0, 0.5, 0.2);
        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);

        // Advancement: mythic cow morph
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.COW_MORPH.get().trigger(serverPlayer);
        }

        MooOfDoom.LOGGER.info("Player {} morphed into a cow!", player.getName().getString());
    }

    /** True if this entity ID is a companion cow currently managed by a live morph. */
    public static boolean isManagedCompanion(int entityId) {
        return morphedPlayers.containsValue(entityId);
    }
```

- [ ] **Step 2: Add the logout handler**

Add to `CowMorphHandler` (the class is already registered on `NeoForge.EVENT_BUS` in
`MooOfDoom.java:72`, so a new `@SubscribeEvent` method needs no registration change):

```java
    /**
     * End the morph on disconnect — otherwise the player entity stops ticking and the
     * invulnerable companion cow is stranded in the world forever (issue #20).
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (isMorphed(player)) {
            endMorph(player);
        }
    }
```

Add `import net.neoforged.neoforge.event.entity.player.PlayerEvent;`. Also replace the
string literal `"MooOfDoom_Companion"` in the old code with `COMPANION_TAG`.

- [ ] **Step 3: Compile and commit**

Run: `./gradlew compileJava --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/CowMorphHandler.java
git commit -m "fix: end morph on logout, discard stale companion on re-morph"
```

---

### Task C2: Companion guard + orphan sweep in `OpCowManager`

**Files:**
- Modify: `src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java`

**Interfaces:**
- Consumes: `CowMorphHandler.COMPANION_TAG`, `CowMorphHandler.isManagedCompanion(int)` from C1.

- [ ] **Step 1: Add the guard at the top of `onEntityJoinLevel`**

Insert immediately after the `isClientSide` check (before the activation-mode logging):

```java
        // Companion (morph disguise) cows are never OP-activated (issue #21). A companion
        // joining without a live morph managing it is an orphan (crash, or persisted from
        // before the logout fix) — cancel the join so it never enters the world (issue #20).
        if (cow.getTags().contains(CowMorphHandler.COMPANION_TAG)) {
            if (!CowMorphHandler.isManagedCompanion(cow.getId())) {
                event.setCanceled(true);
            }
            return;
        }
```

Add `import com.github.netfallnetworks.mooofdoom.cow.CowMorphHandler;` — same package, so
no import actually needed; just use the class directly.

- [ ] **Step 2: Compile, full build, commit**

Run: `./gradlew build --no-configuration-cache` — expected `BUILD SUCCESSFUL`.

```bash
git add src/main/java/com/github/netfallnetworks/mooofdoom/cow/OpCowManager.java
git commit -m "fix: never OP-activate companion cows; sweep orphaned companions on join"
```

- [ ] **Step 3: Push and open PR**

```bash
git push -u origin fix/companion-cow-lifecycle
```

PR title: `fix: companion cow lifecycle — logout cleanup, re-morph safety, OP-activation
guard`. Body: `Closes https://github.com/NetfallNetworks/moo-of-doom/issues/20` and
`Closes https://github.com/NetfallNetworks/moo-of-doom/issues/21` — both are fully
addressed, itemize the three changes against the two issues. Blast radius: `startMorph`
(caller: `DoomAppleItem`/`DoomAppleUseHandler` mythic roll), `onEntityJoinLevel` (event
subscriber; new early-return only for companion-tagged cows — normal cow activation
untouched), `endMorph` (existing callers unchanged; new caller: logout handler). Note the
map-put-before-spawn ordering constraint explicitly for reviewers. Poll `gh pr checks`.

---

## QA gate (every PR)

After each PR is open and CI is green: dispatch an independent QA reviewer subagent scoped
to that PR's diff. HIGH findings are fixed in the PR before it is marked merge-ready —
never filed as follow-up issues. Post the QA verdict as a PR comment.

## Post-merge

Manual playtest 3 checklist lives in the design doc (`docs/plans/2026-07-14-behavior-fixes-design.md`,
Testing section) — 6 scenarios covering rebellion expiry, mid-window reload, protector
chunk-unload expiry, logout mid-morph, ALL_COWS morph, and OP-cow reload stacking.
