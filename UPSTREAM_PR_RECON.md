# Upstream PR Recon — Fabric platform layer → `AppliedEnergistics/Applied-Energistics-2`

Recon date: **2026-07-10**. Read-only investigation of the path to upstreaming this fork's
Fabric platform layer (branch `fabric/phase-0`, 27 commits) as a PR to the real upstream
`github.com/AppliedEnergistics/Applied-Energistics-2` (⚠ NOT the fake hyphenated
"Applied-Energistics-2" GitHub *org* — that distributes likely-malware "premium clients").

Method: `gh api` searches over upstream issues/PRs/releases, `git fetch upstream` +
merge-base diff analysis, PORTING_NOTES.md, and the GuideME upstream tracker.

---

## 1. Upstream sentiment on Fabric

**Unambiguous and repeated: upstream has said "no more Fabric" at least four times, most
recently 2026-05-24.** Every request has been closed quickly, usually within hours.

| Where | When | Who | Quote |
|---|---|---|---|
| [#8273 "No published version for Fabric 1.21.X"](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/8273#issuecomment-2531544225) | 2024-12-10 | **shartte** (MEMBER, lead maintainer) | "We dropped Fabric support and only release for NeoForge now" — and, asked about a Fabric-side Connector equivalent: "No" |
| [#8163 "Fabric 1.21.X"](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/8163) | 2024-08-26 | Kevin-Marsh (community, uncontradicted) | "It was decided that there will no longer be a fabric version and it would be abandoned on anything post 1.20.4." |
| [#8462 "Build for Fabric 1.21.1 ?"](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/8462) | 2025-04-13 | **shartte** | (closing; only defended NeoForge stability: "NeoForge has stable releases for 1.21.1") |
| [#8667 "Fabric 1.21.8 Polymer version"](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/8667#issuecomment-3315876976) | 2025-09-20 | **shartte** | "We don't intend to do that, no." |
| [#8876 "Please Update AE2 Fabric build to 1.21.1 <3"](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/8876) | **2026-05-24** | **Mari023** (COLLABORATOR) | "no ae2 version for fabric will be released beyond 1.20.1, use neoforge" |
| [GuideME #44 "[Feature request] fabric support"](https://github.com/AppliedEnergistics/GuideME/issues/44#issuecomment-2701064214) | 2025-03-05 | **shartte** | **"I currently don't want to deal with Fabric build tooling and the headache that it causes..."** — issue left **open** (its duplicate #43 was closed) |

The GuideME quote is the only place a *reason* is stated, and it is decisive for PR planning:
the objection is not "Fabric ports are impossible/unwanted by users" but **"maintaining Fabric
build tooling is a headache we don't want."** A merged platform-layer PR imposes exactly that
headache permanently (AW sync, loom quirks, a second CI leg), regardless of who writes the
initial code. Note the word "currently" and the open state of GuideME #44 — the door is not
bolted, but the burden argument must be answered head-on.

### Governance of large PRs

- No repo Discussions (`has_discussions: false`). The venue for pre-PR discussion is
  **Discord** ([discord.gg/Zd6t9ka7ne](https://discord.gg/Zd6t9ka7ne), from README).
- README "Contribution" section: *"Before you want to add major changes, you might want to
  discuss them with us first, before wasting your time."* — an explicit discuss-first policy.
- [`.github/CONTRIBUTING.md`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/.github/CONTRIBUTING.md):
  issue-first workflow ("Submit an issue … Waiting for feedback is suggested"), squash-merge,
  spotless formatting, and notably: *"PRs that make changes only to syntax or 'clean up' the
  code will be rejected. Any code clean-up should be coordinated with the core team first."*
  — a pure "loader-abstraction refactor" PR without prior buy-in falls under this clause.

## 2. Upstream activity shape

- **`main` is the active 26.1 line** (MC 26.1.2). Recent releases:
  `v26.1.6/7/8-alpha` (May 2026), `v26.1.9-alpha` (2026-05-22), **`v26.1.10-beta`
  (2026-06-16)** — the line just graduated alpha→beta. Cadence: bursts, roughly monthly lately.
- Since our fork point (`17f9caa3e`, 2026-06-04) upstream `main` has moved only **7 commits**
  (part-outline rendering, autocraft input parser, crafting-terminal fixes, a fluid-transform
  crash fix). Our branch: **27 commits** on top. Rebase cost today is low; our pin
  `version=26.1.10-alpha` is one notch behind the shipped `26.1.10-beta`.
- **Active people (last 12 months of `main`):** Sebastian Hartte / shartte (~38 commits — the
  gatekeeper), Mithi83 (16), Mari023 (4, collaborator), plus one-off community PRs.
  Technici4n — historically the Fabric-side expert (and now a NeoForge core dev) — has been
  essentially absent from AE2 `main` since early 2024. **The person who used to carry AE2's
  Fabric side left for NeoForge**; nobody on the active roster owns Fabric expertise.
- Branch structure: version branches `1.21.1 … 1.21.11`, historical `fabric/1.17–1.19.3`
  branches frozen, and a **still-alive `fabric/1.20.1`** (see §4). 434 open issues.

## 3. Our delta shape (fork vs merge-base `17f9caa3e`)

Total: **577 files changed, +27,934 / −3,302.** Decomposed:

| Bucket | Size | PR-relevant? |
|---|---|---|
| **(a) Additive platform layer** — `loader/fabric/` | **116 files, +13,536** (all new: entrypoints, mixins, `ae2.accesswidener`, FRAPI quad/emissive pipeline, TR-Energy adapter, gametest twins, datagen transform) | Yes — the payload. Purely additive; touches nothing upstream owns. |
| **(a′) NeoForge overlay reshuffle** — `loader/neoforge/` | 143 files, +17,901; but ~102 of those are **moves** out of `src/` (62 from `src/client`, 40 from `src/main`, mostly R100 = verbatim) + 41 new bridge files | Yes — the invasive half. Upstream's NeoForge code physically relocates. |
| **(b) Shared-source modifications** — `src/` | **232 files modified (+2,334/−2,509)** — 150 of the 232 have ≤10 changed lines (import/seam swaps); big ones: `AppEngClient` (568), `AEConfig` (551), `InitNetwork`, `AppEngBase`. Plus **64 new files (+5,498)**: the loader-free seams (`appeng.api.lookup` capability seam, transaction handle, render-data contract, `LoaderPlatform`/`LoaderEventHooks`, config seam). 2 deletions. | Yes — this is what upstream must *accept as architecture*, and what CONTRIBUTING's "no cleanup-only PRs" clause endangers without prior buy-in. Gate M1 invariant: **0 `net.neoforged` imports in shared sources**, NeoForge green at every commit (451 unit tests + 70 gametests per loader). |
| **(c) Build system** | root `build.gradle`/`settings.gradle` **deleted**, replaced by Kotlin DSL (`*.kts` + `buildSrc`) with the dual-project split | **Partially fork-only.** The *split* is PR-relevant; the **Groovy→Kotlin conversion is not** — upstream is on Groovy, and PORTING_NOTES already admits "upstream build.gradle changes must be hand-mirrored… they no longer merge." A PR would have to re-do the split in Groovy. |
| **(d) Fork-only infra** | `CLAUDE.md`, `PORTING_NOTES.md`, CI `GUIDEME_REPO`/`GUIDEME_REF` repo-vars plumbing, `guideme-fabric-mavenlocal` composite action, `-Pae2.skipFabric` escape hatch, `dist/` jars | No — stripped from any PR. CI split itself (build-neoforge / build-fabric / gametest-fabric jobs in `build.yml`, +localization/export_guide path fixes) would be re-proposed in simplified form once guideme-fabric has a real maven home. |

Ongoing taxes a merge would transfer to upstream (from PORTING_NOTES "Rebase playbook"):
**AT↔AW mirror on every access-transformer change** (`:fabric:validateAccessWidener` gates it),
the **datagen transform drift guard** (new `neoforge:` datagen keys fail `:fabric:build` by
design until mapped), and the **silent-NeoForge-patch discipline** (new shared code using Neo's
vanilla patches fails `:fabric:compileJava`). These are exactly the "build tooling headache"
shartte cited — the PR pitch must present them as *automated, self-signaling gates*, not tribal
knowledge.

## 4. Precedents

- **The drop:** Fabric left `main` with commit `6e0962ada` **"Port to Neoforge 1.20.2"**
  (Hartte; the 1.20.2/Neo migration, late 2023). The last multiloader line is **1.20.1**
  (`fabric/v15.x` + `forge/v15.x` twin tags). Community folklore pegs it as "abandoned post
  1.20.4" (#8163); no long-form public rationale exists in the repo — only the GuideME
  build-tooling quote.
- **`fabric/1.20.1` is not dead:** upstream *merged* community PR
  [#8674 "Add native EMI support for Fabric 1.20.1"](https://github.com/AppliedEnergistics/Applied-Energistics-2/pull/8674)
  (2025-09/10) and tagged **`fabric/v15.4.10` on 2025-10-25**. So maintainers still cut Fabric
  releases *on the frozen version* when the community does the work — a real, recent precedent
  for "community writes it, upstream ships it." Counter-signal: backport PRs
  [#8653](https://github.com/AppliedEnergistics/Applied-Energistics-2/pull/8653) (2025-09) and
  [#8703](https://github.com/AppliedEnergistics/Applied-Energistics-2/pull/8703) (2025-10) sit
  **open and unreviewed** for 9+ months — attention is thin.
- **Nobody has attempted a Fabric-port PR against `main` since the drop** (searched PRs 2024→
  present). This fork would be the first — no rejection precedent, but also no template.
- The historical multiloader era worked because Technici4n maintained the Fabric half; his
  departure to NeoForge is plausibly *the* proximate cause of the drop. Any proposal implicitly
  asks "who is the new Technici4n?" — the answer has to be us, credibly.

## 5. Blockers / prerequisites

1. **Maintainer intent (the big one).** Four explicit refusals, most recent ≤2 months old, by
   both active gatekeepers (shartte, Mari023). A cold PR is near-certain to be closed on sight;
   CONTRIBUTING + README both mandate discussion first for major changes.
2. **GuideME cascade.** `:fabric` hard-depends on `org.appliedenergistics:guideme-fabric`,
   which exists only in our unpushed fork / mavenLocal / CI-built-from-fork
   (`GUIDEME_REPO`/`GUIDEME_REF` vars; fabric CI jobs are *skipped* without them). GuideME is a
   separate upstream repo where shartte is effectively sole maintainer and personally refused
   Fabric tooling (issue #44 — open, so trackable). **An AE2 PR cannot be green upstream until
   guideme-fabric has a real published home** — either upstreamed to `AppliedEnergistics/GuideME`
   first (hardest), or published by us to a public maven upstream is willing to depend on
   (unusual for them), or the guide feature made loader-optional in the fabric jar (scope cut).
3. **Groovy/Kotlin divergence.** Our build is Kotlin DSL; upstream is Groovy. The dual-loader
   split must be re-expressed in Groovy for a PR (buildSrc transform class is already
   Java/Kotlin-portable; the scripts are a rewrite, ~days not weeks).
4. **Permanent sync taxes** (AT↔AW, datagen transform, silent-patch discipline, second CI leg,
   wrapper 9.5.1/loom-1.17 constraints) — mitigated but not eliminated by our fail-loud guards;
   this is the exact stated objection and needs an explicit "we co-maintain, and if we vanish
   you can delete `loader/fabric/` without touching a shared file" story.
5. **TR Energy jar-in-jar: NOT a blocker.** `teamreborn:energy` is **MIT** (verified via GitHub
   API), bundled via loom `include()` — standard Fabric practice, license-compatible with AE2's
   LGPL core.
6. Minor: upstream moved to `26.1.10-beta` (7 commits ahead) — trivial rebase; deferred
   fork items (spatial sky, WTHIT runtime, in-world visual checklist §"Deferred") should be
   burned down before claiming parity in a proposal.

---

## Recommendation

**Do not open a PR now, and do not open a cold GitHub issue either. Go Discord-first, and
decouple the ask into (i) a loader-seam refactor question and (ii) the Fabric layer itself.
Meanwhile, keep shipping the fork as the default plan of record.**

Reasoning:

- The request "Fabric on modern MC" has been asked and shut down four times; a fifth GitHub
  issue reads as noise and invites an instant close by association. What has *never* been put
  in front of them is: **a finished, gate-green, dual-loader implementation with 70/70 gametests
  per loader, NeoForge provably untouched, and a named co-maintainer attached.** That is a
  categorically different conversation, and per their own README it belongs on Discord first.
- The stated objection (build tooling burden) is partially answerable (fail-loud guards,
  deletable `loader/fabric/`, we carry rebases — demonstrated by the rebase playbook), but the
  decision-maker's preference is recent and consistent. Expected outcome of even a perfect
  pitch is uncertain at best; plan for "no" gracefully.
- The **only sub-piece with realistic standalone acceptance odds is the shared-source
  decoupling (bucket b + a′)** — pitched *not* as "for Fabric" but as "loader-seam
  architecture; NeoForge remains the only official target; here's the CI proof nothing
  regressed." Even that trips CONTRIBUTING's "coordinate cleanup with the core team first"
  rule, hence Discord before any code lands.
- **GuideME is sequencing-critical:** raise it *first or simultaneously* (issue #44 is still
  open — a legitimate, non-spammy hook to reply on… but per read-only discipline, only when
  we're actually ready to act). If GuideME-fabric is refused, the AE2 PR is dead on arrival
  regardless of AE2-side sentiment.
- **If the answer is no** (likely): the fallback is publishing the fork as a clearly-labeled
  unofficial port (LGPL core permits; assets CC BY-NC-SA — attribution kept, no rebranding,
  non-commercial), with the upstream-rebase playbook as the maintenance story. That path is
  already working and loses nothing by asking first.

### Proposed PR decomposition (contingent on a positive Discord signal)

0. **GuideME-fabric PR** (separate repo, gates everything): loom project + AW + published
   artifact. Smallest possible surface; offer co-maintainer commitment there too.
1. **PR-1 "Loader seams" (NeoForge-only, no Fabric code):** the 64 new shared seam files +
   232 shared-source edits + `src/ → loader/neoforge/` moves + Groovy build split into
   root/`loader/neoforge`. NeoForge CI green, zero behavior change (451 unit + 70 gametests).
   This is ~the merge-risk half of the whole delta and reviewable without any Fabric opinions.
2. **PR-2 "Fabric platform layer" (purely additive):** `loader/fabric/` (116 files), AW,
   mixins, datagen transform + drift guard, gametest twins. Deletable as a unit.
3. **PR-3 "CI + release":** build-fabric + gametest-fabric jobs, release pipeline for the
   fabric jar, Crowdin/export-guide path fixes.

Fork-only material never PRed: Kotlin DSL conversion, CLAUDE.md/PORTING_NOTES.md, dist/,
GUIDEME_REPO repo-var plumbing (PR-3 uses the real published guideme-fabric instead).

### Draft OUTLINE — the pre-PR post (Discord #dev, then a tracking issue if invited)

1. **What exists** (1 paragraph): working AE2 Fabric port on MC 26.1.2, dual-loader fork of
   current `main`; NeoForge kept green at every commit; 70/70 gametests on both loaders;
   links: branch, CI runs, jars. Explicitly: *not asking you to write or debug Fabric code.*
2. **Why bother upstream** (short): user demand receipts (#8163/#8273/#8462/#8876), the
   fabric/1.20.1 EMI precedent (#8674) shows community-carried Fabric already happens — this
   moves it to the current line.
3. **The burden answer** (the core section, addresses the GuideME #44 quote head-on):
   - `loader/fabric/` is additive and deletable; shared sources have 0 loader imports (CI-enforced).
   - AT↔AW, datagen drift, silent-patch usage all **fail loudly in CI** with documented fixes —
     no tribal knowledge required of NeoForge-side contributors.
   - Named co-maintainer(s) + committed rebase SLA; if we disappear, deletion is one directory + one CI job.
4. **Sequencing proposal**: GuideME-fabric first (ref its open #44); then the 3-PR ladder above
   (seams → layer → CI), each independently revertable; Groovy build scripts, their formatting rules.
5. **Honest open items**: spatial-storage sky, WTHIT runtime dep, in-world visual checklist,
   ProGuard-less guideme jar.
6. **The ask**: "Is there any version of this you'd merge — and if not the Fabric layer, would
   you take PR-1 (loader seams) alone so downstream ports stay rebase-cheap?" — gives them a
   cheap yes that still halves our maintenance cost. If both are no: blessing/naming guidance
   for an unofficial published port.
