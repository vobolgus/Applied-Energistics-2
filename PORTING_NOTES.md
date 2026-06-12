# AE2 → Fabric 26.1.2 — Porting Notes

Working notes per CLAUDE.md conventions. Compounds across sessions; newest discoveries at the bottom of each section.

## Build / environment

- **No system JDK on this machine.** Launch Gradle with
  `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.11+10/Contents/Home`
  (Gradle-provisioned JDK 21; the foojay resolver downloads the JDK 25 toolchain for compilation).
- Build split (Phase 0): root = conventions (spotless, crowdin); `:neoforge` = `loader/neoforge`
  (ModDevGradle, everything that was in the root build); `:fabric` = `loader/fabric` (fabric-loom 1.17.8).
  Both point `sourceSets` at the shared root `src/main/java` + `src/client/java`.
- Shared sources in `:fabric` are gated behind `ae2.fabric.shared=false` in `gradle.properties`
  until the Phase 1 decoupling reaches zero `net.neoforged` imports (gate M1).
- Version pins (verified live 2026-06-10): fabric-loader **0.19.3**, fabric-api **0.151.0+26.1.2**,
  loom **1.17.8**, Team Reborn Energy **5.0.0** (`teamreborn:energy` @ maven.modmuss50.me).
- `settings.gradle` uses `FAIL_ON_PROJECT_REPOS`; fabricmc/teamreborn/mojang-libraries repos added at
  settings level. Watch for loom trying to inject project-level repos — fallback is
  `RepositoriesMode.PREFER_PROJECT` or an `includeBuild` composite for `:fabric`.
- MC 26.1+ is unobfuscated: loom gets **no `mappings` line** (per spec).
- AT → AW: `src/main/resources/META-INF/accesstransformer.cfg` (56 lines) must be mirrored into
  `loader/fabric/src/main/resources/ae2.accesswidener`. Method entries carry descriptors already;
  ~12 field entries need descriptors derived from the MC jar (javap). Stub header in place; fill at M1.
  Rebase checklist: diff the AT, mirror changes into the AW.
  **DONE (2026-06-11):** 28 entries translated (16 method + 12 field lines), 1 AT entry omitted.
  `:fabric:validateAccessWidener` + `:fabric:build` green; `:neoforge:build` untouched/green.
  Details in the "AT → AW translation" section below.

## AT → AW translation (ae2.accesswidener, namespace `official`)

Source jar for descriptors/visibility checks:
`~/.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar` (javap -p).

Mapping rules applied:
- AT `public` → `accessible` (1:1).
- AT `public-f` / `protected-f` → `mutable` only where access was already sufficient
  (Slot.x/y already public; imageWidth/imageHeight stay protected — subclass-only writes).
- AT `protected` on **private** methods that AE2 overrides or super-calls
  (`isHovering(Slot,DD)`, `getHoveredSlot(DD)`, `setUnderwaterMovement()`) → `extendable`
  (yields protected, non-final). `accessible` would have made them public **final** and broken
  the `AEBaseScreen.isHovering` override.
- AT `protected` entries only *called* externally → `accessible` (AW has no protected target;
  protected→public is a strict widening, safe).

Field descriptor table (derived via javap, all confirmed in vanilla-fabric jar):

| Class | Field | Descriptor | AW |
|---|---|---|---|
| `AbstractContainerScreen` | `imageWidth` | `I` | mutable (protected final → protected) |
| `AbstractContainerScreen` | `imageHeight` | `I` | mutable |
| `GameTestHelper` | `testInfo` | `Lnet/minecraft/gametest/framework/GameTestInfo;` | accessible |
| `MultiPlayerGameMode` | `destroyDelay` | `I` | accessible |
| `Slot` | `x` / `y` | `I` | mutable (already public final) |
| `AbstractContainerMenu` | `stateId` | `I` | accessible |
| `ServerLevel` | `entityManager` | `Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;` | accessible |
| `PersistentEntitySectionManager` | `permanentStorage` | `Lnet/minecraft/world/level/entity/EntityPersistentStorage;` | accessible |
| `PersistentEntitySectionManager` | `chunkVisibility` | `Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;` | accessible |
| `TextureAtlasSprite` | `padding` | `I` | accessible (AT `public`, not `public-f` — final kept) |

Omitted entries (1):
- `protected AbstractContainerScreen.extractSlot(LGuiGraphicsExtractor;LSlot;II)V` — **already
  `protected` in the vanilla 26.1.2 jar**; the AT entry is a no-op on the Fabric side. AE2's
  overrides (AEBaseScreen, MEStorageScreen, PatternEncodingTermScreen) are declared `public`, which
  is a legal widening of a protected override — no AW entry and no Phase 3 mixin accessor needed.
  Re-add as `extendable method` only if a future MC version narrows it.

Non-omissions worth noting:
- `net.minecraft.client.gui.GuiGraphicsExtractor` **is vanilla** in 26.1 (not a NeoForge patch
  class) — `item(LivingEntity, Level, ItemStack, III)V` exists private in the merged jar and is
  widened with `accessible`.
- `setUnderwaterMovement` has no remaining usage in `src/` (likely stale AT entry / future
  ItemEntity subclass); translated anyway as `extendable` to preserve the AT's protected intent.

## GuideME (Workstream A, ../GuideME)

Survey results (main HEAD = MC 26.1.2, NeoForge 26.1.2.10-beta, publishes 26.1.10-alpha):
- ~29k LoC / 286 files, **only 61 `net.neoforged` imports across 17 files**, concentrated in
  `guideme/internal/` (GuideME.java, GuideMEClient.java + hotkey/network/screen).
- Rendering is vanilla-first (GuiGraphics/RenderType/VertexConsumer; custom RenderPipelines are
  vanilla classes registered via one NeoForge event). **No ModelData, no custom geometry loaders.**
- Single packet (`OpenGuideRequest`, playToClient). Single config (client TOML). Mixins essentially
  empty; 52-line AT (introspection only) → needs its own AW translation.
- All runtime deps loader-neutral and shaded (snakeyaml, lucene, flatbuffers, directory-watcher).
- No client source set yet (runtime split via `@Mod(dist)`) — port adds the split or keeps single
  source set with fabric.mod.json client entrypoints (loom split optional for a mostly-client mod).
- Estimated effort: small (~17 event hooks to remap, 1 packet, item model dispatch, render pipeline
  registration). Far below the risk budgeted in the plan.

## API renames / mappings discovered

(fill in as Phase 1 progresses)

## Phase 1 step 2+3 — transfer/capability decoupling (net.neoforged files in src/main/java: 169 → 133)

- New seam class `appeng.neoforge.transfer.NeoForgeResources` (overlay): static replacements for the removed
  bridges `AEItemKey.of(ItemResource)/toResource()`, `AEFluidKey.of(FluidResource)/toResource()`,
  `GenericStack.from(Item|FluidResource,...)`, `InternalInventory.wrapExternal(...)` and
  `InternalInventory.toResourceHandler()` (instanceof dispatch over the former overriders; adapters for
  `BaseInternalInventory` cached via its new `getOrCreatePlatformAdapter(Function)` Object slot).
- Moved wholesale to `loader/neoforge` (packages UNCHANGED): InternalInventoryResourceHandler (now public),
  PlatformInventoryWrapper (`toResourceHandler()` → `getHandler()`), ResourceConversion, ForgeEnergyAdapter,
  GenericStack{Item,Fluid,Inv}Handler, FluidContainerItemStrategy, ForgeExternalStorageStrategy, HandlerStrategy,
  GenericContainerHelper, InsertionOnlyResourceHandler(+WithJournal), PoweredItemCapabilities, and the
  capability P2P parts (Capability/FE/Item/Fluid/ResourceHandlerP2PTunnelPart). `AEParts`/`InitCapabilityProviders`
  still reference the P2P part classes by name — resolves because `:neoforge` merges both source trees; a
  loader-contributed part-definition hook is deferred.
- `ExternalStorageFacade` split: loader-neutral abstract base stays shared; `ResourceHandlerExternalStorageFacade`
  (overlay, package `appeng.me.storage`) holds the NeoForge handler facades + `ofItemHandler/ofFluidHandler`.
- BE refactors (BEs now neoforge-free): AEBaseInvBlockEntity → `getExposedInventory(side)`;
  AEBasePoweredBlockEntity / EnergyAcceptorPart → `isExternalPowerSide(side)` + cached
  `getOrCreateEnergyAdapter(Function)` (preserves adapter identity, i.e. the FE leftover buffer);
  Condenser inner fluid handler → overlay `CondenserFluidHandler`; MEChest inner FluidHandler → overlay
  `MEChestFluidHandler` + BE keeps `fluidHandlerActive` flag, package-private `updateHandler/canAcceptLiquids/
  pushFluidToNetwork`, and a cached `getFluidHandler(side, factory)` Object slot; ItemGen debug BE →
  `getInventory()`.
- Interim shared→overlay references (`appeng.neoforge.*` imports in shared files, to re-point at a real SPI or a
  fabric twin later): AppEngBase (pre-existing), InitCapabilityProviders, InscriberBlockEntity,
  MolecularAssemblerBlockEntity.
- Deliberately KEPT NeoForge types in shared API for now: `net.neoforged.neoforge.fluids.FluidStack` is the
  internal representation of `AEFluidKey` (and `GenericStack.fromFluidStack`, `Platform`, SkyStoneTank) — needs a
  dedicated fluid-stack abstraction step. SkyStoneTankBlockEntity (tank IS a FluidStacksResourceHandler, used for
  serialization/sync/testplots) and EnergyGeneratorBlockEntity (implements EnergyHandler directly) deferred.

## Phase 1 step 5+6 — capabilities + networking seam (net.neoforged files: main 133 → 105, client 58 → 49)

### Step 5 — capabilities
- `appeng.api.AECapabilities` (shared, PUBLIC API, same package/class) now holds only loader-neutral
  capability ids: `ME_STORAGE_ID`, `CRAFTING_MACHINE_ID`, `GENERIC_INTERNAL_INV_ID`,
  `IN_WORLD_GRID_NODE_HOST_ID`, `CRANKABLE_ID` (all `Identifier`). **Addon-facing rename**:
  `AECapabilities.X` (BlockCapability) → `appeng.neoforge.AENeoForgeCapabilities.X` (overlay), built
  from the shared ids via `BlockCapability.createSided/createVoid`. On Fabric these ids will seed
  `BlockApiLookup`s.
- `InitCapabilityProviders` moved wholesale to `loader/neoforge` (package `appeng.init` unchanged).
- Shared files that need the capability OBJECT (not just the id) — they import
  `appeng.neoforge.AENeoForgeCapabilities` with a `// TODO (fabric)` marker at the usage site, because
  they also use NeoForge-only lookup machinery (`Level.getCapability` extension, `BlockCapabilityCache`,
  `PartAdjacentApi`): GridHelper, ICraftingMachine, ICrankable (all api!), StorageBusPart,
  PatternProviderTargetCache, PatternProviderTarget. These resolve a real capability seam later
  (likely a shared lookup facade); deliberately NOT half-seamed now.

### Step 6 — networking
- IPayloadContext surface actually used by AE2 handlers is tiny: `player()` + `enqueueWork(Runnable)`.
  Captured as shared `appeng.core.network.PacketHandlingContext`; `ServerboundPacket.handleOnServer`
  now takes it (was IPayloadContext). `ClientboundPacket` had no context usage (client handlers are
  registered separately via `RegisterClientPayloadHandlersEvent` in `AEClientboundPacketHandler`
  (src/client) — still NeoForge-specific, to be seamed in a later client step).
- `InitNetwork` (shared) is now a loader-neutral payload manifest: `PayloadEntry(direction, Type,
  StreamCodec)` records (CustomPacketPayload.Type + StreamCodec are vanilla), visited via
  `forEachPayload`. Typed `clientbound/serverbound/bidirectional` builder methods preserve the
  invariant that C2S/BIDI payloads implement `ServerboundPacket`.
- New overlay `appeng.neoforge.network.NeoForgeNetworkInit`: subscribes RegisterPayloadHandlersEvent
  (was `InitNetwork::init` in AppEngBase), iterates the manifest (playToClient/playToServer/
  playBidirectional) and adapts IPayloadContext → PacketHandlingContext for serverbound handling.
  NOTE: a generic `register(registrar, entry)` helper method is required — inlining the switch in the
  lambda fails javac due to independent wildcard captures of `entry.type()`/`entry.codec()`.
- Sends: shared seam `appeng.core.network.NetworkAdapter` (static holder, `init()` once from
  AppEngBase ctor, `get()` elsewhere). Operations enumerated from real usage — exactly three:
  `sendToServer(ServerboundPacket)` (~25 call sites, was ClientPacketDistributor.sendToServer),
  `sendToPlayer(ServerPlayer, ClientboundPacket)` (GridsCommand ×4), and
  `sendToPlayersNear(ServerLevel, @Nullable ServerPlayer excluded, x, y, z, radius, ClientboundPacket)`
  (AppEngBase.sendToAllNearExcept, MolecularAssemblerBlockEntity). No other PacketDistributor ops were
  used in shared code. Overlay impl: `appeng.neoforge.network.NeoForgeNetworkAdapter`.
- `AEStreamCodecs` (shared, `appeng.core.network`) replaces `NeoForgeStreamCodecs` usages (12 files):
  only `CHUNK_POS` and `enumCodec(Class)` were used; reimplemented 1:1 from vanilla
  FriendlyByteBuf read/writeChunkPos / read/writeEnum (verified against NeoForge sources).
- `PartLeftClickPacket`: `CommonHooks.onLeftClickBlock` + `NeoForge.EVENT_BUS.post` removed. The hook
  performs NO validation itself — it only fires PlayerInteractEvent.LeftClickBlock for protection
  mods (and the old AE2 code double-posted the event: onLeftClickBlock already posts internally).
  Replaced with the inlined vanilla validation from `ServerPlayerGameMode.handleBlockBreakAction`
  (START_DESTROY_BLOCK path): `isSpectator()`, `isWithinBlockInteractionRange(pos, 1.0)`,
  `ServerPlayer.mayInteract(level, pos)` (spawn protection + world border). Loader-specific
  protection-mod hook to be re-attached at this seam later (Fabric: AttackBlockCallback).
- AppEngBase wiring: `NetworkAdapter.init(new NeoForgeNetworkAdapter())` right after
  `NeoForgeRegistrar.register`; `modEventBus.addListener(NeoForgeNetworkInit::init)` replaces
  `InitNetwork::init`. AppEngBase keeps its other NeoForge code (lifecycle step later).
- Gotcha: spotless **deletes comments placed inside the import block** — TODO markers must live at
  usage sites, not on imports.

## Phase 1 step 8 — render-data contract (net.neoforged files: main 105 → 80, client 49 → 39)

### NeoForge 26.1 model-data dispatch (investigated via javap on neoforge-26.1.2.21-beta)
- Package moved to `net.neoforged.neoforge.model.data` (was `client.model.data`). Dispatch is unchanged
  otherwise: `IBlockEntityExtension` is interface-injected into `BlockEntity` with default
  `getModelData()` / `requestModelDataUpdate()`; the client-side `ModelDataManager` (one per Level)
  calls `be.getModelData()` virtually and snapshots it per chunk section; models read it back through
  the `BlockAndTintGetter.getModelData(pos)` extension. Consequence: the `getModelData()` override
  must live on a class in the BE hierarchy — solved with an overlay base class, NOT a residual import.

### Shared contract (loader-free)
- `appeng.util.render.AERenderProperty<T>` (typed identity key, debug name only) and
  `appeng.util.render.AERenderData` (immutable property map, **content-based equality** — see gotcha
  below) replace ModelProperty/ModelData for render data contributed incrementally by cable-bus parts.
- **API rename**: `IPart.collectModelData(ModelData.Builder)` → `IPart.collectRenderData(AERenderData.Builder)`
  (9 implementors updated mechanically). `PartModelData` constants are now `AERenderProperty`s.
