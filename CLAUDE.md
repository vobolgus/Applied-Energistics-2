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
| Gradle | 9.1.0+ | Required for Java 25 toolchains |
| Loom plugin | `net.fabricmc.fabric-loom` (latest) | NOTE: plugin id changed from `fabric-loom`; remove any `mappings` line from dependencies |
| Fabric Loader | latest (0.18.x+) | |
| Fabric API | latest for 26.1.2 | |
| IDE | IntelliJ IDEA 2025.3+ | Full Java 25 support |
| Host | macOS (M2 Pro, 64 GB) for dev; test server mirrors the Oracle ARM box | Build with `--no-daemon` on the ARM server if memory-constrained |

### Canonical porting references (read before coding)

- Fabric porting guide for 26.1: https://docs.fabricmc.net/develop/porting/
- ChampionAsh5357 migration primers (1.21.11 → 26.1 vanilla changes)
- NeoForge 26.1 blog post (vanilla-diff technique works for Fabric too: unobfuscated sources can be diffed directly between versions)
- **Create Fly source** — `github.com/ZurrTum/Create-Fly` — the single source of truth for how Create internals look on Fabric 26.x

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

### Content
| Mod | Notes |
|---|---|
| Farmer's Delight Refabricated 3.6.5+ | Has explicit Create Fly compat fixes |
| Tom's Simple Storage | 26.1.x; interim storage until AE2 port lands |
| Terralith (datapack version) | New worlds only; cannot be removed later |
| Macaw's: Furniture, Windows, Trapdoors, Lights & Lamps, Roofs | All on 26.1.x Fabric |
| Supplementaries + Moonlight Lib | VERIFY 26.1 Fabric build before adding |

### UI / QoL
| Mod | Notes |
|---|---|
| REI | Native Create Fly integration (JEI also viable) |
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

| # | Addon | Upstream repo | Difficulty | Key work items |
|---|---|---|---|---|
| 1 | Create Deco | `Source` link on CurseForge (talrey) | 🟢 Low | Block/item registration without Registrate; palettes; minimal logic. Learning vehicle. |
| 2 | Copycats+ | `copycats-plus/copycats` | 🟢🟡 Low-Med | Hooks into Create's copycat system — verify Create Fly kept the same entry points. |
| 3 | Create: Connected | `hlysine/create_connected` | 🟡 Med | Kinetics API deep-dive; several BlockEntities with renderers → new render pattern. |
| 4 | Enchantment Industry | DragonsPlusMinecraft org | 🟡 Med | Fluid handling via **Fabric Transfer API**; integration with Create Fly fluid network. |
| 5 | Crafts & Additions | `mrh0/createaddition` | 🟡🔴 Med-High | Energy bridge: replace Forge Energy with **Team Reborn Energy** (`teamreborn:energy`) over Fabric Transfer API. Defines pack's electrical progression. |
| 6 | **Applied Energistics 2** | `AppliedEnergistics/Applied-Energistics-2` | 🔴 High | See dedicated section below. |
| 7 | Steam 'n' Rails | `Layers-of-Railways/Railway` | 🔴 High | Train-system internals; mixins into Create train logic — highest coupling to Create Fly internals. Optional / stretch. |
| 8 | Big Cannons | `rbasamoyai/CreateBigCannons` | 🔴 High | Custom contraption physics + renderers. Optional / stretch. |

Minimum viable pack = items **1, 3, 5** (+ AE2 when ready). 7–8 are stretch goals.

### AE2 port — dedicated plan

**Repos:** `github.com/AppliedEnergistics/Applied-Energistics-2` (upstream, active: v26.1.9-alpha as of May 2026) and `github.com/AppliedEnergistics/GuideME`. ⚠ Do NOT confuse with the fake `Applied-Energistics-2` (hyphenated) GitHub org distributing "premium tools/clients" — likely malware.

**Status quo:** upstream AE2 dropped Fabric after the 1.20.x line (last Fabric release: 15.4.10 for 1.20.1). Current 26.1 builds are **NeoForge-only alphas** (26.1.9-alpha and counting). A new hard dependency, **GuideME** (in-game guidebook framework), is NeoForge-only and must be ported too. Note: GitHub Packages still hosts `appliedenergistics2-fabric` artifacts (last published Oct 2025), so the multiloader build infrastructure is recent history in the repo — start archaeology from there.

