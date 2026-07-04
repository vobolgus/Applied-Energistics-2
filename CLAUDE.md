# CLAUDE.md — Create Fly 26.1 Modpack & Addon Porting Project

## Project Overview

Two coupled goals:

1. **Modpack**: a Create-centered Fabric modpack for Minecraft **26.1.2** built around **Create Fly** (the actively maintained Fabric port of Create 6.x by ZurrTum).
2. **Porting workspace**: port the missing Create ecosystem addons — and **Applied Energistics 2** — to Fabric 26.1.2, compatible with Create Fly internals.

The modpack is assembled first from mods that already exist; the porting workspace then fills the gaps in priority order.

---

## Environment & Toolchain

| Item | Requirement | Notes |
|---|---|---|
| Minecraft | 26.1.2 (Java Edition) | First fully **unobfuscated** release line; official Mojang parameter names available — no Yarn/intermediary mappings step |
| Java | **JDK 25** | Hard requirement of MC 26.1 |
| Gradle | **9.5+** | Java 25 toolchains need 9.1+, but **fabric-loom 1.17.x hard-requires ≥ 9.5** (AE2 port uses wrapper 9.5.1) |
| Loom plugin | `net.fabricmc.fabric-loom` (1.17.x) | Plugin id changed from `fabric-loom`; no `mappings` line (unobfuscated era); loom 1.17 also **dropped the `mod*` dependency configurations** — use plain `implementation` |
| Fabric Loader | 0.19.3+ | |
| Fabric API | latest for 26.1.2 | |
| IDE | IntelliJ IDEA 2025.3+ | Full Java 25 support |
| Host | macOS (M2 Pro, 64 GB) for dev; test server mirrors the Oracle ARM box | Build with `--no-daemon` on the ARM server if memory-constrained |

### Canonical porting references (read before coding)