- Block entities return ONE typed immutable object from `getRenderData()` (new, declared on the overlay
  hooks class, overridable from shared code): PaintSplotches (paint splotches BE), QnbFormedState (QNB),
  SpatialPylonBlockEntity.ClientState, `DriveModelData(Item[] cells)` (now a record),
  `CraftingCubeModelData(EnumSet<Direction> connections, @Nullable AEColor color)` (now a record; the
  monitor's color folded in as nullable, CraftingMonitorModelData DELETED), CableBusRenderState
  (its `PROPERTY` constant removed; `PartRenderState` record now carries `AERenderData`).
- `AEModelData` DELETED: `SKIP_CACHE` had **no reader anywhere** (dead since the 26.1 model rewrite);
  `SPIN` moved to `PartModelData.SPIN` (`AERenderProperty<Byte>`, set by AbstractReportingPart — also
  currently has no client reader, kept for state parity).

### NeoForge bridge (overlay)
- `appeng.neoforge.render.NeoForgeRenderData`: single `ModelProperty<Object> AE_RENDER_DATA` +
  `wrap(@Nullable Object)` / `unwrap(ModelData)`. The whole AE2 render-data flow goes through this one
  property on NeoForge.
- `appeng.blockentity.AEBaseBlockEntityHooks` (overlay, original package — loader-duplicated class):
  `extends BlockEntity`, final `getModelData()` = `wrap(getRenderData())`, default `getRenderData()` =
  null, final `requestRenderUpdate()` = `requestModelDataUpdate()`. Shared `AEBaseBlockEntity` now
  extends it. The Fabric twin will implement Fabric API's `RenderDataBlockEntity` (same `getRenderData`
  name) and make `requestRenderUpdate()` a no-op (Fabric has no client-side model-data cache).
- `requestModelDataUpdate()` call sites → `requestRenderUpdate()`: AEBaseBlockEntity (3×),
  CraftingBlockEntity.setBlockState, QuantumBaseBlock.updateShape, AbstractCraftingUnitBlock.updateShape
  and PlaneConnectionHelper.updateConnections (the latter two narrowed from `BlockEntity` to
  `instanceof AEBaseBlockEntity` — a hypothetical non-AE2 IPartHost BE would no longer get a refresh).
- `CableBusBlock.getAppearance` (only main-source model-data CONSUMER): server path now calls
  `AEBaseBlockEntity.getRenderData()` directly (was `be.getModelData()`, same freshly-built state);
  client path unwraps `renderView.getModelData(pos)` via NeoForgeRenderData with a `// TODO (fabric)`
  marker. CableBusBlock is now at zero net.neoforged imports.

### Client consumers (src/client, NeoForge imports still allowed)
- Unwrap-once pattern `NeoForgeRenderData.unwrap(level.getModelData(pos)) instanceof X x ? x : null`:
  CableBusModel, DriveModel, SpatialPylonModel, QnbFormedModel, PaintSplotchesModel, CraftingCubeModel.
- CraftingCubeModel.addInnerCube signature: `ModelData` → `@Nullable CraftingCubeModelData`
  (Unit/Light/MonitorBakedModel updated; monitor color fallback TRANSPARENT preserved).
- `PartModel.collectParts` (client API) + 8 implementations now take `AERenderData partModelData`.

### Gotchas / behavior notes
- ModelData equality is identity-based; AERenderData/record equality is content-based. The cable-bus
  model cache (keyed on CableBusRenderState, which now embeds AERenderData via PartRenderState) can now
  HIT after a refresh that produced identical content — strictly fewer rebuilds, identical quads.
- Where `ModelData.builder().build()` (empty) was returned, the bridge now returns `ModelData.EMPTY` —
  semantically identical.
- Residual `neoforge.model.data` usage is client-only (2 files, for the later client sweep):
  QuartzGlassModel.GLASS_STATE (write-only ModelProperty handed to framedblocks via InterModComms) and
  PaintSplotchesModel (keeps a ModelData pass-through parameter and `createGeometryKey` returning the
  ModelData instance).
- AEBaseBlockEntity remains on the net.neoforged list for unrelated reasons (FriendlyByteBufUtil,
  ConnectionType — networking/sync step); render data itself is fully seamed.

## Phase 1 step 9+10 — config seam + misc decoupling (net.neoforged files: main 80 → 56, client 39 → 45*)

\* client went UP by exactly the 6 codechicken quad-transformer files moved from main into
src/client/java (item e). Net across both shared trees: 119 → 101.

### Step 9 — config seam
- New shared SPI `appeng.core.config.ConfigStore`, shaped strictly to what AEConfig uses:
  `comment(String)` (applies to NEXT define/push, ModConfigSpec.Builder semantics), `push/pop`,
  `defineBoolean(name, def)`, `defineInt(name, def, min, max)`, `defineDouble(name, def, min, max)`,
  `defineEnum(name, def)` — each returning `ConfigStore.Value<T>` (`get()`/`set(T)`) — plus `save()`
  and `onLoadOrReload(Runnable)`. No string/list values exist in AEConfig; not added.