**Strategy — re-add a Fabric platform layer to the current 26.1 branch** (NOT forward-porting the ancient 1.20.1 Fabric branch — two years of drift makes that strictly worse):

1. **Archaeology**: study the last multiloader commit range in AE2 history (1.20.1-era, `fabric/` platform module) to map which abstraction seams existed: platform hooks, energy, item/fluid transfer, networking, capabilities/attachments, client init.
2. **GuideME first**: port GuideME to Fabric 26.1.2 as a standalone project — AE2 won't compile without it. Scope it; it's a document/render framework, mostly client-side.
3. **Platform layer**: reintroduce a `fabric` source set / module. Map NeoForge concepts:
   - Capabilities/Attachments → Fabric API lookup (`ItemApiLookup`/`BlockApiLookup`) + custom attachments via Fabric data attachments or mixin-backed storage.
   - Forge Energy → **Team Reborn Energy**; keep AE2's internal AE-energy intact, only convert at edges.
   - Item/fluid handlers → Fabric Transfer API (`Storage<ItemVariant>` / `Storage<FluidVariant>`).
   - NeoForge networking → Fabric networking API (payload-based, similar shape since 1.20.5+, mapping is mechanical).
   - Client/render: AE2 26.1 already targets the new render pipeline (it builds for 26.1) — reuse as-is where loader-agnostic; route loader-specific init through the platform layer.
4. **Out of scope initially**: AE2 NeoForge-only addon ecosystem, The One Probe integration. Jade/REI/WTHIT integrations are Fabric-friendly — keep.
5. **Create ↔ AE2 bridge (later, optional)**: KubeJS recipes are enough for pack progression; a dedicated compat addon is a separate project.

**Licensing**: AE2 is open source under its own license stack ("Multiple" — LGPL core, art assets CC BY-NC-SA). A public Fabric fork is fine; keep attribution, don't rebrand assets. Aim to upstream the platform layer as a PR if maintainers are receptive — check their issue tracker for "Fabric support" discussions first.

---

## Repository Layout (porting workspace)

```
create26-ports/
  CLAUDE.md                  # this file
  settings.gradle            # composite/multi-project build
  gradle/                    # wrapper 9.1+, version catalog (libs.versions.toml)
  ports/
    create-deco/             # each port = standalone fabric mod project
    copycats-plus/
    create-connected/
    enchantment-industry/
    crafts-additions/
    guideme-fabric/
    ae2-fabric/
    steam-n-rails/           # stretch
    big-cannons/             # stretch
  pack/
    modrinth.index.json      # mrpack manifest for the modpack itself
    kubejs/                  # pack scripts: recipes, progression, unification
    config/
```

### Dependency wiring (per port)

```gradle
repositories {
  exclusiveContent {
    forRepository { maven { name = "Modrinth"; url = "https://api.modrinth.com/maven" } }
    filter { includeGroup "maven.modrinth" }
  }
}
dependencies {
  modImplementation "maven.modrinth:create-fly:26.1.2-6.0.9-3"
  // pin exact versions in libs.versions.toml; bump deliberately
}
```

## Workflow Conventions (for Claude Code sessions)

- One port = one branch = one Gradle subproject. Get `runClient` booting with the dep tree before writing feature code.
- Port order inside a mod: **registration → logic → datagen → renderers → integrations** (renderers last; they changed most in 26.x).
- When a Create API call doesn't resolve: grep Create Fly sources for the renamed/moved equivalent before assuming it's gone.
- Every removed-Registrate rewrite gets a comment `// was Registrate: <original builder chain>` for reviewability.
- Vanilla-diff technique: when behavior changed between 1.21.x and 26.1, diff the unobfuscated vanilla sources of both versions directly.
- Test gates per port: `runClient` smoke test → dedicated `runServer` boot (catches client-class leaks, the #1 Create Fly compat failure) → in-world feature checklist.
- Keep a `PORTING_NOTES.md` per subproject: API renames discovered, mixin targets, open questions. These compound across ports.

## Definition of Done (per port)

1. Builds against MC 26.1.2 + Create Fly pin, Java 25.
2. Dedicated server boots with the mod (no client classes on server path).
3. Feature checklist in `PORTING_NOTES.md` passes in-world.
4. REI shows recipes; Jade shows block info where applicable.
5. Published as alpha to a Modrinth project page (or kept as local jars in `pack/mods-local/`).