- Fabric porting guide for 26.1: https://docs.fabricmc.net/develop/porting/
- ChampionAsh5357 migration primers (1.21.11 → 26.1 vanilla changes)
- NeoForge 26.1 blog post (vanilla-diff technique works for Fabric too: unobfuscated sources can be diffed directly between versions)
- **Create Fly source** — `github.com/ZurrTum/Create-Fly` — the single source of truth for how Create internals look on Fabric 26.x. ⚠ **Version trap (found 2026-07-01):** the checkout `~/IdeaProjects/refs/Create-Fly` tracks MC **26.2-pre** — cribbing from it produces phantom APIs (the EMISSIVE incident). Use the version-matched worktree **`~/IdeaProjects/refs/Create-Fly-26.1`** (tag `v6.0.9-26.1.2-4` = the pin) for source, and `javap` on the jar in the gradle cache for binary truth.
- **`~/IdeaProjects/FABRIC_PORTING_PLAYBOOK.md`** — distilled lessons from the completed AE2 + GuideME port (loom 1.17 facts, NeoForge→Fabric mapping crib, "silent NeoForge patches" trap, MC 26.1 gotchas, verification ladder, CI patterns). **Read this first before any port.**
- Reference implementations of the dual-loader recipe: `vobolgus/Applied-Energistics-2` branch `fabric/phase-0` and `vobolgus/GuideME` branch `fabric-26.1` (full per-decision detail in each repo's PORTING_NOTES.md)
- `~/IdeaProjects/refs/fabric-example-mod` (branch 26.1) — ground truth for loom 1.17 build syntax

---

## Part 1 — Modpack Manifest (available today, Fabric 26.1.2)

### Core
| Mod | Version pin | Source |
|---|---|---|
| Fabric API | latest 26.1.2 | Modrinth |
| Create Fly | `26.1.2-6.0.9-3` (or newer) | Modrinth `create-fly` |
| KubeJS | 26.1.2 line | Modrinth |
| Architectury API | 26.1.2 | Modrinth |
| Cloth Config | 26.1.2 | Modrinth |
| Collective | 26.1.2 | Modrinth |

### Content — ported in-house (local jars in `create26-ports/pack/mods-local/`, 2026-07-04)
| Mod | Jar | Notes |
|---|---|---|
| Create Deco | `createdeco-2.2.0-alpha.1.jar` | Prism-verified |
| Crafts & Additions | `createaddition-1.6.0-alpha.2.jar` | energy chain verified; full pass pending |
| Copycats+ | `copycats-3.0.7-alpha.1.jar` | ⚠ ARR — pack-internal only; pass pending. Note: Connected also ships a small copycat module — content overlap is a pack-UX question (recipes/tabs), not a code conflict |
| Create: Connected | `createconnected-1.3.2-alpha.1.jar` | pass pending |
| AE2 + GuideME | `dist/` jars in the AE2 fork | Prism-verified; retire Tom's Simple Storage once proven stable in-pack |

### Content — third-party
| Mod | Notes |
|---|---|
| Farmer's Delight Refabricated 3.6.5+ | Has explicit Create Fly compat fixes |
| Tom's Simple Storage | 26.1.x; interim storage — the AE2 port HAS landed (see Part 2); keep until ae2-fabric proves stable in-pack, then retire |
| Terralith (datapack version) | New worlds only; cannot be removed later |
| Macaw's: Furniture, Windows, Trapdoors, Lights & Lamps, Roofs | All on 26.1.x Fabric |
| Supplementaries + Moonlight Lib | ❌ Verified 2026-06-12: NO 26.1 builds exist (newest = 1.21.1 Fabric). Excluded for now; forward-port candidate (see addon survey in Part 2) |

### UI / QoL
| Mod | Notes |
|---|---|
| **JEI** (not REI) | **REI has NO 26.1 build** (verified 2026-06-12: newest 21.11.814 targets MC 1.21.11). JEI ships Fabric 26.1.2 (29.5.0.26+, maven.blamejared.com). ae2-fabric's REI integration is wired but dormant — add REI alongside/instead when it ships |
| Jade | 26.1.2 Fabric |
| Xaero's Minimap + World Map | 26.1.2 |
| Mouse Tweaks, AppleSkin, Controlling, Clumps | 26.1.2 |

### Performance
| Mod | Notes |
|---|---|
| ImmediatelyFast, ModernFix | 26.1.2 |
| Sodium, Lithium, FerriteCore | Verify exact 26.1.2 builds on Modrinth |
| Iris (optional) | ⚠ Shaders disable Create Fly's Flywheel optimizations — document this for players |

### Server notes
- Java 25 runtime on the server.
- Recipes are server-synced since 1.21.2+: REI/JEI must be installed server-side too.
- Create Fly ships a separate `-server.jar` build — use it for the dedicated server.

---

## Part 2 — Porting Roadmap

All ports target: **Fabric Loader + Fabric API on MC 26.1.2, compiled against Create Fly** (not official Create).

### Critical context: Create Fly ≠ official Create internally

Create Fly deliberately diverges from the official Fabric fork's architecture:
- Uses **targeted mixins** for Create features instead of Porting-Lib (no NeoForge-feature emulation layer).
- Strict **client/server source separation** — server code must never reference client classes.
- Dropped **Registrate-Refabricated**; registration is done directly. Addon code that uses Registrate builders must be rewritten to plain registration.
- New-version rendering: item models live in a dedicated render folder; Entity/BlockEntity/GUI rendering extracts state first, then renders. Any addon renderer must be rewritten to this pattern.
- New data-loading pipeline (error-capturing). Datagen output from old addons may need regeneration.

**Before each port: read the equivalent subsystem in Create Fly's source and mirror its patterns.** Check Create Fly issues/Discord for existing WIP addon ports to avoid duplicate work.

### Porting order

| # | Addon | Upstream repo | Status | Notes |
|---|---|---|---|---|
| 1 | Create Deco | talrey/CreateDeco | ✅ **DONE** 2026-06-15 (+CatwalkBlock fix 07-01) | `2.2.0-alpha.1`, CC0. Prism-verified. |
| 2 | Copycats+ | `copycats-plus/copycats` | ✅ **DONE** 2026-07-03 (W1–W7) | `3.0.7-alpha.1`. ⚠ **ARR license — private use only**, no publish without authors' permission. Full-fidelity scope (kinetic copycats, feature-toggle, cat gag). Prism pass pending. |
| 3 | Create: Connected | `hlysine/create_connected` | ✅ **DONE** 2026-07-04 (W1–W7) | `1.3.2-alpha.1`, AGPL-3.0 (publishable). Kinetics fear didn't materialize — all Create bases survive in CF. First menus/screens port. Prism pass pending. |
| 4 | Enchantment Industry | DragonsPlusMinecraft org | 🟡 Med — next up | Fluid handling via **Fabric Transfer API**; integration with Create Fly fluid network. |
| 5 | Crafts & Additions | `mrh0/createaddition` | ✅ **DONE** 2026-06-30 (code) | `1.6.0-alpha.2`, MIT. Energy chain Prism-verified 06-16; full Prism pass pending (accumulator merge/split is the high-risk item). |
| 6 | **Applied Energistics 2** | `AppliedEnergistics/Applied-Energistics-2` | ✅ **DONE** 2026-06-12 | Shipped (incl. GuideME): dual-loader fork, CI fully green. See dedicated section below. |
| 7 | Steam 'n' Rails | `Layers-of-Railways/Railway` | 🔴 High | Train-system internals; mixins into Create train logic — highest coupling to Create Fly internals. Optional / stretch. |
| 8 | Big Cannons | `rbasamoyai/CreateBigCannons` | 🔴 High | Custom contraption physics + renderers. Optional / stretch. |

**Minimum viable pack (1, 3, 5 + AE2) is CODE-COMPLETE as of 2026-07-04** — all jars staged in
`create26-ports/pack/mods-local/`. Remaining before pack assembly: the in-world **Prism pass**
(`create26-ports/PRISM_TEST_CHECKLIST.md`, 8 sections covering Crafts & Additions + Copycats+ +
Connected) and fixing whatever it finds. 7–8 are stretch goals; #4 is the next port if desired.

**Structural note (learned from AE2):** the Create addon ports (1–5, 7–8) are **single-loader Fabric projects** — Create Fly is Fabric-only, so there is no NeoForge twin to keep green. The dual-loader recipe is only for NeoForge-upstream mods (the AE2 pattern). Also: these addons' upstream code is 1.20.x–1.21.x era, so each port = **MC forward-port + Create Fly adaptation at once** (AE2 was easier in this one respect — its upstream was already on 26.1). Use the vanilla-diff technique and Create Fly's source as the rosetta stone.

### AE2 port — ✅ COMPLETED 2026-06-12

**Repos:** upstream `github.com/AppliedEnergistics/Applied-Energistics-2` + `github.com/AppliedEnergistics/GuideME`. ⚠ Do NOT confuse with the fake `Applied-Energistics-2` (hyphenated) GitHub org distributing "premium tools/clients" — likely malware.

The planned strategy (Fabric platform layer on the current 26.1 branch, GuideME first, dual-loader with NeoForge kept green) was executed as designed. Full per-decision detail lives in each repo's `PORTING_NOTES.md`; the distillation is `~/IdeaProjects/FABRIC_PORTING_PLAYBOOK.md`.

**Outcome:**
- **AE2**: fork `vobolgus/Applied-Energistics-2`, branch `fabric/phase-0`, version `26.1.10-alpha`. Kotlin DSL dual-loader build: `:neoforge` (ModDevGradle) + `:fabric` (loom 1.17.8) over shared root sources; NeoForge green at every commit (regression harness, upstream-rebaseable).
- **GuideME**: fork `vobolgus/GuideME`, branch `fabric-26.1`, published to mavenLocal as `org.appliedenergistics:guideme-fabric:26.1.10-alpha`.
- **CI** (AE2 fork): `build-neoforge` + `build-fabric` + `gametest-fabric` (68 tests) + `Export Guide` all green. Fabric jobs build GuideME from the fork via repo vars `GUIDEME_REPO`/`GUIDEME_REF`; jobs that don't touch `:fabric` pass `-Pae2.skipFabric=true` (loom resolves deps at configuration time).
- **Jars**: `dist/appliedenergistics2-fabric-26.1.10-alpha.jar` + `dist/guideme-fabric-26.1.10-alpha.jar`. Client needs fabric-loader ≥ 0.19.3 + fabric-api; TR Energy is bundled jar-in-jar. Verified in a real Prism instance.
- **Deferred** (tracked in PORTING_NOTES "Status & remaining work"): REI runtime (REI has no 26.1 build — integration dormant), part LED fullbright emissive, spatial-storage custom sky, WTHIT runtime (needs badpackets), guideme-fabric un-ProGuard.

**Maintenance:** rebase onto upstream alphas per the rebase playbook in PORTING_NOTES.md (AT↔AW sync checklist; the datagen-transform drift guard fails the build on new `neoforge:` keys automatically). Bump the `version=` pin in gradle.properties per release.

**Licensing**: AE2 is open source under its own license stack ("Multiple" — LGPL core, art assets CC BY-NC-SA). The public Fabric fork keeps attribution, no asset rebranding. Upstreaming the platform layer as a PR remains the goal — check their issue tracker for "Fabric support" discussions before opening one.

### Create addon ports — ✅ CODE-COMPLETE 2026-06-15 … 2026-07-04

All four live in `~/IdeaProjects/create26-ports/ports/` (remote `vobolgus/create26-ports`, private);
authoritative per-port state = each port's `PORTING_NOTES.md`. Wave structure everywhere:
foundation → content → registration → mixins → datagen → client/render → config/network → ponder/polish,
checkpoint-committed per green gate (build → dedicated `runServer` → `runClient`).

- **Create Deco** `2.2.0-alpha.1` (CC0) — done 06-15, Prism-verified 06-15; post-completion review
  found + fixed a stale M1-trim stub in CatwalkBlock (07-01). Deferred: placement helpers, pipe I/O check.
- **Crafts & Additions** `1.6.0-alpha.2` (MIT) — done 06-30. Energy chain Prism-verified 06-16;
  the rest of the in-world pass pending (accumulator merge/split = highest risk). Deferred: Tesla-coil
  BE renderer (static model), item charging, wire cosmetics.
- **Copycats+** `3.0.7-alpha.1` (⚠ ARR — private use until permission) — done 07-01→07-03, full-fidelity
  scope. Its PORTING_NOTES (§3, §11–§19) became the **drift encyclopedia** every later port cribs from:
  ValueInput/Output NBT, InteractionResult rework, BlockAndLightGetter CT surface, client/server
  behaviour splits, CF model pipeline (WrapperBlockStateModel + AllModels), kinetic rendering without
  Flywheel (SuperByteBuffer), fabric-api FabricBlock default-method diamond, getOcclusionShape
  null-before-cache boot trap. Zero in-world hours yet.
- **Create: Connected** `1.3.2-alpha.1` (AGPL-3.0, publishable) — done 07-03→07-04 in ~1 day (the cribs
  compound). Kinetics recon fear didn't materialize (all Create bases survive in CF). Firsts: container
  menus + screens on 26.1, reflective feature-toggle interop with the sibling Copycats+ port,
  SequencerInstructions enum-extension CODEC rebuild. Zero in-world hours yet.

**Verification honesty:** "done" above = code-complete + headless gates green. The in-world pass
(`create26-ports/PRISM_TEST_CHECKLIST.md`) has NOT been run for Copycats+/Connected and only partially
for Crafts & Additions — playtest bugs are expected, especially render fidelity and save/load round-trips.

### Addon survey — what the AE2 proof unlocks (checked 2026-06-12)

AE2 proves the dual-loader recipe works for NeoForge-only mods on 26.1. Candidates beyond the Create roadmap, with verified upstream status:

| Mod | Newest upstream (2026-06-12) | Port shape | Call |
|---|---|---|---|
| Extended AE (`extended-ae`) | 1.21.1 | Needs MC forward-port first; compiles against our ae2-fabric | Wait for upstream 26.1 NeoForge — then the AE2 recipe applies directly |
| MEGA Cells (`mega`) | 1.21.1 | same | same |
| AE2WTLib wireless terminals | 1.21.1 | same; high pack value | medium priority once unblocked |
| AppFlux (`appflux`) | 1.21.1 NeoForge | TR-Energy edge work we already mastered | wait for 26.1 upstream |
| AE2 Things (`ae2things`) | 1.20.1 Fabric | oldest base, heaviest forward-port | low |
| Supplementaries + Moonlight | 1.21.1 Fabric | pure MC forward-port (already Fabric, no loader work); big content value for the pack | medium — independent of Create/AE2 skills |
| REI | 1.21.11 | do NOT port — upstream is active and will ship; ae2-fabric integration is ready | wait |

Rules of thumb learned: upstream already on 26.1 NeoForge (like AE2 was) → dual-loader recipe, known cost. Upstream stuck on 1.21.x → a vanilla forward-port comes FIRST and dominates the cost. Recheck this table before starting anything (one Modrinth API call) — and note Create Fly already publishes 26.2-pre builds, so an MC 26.2 wave is on the horizon.

---

## Repository Layout (porting workspace)

**Reality (2026-07):** AE2 and GuideME live as standalone sibling forks (own history, upstream-rebaseable, own CI) — that proved right. The `create26-ports` workspace exists and holds the four completed Create-addon ports + the pack skeleton. Remote: `github.com/vobolgus/create26-ports` (private).

```
~/IdeaProjects/
  Applied-Energistics-2/     # AE2 dual-loader fork (branch fabric/phase-0) — done
  GuideME/                   # GuideME dual-loader fork (branch fabric-26.1) — done
  refs/
    Create-Fly/              # ⚠ checkout is MC 26.2-pre — do NOT crib from it
    Create-Fly-26.1/         # git worktree at tag v6.0.9-26.1.2-4 = EXACTLY our pin — the ONLY valid CF source crib
    fabric-example-mod/      # loom 1.17 ground truth
  FABRIC_PORTING_PLAYBOOK.md # distilled porting lessons — read before each port
  create26-ports/            # workspace: Create addon ports + pack (Kotlin DSL, wrapper 9.5+)
    CLAUDE.md                # workspace copy of this spec
    HANDOFF.md               # cross-machine handoff (⚠ its "no remote" §0 is stale — remote exists)
    PRISM_TEST_CHECKLIST.md  # the in-world verification ladder (8 sections, 3 untested ports)
    settings.gradle.kts / gradle/libs.versions.toml   # shared pins
    ports/
      create-deco/           # ✅ each port = single-loader fabric mod + own PORTING_NOTES.md
      create-addition/       # ✅ (upstream name of crafts-additions)
      copycats-plus/         # ✅ its PORTING_NOTES is the drift ENCYCLOPEDIA (§3/§11-§19) — the crib for every later port
      create-connected/      # ✅
      enchantment-industry/  # next, if taken
    pack/
      mods-local/            # staged alpha jars (gitignored — rebuild per HANDOFF §5)
      modrinth.index.json    # TODO: mrpack manifest (pack assembly not started)
      kubejs/  config/       # TODO
```

### Dependency wiring (per port)

```kotlin
// build.gradle.kts (Kotlin DSL — project convention since the AE2 port)
repositories {
  exclusiveContent {
    forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
    filter { includeGroup("maven.modrinth") }
  }
}
dependencies {
  // loom 1.17 dropped modImplementation & friends — plain implementation for everything
  implementation("maven.modrinth:create-fly:26.1.2-6.0.9-4")
  // pin exact versions in libs.versions.toml; bump deliberately.
  // ⚠ EXACT pin matters: 6.0.9-3→-4 broke AllCTTypes at verify-time (create-deco lesson);
  //   and the source you crib from must match the pin (refs/Create-Fly-26.1 worktree).
}
```

## Workflow Conventions (for Claude Code sessions)

- **Read `~/IdeaProjects/FABRIC_PORTING_PLAYBOOK.md` before starting any port** — it encodes the AE2/GuideME lessons (toolchain facts, mapping crib, verification ladder, CI patterns).
- One port = one branch = one Gradle subproject. Get `runClient` booting with the dep tree before writing feature code.
- Checkpoint-commit at every green gate (cheap commits = cheap recovery; learned the hard way).
- Port order inside a mod: **registration → logic → datagen → renderers → integrations** (renderers last; they changed most in 26.x).
- When a Create API call doesn't resolve: grep Create Fly sources for the renamed/moved equivalent before assuming it's gone.
- Every removed-Registrate rewrite gets a comment `// was Registrate: <original builder chain>` for reviewability.
- Vanilla-diff technique: when behavior changed between 1.21.x and 26.1, diff the unobfuscated vanilla sources of both versions directly.
- Test gates per port: `runClient` smoke test → dedicated `runServer` boot (catches client-class leaks, the #1 Create Fly compat failure) → in-world feature checklist.
- Keep a `PORTING_NOTES.md` per subproject: API renames discovered, mixin targets, open questions. These compound across ports — **copycats-plus's is the drift encyclopedia; read its §11/§13/§15–§19 before re-deriving anything**.
- **Binary truth discipline** (post-EMISSIVE): source cribs must be version-matched to the pin (`refs/Create-Fly-26.1`); when in doubt, `javap -classpath <jar-from-gradle-cache> <fqcn>` beats any source reading. Re-verify EVERY mixin target — recon line numbers drift.
- **Multi-agent waves work** (proven on Copycats+ 3 days, Connected 1 day): recon agents → PORTING_NOTES synthesis (coordinator corrects recon errors!) → parallel wave batches with disjoint file zones → coordinator runs gates + commits. Agents die on session limits mid-wave: instruct them to write files INCREMENTALLY (one read→write per file), then resume via SendMessage with a disk-state summary.
- Trust but verify agent reports: check the files actually landed on disk (two recon agents "delivered" reports without writing them), and gate results yourself before committing.

## Definition of Done (per port)

1. Builds against MC 26.1.2 + Create Fly pin, Java 25.
2. Dedicated server boots with the mod (no client classes on server path).
3. Feature checklist passes in-world (per-port list in `PORTING_NOTES.md`; consolidated ladder in `create26-ports/PRISM_TEST_CHECKLIST.md`). ← the four Create ports are code-complete but this step is still OPEN for Crafts & Additions (partial), Copycats+ and Connected.
4. JEI shows recipes (REI too once it ships a 26.1 build); Jade shows block info where applicable.
5. Published as alpha to a Modrinth project page (or kept as local jars in `pack/mods-local/`). ⚠ License-gated: Copycats+ is ARR (no publish without the authors' permission); Deco CC0 / C&A MIT / Connected AGPL are publishable.