- `AEConfig` keeps its entire public getter/setter API; internally ClientConfig/CommonConfig now take
  a `ConfigStore` and hold it for `save()`. `BooleanValue.getAsBoolean()` → `Value.get()` (unboxing,
  identical). Unbounded int/double defaults (Integer.MIN/MAX, Double.MIN/MAX) stay in AEConfig's
  private define helpers. `AEConfig.register(ModContainer)` → `register(ConfigStore, ConfigStore)`;
  the old modId guard is gone (the overlay only ever passes AE2's own container).
- Upstream QUIRK preserved: `setChannelModel` mutates a COMMON value but saves the CLIENT spec
  (now `client.store.save()`) — kept bug-for-bug.
- Overlay `appeng.neoforge.config.NeoForgeConfigStore`: wraps ModConfigSpec.Builder 1:1 (same TOML
  file names `ae2-client.toml`/`ae2-common.toml`, same sections/comments/ranges, zero config-file
  diff). `initConfigs(ModContainer)` builds both stores, calls `AEConfig.register`, then
  `container.registerConfig` + ModConfigEvent Loading/Reloading listeners that fire the store's
  load listeners (only the common store registers one: `CommonConfig::sync`, same as before).
- AppEngBase wiring: `AEConfig.register(container)` → `NeoForgeConfigStore.initConfigs(container)`.
- `Value<Integer>::get` method refs still satisfy `DoubleSupplier` (unboxing + widening in method
  reference adaptation) — the battery `DoubleSupplier` getters were untouched.

### Step 10a — Curios
- `CuriosIntegration` (just the `EntityCapability` constant) moved wholesale to overlay (package
  `appeng.integration.modules.curios` unchanged).
- New shared seam `appeng.integration.modules.curios.CuriosSupport` (NetworkAdapter-style holder):
  `@Nullable Inventory getCuriosInventory(Player)` with `Inventory { int size(); ItemStack
  getStack(int slot); }` — null maps to the old `getCapability(...) == null` checks, so behavior of
  the 3 shared consumers (SearchInventoryEvent curios listener, CuriosHotkeyAction,
  CuriosItemLocator) is identical including slot indexing. Overlay impl
  `appeng.neoforge.integration.NeoForgeCuriosSupport`.
- NOTE: there was no isModLoaded guard upstream and none is needed — the capability is deliberately
  created without loading Curios classes and the lookup returns null when Curios is absent.
  `Integrations.java` does NOT invoke curios at all (it only has a commented-out TOP IMC stub).
- SearchInventoryEvent keeps both bus listeners in its static block (registration order → stack
  order preserved); the file itself still imports neoforge for the EVENT_BUS mechanism (lifecycle
  step later).

### Step 10b — gametest registration + testplot split
- Overlay `appeng.neoforge.gametest.AENeoForgeGameTests.init(modEventBus)` carries the
  RegisterGameTestsEvent subscription (was `AppEngBase#registerTests`, incl. the
  `appeng.tests` system property gate). `GameTestPlotAdapter` stays shared (no neoforge imports;
  AppEngBase still registers its TEST_INSTANCE_TYPE codec).
- New shared seam `appeng.server.testplots.TestPlotPlatform` (holder pattern):
  `findTestPlotClasses()` (was ModList scan-data lookup in TestPlots), `postKitOutPlayer(...)`,
  `postSpawnExtraGridTestTools(...)` (were `NeoForge.EVENT_BUS.post` from shared code). Overlay impl
  `appeng.neoforge.gametest.NeoForgeTestPlotPlatform`.
- File split (the "6 testplot files with neoforged imports"):
  - MOVED to overlay (packages unchanged): `KitOutPlayerEvent` (extends neoforge PlayerEvent),
    `SpawnExtraGridTestTools` (extends neoforge Event; keeps its odd `@TestPlotClass` annotation —
    harmless, scanning finds no plot methods), `SpawnTestTools` (`@EventBusSubscriber` glue;
    annotation scanning still picks it up from the overlay source set).
  - STAYED shared with edits: `TestPlots` (scan via seam), `MemoryCardTestPlots` (cast
    `(FakePlayer)` → `(ServerPlayer)`; only `getInventory()`/`gameMode` are used — identical),
    `InterfaceTestPlots` (4 pure-vanilla plots stay; the capability-asserting
    `interface_slot_filtering` plot moved to NEW overlay class
    `appeng.server.testplots.InterfaceCapabilityTestPlots`, same plot id).
  - Shared posting sites de-neoforged: `SetupTestWorldCommand`, `SpawnExtraGridTestToolsChest`.
  - `PlotTestHelper.getCapability(BlockPos, BlockCapability, C)` REMOVED (public test-helper API);
    its only caller was the moved plot, which now has a private equivalent via
    `helper.getLevel().getCapability(cap, helper.absolutePos(ref), context)`.

### Step 10c — ChunkLoadingService
- Shared `appeng.server.services.ChunkLoadingService` is now an interface (same name/package,
  `getInstance()` kept so the 3 callers — SpatialAnchorBlockEntity, ChunkMapMixin, AppEngBase init —
  are stable): `forceChunk`, `releaseChunk`, and DEFAULT `isChunkForced` (reads the shared
  package-private `ChunkLoadState` SavedData, exactly as before — note upstream never WRITES that
  state on NeoForge, so the ChunkMapMixin check remains effectively inert; faithful).
- Overlay `appeng.neoforge.service.NeoForgeChunkLoadingService`: TicketController + running flag +
  validateTickets unchanged; its ctor self-wires `RegisterTicketControllersEvent` (mod bus) and
  ServerAboutToStart/ServerStopping (game bus). AppEngBase lost its `onServerAboutToStart`/
  `serverStopping` methods + listeners (they only delegated to the service); `serverStopped`
  (TickHandler) stays.

### Step 10d — worldgen (investigation result: NO java biome modifiers exist)
- The actual neoforge usage in worldgen was: `InitBiomes` (datagen bootstrap, uses
  `NeoForgeEnvironmentAttributes` for custom sky/weather/cloud renderer of the spatial storage
  biome) → moved to overlay (package `appeng.init.worldgen` unchanged); its only caller is
  `AE2DataGenerators` (src/client), which resolves because :neoforge merges the trees.
- `FalloutCopy`/`FalloutMode` used NeoForge convention biome tags → new aliases in `ConventionTags`
  (existing pattern, c.f. METEORITE_OCEAN): `SANDY_BIOMES`, `SNOWY_BIOMES`, `COLD_BIOMES`,
  `PLAINS_BIOMES` = `Tags.Biomes.IS_*` (identity-aliased, no tag id risk). ConventionTags itself
  remains neoforge-dependent — dedicated tags step later.
- `SpatialStorageDimensionIds` only matched the grep via a JAVADOC `{@link}` FQN → reworded to
  `{@code}`. JSON biome resources untouched.

### Step 10e — codechicken quad transformers
- Whole `appeng/thirdparty/codechicken` tree (7 files incl. InterpHelper) git-mv'd from
  src/main/java to src/client/java. They are NOT loader-neutral (import
  `net.neoforged.neoforge.client.model.quad.MutableQuad`) but their only consumer is
  `FacadeBuilder` (src/client) — verified no main-source consumer. They join the client
  neoforge-sweep backlog.

### Step 10f — AEComponents
- `appeng.api.ids.AEComponents` (the prompt's "core/definitions" path was stale): DeferredRegister
  removed; the eager-built `DataComponentType`s now go through
  `AERegistries.register(Registries.DATA_COMPONENT_TYPE, AppEng.makeId(name), () -> componentType)`
  and are flushed by NeoForgeRegistrar. New `AEComponents.init()` force-load hook called from
  AppEngBase in the same ctor position (raw-id order within the registry unchanged — AE2 is the
  only AE2-listener registering data components).
- `DataComponentTypeTagProvider` (src/client datagen) iterated `AEComponents.DR.getEntries()` →
  now iterates `AERegistries.entries(Registries.DATA_COMPONENT_TYPE)` (ResourceKey.create returns
  the interned key, identical map contents).

### Step 10g — HOLDING_CTRL attachment
- Usages enumerated: `UpdateHoldingCtrlPacket.handleOnServer` (set), `PartPlacement` (get),
  `AppEngClient.ctrlEvent` (get+set on the client player). VERIFIED transient: the AttachmentType
  has no serializer and no sync — semantics preserved (defaults to false).
- New shared seam `appeng.core.PlayerCtrlAttachment` (holder pattern):
  `isHoldingCtrl(Player)` / `setHoldingCtrl(Player, boolean)`. Overlay impl
  `appeng.neoforge.NeoForgePlayerCtrlAttachment`, whose ctor registers the (moved, package
  unchanged) `appeng.core.definitions.AEAttachmentTypes` DR on the mod bus — same ctor position as
  the old `AEAttachmentTypes.register(modEventBus)` call.

### AppEngBase ctor wiring after this step
`NeoForgeConfigStore.initConfigs` / `AEComponents.init` / `PlayerCtrlAttachment.init(new
NeoForgePlayerCtrlAttachment(bus))` / `CuriosSupport.init(new NeoForgeCuriosSupport())` /
`ChunkLoadingService.init(new NeoForgeChunkLoadingService(bus))` / `TestPlotPlatform.init(new
NeoForgeTestPlotPlatform())` / `AENeoForgeGameTests.init(bus)` — all next to the existing
NetworkAdapter/NeoForgeRegistrar wiring. AppEngBase remains the single interim shared file with
`appeng.neoforge.*` imports (lifecycle extraction is the next step).

## Phase 1 step 11 — main-source stragglers (net.neoforged files: main 56 → 34, client 45 → 48*)

\* client went UP by exactly the 3 files moved from main into src/client (CubeBuilder, StorageCellModels)
plus MatterCannonAmmoProvider gaining the recipe-condition imports moved out of main. Net across both
shared trees: 101 → 82.

### Part A — FluidStack removed from the core API

- **AEFluidKey** no longer wraps a NeoForge `FluidStack`. New internals mirror what FluidStack did:
  `Holder<Fluid>` + `PatchedDataComponentMap.fromPatch(holder.components(), patch)` (vanilla 26.1
  `Holder` has `components()`/`areComponentsBound()` via the new `TypedInstance` machinery) + cached
  hash using the exact `FluidStack.hashFluidAndComponents` formula (31*(31+fluid identityHash) +
  componentMap.hashCode()). Equality = fluid identity + `PatchedDataComponentMap` equality, exactly
  `isSameFluidSameComponents`. **Format compatibility evidence**: `AEFluidKeyTest` (new) pins the JSON
  shape, asserts the packet bytes are byte-identical to `FluidStack.STREAM_CODEC` (varint amount=1,
  `ByteBufCodecs.holderRegistry(FLUID)`, `DataComponentPatch.STREAM_CODEC`, incl. the
  empty-stack `DecoderException`), and asserts hash equality with `hashFluidAndComponents`.
  MAP_CODEC is unchanged (was already built from vanilla primitives). Note: the OLD `fromPacket`
  retained the decoded amount inside the internal stack and `writeToPacket` re-encoded it; since all
  writers always encode amount 1, the new code always writes 1 — in-practice identical.
- **API changes**: `AEFluidKey.of(FluidStack)`, `matches(AEKey, FluidStack)`, `matches(FluidStack)`,
  `toStack(int)` and `GenericStack.fromFluidStack(FluidStack)` moved to overlay
  `appeng.neoforge.transfer.NeoForgeResources` (`of(FluidStack)`, `matches(AEKey, FluidStack)`,
  `toFluidStack(AEFluidKey,int)`, `fromFluidStack(FluidStack)`). New loader-neutral API:
  `AEFluidKey.of(Fluid, DataComponentPatch)` and `AEFluidKey.getComponentsPatch()`.
  Client files updated to call the overlay statics (FluidKeyRenderer, FluidBlitter,
  FluidIngredientConverter, SkyStoneTankRenderer) — they keep working in :neoforge because client
  compiles against main+overlay; **fabric client step must re-point these at fabric equivalents**.
- New seam `appeng.util.fluid.FluidPlatform` (holder, init from AppEngBase):
  `getFluidDisplayName(Fluid, DataComponentPatch)` (NeoForge impl: `FluidStack.getHoverName()` →
  FluidType description; Fabric will use FluidVariantAttributes) and
  `interactWithTank(Player, Hand, SkyStoneTankBlockEntity)` (NeoForge impl: FluidUtil). Used by
  `AEFluidKey.computeDisplayName` and `Platform.getFluidDisplayName`. Overlay impl
  `appeng.neoforge.fluids.NeoForgeFluidPlatform`.
- `FluidSoundHelper` moved wholesale to overlay (only caller was the overlay
  FluidContainerItemStrategy); no sound seam needed.
- **SkyStoneTankBlockEntity**: tank is now loader-neutral state `(@Nullable AEFluidKey, int amount)`
  with accessors `getStoredFluid/getStoredAmount/getCapacity`, internal `setContents` (raw) +
  `onTankContentsChanged` (markForUpdate+setChanged) + MEChest-style cached
  `getOrCreateFluidHandler(Function)` Object slot. NBT shape kept byte-identical to the old
  `FluidStacksResourceHandler` (`tank.stacks` = list of `FluidStack.OPTIONAL_CODEC`-shaped entries,
  `{}` for empty; codec built from `AEFluidKey.MAP_CODEC` + `ExtraCodecs.POSITIVE_INT("amount")` +
  `ExtraCodecs.optionalEmptyMap`) — pinned by new `SkyStoneTankSerializationTest` against the real
  `FluidStack.OPTIONAL_CODEC`. Sync stream payload unchanged (same NBT roundtrip). Overlay
  `appeng.blockentity.storage.SkyStoneTankFluidHandler` replicates the single-slot
  `StacksResourceHandler` insert/extract/journal semantics (snapshot = (key, amount); root commit →
  `onTankContentsChanged`, matching old `onContentsChanged`); shared via the cached slot between
  InitCapabilityProviders and the FluidPlatform tank interaction. Shared testplot/renderer consumers
  rewritten onto the shared accessors.
- **EnergyGeneratorBlockEntity** (debug): no longer implements `EnergyHandler` — cap registration in
  InitCapabilityProviders now exposes NeoForge's stock `InfiniteEnergyHandler.INSTANCE` (identical
  semantics: amount/capacity MAX, insert→0, extract→amount; only adds checkNonNegative
  preconditions). The serverTick energy push goes through new seam
  `appeng.debug.EnergyGeneratorPlatform.pushEnergy(Level,BlockPos,Direction,int)`; overlay impl
  `appeng.neoforge.debug.NeoForgeEnergyGenerator` (getCapability + Transaction, as before).

### Part B

- **(a) ConventionTags** is neoforge-free. Translated NeoForge `Tags` constants to literal `c:` ids
  (verified against NeoForge 26.1.2.21 sources; all used constants are in the `c` namespace):
  items `c:dusts`, `c:gems`, `c:gems/quartz`, `c:ingots/copper`, `c:nuggets/gold`, `c:ingots/gold`,
  `c:nuggets/iron`, `c:ingots/iron`, `c:gems/diamond`, `c:dusts/redstone`, `c:dusts/glowstone`,
  `c:ender_pearls`, `c:rods/wooden`, `c:chests/wooden`, `c:stones`, `c:glass_blocks`,
  `c:glass_blocks/cheap`, `c:budding_blocks`, `c:buds`, `c:clusters`; blocks `c:glass_blocks`,
  `c:budding_blocks`, `c:buds`, `c:clusters`, `c:relocation_not_supported`; biomes `c:is_ocean`,
  `c:is_sandy`, `c:is_snowy`, `c:is_cold`, `c:is_plains`. `TagKey.create` interns, so identity with
  the NeoForge constants is preserved on :neoforge.
- **(b) RegistryFriendlyByteBuf ConnectionType**: AEBaseBlockEntity (update tag write/read — the
  `FriendlyByteBufUtil.writeCustomData` helper is now inlined as a private `writeCustomData`),
  AEBaseMenu (client-action arg encode/decode) and MEInventoryUpdatePacket (entry buffer) now use the
  vanilla 2-arg constructor (Neo-deprecated, defaults to `ConnectionType.OTHER`). Investigated impact:
  the flag only changes encoding through `connectionAware` codecs (custom-ingredient payloads, modded
  recipe-book settings) and the holder-set codec's custom-holder-set branch — none of which occur in
  AE2's BE sync, menu args or ME inventory entries; and where AE2 controls both encode and decode the
  switch is symmetric. The only asymmetric pair (MEInventoryUpdatePacket entries encoded into a
  detached OTHER buffer, decoded from the NEOFORGE connection buffer) is safe because vanilla-format
  data decodes identically under the NEOFORGE flag.
- **(c) P2P attunement**: `P2PTunnelAttunement.registerAttunementApi(ItemLike, ItemCapability, ...)` and
  `registerItemAccessAttunementApi` REMOVED from the shared API (**source-compat break for addons on
  NeoForge** — equivalents live in overlay `appeng.neoforge.AENeoForgeP2PAttunement`). Shared API
  gained the loader-neutral `registerAttunementApi(ItemLike, Object api, Predicate<ItemStack>,
  Component)`; `ApiAttunement.capability` and `P2PTunnelAttunementInternal.AttunementInfo` now hold
  opaque `Object`s (`getAttunementInfo` has no callers anywhere — dead reporting API, kept).
  `InitP2PAttunements` keeps only the tag attunements; the two capability attunements (energy/fluid)
  moved to `AENeoForgeP2PAttunement.init()`, called from AppEngBase.postRegistrationInitialization
  immediately after `InitP2PAttunements.init()` (apiAttunements order [energy, fluid] preserved →
  identical JEI/REI listing and trigger-item resolution order).
- **(d) other sweeps**:
  - `IManagedGridNode` no longer extends NeoForge `ValueIOSerializable` — it declares
    `serialize/deserialize` itself (they were already declared; dropped the redundant `@Override`s).
  - `BlockTransitionEffectPacket`: `GameData.getBlockStateIDMap()` → vanilla
    `Block.BLOCK_STATE_REGISTRY` (verified IDENTICAL instance on NeoForge — Neo patches the vanilla
    field to alias its own map; byte-identical ids).
  - `MainCreativeTab.initExternal(BuildCreativeModeTabContentsEvent)` →
    `initExternal(ResourceKey<CreativeModeTab>, Consumer<ItemLike>)`; AppEngBase adapts the event in
    its listener lambda.
  - Moved wholesale to overlay (packages unchanged): `RaidHeroGiftsProvider` (NeoForge DataMapProvider;
    caller AE2DataGenerators in src/client resolves via merged trees).
  - Moved main → src/client (10e-precedent, joins the client sweep backlog): `CubeBuilder`
    (MutableQuad; all consumers client) and `appeng.api.client.StorageCellModels` (StandaloneModelKey;
    consumers AppEngClient/MEChestRenderer/DriveModel — **API break**: class is now client-only).
  - `MatterCannonAmmo`: the tag-conditioned datagen helper (NotCondition/TagEmptyCondition) moved into
    `MatterCannonAmmoProvider` (src/client) as private `tagAmmo`; the two vanilla `ammo` overloads stay
    shared.
  - Javadoc-only FQN matches reworded: AECapabilities, StorageCells, SpatialAnchorBlockEntity.
- **Tests**: suite is now 451 (443 + 8 new). New tests need an `EphemeralTestServerProvider` server
  parameter: fluid holder data components are only **bound** during a server datapack load
  (`DataComponentInitializers` via ReloadableServerResources); without it, `Holder.components()`
  throws "Components not bound yet" (this equally affected the old FluidStack-based code when tests
  ran in isolation).

### Remaining main-source net.neoforged files (34) — for later steps

- Lifecycle/events step (excluded by design from this step): AppEngBase, AppEngServer,
  hooks/{SkyStoneBreakSpeed, WrenchHook, ticking/TickHandler}, SearchInventoryEvent, Integrations,
  ChunkLogger, mixins/{ConfigPlugin, spatial/MinecraftServerMixin}, Platform (ModList/FMLEnvironment/
  FMLLoader/SidedThreadGroups/FakePlayerFactory).
- Game-event posts needing an events seam: CraftingEvent, QuartzKnifeMenu (PlayerDestroyItemEvent),
  AppEngCraftingSlot (CommonHooks.setCraftingPlayer + firePlayerCraftingEvent), MatterCannonItem
  (BlockSnapshot/BreakBlockEvent protection hook), QuantumCluster (LevelEvent.Unload listener),
  TinyTNTPrimedEntity (IEntityWithComplexSpawn spawn payload + onExplosionDetonate),
  TransformLogic (ServerStartedEvent/AddServerReloadListenersEvent + OrHolderSet).
- Capability-lookup seam (deliberately deferred since step 5): PatternContainerGroup, PartAdjacentApi,
  StorageExportStrategy, StorageImportStrategy, StorageBusPart (ICapabilityInvalidationListener),
  PatternProviderTargetCache, RegisterPartCapabilitiesEvent(+Internal — overlaps part-capability glue).
- Transfer-transaction seam: GenericInternalInventory.updateSnapshots(TransactionContext),
  GenericStackInv (SnapshotJournal), P2PTunnelPart (energy-cost SnapshotJournal).
- Misc: AEKeyTypesInternal (NeoForge registry BakeCallback — belongs with NeoForgeRegistrar),
  CrankBlock (FakePlayer instanceof check — needs a Platform.isFakePlayer-style seam),
  MenuTypeBuilder (IMenuTypeExtension.create — menus-with-data seam; Fabric:
  ExtendedScreenHandlerType), QuartzCuttingRecipe (CommonHooks.getCraftingPlayer, RecipeMatcher,
  ServerLifecycleHooks), CompatLayerHelper (ModList.isLoaded — mod-loaded seam with Platform).

## Phase 1 step 12 — capability-lookup + transaction seams + stragglers (net.neoforged files: main 34 → 18, client 48 = 48)

### Part 1 — sided-API lookup seam (`appeng.api.lookup`)

- New shared API package `appeng.api.lookup`:
  - `AEApiLookup<A>` — "find API A at (level, pos, side)". Surface derived from actual usage:
    `find(Level, BlockPos, @Nullable Direction)`, hinted
    `find(Level, BlockPos, BlockState, @Nullable BlockEntity, @Nullable Direction)` (the
    state/BE-known optimization used by ICraftingMachine/PatternProviderTarget), plain
    `createCache(ServerLevel, BlockPos, side)` and
    `createCache(ServerLevel, BlockPos, side, BooleanSupplier isValid, Runnable invalidationListener)`
    returning `AEApiCache<A>` (`@Nullable A get()`). Cache semantics documented = NeoForge
    BlockCapabilityCache exactly (null while unloaded; once isValid returns false the cache is dead
    and get() must not be called; don't query from inside the listener). Note `find` takes `Level`
    (not ServerLevel) because GridHelper.getNodeHost is callable client-side.
  - Static factories `AEApiLookup.sided(id, class)` / `unsided(id, class)` return `IdAEApiLookup`,
    which lazily resolves the loader lookup via the `AEApiLookups` holder on FIRST USE — so the
    `AECapabilities` constants are clinit-safe regardless of injection order.
  - `AEApiLookups` (holder-pattern seam): `createLookup(id, class, sided)` (internal),
    `registerInvalidationListener(ServerLevel, BlockPos, AEApiInvalidationListener)`, and the two
    semantic external-handler queries `hasNonEmptyItemHandler` / `hasNonEmptyFluidHandler`
    (replacing PatternContainerGroup's direct `Capabilities.Item/Fluid.BLOCK` + `size()` checks —
    item is `size() > 0`, fluid is `size() != 0`, short-circuit order preserved). DEVIATION from
    plan: external Item/Fluid lookups did NOT become typed `AEApiLookup` constants because there is
    no loader-neutral type for the looked-up handler; the only shared consumer was this heuristic.
    No shared external Energy lookup exists (verified).
  - `AEApiInvalidationListener` is a **loader-duplicated interface** (lives in
    loader/neoforge/.../appeng/api/lookup): on NeoForge it `extends ICapabilityInvalidationListener`
    and is registered with `ServerLevel.registerCapabilityListener` DIRECTLY — zero wrappers, so the
    weak-reference semantics of the invalidation system are preserved exactly (a wrapper would be
    collected immediately since NeoForge only holds listeners weakly). Fabric twin: declares
    `boolean onInvalidate()` itself; registration is a no-op (no invalidation events on Fabric —
    storage bus then relies on its tick/neighbor-update paths).
- `AECapabilities` now holds the 5 typed lookups next to the ids: `ME_STORAGE`, `CRAFTING_MACHINE`,
  `GENERIC_INTERNAL_INV`, `IN_WORLD_GRID_NODE_HOST` (unsided), `CRANKABLE`.
- Overlay backend `appeng.neoforge.lookup.NeoForgeApiLookups`: `Sided/VoidLookup` records wrap
  `BlockCapability`; `createLookup` calls `BlockCapability.createSided/createVoid` — VERIFIED
  (CapabilityRegistry source): create() is get-or-create by name, so the backend resolves the SAME
  instances as `AENeoForgeCapabilities` builds. `NeoForgeApiLookups.of(BlockCapability)` wraps
  arbitrary caps for overlay callers (CapabilityP2PTunnelPart keeps its public BlockCapability ctor).
- Consumers rewritten (all `// TODO (fabric)` capability markers from step 5 resolved): GridHelper,
  ICraftingMachine (both `of` overloads), ICrankable, PatternContainerGroup, PatternProviderTarget,
  PatternProviderTargetCache (plain cache), PartAdjacentApi (cache w/ validity+listener; now takes
  `AEApiLookup<T>`), StorageBusPart (ME_STORAGE lookup + AEApiInvalidationListener field +
  registerInvalidationListener seam).
- `StorageExportStrategy`/`StorageImportStrategy` moved WHOLESALE to overlay (packages unchanged)
  instead of being seamed: their guts are `HandlerStrategy<T,S>` (already overlay) over NeoForge
  `ResourceHandler`s — nothing loader-neutral remains. Shared `StackWorldBehaviors` keeps referencing
  them (same interim pattern as ForgeExternalStorageStrategy; a loader-contributed default-strategy
  hook remains deferred).

### Part 1b — part-capability registration

- New shared `appeng.api.parts.PartApiRegistry`: capability key is an opaque `Object` token
  (NeoForge: the BlockCapability; Fabric: will be the BlockApiLookup), `PartApiProvider<P,C,T>`
  mirrors ICapabilityProvider, `Registration<T,C>.find(IPartHost, C)` carries the old
  buildProvider dispatch (context→side→part→exact-class provider map; validation messages and
  putIfAbsent duplicate check preserved verbatim).
- `RegisterPartCapabilitiesEvent` + `...EventInternal` moved to overlay (package `appeng.api.parts`
  unchanged). The event keeps its EXACT addon-facing NeoForge surface (registerContext/register/
  addHostType with BlockCapability/ICapabilityProvider) and delegates into a package-private
  `PartApiRegistry`; Internal iterates `registry.getRegistrations()` and registers
  `registration::find` per host type with RegisterCapabilitiesEvent. InitCapabilityProviders
  unchanged. **No addon API break on NeoForge for part capabilities.**

### Part 2 — transaction seam (option (a)-minus: opaque handle, NO open())

- Inventory result: shared code NEVER opens transactions (all `Transaction.open*` sites are
  overlay); it only (1) receives a transaction and forwards it into journal participation
  (`GenericInternalInventory.updateSnapshots`, P2P `deduct*Cost`) and (2) IS a journal
  (GenericStackInv extends SnapshotJournal, P2PTunnelPart.EnergyCostJournal). So no
  `AETransactions.open()` holder was added — only the opaque handle.
- `appeng.api.behaviors.AETransaction` — empty shared marker interface. Overlay
  `appeng.neoforge.transfer.NeoForgeTransaction(TransactionContext)` record with `of`/`unwrap`
  (unwrap restores the IDENTICAL TransactionContext, so SnapshotJournal depth/registration
  semantics are untouched; the per-call wrapper allocation is the entire runtime cost).
- `appeng.util.AESnapshotJournal<T>` — **loader-duplicated abstract class** (loader/neoforge):
  `extends SnapshotJournal<T>` + `public final void updateSnapshots(AETransaction)` overload that
  unwraps. Subclass contract = NeoForge names (`createSnapshot`/`revertToSnapshot`/
  `onRootCommit(originalState)`), documented as the cross-loader contract. Fabric twin plan: extend
  `SnapshotParticipant<T>`, map `readSnapshot`→`revertToSnapshot`, and capture the outermost
  original state itself to drive `onRootCommit(originalState)` from `onFinalCommit` — semantics are
  implementable because Fabric also snapshots per transaction nesting level.
- Shared signature changes (**addon source break, NeoForge**):
  `GenericInternalInventory.updateSnapshots(TransactionContext→AETransaction)`;
  `P2PTunnelPart.deductEnergyCost(double, PowerUnit, TransactionContext→AETransaction)` and
  `deductTransportCost(long, AEKeyType, TransactionContext→AETransaction)` (protected, affects
  addon P2P parts). Overlay callers wrap at the boundary: GenericStackInvHandler (×2),
  FEP2PTunnelPart (×2), ResourceHandlerP2PTunnelPart (×3).
- `GenericStackInv` now extends `AESnapshotJournal<GenericStack[]>` (journal overrides unchanged);
  `EnergyCostJournal extends AESnapshotJournal<Double>`.

### Part 3 — stragglers

- New holder seam `appeng.util.LoaderPlatform` (overlay `appeng.neoforge.NeoForgeLoaderPlatform`):
  `isFakePlayer(Player)` (CrankBlock `instanceof FakePlayer`), `isModLoaded(String)`
  (CompatLayerHelper), `getCraftingPlayer()` (QuartzCuttingRecipe ← CommonHooks; the SETTER stays in
  AppEngCraftingSlot for the later events step), `recipeInputsMatch(List<ItemStack>,
  List<Ingredient>)` (QuartzCuttingRecipe ← `RecipeMatcher.findMatches(...) != null`; Fabric will
  need its own matching algorithm).
- QuartzCuttingRecipe's `ServerLifecycleHooks.getCurrentServer()` → existing shared
  `AppEng.instance().getCurrentServer()` (AppEngBase delegates to ServerLifecycleHooks — identical;
  no new seam needed). NOTE: would NPE if called before mod construction, but no such caller/test.
- New holder seam `appeng.menu.implementations.MenuTypePlatform` (overlay
  `appeng.neoforge.menu.NeoForgeMenuTypePlatform`): `createMenuType(MenuFromNetworkFactory)`
  (← IMenuTypeExtension.create) and `openMenu(ServerPlayer, Component title, MenuConstructor,
  Consumer<RegistryFriendlyByteBuf>)` (← the import-less NeoForge `player.openMenu(provider,
  writer)` extension + the `shouldTriggerClientSideContainerClosingOnOpen` override, both now built
  overlay-side; the AE-menu-switch no-close behavior is preserved verbatim). Fabric:
  ExtendedScreenHandlerType + ExtendedScreenHandlerFactory. MenuTypeBuilder still references
  `Minecraft.getInstance()` in `fromNetwork` (pre-existing main-source client leak; for the
  server-split sweep).
- `AEKeyTypesInternal`: BakeCallback removed; new `updateAllTypes()` rebuilds the allTypes cache
  from the registry. AppEngBase.registerRegistries re-attaches
  `registry.addCallback((BakeCallback<AEKeyType>) ignored -> AEKeyTypesInternal.updateAllTypes())`
  right after setRegistry (same firing points as before). Fabric: call updateAllTypes after
  registration freeze.
- AppEngBase ctor wiring added at the top (right after initConfigs, before any registration class
  loading): `AEApiLookups.init(new NeoForgeApiLookups())`, `LoaderPlatform.init(new
  NeoForgeLoaderPlatform())`, `MenuTypePlatform.init(new NeoForgeMenuTypePlatform())`.

### Remaining main-source net.neoforged files (18) — all lifecycle/events scope by design

AppEngBase, AppEngServer, hooks/{SkyStoneBreakSpeed, WrenchHook, ticking/TickHandler},
SearchInventoryEvent, Integrations, ChunkLogger, mixins/{ConfigPlugin, spatial/MinecraftServerMixin},
Platform, and the events-seam files: CraftingEvent, QuartzKnifeMenu, AppEngCraftingSlot,
MatterCannonItem, QuantumCluster, TinyTNTPrimedEntity, TransformLogic.

### Gotchas

- Spotless' "JVM-local cache is stale" failure after touching build state → `rm -rf
  .gradle/configuration-cache` and re-run.
- NeoForge holds capability invalidation listeners ONLY weakly — never hand the system a wrapper
  lambda nobody else references; that was the reason for the loader-duplicated listener interface.

## Phase 1 step 13 — lifecycle + events (net.neoforged files: main 18 → **0** = gate M1 for main sources, client 48 = 48)

`grep -rl "net.neoforged" src/main/java | wc -l` → 0. `:neoforge:build` green, 451 tests (1 skipped, as before).

### Entrypoint architecture (Phase 2's AppEngFabric mirrors this)

- **AppEngBase is loader-free.** Ctor takes NO arguments and only sets the singleton (`AppEng.instance()`
  contract unchanged). The loader entrypoint drives the lifecycle:
  1. construct the dist-specific subclass (client: `AppEngClient`, server: `AppEngServer` — both now
     loader-free; the NeoForge `@Mod` shells live elsewhere, see below);
  2. inject ALL static-holder seams (AEApiLookups, LoaderPlatform, **LoaderEventHooks** (new),
     MenuTypePlatform, config stores, then after registerContent: PlayerCtrlAttachment, registrar,
     NetworkAdapter, FluidPlatform, EnergyGeneratorPlatform, CuriosSupport, ChunkLoadingService,
     TestPlotPlatform);
  3. `base.registerContent()` — the shared force-load block (InitGridServices,
     InitBlockEntityMoveStrategies, AEParts, AEBlocks, AEItems, AEBlockEntities, AEComponents,
     AEEntities, AERecipeTypes, AERecipeSerializers, InitStructures.register);
  4. wire registry fills: `registerSounds(Registry)`, `registerCreativeTabs(Registry)`,
     `registerKeyTypes(Registry<AEKeyType>)` + the loader-side AEKeyType registry creation
     (`AEKeyTypesInternal.setRegistry` + updateAllTypes after freeze/bake), plus the shared
     InitStats/InitAdvancementTriggers/InitParticleTypes/InitMenuTypes/InitVillager/chunk-generator/
     GameTestPlotAdapter.CODEC registrations (all loader-neutral classes, called with the registry);
  5. common setup (main thread, after registration): `base.postRegistrationInitialization()`
     (+ loader extras, NeoForge: `AENeoForgeP2PAttunement.init()`);
  6. game hooks: `TickHandler.instance().onServerTickStart/onServerTickEnd/
     onServerLevelTickStart(ServerLevel)/onServerLevelTickEnd(ServerLevel)/
     onUnloadChunk(LevelAccessor, packedChunkPos)/onUnloadLevel(LevelAccessor)` (level unload LAST —
     was EventPriority.LOWEST), `QuantumCluster.onLevelUnload(LevelAccessor)` (BEFORE TickHandler's
     unload), `ChunkLogger.chunkLoaded/chunkUnloaded(LevelAccessor, ChunkAccess)` (always-on no-op
     forwarders), `base.onServerStopped()`, `base.registerCommands(CommandDispatcher)`,
     `WrenchHook.onPlayerUseBlock(...)` → cancel interaction if != PASS,
     `SkyStoneBreakSpeed.getIncreasedBreakSpeed(...)` → set if non-null,
     `base.getServerSyncedRecipeTypes()` → loader's datapack-sync recipe push;
  7. `HotkeyActions.init()` last (virtual `registerHotkey` dispatch on the half-constructed subclass —
     same timing as the old super-ctor call).
- **NeoForge entrypoints**: overlay `appeng.neoforge.AppEngNeoForge.init(AppEngBase, IEventBus,
  ModContainer)` carries ALL the above wiring (mod-bus listeners verbatim from the old AppEngBase ctor:
  NewRegistryEvent, BuildCreativeModeTabContents, NeoForgeNetworkInit, InitCapabilityProviders ×3 with
  priorities, the RegisterEvent dispatch, Integrations::enqueueIMC, commonSetup incl. the error
  logging). Called from the two `@Mod` ctors: NEW overlay `appeng.neoforge.AppEngNeoForgeServer`
  (`@Mod(dist=DEDICATED_SERVER)`, extends shared `AppEngServer`) and `appeng.client.AppEngClient`
  (`@Mod(dist=CLIENT)` unchanged in src/client; its ctor's `super(bus, container)` became
  `AppEngNeoForge.init(this, bus, container)` — only touch to the client). Dist semantics identical.
- ORDER NOTE 1: seam injection now happens BEFORE `NeoForgeConfigStore.initConfigs` (was after) so no
  class with seam-touching clinit can load first; config init has no seam dependency — inert swap.
- ORDER NOTE 2: `AENeoForgeP2PAttunement.init()` moved from inside postRegistrationInitialization
  (right after InitP2PAttunements.init) to right after the whole method in the overlay's enqueueWork.
  Tag-attunements-before-capability-attunements preserved; only the relative position vs
  cauldron/dispenser/upgrade init changed (no interdependency).

### New seam: `appeng.core.LoaderEventHooks` (holder; overlay impl `appeng.neoforge.NeoForgeEventHooks`)

Outbound game events AE2 posts for OTHER mods: `firePlayerCraftingEvent(Player, ItemStack, Container)`,
`firePlayerDestroyItem(Player, ItemStack, @Nullable InteractionHand)`,
`isBlockPlaceCanceled(Player, Level, BlockPos, Direction)` (EventHooks.onBlockPlace + BlockSnapshot),
`isBlockBreakCanceled(Level, BlockPos, BlockState, Player)` (BreakBlockEvent),
`fireExplosionDetonate(Level, ServerExplosion, List<Entity>, List<BlockPos>)`,
`fireLevelLoad(ServerLevel)`. Fabric twins: PlayerBlockBreakEvents.BEFORE for break; most others
likely no-ops (Fabric has no equivalents — protection mods hook differently).

### LoaderPlatform additions (overlay impl extended)

`setCraftingPlayer(@Nullable Player)` (pairs the step-12 getter; AppEngCraftingSlot),
`getServerThreadGroup()` (SidedThreadGroups.SERVER → Platform.isClient/isServer/assertServerThread;
Platform clinit-caches it, `serverThreadGroup` test override field kept),
`hasClientClasses()` / `isDevelopmentEnvironment()` (FMLLoader, incl. null-loader test fallback),
`getModDisplayName(String)` (ModList), `getFakePlayer(ServerLevel, GameProfile)` (FakePlayerFactory;
profile construction stays shared), `getCurrentServer()` (ServerLifecycleHooks → AppEngBase delegates).
Fabric: thread-group has no equivalent — likely `server.isSameThread()`-style rework or a custom
group; flagged for Phase 2.

### Per-file resolutions (section B of this step)

- **SearchInventoryEvent**: no longer a NeoForge event — now an AE2-internal callback list
  (`addStackSource(BiConsumer<Player, List<ItemStack>>)`); the two upstream listeners (player inv,
  curios via CuriosSupport) moved into the static block in identical order. **NeoForge addon break**:
  addons that subscribed to the event must call addStackSource instead (the event cannot keep its FQN
  on the bus since the shared class occupies it).
- **CraftingEvent** + **AppEngCraftingSlot**: both posted ItemCraftedEvent (directly / via
  EventHooks.firePlayerCraftingEvent) → single seam op `firePlayerCraftingEvent`. Slot's
  CommonHooks.setCraftingPlayer pair → LoaderPlatform.setCraftingPlayer.
- **QuartzKnifeMenu**: PlayerDestroyItemEvent post → `firePlayerDestroyItem(player, before, null)`.
- **MatterCannonItem**: paint-placement protection check → `isBlockPlaceCanceled`; block-destroy
  protection check → `isBlockBreakCanceled`.
- **TinyTNTPrimedEntity**: `IEntityWithComplexSpawn` + write/readSpawnData REMOVED — verified via
  javap on the 26.1.2 jar that PrimedTnt's fuse is synched entity data (`DATA_FUSE_ID`), so the custom
  spawn payload only duplicated it (renderer reads `getFuse()` from synched data). The explosion
  notification → `fireExplosionDetonate` (informational on NeoForge too: AE2 applies effects before
  posting).
- **QuantumCluster**: dynamic `NeoForge.EVENT_BUS.register/unregister(this)` for LevelEvent.Unload →
  static `ACTIVE_CLUSTERS` CopyOnWriteArrayList + `onLevelUnload(LevelAccessor)` dispatched by the
  entrypoint (CoW handles self-removal during dispatch, like the bus did). Runs before TickHandler's
  level-unload cleanup (was: default priority vs LOWEST).
- **TransformLogic**: ⚠ the two `@SubscribeEvent` methods (ServerStartedEvent cache clear,
  AddServerReloadListenersEvent reload listener) were NEVER REGISTERED to any bus upstream (no
  @EventBusSubscriber, no register call — verified across history & whole repo) i.e. dead code.
  Converted to loader-neutral `onServerStarted()` / `createCacheInvalidationReloadListener()` +
  `CACHE_INVALIDATION_RELOAD_LISTENER_ID`, deliberately NOT wired by the entrypoint (bug-for-bug;
  upstream-report candidate). NeoForge `OrHolderSet` → private `unionOf` building
  `HolderSet.direct(flatMap distinct)` — equivalent: CompositeHolderSet also flattens into a cached
  union set, holder equality is identity in both, and the caches rebuild whenever recipes change.
- **hooks/WrenchHook**: NeoForge RightClickBlock wrapper (isCanceled guard + setCancellationResult)
  moved into AppEngNeoForge; shared keeps `onPlayerUseBlock(Player, Level, Hand, BlockHitResult)`.
  Fabric: UseBlockCallback.
- **hooks/SkyStoneBreakSpeed**: BreakSpeed event → shared
  `@Nullable Float getIncreasedBreakSpeed(Player, BlockState, float)`; overlay only calls setNewSpeed
  for non-null (identical to the old conditional set).
- **hooks/ticking/TickHandler**: `init()` removed; the 6 bus subscriptions became public loader-free
  methods (see lifecycle list above); event-type filtering (`instanceof ServerLevel`) moved to the
  overlay forwarders, `isClientSide` guards stayed shared.
- **ChunkLogger**: dynamic bus register/unregister → static `ACTIVE_LOGGERS` list + always-on
  entrypoint forwarders (no-op when disabled). Ordering vs TickHandler.onUnloadChunk preserved
  (registered after = runs after). The stack-trace trigger ("EventBus...post") still matches on
  NeoForge since forwarders run inside bus dispatch.
- **Integrations** (dormant TOP IMC stub): moved wholesale to overlay (package unchanged); the
  enqueueIMC mod-bus listener lives in AppEngNeoForge.
- **Platform**: all FML/NeoForge remnants → LoaderPlatform (see additions above). `isClient/isServer`
  still compare against the loader thread-group CONSTANT; `assertServerThread` against the
  test-overridable field — exact upstream split kept.
- **mixins/MinecraftServerMixin**: the emulated LevelEvent.Load post for the injected spatial level →
  `LoaderEventHooks.fireLevelLoad` (holder is initialized long before createLevels runs).
- **mixins/ConfigPlugin**: moved wholesale to overlay (package `appeng.mixins` unchanged) — it is a
  **loader-duplicated class** (AEApiInvalidationListener precedent): mixin plugins run before mod
  construction, so the LoaderPlatform holder is NOT available; the shared `ae2.mixins.json` keeps
  referencing `appeng.mixins.ConfigPlugin` and each loader supplies its own impl (Fabric twin:
  FabricLoader.getInstance().isModLoaded).
- **AppEngBase / AppEngServer**: see entrypoint architecture. AppEngBase is no longer on the interim
  `appeng.neoforge.*` import list (remaining 3: InscriberBlockEntity, MolecularAssemblerBlockEntity,
  CableBusBlock — capability/render TODOs from earlier steps).

### Gotchas

- `OnDatapackSyncEvent.sendRecipes` takes varargs → `getServerSyncedRecipeTypes()` returns
  `RecipeType<?>[]` (a List would need an unchecked toArray).
- The 1-skipped test was already skipped before this step (suite total unchanged at 451).

## Phase 1 step 14 — client-source sweep (net.neoforged files: client 48 → **0**; gate M1 COMPLETE)

`grep -rl "net.neoforged" src/client/java | wc -l` → 0 (main already 0). `:neoforge:build` green,
451 tests (1 skipped, unchanged). `runData`/`generateModMetadata`/`apiJar` still configure; the
distribution jar and api jar contents are unchanged (overlay client classes are part of
`sourceSets.client`, so jar/sourcesJar/javadoc/apiJar pick them up exactly as before).

### Build setup

- `loader/neoforge/build.gradle`: client source set gained the overlay dir —
  `java.srcDirs = [rootProject.file('src/client/java'), 'src/client/java']` (mirrors main).

### Client entrypoint split (a) — the Phase 3 surface

- **`appeng.client.AppEngClient` is now loader-free** (no `@Mod`, no NeoForge imports). Ctor takes the
  two `KeyMapping`s (`mouseWheelItemModifier`, `partPlacementOpposite`) because their NeoForge
  construction uses `KeyConflictContext.IN_GAME`; the loader subclass creates and passes them
  (Fabric: KeyBindingHelper-created vanilla mappings). Ctor only assigns fields + `INSTANCE = this`
  (INSTANCE is now set slightly EARLIER than upstream — before the AppEngNeoForge.init lifecycle —
  strictly more available, nothing reads it during init).
- New overlay entrypoint `appeng.neoforge.client.AppEngNeoForgeClient`
  (`@Mod(value = "ae2", dist = CLIENT)`, extends AppEngClient — mirrors AppEngNeoForgeServer). Its
  ctor: `super(keymappings)` → `ClientLoaderHooks.init(new NeoForgeClientLoaderHooks())` →
  `AppEngNeoForge.init(this, bus, container)` → all event wiring in the exact original statement
  order (incl. `initGuide()` at the original guide-creation position, i.e. AFTER the full common
  init — Guide.build timing unchanged).
- **Loader-free client-init surface on AppEngClient** (Phase 3's AppEngFabricClient calls these; all
  registrar parameter types are vanilla):
  - `registerClientCommands(CommandDispatcher<CommandSourceStack>)`
  - `registerClientTooltipComponents(ClientTooltipComponentRegistrar)` (nested generic-method iface)
  - `registerKeyMappings(Consumer<KeyMapping>)` (registers the 2 fixed mappings +
    `Hotkeys.finalizeRegistration`; category registration `Hotkeys.CATEGORY` stays loader-side —
    `KeyMapping.Category` is vanilla but NeoForge wants an explicit `registerCategory`)
  - `clientSetup()` (AEKeyRendering registration incl. the try/catch error logging; loader enqueues
    on the main thread)
  - `onMouseWheel(double scrollDeltaY) → boolean cancel` / `onKeyInput(int key, int action)` (loader
    adapts its input events; NeoForge cancels MouseScrolling when true)
  - `clientTickStart()` (cable render mode refresh; register as LATE as possible — was
    EventPriority.LOWEST on ClientTickEvent.Pre) / `clientTickEnd()` (pinned keys + hotkeys)
  - `onPlayerLoggingIn()` / `receiveRecipes(RecipeMap, Collection<RecipeType<?>>)` (server recipe sync)
  - `registerEntityRenderers(EntityRendererRegistrar)` /
    `registerBlockEntityRenderers(BlockEntityRendererRegistrar)` (vanilla
    `BlockEntityRendererProvider<T, S>`) / `registerEntityLayerDefinitions(BiConsumer<...>)`
  - `registerParticleProviders(SpriteSetRegistrar)` (vanilla
    `ParticleResources.SpriteParticleRegistration`) / `registerParticleGroups(BiConsumer<...>)`
  - `registerRenderPipelines(Consumer<RenderPipeline>)` (vanilla blaze3d type)
  - `registerItemModelProperties(...)` / `registerItemModels(...)` / `registerItemTintSources(...)`
    (BiConsumer<Identifier, MapCodec<...>>; all vanilla item-model/tint types) /
    `registerBlockTintSources(BlockTintSourceRegistrar)` (vanilla `BlockTintSource` lists per Block)
  - `registerPartModelTypes(BiConsumer<Identifier, MapCodec<? extends PartModel.Unbaked>>)` /
    `registerPartRenderers(PartRendererRegistrar)` (AE2's own registrations into its own addon events)
  - `initCustomClientRegistries()` (constructs PartModels + StorageCellModels registrations; NeoForge
    fires it from InitializeClientRegistriesEvent — must run before model bake)
  - `initGuide()` (protected; creates the GuideME guide — GuideME API calls are loader-neutral and
    stay in the shared core)
  - Screen registration is NOT on AppEngClient: `InitScreens.init(InitScreens.MenuScreenRegistrar)`
    (see below).
- **Stayed overlay-side** (NeoForge-only client APIs; Phase 3 needs Fabric equivalents):
  reload-listener registration (PartRendererDispatcher.ID with a dependency on
  `VanillaClientListeners.BLOCK_ENTITY_RENDERER`, + "styles"/StyleManager — Fabric:
  IdentifiableResourceReloadListener), block state model codecs (RegisterBlockStateModels),
  standalone models (ModelEvent.RegisterStandalone + SimpleUnbakedStandaloneModel +
  StorageCellModels), `RegisterClientExtensionsEvent` (CableBusBlockClientExtensions),
  environment effect renderers (spatial storage sky/clouds/weather), PiP renderers, IMC messages
  (darkmodeeverywhere, framedblocks GLASS_STATE), the config screen extension point.
- GOTCHA: the overlay's `FMLClientSetupEvent` handler must not be named `clientSetup` — it would
  overload the inherited no-arg `clientSetup()` and break both `this::clientSetup` inference and
  `enqueueWork(this::clientSetup)` (Runnable/Supplier ambiguity). Named `onClientSetup`, enqueues
  `() -> clientSetup()`.

### Client packets (b)

- `AEClientboundPacketHandler` (shared): `register(RegisterClientPayloadHandlersEvent)` →
  `registerAll(Registrar)` with public nested `Registrar`
  (`<T extends ClientboundPacket> register(Type<T>, ClientPacketHandler<T>)`) and the existing
  `ClientPacketHandler` (payload, Minecraft, Player) made public. Handlers themselves were already
  loader-neutral except one call: `fluid.getFluidType().getSound(SoundActions.BUCKET_FILL)` →
  new seam op `FluidPlatform.getBucketFillSound(Fluid)` (nullable; vanilla bucket-sound fallback
  stays shared). `ClientboundPacket` itself stays contextless — no handler needed
  player()/enqueueWork beyond what the (Minecraft, Player) signature already carries.
- Overlay glue `appeng.neoforge.client.NeoForgeClientNetworkInit.init(RegisterClientPayloadHandlersEvent)`
  adapts (payload, context) → (payload, Minecraft.getInstance(), context.player()) — NeoForge client
  payload handlers already run on the main thread, as will Fabric's ClientPlayNetworking.

### Datagen (c)

- ENTIRE `appeng/datagen/**` tree under src/client (35 files) moved to
  `loader/neoforge/src/client/java` (packages unchanged). Datagen stays NeoForge-only until Phase 4.
  `runData` (clientData, sourceSets.client) and `generateModMetadata` verified still wired.
  Note: `appeng/datagen/providers/loot/RaidHeroGiftLootProvider` lives in src/MAIN and is
  neoforged-free — left shared.

### JEI / item-list integrations (d)

- Only 2 of the ~20 JEI files import neoforged — both moved to overlay (packages unchanged):
  `FluidIngredientConverter`, `FluidBlockRenderer` (both consume NeoForge `FluidStack`, the
  JEI-NeoForge fluid ingredient type). The rest of the JEI plugin (pure JEI API) stays shared;
  `JEIPlugin`/`TransformCategory` keep interim references to the moved files (Fabric JEI uses a
  different fluid ingredient type — these two need Fabric twins if/when JEI-Fabric is supported).
- `FluidBlockPictureInPictureRenderer` (itemlists, NeoForge `VertexConsumerWrapper`) moved to
  overlay; shared `FluidBlockRendering` keeps an interim reference (twin needed).

### Models/render wholesale moves (e) + misc (f)

All moved to `loader/neoforge/src/client/java`, packages UNCHANGED; shared files referencing them
compile because :neoforge merges both client trees (same interim pattern as AEParts→P2P parts).

- **Part-model/renderer event plumbing**: `RegisterPartModelsEvent` + `RegisterPartRendererEvent`
  (appeng.client.api.**, extend NeoForge Event/ParallelDispatchEvent — addon-facing surface kept
  verbatim, still in apiJar) moved to overlay. The shared firing sites were seamed through the NEW
  holder `appeng.client.ClientLoaderHooks` (overlay impl
  `appeng.neoforge.client.NeoForgeClientLoaderHooks`, injected first in the client ctor):
  `postRegisterPartModels(LateBoundIdMapper)` (PartModels ctor ← ModLoader.postEvent) and
  `collectPartRenderers(PartRendererCollector)` (PartRendererDispatcher reload ←
  ModLoader.dispatchParallelEvent; collector adds the modId so the seam interface has no overlay
  types). Fabric twins of the two event classes will be plain classes with the same FQN/methods,
  driven from a custom entrypoint.
- `InitScreens.init` now takes nested `MenuScreenRegistrar` (generic method mirroring
  `RegisterMenuScreensEvent.register`'s vanilla signature); overlay adapts with `e::register`
  (method refs CAN implement generic-method functional interfaces; lambdas can't).
  `InitScreensTest` updated the same way (tests may keep neoforged imports).

### Moved-file inventory

**Fabric twin needed** (same FQN, loader-duplicated class — shared code references them):
- `appeng.api.client.StorageCellModels` (API; NeoForge StandaloneModelKey in its surface — Fabric
  twin must offer registerModel/model/standaloneModels equivalents; referenced by shared
  AppEngClient.initCustomClientRegistries)
- `appeng.client.api.model.parts.RegisterPartModelsEvent`,
  `appeng.client.api.renderer.parts.RegisterPartRendererEvent` (addon registration surface)
- `appeng.client.gui.style.FluidBlitter` (referenced by shared FluidKeyRenderer; Fabric:
  FluidVariantRendering sprite/color)
- `appeng.client.hooks.BlockAttackHook` (Fabric: AttackBlockCallback)
- `appeng.client.hooks.RenderBlockOutlineHook` (Fabric: WorldRenderEvents block outline)
- `appeng.client.areaoverlay.AreaOverlayRenderer` (Fabric: WorldRenderEvents)
- `appeng.client.renderer.blockentity.CrankRenderer`, `MEChestRenderer` (referenced by shared
  registerBlockEntityRenderers; standalone-model/quad-transform usage needs Fabric rework)
- `appeng.client.renderer.spatialstorage.SpatialStorage{Sky,Clouds,WeatherEffects}Renderer`
  (Fabric: DimensionRenderingRegistry)
- Block-state models (Phase 3 model-system port): `CableBusModel`, `DriveModel`,
  `CraftingCubeModel`, `QuartzGlassModel`, `QnbFormedModel`, `SpatialPylonModel`,
  `PaintSplotchesModel`, `SingleSpinnableVariant`, and part models `PlanePartModel`,
  `P2PFrequencyPartModel` (both referenced by shared registerPartModelTypes), item model
  `MeteoriteCompassModel` (referenced by shared registerItemModels)
- Quad pipeline (needed for facades/cable rendering on Fabric, as a group):
  `appeng.client.render.CubeBuilder`, `appeng.client.render.cablebus.FacadeBuilder`,
  `appeng.thirdparty.codechicken.**` (7 files)
- `appeng.client.integrations.itemlists.FluidBlockPictureInPictureRenderer` (referenced by shared
  FluidBlockRendering)

**NeoForge-only (no Fabric twin planned)**:
- `appeng/datagen/**` under client (35 files; datagen stays NeoForge until Phase 4)
- `appeng.client.integrations.jei.FluidIngredientConverter`, `...jei.FluidBlockRenderer`
  (JEI-NeoForge fluid type; revisit only with JEI-Fabric support)
- `appeng.client.block.cablebus.CableBusBlockClientExtensions` (IClientBlockExtensions
  particle/sound hooks; Fabric likely handles via block methods — decide in Phase 3)
- `appeng.neoforge.client.{AppEngNeoForgeClient, NeoForgeClientLoaderHooks, NeoForgeClientNetworkInit}`
  (entrypoint glue; Phase 3 mirrors them as AppEngFabricClient etc.)

## API renames / mappings discovered (build layer)

- **Gradle wrapper bumped 9.2.1 → 9.5.1**: loom 1.17.x requires Gradle plugin API 9.5.0
  (1.16.x requires 9.4.0). Rebase note: upstream is still on 9.2.1.
- **`FAIL_ON_PROJECT_REPOS` → `PREFER_PROJECT`** in settings.gradle: loom injects project-level
  repos (LoomLocalRemappedMods) and cannot work under FAIL mode.
- **loom 1.17 has no `modImplementation`** (remapping configs removed with obfuscation):
  mods/loader/api are plain `implementation`. `minecraft "com.mojang:minecraft:<ver>"` stays;
  no `mappings` line (per spec). Confirmed against FabricMC/fabric-example-mod branch `26.1`.
- **Access widener namespace on unobfuscated MC is `official`**, not `named`
  (loom error: "Expected official namespace for access widener entry, found: named").

## Phase 2a — Fabric compile gate (`:fabric:compileJava` + `:fabric:build` GREEN, `:neoforge:build` GREEN)

Shared MAIN sources now compile against the vanilla (fabric-loom merged) jar. The bulk of the work was
"silent NeoForge patches": members that exist/are public only because NeoForge patches vanilla directly
(NOT via AE2's accesstransformer.cfg), which the shared sources silently relied on. Every instance found
in this step is documented below. Verified with javap against `fabric-loom/26.1.2/minecraft-merged.jar`
vs. the NeoForge `mergeWithSources` jar (Neo patches are `// Neo:`-commented inline) and the
`neoforge-26.1.2.21-beta-sources.jar` extension interfaces.

### Silent-patch table (vanilla member → Neo patch → resolution)

| Vanilla member | NeoForge patch | Resolution |
|---|---|---|
| `Slot#getContainerSlot()` | adds alias `getSlotIndex()` (identical body) | renamed all 14 shared uses to the vanilla `getContainerSlot()` |
| `ItemStack` (no such method) | adds `isComponentsPatchEmpty()` | vanilla equivalent inline: `stack.isEmpty() \|\| stack.getComponentsPatch().isEmpty()` (AEItemKey) |
| `ItemStack.isSameItemSameComponents(ItemStack, ItemStack)` | adds `(ItemStack, @Nullable ItemStackTemplate)` overload | private helper in InscriberTestPlots with Neo's null/empty semantics, comparing against `template.create()` |
| `ItemStackTemplate` ctors | adds `(Item, int, DataComponentPatch)` overload | vanilla `(Holder<Item>, int, patch)` ctor via `Item#builtInRegistryHolder()` |
| `RecipeManager.recipes` (private field) | adds `recipeMap()` accessor | seam `LoaderPlatform#getRecipeMap(RecipeManager)`; fabric impl reads the AW-widened field, Neo impl calls `recipeMap()` (11 call sites across 10 files) |
| `RecipeType` (no such method) | adds `simple(Identifier)` | inlined Neo's exact body (anonymous `RecipeType` with toString) in `AERecipeTypes#register`; vanilla `register(String)` would eagerly self-register and bypass AERegistries |
| `Ingredient` (no such methods) | adds `isCustom()`, `isSimple()`, `getValues()` | seams `LoaderPlatform#isCustomIngredient` (fabric: Fabric-API-injected `getCustomIngredient() != null`) and `#isSimpleIngredient` (fabric: `!requiresTesting()`); `getValues().stream()` → vanilla `items()` (equal for non-custom); HolderSet collection in TransformLogic → `HolderSet.direct(items().toList())` |
| `NonNullList` (no such method) | adds `copyOf(Collection)` | `NonNullList.create()` + addAll (FillCraftingGridFromRecipePacket) |
| `ContextMap` (no such field) | adds `EMPTY` constant | inlined vanilla equivalent `new ContextMap.Builder().create(new ContextKeySet.Builder().build())` (CraftingRecipeUtil) |
| `CustomData` (no such method) | adds `contains(String)` | vanilla `copyTag().contains(...)` (MissingContentItem, error path only) |
| `TooltipFlag` (no such method) | adds `hasShiftDown()` | seam `LoaderPlatform#tooltipHasShiftDown`; fabric impl reads a client-injected `BooleanSupplier` (`FabricLoaderPlatform.setShiftDownSupplier`, **TODO Phase 3**: wire `Screen::hasShiftDown`), false until then |
| `CreativeModeTab.builder(Row, int)` | adds no-arg `builder()` == `new Builder(Row.TOP, 0)` | call the vanilla overload with `(Row.TOP, 0)` (Main/Facade tabs) |
| `CreativeModeTab.Builder` (no such methods) | adds `withTabsBefore/After` (tab ordering is a Neo feature) | seam `LoaderPlatform#orderCreativeTabAfter`; fabric impl is a documented no-op (vanilla sorts by registration order, MAIN registers before FACADES) |
| `EntityType.Builder.clientTrackingRange/updateInterval` | adds `setTrackingRange/setUpdateInterval/setShouldReceiveVelocityUpdates` (supplier-backed, same units/effect) | vanilla `clientTrackingRange(16).updateInterval(4)`; the velocity-updates call dropped — vanilla `trackDeltas()` defaults to true for everything but a fixed exclusion list (TinyTNT not in it) |
| `BlockEntityType(BlockEntitySupplier, Set<Block>)` is **private**, `BlockEntitySupplier` package-private | makes both public + adds varargs ctors | AW `accessible` for ctor + inner interface; shared code uses `Set.<Block>of(blocks)` with the (widened) vanilla ctor |
| `SimpleParticleType(boolean)` protected | public | AW `accessible method <init> (Z)V` (appeng.core.particles.ParticleTypes) |
| `CauldronInteraction.Dispatcher#put` package-private | public | AW (InitCauldronInteraction) |
| `ItemStackLinkedSet.TYPE_AND_TAG` private | public | AW (MemoryCardItem) |
| `Player#closeContainer()` protected | public | AW (ConversionMonitorPart; the `ServerPlayer` override is already public in vanilla) |
| `PrimedTnt#explode()` private | protected | AW `extendable` (TinyTNTPrimedEntity's override) |
| `TntBlock.prime(Level, BlockPos, LivingEntity)` private (Neo deprecates prime in favor of onCaughtFire) | n/a | AW `accessible`; used by the fabric `onCaughtFire` seam impl to replicate Neo's TNT ignition incl. igniter |
| `BlockEntity` (no such methods) | injects `invalidateCapabilities()`, `onChunkUnloaded()` (via IBlockEntityExtension) | `invalidateCapabilities` → new seam op `AEApiLookups#invalidateApis(BlockEntity)` (Neo: delegate; fabric: documented no-op — lookups are uncached) at all 5 call sites. `onChunkUnloaded` → declared on the **fabric twin** of `AEBaseBlockEntityHooks` (no-op, matching Neo's default); the Neo twin needs no declaration since subclass overrides target the injected hook. **TODO Phase 2b**: invoke from `ServerChunkEvents.CHUNK_UNLOAD` |
| `RecipeManager` send: `ServerGamePacketListenerImpl#send(CustomPacketPayload)` | Neo makes payloads directly sendable | 3 call sites (`CraftingCpuLogic`, `AEBaseMenu#sendPacketToClient`, `RequestClosestMeteoritePacket`) now use the existing `NetworkAdapter#sendToPlayer` seam (Neo impl = PacketDistributor.sendToPlayer, identical wire behavior) |
| `Block#getExplosionResistance()` (context-free) | adds contextual `(BlockState, BlockGetter, BlockPos, Explosion)` overload | seam `LoaderPlatform#getExplosionResistance`; fabric impl uses the context-free value (no AE2 block overrides the contextual one) — TinyTNTPrimedEntity |
| `BlockStateBase#getSoundType()` (context-free) | adds `(LevelReader, BlockPos, Entity)` overload | seam `LoaderPlatform#getSoundType`; fabric impl context-free (no AE2 override) — PartPlacement |
| `ItemStack#getBurnTime` (no such method) | adds `getBurnTime(RecipeType, FuelValues)` incl. item overrides + event | seam `LoaderPlatform#getBurnTime(ItemStack, FuelValues)`; fabric impl `FuelValues#burnDuration` (Fabric's FuelRegistryEvents feed FuelValues directly) — VibrationChamber, RestrictedInputSlot |
| `Block#onCaughtFire` (no such method) | injected via IBlockExtension (TntBlock overrides = prime-with-igniter) | seam `LoaderPlatform#onCaughtFire(Block, BlockState, ...)` (EntropyManipulatorItem); fabric impl dispatches to the `BlockCaughtFireHook` shim, then TntBlock via AW'd `prime`, else Neo's default `true` |
| `SavedDataType` record `(Identifier, Supplier, Codec, DataFixTypes)` | rewritten: level-sensitive `Factory<T>`/`Factory<Codec<T>>` + nullable dataFixType | new shared `appeng.util.AESavedDataType` + seam `LoaderPlatform#computeSavedDataIfAbsent(ServerLevel, type)`. Neo impl: memoized translation to Neo's Factory ctor (null dataFixType — exactly the pre-seam code). Fabric impl: per-(level, id) memoized vanilla `SavedDataType` with the level captured in the closures; **dataFixType = `SAVED_DATA_COMMAND_STORAGE`** since vanilla NPEs on null when a data file exists (**TODO Phase 2b**: verify loading pre-existing data; DFU is a no-op while the file's DataVersion is current). Converted: PlayerRegistryInternal, ChunkLoadState, CompassRegion, SpatialStoragePlotManager (the last also fixes a latent fabric-runtime NPE from its former `null` dataFixType) |
| `RenderPipeline` (no such method) | adds `toBuilder()` | `RenderPipelines.LINES.toBuilder()` → `RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)` — verified vanilla `LINES` is exactly `builder(LINES_SNIPPET).withLocation(...)` (AERenderPipelines) |
| `WitherSkull` package | (no patch — AE2-side error) | lives in `net.minecraft.world.entity.projectile.hurtingprojectile` on both loaders (EntityDestroyHook import) |

### NeoForge extension-method overrides → `appeng.hooks.extensions` shims

Shared classes override NeoForge-injected virtual methods. New shared shim interfaces declare the exact
Neo signatures with default bodies replicating Neo's defaults (see `appeng.hooks.extensions.package-info`):

| Shim | Neo origin (default) | Implementors |
|---|---|---|
| `ItemUseFirstHook` | `IItemExtension#onItemUseFirst` (PASS) | AbstractPortableCell, NetworkToolItem, FacadeItem, UpgradeCardItem (its `super.onItemUseFirst` → `ItemUseFirstHook.super`, same PASS), BasicStorageCell, EncodedPatternItem, EraserItem, MeteoritePlacerItem, DebugCardItem, ReplicatorCardItem |
| `ReequipAnimationHook` | `shouldCauseReequipAnimation` (`oldStack != newStack`) | AEBasePoweredItem |
| `SneakBypassUseHook` | `doesSneakBypassUse` (false) | MemoryCardItem |
| `BlockCaughtFireHook` | `IBlockExtension#onCaughtFire` (true) | TinyTNTBlock |
| `BlockExplodedHook` | `onBlockExploded` (setBlock AIR + wasExploded) | MatrixFrameBlock |
| `EntityDestroyHook` | `canEntityDestroy` (dragon/wither logic, replicated) | MatrixFrameBlock |
| `LadderHook` | `isLadder` (CLIMBABLE tag) | CableBusBlock |
| `RedstoneConnectHook` | `canConnectRedstone` (wire/repeater/observer logic, replicated) | CableBusBlock |
| `CloneItemStackHook` | player-aware `getCloneItemStack` (delegates to vanilla overload) | CableBusBlock |
| `NeighborChangeHook` | `onNeighborChange` (no-op) | CableBusBlock |

Java guarantees zero Neo behavior change: each implementor overrides the method, and a same-signature
override simultaneously overrides Neo's injected method (the unrelated-defaults conflict rule would even
force the override if it were missing).

**TODO Phase 2b — fabric dispatch wiring for the shims** (nothing calls them on Fabric yet):
- `ItemUseFirstHook`: UseBlockCallback (fire BEFORE block use, mirror `ServerPlayerGameMode#useItemOn` order).
- `ReequipAnimationHook`: no Fabric hook — mixin into `ItemInHandRenderer`/equip-animation check (client).
- `SneakBypassUseHook`: no Fabric hook — mixin into the sneak-bypass check in `BlockBehaviour`/interaction logic.
- `BlockCaughtFireHook`: covered for AE2's own call site via `LoaderPlatform#onCaughtFire`; fire-spread/flint&steel
  ignition of TinyTNT needs FireBlock/Flint&Steel mixins or stays vanilla (TinyTNT not flammable by default — verify).
- `BlockExplodedHook`/`EntityDestroyHook`: explosion + dragon/wither destruction mixins (MatrixFrame immunity).
- `LadderHook`/`RedstoneConnectHook`/`NeighborChangeHook`: livings-climbing / redstone-connection / comparator-style
  neighbor notifications — mixins; cable bus parts lose these behaviors until wired.
- `CloneItemStackHook`: override vanilla 4-arg `getCloneItemStack` on CableBusBlock for fabric pick-block (loses
  player-ray part picking until then) or mixin the pick-block path.

### Fabric layer classes finished in this step

- `FabricConfigStore`: the 3 `Function.identity()` serializer args broke diamond inference against
  `Function<T, Object>` (identity is `Function<T,T>`, invariant) → `value -> value` lambdas.
- `FabricRegistrar`: `ResourceKey#location()` does not exist in 26.1 — it is `identifier()`.
- `InternalInventoryStorage`: implemented the abstract `Storage#iterator()` (fabric-api 8.0.5's
  `SlottedStorage` does NOT default it): index-based iterator over `getSlot(i)` in slot order.
- `FabricLoaderPlatform`: + isCustomIngredient, isSimpleIngredient, getRecipeMap (AW'd field),
  getBurnTime, onCaughtFire, getExplosionResistance, getSoundType, tooltipHasShiftDown (client-injected
  supplier), orderCreativeTabAfter (no-op), computeSavedDataIfAbsent (per-level memoized vanilla types).
- `FabricApiLookups`: + `invalidateApis` (documented no-op).
- `AEBaseBlockEntityHooks` (fabric twin): + `onChunkUnloaded()` no-op hook (Phase 2b wires chunk-unload events).
- fabric build.gradle: javadoc now mirrors :neoforge (public-API include patterns + `Xdoclint:none`;
  upstream javadoc is not doclint-clean); `sourcesJar` gets `DuplicatesStrategy.EXCLUDE` (loom adds the
  main sources on top of `withSourcesJar()`'s configuration — identical duplicates).

### Incident note (recovered)

A botched in-place edit during this step truncated 8 shared files; they were restored from git HEAD,
which **reverted their uncommitted Phase 1 decoupling**. Re-decoupled from the PORTING_NOTES specs (and
re-verified against the seams): `TestPlots` (scan via TestPlotPlatform; SkyStoneTank assertions via the
loader-neutral `getStoredFluid()/getStoredAmount()` instead of Neo's `getFluidHandler()`),
`TransformLogic` (dead `@SubscribeEvent`s → `onServerStarted()`/`createCacheInvalidationReloadListener()`
+ id constant, still deliberately unwired; `OrHolderSet` → private `unionOf` via `HolderSet.direct`),
`MatterCannonItem` (`EventHooks.onBlockPlace`/`BreakBlockEvent` → `LoaderEventHooks.isBlockPlaceCanceled`/
`isBlockBreakCanceled`). The other 5 files only carried Phase 2a recipeMap edits, which were reapplied.
If any subtle Phase 1 edit in these 3 files is missed, `:neoforge:build` (451 tests, green) is the guard.

### Remaining Phase 2b (runtime gate) TODO list

(STATUS: all items below were addressed in "Phase 2b (part 1)" — see that section for outcomes.)

- Wire `AEBaseBlockEntityHooks#onChunkUnloaded` from `ServerChunkEvents.CHUNK_UNLOAD`.
- Wire the `appeng.hooks.extensions` shims (see per-hook list above).
- `FabricLoaderPlatform#computeSavedDataIfAbsent`: validate `SAVED_DATA_COMMAND_STORAGE` DFU behavior on
  pre-existing saves.
- `FabricLoaderPlatform#tooltipHasShiftDown`: Phase 3 client entrypoint must call `setShiftDownSupplier`.
- night-config TOML must be shipped (jar-in-jar `include` or shade) before runtime.
- TinyTNT flammability/ignition parity check (fire spread does not call the hook on fabric yet).

## Phase 2b (part 1) — Fabric dedicated-server boot (gate M2 part 1 REACHED)

`:fabric:runServer` boots to `Done (3.174s)!` on a fresh world (creates overworld/nether/end +
`ae2:spatial_storage`, writes `ae2-client.toml`/`ae2-common.toml` via night-config) and to
`Done (0.195s)!` on the reload of that world. No crash during mod init; payload types + server
receivers registered by `FabricNetworkInit` without error. Boot loop: pre-created
`loader/fabric/run/eula.txt`, `perl -e 'alarm 240; exec @ARGV' ./gradlew :fabric:runServer`
(macOS has no `timeout`); the server run config got `programArgs 'nogui'`.
`:fabric:build` GREEN, `:neoforge:build` GREEN (451 tests, 1 skipped — unchanged),
`grep -rl "net.neoforged" src/main/java | wc -l` → 0.

### Shim dispatch wiring (the Phase 2a TODO list)

All call-site research was done against the NeoForge-patched merged sources
(`~/.gradle/caches/neoformruntime/.../mergeWithSources_*.jar`, `// Neo:`-commented) vs the vanilla
decompile (`decompile_*.jar`), and every bytecode injection point was verified with `javap -c`
against the fabric-loom `minecraft-merged.jar`. New mixins live in `appeng.fabric.mixins`
(loader/fabric), registered via the NEW `ae2-fabric.mixins.json` in `fabric.mod.json`
(shared `ae2.mixins.json` untouched).

| Hook | Neo call site | Fabric wiring |
|---|---|---|
| `onChunkUnloaded` (AEBaseBlockEntityHooks) | `LevelChunk#clearAllBlockEntities` (runs after ChunkEvent.Unload, see `ChunkMap#scheduleUnload`) | third `ServerChunkEvents.CHUNK_UNLOAD` handler in AppEngFabric (after the TickHandler + ChunkLogger forwarders = Neo order), iterating `chunk.getBlockEntities().values()` |
| `ItemUseFirstHook` | `ServerPlayerGameMode#useItemOn`: `itemStack.onItemUseFirst(context)` after the RightClickBlock event, before block use; never for spectators | second `UseBlockCallback` in AppEngFabric (after WrenchHook = the RightClickBlock wrapper; Fabric fires same-event listeners in registration order); spectator-guarded; non-PASS result cancels the use chain — same short-circuit |
| `SneakBypassUseHook` | `useItemOn`: `suppressUsingBlock = (secondary && haveItems) && !(main.doesSneakBypassUse && off.doesSneakBypassUse)`; Neo's ItemStack overload returns TRUE for empty stacks | `SneakBypassUseMixin`: @Redirect of the single `ServerPlayer.isSecondaryUseActive()Z` call — `(a && !c) && b == (a && b) && !c`. Client twin (MultiPlayerGameMode) is Phase 3 |
| `BlockExplodedHook` | `BlockBehaviour#onExplosionHit`: trailing `setBlock(AIR)+wasExploded` pair replaced by `state.onBlockExploded(...)` (default = that pair) | `BlockExplodedMixin`: cancellable @Inject at the `ServerLevel.setBlock` INVOKE (drops already handled = Neo order), calls the hook + cancels for implementors |
| `EntityDestroyHook` (dragon) | `EnderDragon#checkWalls`: `CommonHooks.canEntityDestroy(...)` gates `removeBlock` | `EnderDragonEntityDestroyMixin`: @Redirect of `ServerLevel.removeBlock` — veto returns false. Nuance: a vetoed block does not set Neo's `hitWall=true` (no collision response); fine for the matrix frame |
| `EntityDestroyHook` (wither) | `WitherBoss#customServerAiStep`: `state.canEntityDestroy(...)` gates `destroyBlock` | `WitherBossEntityDestroyMixin`: @Redirect of `ServerLevel.destroyBlock` |
| `EntityDestroyHook` (wither skull / mob goals) | `WitherSkull#getBlockExplosionResistance` caps resistance at 0.8 unless vetoed; BreakDoorGoal/RemoveBlockGoal | NOT wired: the skull's explosion still cannot destroy a matrix frame because `BlockExplodedMixin` makes it explosion-immune regardless of the resistance cap; the mob goals never target AE2 blocks |
| `LadderHook` | `LivingEntity#onClimbable`: CLIMBABLE-tag check replaced by `state.isLadder(...)` (CommonHooks.isLivingOnLadder) | `LivingEntityLadderMixin`: cancellable HEAD @Inject replicating the vanilla pre-checks (spectator, glide-through) then substituting the hook verdict both ways (@Shadow `lastClimbablePos`; mixin extends Entity for the inherited members) |
| `NeighborChangeHook` | `Level#updateNeighbourForOutputSignal`: Neo calls `state.onNeighborChange` on every direct neighbor, horizontal then vertical | `LevelNeighborChangeMixin`: HEAD @Inject doing the same 6-direction loop for implementors before the vanilla comparator updates |
| `CloneItemStackHook` | `ServerGamePacketListenerImpl#handlePickItemFromBlock`: player-aware `getCloneItemStack(pos, level, includeData, player)` | `PickBlockCloneItemStackMixin`: @Redirect of the vanilla 3-arg `BlockState.getCloneItemStack`, passing `this.player` (public field) to the hook. Client pick-block path is Phase 3 |
| `RedstoneConnectHook` | — | NOT wired, deliberately: `canConnectRedstone` has ZERO call sites in NeoForge 26.1's patched vanilla AND in neoforge's own sources (only the extension declarations) — the hook is vestigial on 26.1; wiring it on Fabric would create NEW behavior NeoForge doesn't have. Revisit if Neo re-adds the RedStoneWireBlock patch |
| `BlockCaughtFireHook` | `FireBlock#checkBurnOut` calls `state.onCaughtFire` for burned-out blocks | NOT wired, parity holds: fire spread only burns blocks with registered burn odds; AE2 registers no flammability anywhere (verified), so TinyTNT never burns on EITHER loader. Vanilla's TntBlock special case doesn't apply (TinyTNTBlock is not a TntBlock); flint&steel/fire charge ignition is TinyTNTBlock's own `useItemOn`; AE2's entropy-manipulator call site goes through `LoaderPlatform#onCaughtFire` (Phase 2a) |
| `ReequipAnimationHook` | client-only (ItemInHandRenderer) | Phase 3 |

### Boot incident log (symptom → root cause → fix)

1. **Mixin apply error: `AnvilMenuMixin` target `createResultInternal` not found** → NeoForge renames
   the vanilla `createResult()` body to `createResultInternal()` (`createResult()` becomes a wrapper
   adding an event hook; the wrapper contains no `isDamageableItem` call) → shared mixin now lists
   BOTH method names (`method = {"createResultInternal", "createResult"}`); on each loader exactly one
   name resolves to the real body and the other contributes nothing, so Neo behavior is unchanged
   (require=1 satisfied by the single expression match).
2. **`IllegalStateException: Registry entry ae2:flawless_budding_quartz ... not registered yet` from
   `FabricRegistrar.registerAll`** → FabricRegistrar flushed registries in FIRST-SEEN collector order;
   AEParts/AEItems class-init touches ITEM before AEBlocks collects BLOCK, so block-item factories
   resolved unbound block entries. NeoForge never sees this because `RegisterEvent` fires in
   `GameData#getRegistrationOrder` order → FabricRegistrar now sorts the collected registry keys the
   same way: ATTRIBUTE, DATA_COMPONENT_TYPE, PARTICLE_TYPE pinned first, then root-registry raw-id
   order (= vanilla bootstrap order; BLOCK < ITEM), modded registries (raw ids past vanilla) last.
3. **`:fabric:processResources` configuration-cache failure** (`rootProject` referenced from a closure
   at execution time) → hoist `rootProject.file(...).toPath()` into a local before `filesMatching`.

### Pre-empted (fixed before they could crash the boot)

- **`data/ae2/worldgen/biome/spatial_storage.json` carries `"attributes"` with
  `neoforge:custom_clouds/custom_skybox/custom_weather_effects`** — these are NeoForge-REGISTERED
  vanilla `EnvironmentAttribute`s (26.1's attribute system replaced Neo's custom sky render events);
  vanilla's `EnvironmentAttributeMap` codec (`Codec.dispatchedMap` over the attribute registry)
  hard-fails on unknown keys, and worldgen registry load errors are FATAL at server start.
  Fix: `loader/fabric/src/main/resources` ships the same biome minus `"attributes"`, and
  :fabric:processResources excludes the generated copy (Phase 3: spatial sky via Fabric's
  `DimensionRenderingRegistry`, code-driven, no biome attributes needed). DATAGEN-PARITY note:
  needs per-loader emission.
- **night-config jar-in-jar**: `include "com.electronwill.night-config:{core,toml}"` (include pulls no
  transitives — core listed explicitly). Verified nested under `META-INF/jars/` + `jars` entries in
  the built fabric.mod.json.

### Non-fatal datapack errors (documented, for the datagen-parity step)

- **67 matter-cannon ammo recipes** (`data/ae2/recipe/matter_cannon/nuggets/*.json`) log
  `Couldn't parse data file ... Missing tag: 'c:nuggets/...'`. NOT the `neoforge:conditions` key
  (unknown top-level keys are ignored by the map codec): on NeoForge these recipes are
  condition-disabled when the `c:nuggets/*` tag is empty; on Fabric the condition is ignored, the
  ingredient parses eagerly and fails because the tag doesn't exist. Vanilla 26.1's error-capturing
  data pipeline logs and skips them — when another mod provides the tag, the recipe loads and works,
  which matches the Neo behavior exactly, just noisier. Proper fix in datagen parity: also emit
  `fabric:load_conditions`.
- **5 cable "clean" recipes** (`data/ae2/recipe/network/cables/*_fluix_clean.json`) fail on the
  `neoforge:difference` custom ingredient (`No key fabric:type`). Fabric has an equivalent
  (`fabric:difference` via fabric-recipe-api custom ingredients); until datagen parity these 5
  crafting-based cable-cleaning recipes are missing on Fabric (the water-cauldron path is unaffected).
- `Failed to load properties from file: server.properties` on FIRST boot only — vanilla logs this
  before writing the initial file; benign.
- WARN `duplicate API provider registration for block: ae2:condenser` — expected: `initCondenser()`
  registers the condenser-specific `ItemStorage` provider, then the generic
  `AEBaseInvBlockEntity` loop hits the same BE type; Fabric keeps the FIRST registration, which is the
  specific one — same effective priority as NeoForge's specific-before-generic provider order.

### Validated Phase 2a follow-ups

- `computeSavedDataIfAbsent` / `SAVED_DATA_COMMAND_STORAGE`: second boot loads the existing world
  (incl. `ae2:spatial_storage` dimension dir) cleanly. AE2 SavedData files are created lazily and none
  existed yet on the reload — the DFU path gets full coverage once gametests/players touch
  PlayerRegistry/CompassRegion data; current DataVersion files are a no-op fix anyway.
- Fabric gametests stay UNWIRED (placeholder `AEFabricGameTests` documented: Fabric's gametest API on
  26.1 has no dynamic-registration hook for `GameTestPlotAdapter.registerAll`); no
  `fabric-gametest` entrypoint added. (SUPERSEDED in part 2 below: registry-injection mixin found.)
- Dist arrangement: `AppEngFabric.onInitialize` constructs `AppEngServer` only under
  `EnvType.SERVER`; client construction stays with the Phase 3 client entrypoint (no
  double-construction possible).

### Remaining for Phase 2b part 2 / Phase 3

- SkyStoneBreakSpeed (needs a small `Player#getDestroySpeed` mixin; documented TODO in AppEngFabric).
- Server-synced recipe push (`OnDatapackSyncEvent#sendRecipes` equivalent) — runtime/networking step.
- Sneak-bypass + pick-block client twins (`MultiPlayerGameMode`), `ReequipAnimationHook`,
  client chunk-unload forwarding for `onChunkUnloaded` — Phase 3.
- Datagen parity: fabric load conditions + `fabric:difference` ingredient + per-loader biome json.

## Phase 2b (part 2) — Fabric gametests (gate M2 COMPLETE)

`:fabric:runGametest` runs the full plot-based suite headlessly: **68/68 tests pass** (67 shared AE2
plots + vanilla `minecraft:always_pass`). NeoForge baseline `:neoforge:runGametest`: **69/69 pass**
(68 AE2 plots + `always_pass`); the delta of exactly one test is `ae2:interface_slot_filtering` from
the NeoForge-only `InterfaceCapabilityTestPlots` (capability-overlay plot split loader-side in the
Phase 2a step; a Fabric twin is a follow-up, not part of this gate). `:fabric:build` GREEN,
`:neoforge:build` GREEN (incl. the 451 unit tests), `grep -rl "net.neoforged" src/main/java | wc -l` → 0.

### Registration mechanism found (the "no dynamic-registration hook" question resolved)

On 26.1 game tests are entries of the **data-driven `minecraft:test_instance` registry**
(`RegistryDataLoader#WORLDGEN_REGISTRIES`); NeoForge's `RegisterGameTestsEvent#registerTest` injects
dynamic `GameTestInstance`s into that registry during datapack registry load. fabric-api's gametest
module (`fabric-gametest-api-v1` 4.0.17) does the same for its `@GameTest`-annotated `fabric-gametest`
entrypoint methods — via its own `RegistryDataLoaderMixin` that registers extra entries into the
in-flight `WritableRegistry` before it is frozen. That entrypoint path is useless for AE2 (plots are
generated `GameTestInstance`s, not annotated methods), but the registry-injection technique is exactly
the NeoForge event's mechanics, so AE2 mirrors it one level lower:

- NEW `appeng.fabric.mixins.GameTestRegistryLoadTaskMixin` — `@Inject` at HEAD of vanilla
  `RegistryLoadTask#freezeRegistry`; when the task's registry is `Registries.TEST_INSTANCE` (and
  `appeng.tests=true`), `GameTestPlotAdapter.registerAll` registers every plot adapter into the
  `@Shadow`'d private `WritableRegistry` right before the freeze. `test_instance` is only listed in
  `WORLDGEN_REGISTRIES` (not `SYNCHRONIZED_REGISTRIES`), so this never fires for the network-received
  registry path — no flag dance needed (fabric-api's mixin needs one because it targets the shared
  `load` lambda; `freezeRegistry` is per-task and self-identifying).
- The `ae2:plot_adapter` `TEST_INSTANCE_TYPE` codec was already registered by `AppEngFabric` (Phase 2a),
  and the shared `tests.TestInstanceBlockEntityMixin`/`tests.TestCommandMixin` (structure synthesis +
  placement for plots) apply on both loaders. The placeholder `AEFabricGameTests` class was deleted.

### Run configuration

- loom run config `gametest` in `loader/fabric/build.gradle` (`server()`, runDir `build/gametest`,
  `-Dfabric-api.gametest=true`) → gradle task `:fabric:runGametest`.
- Mechanics: fabric-api's `MainMixin` hijacks the dedicated-server `Main.main` when
  `fabric-api.gametest` is set and spins a vanilla `GameTestServer` (auto-agrees EULA, superflat,
  exits non-zero on failures), which runs every non-`manualOnly` entry of the `test_instance`
  registry. Optional properties: `fabric-api.gametest.filter` (resource selector, e.g. `ae2:subnet` —
  invaluable for debugging single tests), `.report-file` (JUnit XML), `.verify`.
- `appeng.tests=true` comes from the shared `configureEach` block, mirroring :neoforge.

### Transfer/lookup-layer bug found & fixed (what this gate exists for)

`ae2:subnet` and `ae2:multi_storage_bus` failed under full-suite load ("inserted != 1: 0",
"Network storage does not contain minecraft:red_concrete. Available keys: []") but PASSED when run
alone with the filter — a load-dependent ordering race, root cause **missing capability invalidation**:

- Both plots point a storage bus at an AE2 interface (part or block). Under load, the bus's first
  device tick runs before the interface's grid is online → `updateTarget` finds no `ME_STORAGE` and no
  external storage → delegate `NullInventory` → `sleepDevice`. On NeoForge the interface's
  `InterfaceLogic#gridChanged → notifyNeighbors → invalidateCapabilities()` then triggers the bus's
  registered invalidation listener → `scheduleUpdate` → recovery. On Fabric both seam methods
  (`AEApiLookups#registerInvalidationListener`/`#invalidateApis`) were documented no-ops → the bus
  slept forever.
- FIX in `FabricApiLookups`: an own per-`ServerLevel`, per-position invalidation-listener registry
  (weakly-held listeners per the NeoForge contract; level keyed weakly). `invalidateApis(blockEntity)`
  — called from AE2's 5 shared `invalidateCapabilities()` call sites (InterfaceLogic.notifyNeighbors,
  CableBusContainer part changes, orientation changes, Inscriber, MEChest) — now notifies the
  listeners at that position. The `createCache(level, pos, side, isValid, listener)` overload also
  participates: it registers a wrapper (unregister-when-`isValid`-fails, mirroring NeoForge's
  `BlockCapabilityCache`) which the returned cache holds strongly.
- Remaining documented delta vs NeoForge: placing/removing non-AE2 blocks produces no invalidation
  (NeoForge auto-invalidates on any block change); those cases are covered by vanilla neighbor
  updates (`onNeighborChanged` → `alertDevice` → re-query), and Fabric's `BlockApiCache` re-validates
  on every query, so re-querying consumers always observe changes.

After the fix: two consecutive full-suite runs 68/68 (no flakiness), and the storage/network/automation
core all passes — ME network formation (`ChannelTests`, `QnbTestPlots`), import/export/storage buses
and subnets (`SubnetPlots`, `TestPlots` bus plots, `AnnihilationPlaneTests`), P2P
(`P2PTestPlots`, `ItemP2PTestPlots`), autocrafting/pattern providers, interfaces, inscriber, spatial.

### Non-blockers / deferred

- The 67 `c:nuggets/*` matter-cannon recipe load errors + 5 `neoforge:difference` cable-clean recipe
  errors still log during the run (known datagen-parity issue, Phase 2b part 1). **No gametest depends
  on those recipes** — nothing was skipped because of them; noise only.
- `ae2:interface_slot_filtering` (NeoForge-only plot) — port alongside the Fabric capability-overlay
  work; tracked by the `FabricTestPlotPlatform` TODO.
- Flaky-looking `[unregistered]` shown as the batch environment name in the log is cosmetic: plot
  adapters use `Holder.direct(TestEnvironmentDefinition.AllOf())` (same on NeoForge).

## Phase 3a — Fabric client COMPILE gate (`:fabric:build` GREEN with shared client sources)

Gates re-verified at the end of this step: `:fabric:build` GREEN (shared src/client compiled in),
`:neoforge:build` GREEN (451 tests, 1 skipped — unchanged), `:fabric:runGametest` 68/68,
`:fabric:runServer` boots to `Done (0.212s)!` (client classes in the single jar do not leak onto the
server path — vanilla only loads them on the client). Grep invariants: `net.neoforged` in src/main +
src/client = 0/0; zero `net.neoforged` anywhere under loader/fabric.

### Build setup

- `loader/fabric/build.gradle`: the `ae2.fabric.client` block now adds root `src/client/java` +
  `src/client/resources` + the NEW fabric client overlay dir `loader/fabric/src/client/java` into the
  MAIN source set (mirrors :neoforge's single-jar layout; loom's splitEnvironmentSourceSets deliberately
  NOT used — the known main-source client leak makes a strict split impossible, and the dedicated-server
  boot remains the leak gate). `ae2.fabric.client=true` is now the default in gradle.properties.
- Client-dir exclusions: `appeng/client/integration/{rei,emi}/**` (mirrors :neoforge, TODO 1.21.11),
  plus fabric-only gates until the Phase 4 integrations step: `appeng/client/integrations/jei/**`,
  `appeng/client/api/integrations/jei/**` (compile against the JEI API, not wired in :fabric) and
  `appeng/client/api/integrations/emi/**` (EMI API, :neoforge wires emi-neoforge:api clientCompileOnly).
  `appeng/client/guidebook/**` is NOT excluded — it compiles against guideme-fabric as-is
  (RecipeTypeContributions already called the surviving `renderFluid(Fluid)` overload; the removed
  `renderFluid(FluidStack)` overload had no shared callers). datagen was already moved to the :neoforge
  overlay in step 14 — nothing to exclude.

### AT/AW additions (shared loader-neutral code now uses widened vanilla members)

NEW AT entries (mirrored 1:1 in ae2.accesswidener, GuideME precedent):
`GuiGraphicsExtractor.scissorStack` + `GuiGraphicsExtractor$ScissorStack` class +
`GuiGraphicsExtractor.guiRenderState` (replace the Neo `peekScissorStack()`/
`submitGuiElementRenderState()`/`submitPictureInPictureRenderState()` patches),
`RenderPipelines.GUI_TEXTURED_SNIPPET` (replaces `RenderPipeline#toBuilder()` in Blitter).
AW-only entries (Neo patches vanilla access directly, no AT needed): `Screen.renderables`
(private→accessible; AEBaseScreen/PatternAccessTermScreen), `ParticleResources$SpriteParticleRegistration`
(package-private interface in the AppEngClient.SpriteSetRegistrar signature),
`KeyMapping$Category.SORT_ORDER` (category registration, see lifecycle table).

### Silent-patch table additions (client sweep; same format as Phase 2a)

| Vanilla member | NeoForge patch | Resolution |
|---|---|---|
| `Slot#getContainerSlot()` | `getSlotIndex()` alias | renamed 5 remaining client uses (AEBaseScreen ×3, PatternAccessTermScreen, InterfaceScreen) |
| `GuiGraphicsExtractor` (private members) | adds `peekScissorStack()`/`submitGuiElementRenderState()`/`submitPictureInPictureRenderState()` | AT+AW widened `scissorStack.peek()` / `guiRenderState.addGuiElement/addPicturesInPictureState` (Blitter, FluidBlockRendering) |
| `RenderPipelines.GUI_TEXTURED` | adds `toBuilder()` | `RenderPipeline.builder(GUI_TEXTURED_SNIPPET)` — vanilla GUI_TEXTURED is exactly builder(snippet)+location (AT+AW for the private snippet) |
| `setTooltipForNextFrame(Font,List,Optional,int,int)` | adds ItemStack-carrying overload (feeds tooltip events) | seam op `ClientLoaderHooks#setTooltipForNextFrame`; Neo impl keeps the stack overload, fabric impl uses the vanilla one (MEStorageScreen) |
| `KeyMapping` (no such method) | adds `getKey()` | seam op `ClientLoaderHooks#getBoundKey` (Neo: `getKey()`; fabric: `KeyMappingHelper.getBoundKeyOf`) — AppEngClient.onKeyInput |
| `FluidModel` (record) | adds `fluidTintSource()` (FluidStack-aware) + Neo `FluidType` | seam op `ClientLoaderHooks#getFluidRenderInfo(AEFluidKey)` → (sprite, tint, lighterThanAir); fabric impl: vanilla `tintSource().color(legacyBlock)` + `FluidVariantAttributes.isLighterThanAir` (FluidKeyRenderer, SkyStoneTankRenderer, fabric FluidBlitter twin) |
| `EntityRenderState` (no such field) | adds `partialTick` | TinyTNTPrimedRenderer now folds partial ticks into `fuseRemainingInTicks` at extract time, exactly like vanilla TntRenderer (`getFuse() - partialTicks + 1`); flash cadence cast to int like vanilla |
| `BlockEntityRenderer` (no such method) | injects `getRenderBoundingBox(T)` (default `new AABB(pos)`) | NEW client shim `appeng.client.hooks.extensions.RenderBoundingBoxHook` (appeng.hooks.extensions precedent); SkyStoneChestRenderer implements it. NOT dispatched on fabric yet (3b) |
| `BlockStateBase` (no such method) | adds `isEmpty()` (AIR/CAVE_AIR/VOID_AIR) | `isAir()` — equivalent for the air default-states FacadeItemModel checks |
| `UnbakedModel` | Neo makes it extend `ResolvableModel` | BasicUnbakedModel (dead code) now extends `UnbakedModel, ResolvableModel` explicitly |
| `Minecraft#hasShiftDown()` | (vanilla! Screen#hasShiftDown no longer exists in 26.1) | `FabricLoaderPlatform.setShiftDownSupplier(() -> Minecraft.getInstance().hasShiftDown())` — resolves the Phase 2a tooltipHasShiftDown TODO; same accessor Neo's ClientTooltipFlag uses |
| `BlockStateModel` world-aware overloads | Neo `BlockStateModelExtension` `collectParts/materialFlags/particleMaterial/createGeometryKey(level,pos,state[,random])` | FRAPI-injected `FabricBlockStateModel` equivalents (same names; the world-aware `materialFlags`/`createGeometryKey` additionally take RandomSource) — used by the fabric model twins only; shared code has no world-aware model calls |

### Lifecycle wiring table (AppEngClient op → fabric mechanism, in AppEngFabricClient)

| Loader-free op (step 14 surface) | Fabric mechanism |
|---|---|
| construction | `AppEngFabricClient extends AppEngClient implements ClientModInitializer`; vanilla `KeyMapping` ctor (no key conflict contexts on Fabric); `AppEngFabric.init(this)` runs the common init (main entrypoint defers on the client) |
| `registerClientCommands` | NOT bridged: fabric client commands use `FabricClientCommandSource`, not CommandSourceStack — the single debug command (`ae2client highlight_gui_areas`) is mirrored natively via `ClientCommandRegistrationCallback` |
| `registerClientTooltipComponents` | `ClientTooltipComponentCallback.EVENT` (instanceof dispatch per registered class) |
| key mappings + `Hotkeys.finalizeRegistration` | `KeyMappingHelper.registerKeyMapping`; category: `Hotkeys.CATEGORY` added to AW'd `KeyMapping.Category.SORT_ORDER` (vanilla `Category.register` only constructs NEW instances) |
| `clientSetup()` | called directly at the end of `onInitializeClient` (mod init runs on the main thread; Neo enqueued on FMLClientSetupEvent) |
| `onMouseWheel` / `onKeyInput` | NEW client mixins `appeng.fabric.mixins.client.{MouseScrollMixin,KeyInputMixin}` — javap-verified against the Neo firing points (`MouseHandler#onScroll` after `ScrollWheelHandler.onMouseScroll`, recomputing the scaled offset with the vanilla formula; `KeyboardHandler#keyPress` TAIL with a window-handle guard) |
| `clientTickStart`/`clientTickEnd` | `ClientTickEvents.START/END_CLIENT_TICK` (Neo registered start with LOWEST priority; fabric order not controllable — 3b risk note) |
| `onPlayerLoggingIn` | `ClientPlayConnectionEvents.JOIN` |
| `receiveRecipes` | NEW chunked payload `ae2:sync_recipes` (`appeng.fabric.network.{SyncRecipesPayload,RecipeSync}`, GuideME precedent): server sends on `ServerPlayConnectionEvents.JOIN` + `END_DATA_PACK_RELOAD` (guarded by `canSend`), client receiver rebuilds `RecipeMap.create` when the type-id payload arrives |
| entity / BE renderers, layer definitions | `EntityRendererRegistry` / `BlockEntityRendererRegistry` (raw-typed adapter for the wildcard mismatch) / `ModelLayerRegistry.registerModelLayer` |
| particles | `ParticleProviderRegistry.getInstance().register(type, registration::create)` (FabricSpriteSet extends SpriteSet) + `ParticleGroupRegistry.register` (exact signature match) |
| render pipelines | NOT pre-registered (no fabric API; backend compiles lazily on first use — GuideME precedent) |
| item model properties / item models / item tint sources | AW'd vanilla mappers `RangeSelectItemModelProperties/ItemModels/ItemTintSources.ID_MAPPER::put` (fabric transitive AWs widen them) |
| block tint sources | `BlockColorRegistry.register` (exact signature match) |
| block state model codecs | `CustomUnbakedBlockStateModel.register` (fabric-model-loading; same interface shape as Neo's) for all 8 models |
| standalone models | `ModelLoadingPlugin` + `ExtraModelKey`/`SimpleUnbakedExtraModel` (crank handle as BlockStateModelPart, storage-cell models via `blockStateModel(id)`; the plugin lambda reads StorageCellModels freshly per load) |
| part model types / part renderers | NEW addon-facing entrypoint `ae2:client_registration` (`appeng.fabric.client.AE2FabricClientRegistration`); `FabricClientLoaderHooks` posts the fabric twins of Register{PartModels,PartRenderer}Event through it; AE2 registers itself as the first entrypoint |
| `initCustomClientRegistries()` | called directly in `onInitializeClient` (before resource/model loading starts) |
| `initGuide()` | called directly (guideme-fabric API is loader-neutral) |
| `InitScreens` | `InitScreens.init(MenuScreens::register)` — vanilla private method, fabric transitive AW widens it |
| reload listeners (PartRendererDispatcher, styles) | `ResourceManagerHelper` + NEW `FabricReloadListenerWrapper` (GuideME precedent). Neo orders the dispatcher BEFORE the vanilla BER listener; fabric runs mod listeners after vanilla — only queried at render time, noted for 3b verification |
| client packet receivers | NEW `FabricClientNetworkInit` → `ClientPlayNetworking.registerGlobalReceiver` per `AEClientboundPacketHandler.Registrar` entry |
| `setShiftDownSupplier` | `Minecraft.getInstance().hasShiftDown()` (Phase 2a TODO resolved) |
| config screen extension | none (needs ModMenu integration; deferred) |
| IMC (darkmodeeverywhere, framedblocks GLASS_STATE) | nothing to do — both are NeoForge-only mods; the fabric QuartzGlassModel twin drops the write-only `GLASS_STATE` ModelProperty |

### Fabric twins (same FQN, loader/fabric/src/client/java) and the FRAPI quad pipeline

Mechanical twins (≈verbatim, Neo APIs swapped): StorageCellModels (`StandaloneModelKey` →
`ExtraModelKey`), RegisterPartModelsEvent/RegisterPartRendererEvent (plain classes driven by the
entrypoint), FluidBlitter (fluid seam), BlockAttackHook (`AttackBlockCallback`), RenderBlockOutlineHook
(`LevelRenderEvents.BEFORE_BLOCK_OUTLINE`; extraction+render merged; returning false ≙ Neo renderer
returning true), AreaOverlayRenderer (`LevelRenderEvents.END_EXTRACTION` + injected
`FabricRenderState.setData` on LevelRenderState, render at `END_MAIN` ≈ Neo AfterWeather),
FluidBlockPictureInPictureRenderer (Neo `VertexConsumerWrapper` → plain delegating VertexConsumer),
CrankRenderer/MEChestRenderer (`getStandaloneModel` → AW-free `FabricModelManager#getModel`;
world-aware part collection downgraded to the context-free vanilla calls — both models are static).

Model twins ride on the NEW glue `appeng.fabric.client.render.FabricDynamicBlockStateModel`
(mirror of Neo's DynamicBlockStateModel: world-aware `collectParts` + FRAPI `emitQuads` default that
emits the collected parts), with `NeoForgeRenderData.unwrap(level.getModelData(pos))` →
`((FabricBlockGetter) level).getBlockEntityRenderData(pos)`: QnbFormedModel, SpatialPylonModel,
PaintSplotchesModel (`ModelData` pass-through → plain `Object`), DriveModel (`ComposedModelState` →
fabric-glue mirror), CraftingCubeModel, QuartzGlassModel (`QuadBakingVertexConsumer` → direct vanilla
BakedQuad construction), SingleSpinnableVariant, P2PFrequencyPartModel/PlanePartModel/
MeteoriteCompassModel (`QuadTransforms` → fabric-glue mirror transforming positions only).

Quad pipeline (the deep rewrite):
- **Vanilla 26.1 BakedQuad carries NO per-vertex colors and NO normals — both are NeoForge record
  patches (`bakedColors`/`bakedNormals`).** This is the central FRAPI impedance. Colors are carried
  through the NEW side channel `appeng.fabric.client.render.QuadColors` (Guava weakKeys = identity);
  the fabric model twins re-apply them when re-emitting through FRAPI meshes (whose format does store
  per-vertex colors). Normals: Neo bakes face normals — the render default — so dropping them is lossless.
- fabric CubeBuilder twin: same public surface (`Consumer<BakedQuad>`), builds vanilla BakedQuads
  directly — positions verbatim from Neo's `MutableQuad#setCubeFaceFromSpriteCoords`, UV packing via
  vanilla `UVPair.pack`, material resolution verbatim from `MutableQuad#setSprite` (transparency →
  ChunkSectionLayer/item RenderType), emissive → MaterialInfo.lightEmission(15). Colors → QuadColors.
- codechicken transformers (7 files): ported VERBATIM onto FRAPI `MutableQuadView`
  (`positionComponent→posByIndex`, `direction()→nominalFace()`, setter renames); InterpHelper copied
  unchanged (pure math). They had ALREADY been written against this API shape pre-26.1
  (`appeng.thirdparty.fabric` era) — the current Neo MutableQuad versions are the same math.
- fabric FacadeBuilder twin: pipeline runs on a scratch `Renderer.get().quadEmitter()` (never emitted):
  `clear→fromBakedQuad→clamper→faceStripper→kicker→reInterpolator→tintIndex(-1)→toBakedQuad(sprite)`;
  pre-baked block tints go into QuadColors. Facade source model parts are collected with the
  context-free vanilla `collectParts` (facades are full blocks without BEs — no render-data models).
- fabric CableBusModel twin: overrides FRAPI `emitQuads`; cable-model and facade parts are re-emitted
  color-aware from QuadColors, part models emit via the injected `BlockStateModelPart#emitQuads`
  default; `createGeometryKey` returns the CableBusRenderState (content-equality, mirrors the Neo
  cache semantics).

### Phase 3b runtime-risk / minimal-implementation list (compile gate accepted these)

1. **Spatial storage dimension visuals**: no fabric-api 26.1 equivalent of
   RegisterCustomEnvironmentEffectRendererEvent; the SpatialStorage{Sky,Clouds,WeatherEffects}Renderer
   twins were NOT created — the dimension renders the vanilla default sky (the fabric biome json already
   ships without the Neo environment attributes). Needs investigation (vanilla environment-attribute
   registry mixin?).
2. **CableBusBlockClientExtensions** (Neo IClientBlockExtensions): particle/sound hooks for cable busses
   not twinned — vanilla fallback break/run particles+sounds on fabric.
3. **Per-vertex color loss on vanilla-pipeline paths**: QuadColors only helps where AE2's fabric twins
   re-emit quads. Lost on: MemoryCardItemModel hash colors (vanilla item path), FacadeItemModel
   (facade ITEM rendering of tinted blocks, e.g. grass facades), CellLedRenderer-adjacent paths are
   unaffected (submit-time QuadInstance colors are vanilla). Candidate fix: FabricLayerRenderState mesh
   support or tint sources.
4. **RenderBoundingBoxHook** not dispatched (SkyStoneChest lid may cull at screen edges).
5. `clientTickStart` ordering: Neo used LOWEST priority; fabric registration order — cable render mode
   refresh may run before other mods' tick handlers.
6. Mixin risk: KeyInputMixin @At("TAIL") assumes the fall-through return is last in bytecode;
   MouseScrollMixin fires before the discrete-scroll zero-check (Neo fires after) — sub-threshold
   discrete scrolls can reach AE2 slightly earlier; verify both in runClient.
7. `KeyMapping.Category.SORT_ORDER` direct add: verify the controls screen lists the AE2 category.
8. Reload-listener ordering (PartRendererDispatcher after vanilla BER listener) — verify part renderers
   after F3+T.
9. Facade `collectParts` is context-free on fabric (Neo passes level/pos) — dynamic facade source
   models (none known in practice) would render their fallback state.
10. BEFORE_BLOCK_OUTLINE runs once per frame (Neo renders its custom outline renderers possibly in two
    passes via `translucentPass`) — verify part placement preview depth behavior.
11. Recipe-sync payload size: 250-recipe chunks (GuideME precedent) — verify with large packs.
12. `ae2:interface_slot_filtering` NeoForge-only gametest plot still needs a fabric capability-overlay
    twin (pre-existing, Phase 2b).

### Gotchas

- fabric-api 26.1 renamed/moved several client APIs: `WorldRenderEvents` → `LevelRenderEvents`
  (fabric-rendering-v1), key bindings → `KeyMappingHelper` (fabric-key-mapping-api-v1; the old
  fabric-key-binding-api-v1 jar in the cache is an intermediary-named leftover), block colors →
  `BlockColorRegistry`, render data → `FabricBlockGetter` (fabric-block-getter-api-v2).
- `BlockStateModel`/`BlockStateModelPart`/`MaterialBaker`/`TextureAtlas`/`ModelManager` get FRAPI
  interface injections (`FabricBlockStateModel#emitQuads` etc.) via transitive class tweakers — shared
  code could even call the world-aware overloads, but only the fabric twins do.
- `ClientCommands` name clash: fabric's `net.fabricmc...command.v2.ClientCommands` vs AE2's
  `appeng.client.commands.ClientCommands` — the entrypoint imports the fabric one.
- Generic-method functional interfaces (SpriteSetRegistrar, ClientTooltipComponentRegistrar,
  BlockEntityRendererRegistrar) need anonymous classes or method refs — lambdas do not compile
  (step-14 note still applies).

## Open questions

- TR Energy 5.0.0: confirm it targets MC 26.1 Fabric API at compile time.
