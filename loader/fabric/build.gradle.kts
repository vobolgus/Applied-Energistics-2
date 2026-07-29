/*
 * Fabric loader project for Applied Energistics 2.
 *
 * Compiles the shared sources from the repository root (src/main/java, src/client/java)
 * against Fabric Loader + Fabric API, plus the Fabric-specific platform layer in this
 * directory (loader/fabric/src).
 *
 * NOTE: The shared sources still contain net.neoforged imports until the Phase 1
 * decoupling is complete (gate M1). Until then they are excluded from this project;
 * flip them on with -Pae2.fabric.shared=true (or by setting it in gradle.properties).
 */

import appengbuild.NeoForgeToFabricRecipeTransform

plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
}

apply<appengbuild.ProjectDefaultsPlugin>()

// Gradle property accessor (the Groovy script used the dynamic `project.foo` / `${foo}` lookup)
fun prop(name: String): String = providers.gradleProperty(name).get()

// fabric-loom injects project-level repositories into this project, which (under PREFER_PROJECT)
// makes Gradle ignore the settings-level repositories entirely for :fabric. Declare what we need here.
repositories {
    mavenLocal {
        content {
            includeModule("org.appliedenergistics", "guideme-fabric")
        }
    }
    maven {
        name = "TeamReborn"
        url = uri("https://maven.modmuss50.me/")
        content {
            includeGroup("teamreborn")
        }
    }
    maven {
        url = uri("https://maven.shedaniel.me/")
        content {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
    }
    maven {
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
        }
    }
    maven {
        url = uri("https://maven2.bai.lol")
        content {
            includeGroup("mcp.mobius.waila")
            includeGroup("lol.bai")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    mavenCentral()
}

base {
    archivesName = "appliedenergistics2-fabric"
}

loom {
    // NOTE: splitEnvironmentSourceSets() is deliberately NOT used (yet): the shared src/main/java still
    // contains client-referencing classes (appeng.client.*, e.g. MenuTypeBuilder/Hotkey — the known
    // "main-source client leak", see PORTING_NOTES). Like the NeoForge dev environment, main compiles
    // against the merged Minecraft jar; the dedicated-server boot test is the gate for actual leaks.
    // Phase 3 revisits the client source set split.

    mods {
        create("ae2") {
            sourceSet(sourceSets["main"])
        }
    }

    accessWidenerPath = file("src/main/resources/ae2.accesswidener")

    runs {
        configureEach {
            runDir = "run"
            property("appeng.tests", "true")
            property("guideme.ae2.guide.sources", rootProject.file("guidebook").absolutePath)
        }
        named("server") {
            programArgs("nogui")
        }
        // Headless game-test server (the twin of :neoforge's `gametest` run, type 'gameTestServer').
        // fabric-api's gametest module hijacks the dedicated-server main when `fabric-api.gametest`
        // is set and boots a vanilla GameTestServer that runs every non-manualOnly entry of the
        // `minecraft:test_instance` registry; AE2's plot tests are injected into that registry by
        // GameTestRegistryLoadTaskMixin (gated on `appeng.tests`, set in configureEach above).
        create("gametest") {
            server()
            name = "Game Test Server"
            runDir = "build/gametest"
            property("fabric-api.gametest", "true")
        }
        // Client boot straight into a singleplayer world (the twin of :neoforge's `gametestWorld`
        // run). NOTE: vanilla quickplay does NOT create missing worlds - the `GametestWorld` save
        // must exist in run/saves (create it once via the world-creation screen); afterwards the
        // run is fully hands-off (`appeng.tests` comes from configureEach above).
        create("gametestWorld") {
            client()
            name = "Game Test World (Client)"
            programArgs("--username", "AE2Dev", "--quickPlaySingleplayer", "GametestWorld")
        }
    }
}

// Shared MAIN sources (gate flipped on with Phase 2a; ae2.fabric.shared now only gates main).
val includeSharedSources = providers.gradleProperty("ae2.fabric.shared").getOrElse("false") == "true"
// Shared CLIENT sources stay off until Phase 3.
val includeSharedClientSources = providers.gradleProperty("ae2.fabric.client").getOrElse("false") == "true"
if (includeSharedSources) {
    sourceSets {
        named("main") {
            java {
                srcDir(rootProject.file("src/main/java"))
                // TODO 1.21.11 (kept in sync with :neoforge)
                exclude("appeng/integration/modules/theoneprobe/**")
                // Jade/WTHIT (Phase 4): compiled against the loader-specific API jars wired below.
                // Discovery: Jade via the `jade` entrypoint in fabric.mod.json; WTHIT via the shared
                // wthit_plugins.json (scanned from the mod jar root on both loaders).
            }
            resources {
                srcDir(rootProject.file("src/main/resources"))
                srcDir(rootProject.file("src/generated/resources"))
                // NeoForge metadata must not ship in the Fabric jar
                exclude("META-INF/accesstransformer.cfg")
            }
        }
    }
}
if (includeSharedClientSources) {
    // Phase 3a: the shared CLIENT sources are compiled into the MAIN source set (mirroring :neoforge's
    // single-jar layout; vanilla only loads client classes on the client). loom's splitEnvironmentSourceSets
    // is deliberately NOT used: the shared main sources already contain client-referencing classes (the known
    // "main-source client leak"), and the dedicated-server boot test remains the actual leak gate.
    sourceSets {
        named("main") {
            java {
                srcDir(rootProject.file("src/client/java"))
                // Fabric client overlay (twins of the NeoForge client overlay classes + appeng.fabric.client glue)
                srcDir("src/client/java")
                // TODO 1.21.11 (kept in sync with :neoforge's client source set excludes)
                exclude("appeng/client/integration/emi/**")
                // JEI (Phase 4): the shared plugin + the addon-facing converter API compile against the JEI
                // common API (pulled in by the jei-fabric-api compileOnly below). Discovery on Fabric is the
                // `jei_mod_plugin` entrypoint in fabric.mod.json (NeoForge uses @JeiPlugin annotation scanning).
                // The addon-facing EMI converter API compiles against the EMI API (:neoforge wires
                // emi-neoforge:api as clientCompileOnly); no EMI artifact in :fabric yet (Phase 4).
                exclude("appeng/client/api/integrations/emi/**")
                // NOTE: appeng/client/guidebook/** is NOT excluded: it compiles against guideme-fabric
                // (renderFluid(Fluid) etc.). appeng/datagen/** no longer exists in the shared client tree
                // (moved to the :neoforge overlay in Phase 1 step 14) - nothing to exclude for it.
            }
            resources {
                srcDir(rootProject.file("src/client/resources"))
            }
        }
    }
}

// Dev-runtime-only mods (twin of :neoforge's localRuntimeOnly): on the run configs' classpath, never published.
val localRuntimeOnly = configurations.create("localRuntimeOnly")
configurations["runtimeClasspath"].extendsFrom(localRuntimeOnly)

dependencies {
    "minecraft"("com.mojang:minecraft:${prop("minecraft_version")}")
    // MC 26.1+ is unobfuscated: no mappings dependency, and mods are plain `implementation`
    // (loom 1.17 dropped the modImplementation remapping configurations)

    implementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

    // Energy at the edges: Team Reborn Energy over the Fabric Transfer API.
    // Shipped jar-in-jar so the published jar is self-contained (it is a library, not a mod
    // players install themselves).
    implementation("teamreborn:energy:${prop("tr_energy_version")}")
    "include"("teamreborn:energy:${prop("tr_energy_version")}")

    // TOML config files matching NeoForge's format (NeoForge bundles the same library).
    // Shipped jar-in-jar so the published jar carries it at runtime (the dev runtime gets it from
    // `implementation`); `include` does not pull transitives, so the core module is listed explicitly.
    implementation("com.electronwill.night-config:toml:${prop("night_config_version")}")
    "include"("com.electronwill.night-config:core:${prop("night_config_version")}")
    "include"("com.electronwill.night-config:toml:${prop("night_config_version")}")

    // javax.annotation (@Nonnull etc.) is still used by a few shared files; NeoForge provides it transitively
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // REI compile-time API for the restored integration (appeng/integration/modules/rei + the addon-facing
    // converter API in appeng/api/integrations/rei). The -neoforge artifacts were chosen when the -fabric ones
    // were still intermediary-mapped; since REI 26.1.x both flavors are mojmap and carry the identical API, so
    // the pin is kept as-is (compile-only, never ships, never on any runtime classpath; the runtime jar below
    // provides the real classes).
    // transitive=false: their poms pull cloth-config-neoforge etc., which we neither need nor want resolved here;
    // architectury (REI's fluid entry type dev.architectury.fluid.FluidStack) is pinned explicitly instead.
    compileOnly("me.shedaniel:RoughlyEnoughItems-api-neoforge:${prop("rei_version")}") { isTransitive = false }
    compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-neoforge:${prop("rei_version")}") { isTransitive = false }
    compileOnly("dev.architectury:architectury-neoforge:${prop("architectury_version")}") { isTransitive = false }
    compileOnly("me.shedaniel.cloth:basic-math:${prop("cloth_basic_math_version")}") { isTransitive = false }

    // JEI compile-time API (Phase 4): jei-26.1.2-fabric-api carries the Fabric-only classes
    // (mezz.jei.api.fabric.*, e.g. FabricTypes/IJeiFluidIngredient used by the fluid converter twins) and
    // pulls jei-26.1.2-common-api transitively, which is what the shared plugin code compiles against.
    // Same version as :neoforge's jei-26.1.2-neoforge-api pin - the shared-code API is identical.
    compileOnly("mezz.jei:jei-${prop("jei_minecraft_version")}-fabric-api:${prop("jei_version")}")

    // Jade compile-time API (Phase 4): the Modrinth maven ships the full Fabric mod jar; only used to
    // compile the shared appeng/integration/modules/jade plugin (snownee.jade.api.*).
    compileOnly("maven.modrinth:jade:${prop("jade_fabric_version")}")

    // WTHIT compile-time API (Phase 4), Fabric edition of the same wthit-api the :neoforge build uses.
    compileOnly("mcp.mobius.waila:wthit-api:fabric-${prop("wthit_version")}")

    // Dev-runtime item-list mod, mirroring :neoforge's runtime_itemlist_mod switch. REI is the primary recipe
    // viewer for the Fabric port. ACTIVATED 2026-07-04: REI shipped 26.1.819 for MC 26.1.2 (fabric+neoforge),
    // rei_version bumped and runtime_itemlist_mod flipped to "rei"; both AE2 plugin providers register at boot
    // (the ctor-timing crash this uncovered is fixed in ReiPlugin/ReiClientPlugin - see PORTING_NOTES).
    when (prop("runtime_itemlist_mod")) {
        "rei" -> localRuntimeOnly("me.shedaniel:RoughlyEnoughItems-fabric:${prop("rei_version")}")
        "jei" -> localRuntimeOnly("mezz.jei:jei-${prop("jei_minecraft_version")}-fabric:${prop("jei_version")}")
        // "emi" has no Fabric wiring here (yet); the dev client runs without an item-list mod.
    }

    // Dev-runtime tooltip mod, mirroring :neoforge's runtime_tooltip_mod switch.
    when (prop("runtime_tooltip_mod")) {
        // WTHIT requires badpackets at runtime (fabric-0.12.2 / wthit fabric-19.0.1 shipped for
        // 26.1.x 2026-04; both resolve from maven2.bai.lol). :neoforge still has the upstream gap
        // (no badpackets on its runtime) - flip its switch only after wiring the neo flavor there.
        "wthit" -> {
            localRuntimeOnly("mcp.mobius.waila:wthit:fabric-${prop("wthit_version")}")
            localRuntimeOnly("lol.bai:badpackets:fabric-${prop("badpackets_version")}")
        }
        "jade" -> localRuntimeOnly("maven.modrinth:jade:${prop("jade_fabric_version")}")
    }

    if (includeSharedSources) {
        // Published by the GuideME Fabric port (Workstream A) to mavenLocal until released
        implementation("org.appliedenergistics:guideme-fabric:${prop("guideme_fabric_version")}")
    }
}

tasks.named<Jar>("jar") {
    exclude("/.cache")
    from(rootProject.file("guidebook")) {
        into("assets/ae2/ae2guide")
    }
}

tasks.named<ProcessResources>("processResources") {
    val expandProps: Map<String, String> = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to prop("minecraft_version"),
        "fabric_loader_version" to prop("fabric_loader_version")
    )
    inputs.properties(expandProps)

    filesMatching("fabric.mod.json") {
        expand(expandProps)
    }

    // ---------------------------------------------------------------------------------------------
    // DATAGEN-PARITY TRANSFORMS - the single place where generated (NeoForge-datagen) resources are
    // rewritten for Fabric on copy. Inventory (kept in sync with PORTING_NOTES.md):
    //   1. data/ae2/worldgen/biome/spatial_storage.json  -> generated copy EXCLUDED, fabric override
    //   2. assets/ae2/blockstates/*.json                 -> "type" -> "fabric:type" dispatch key
    //   3. data/ae2/recipe/**/*.json                     -> NeoForgeToFabricRecipeTransform (buildSrc)
    // Gradle does NOT track filter closures/classes as task inputs; bump this property whenever any
    // of the transforms below (or the transform class in buildSrc) changes, or the task stays UP-TO-DATE.
    inputs.property("ae2.datagenParityTransform", "v2")

    // 1. The generated biome JSON carries NeoForge-registered environment attributes (custom
    // sky/clouds/weather renderer ids); vanilla's EnvironmentAttributeMap codec hard-fails on unknown
    // attribute keys, which would be FATAL at server start (worldgen registry loading). Ship the
    // Fabric copy (same file minus "attributes", in loader/fabric/src/main/resources) instead;
    // Phase 3 wires the spatial-storage sky via Fabric's DimensionRenderingRegistry, which is
    // code-driven and needs no biome attributes.
    val generatedResourcesPath = rootProject.file("src/generated/resources").toPath()
    filesMatching("data/ae2/worldgen/biome/spatial_storage.json") {
        if (file.toPath().startsWith(generatedResourcesPath)) {
            exclude()
        }
    }

    // 2. Custom block-state model dispatch: NeoForge's patched vanilla codec dispatches on the "type"
    // key, while fabric-model-loading-api-v1 leaves the vanilla codec alone and dispatches on
    // "fabric:type". The generated blockstate JSONs target NeoForge; rewrite the key on copy (vanilla
    // variant objects never carry a "type" key, and only AE2's custom models use "ae2:"-namespaced
    // type ids - the match cannot hit anything else).
    filesMatching("assets/ae2/blockstates/*.json") {
        filter { line: String -> line.replace("\"type\": \"ae2:", "\"fabric:type\": \"ae2:") }
    }

    // 3. Recipe resource conditions + custom ingredient types (see the transform class in buildSrc).
    // Covers the 67 matter-cannon ammo recipes (neoforge:conditions tag gate) and the 5 cable "clean"
    // recipes (neoforge:difference ingredient); applied to the whole recipe tree so upstream additions
    // are either mapped or fail the build.
    filesMatching("data/ae2/recipe/**/*.json") {
        filter(NeoForgeToFabricRecipeTransform::class.java)
    }
}

// was: static void publicApiIncludePatterns(PatternFilterable spec)
fun publicApiIncludePatterns(spec: PatternFilterable) {
    spec.exclude("**/*Internal.*")
    spec.exclude("**/*Internal\$*.*")
    spec.include("appeng/api/**")
    spec.include("appeng/client/api/**")
}

// Mirrors :neoforge's javadoc setup: restrict to the public API packages and disable doclint —
// upstream's shared javadoc is not doclint-clean (self-closing tags etc.).
tasks.named<Javadoc>("javadoc") {
    val docletOptions = options as StandardJavadocDocletOptions
    docletOptions.addStringOption("Xdoclint:none", "-quiet")
    docletOptions.encoding = "UTF-8"
    docletOptions.charSet = "UTF-8"
    publicApiIncludePatterns(this)
}

tasks.named<Jar>("sourcesJar") {
    // fabric-loom adds the main sources to the sources jar on top of withSourcesJar()'s own configuration,
    // producing identical duplicate entries; excluding the second copy is lossless.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// mavenLocal publication for downstream dual-loader ports (AE2WTLib consumes
// org.appliedenergistics:appliedenergistics2-fabric — same shape as GuideME's fabric module).
// Re-run :fabric:publishToMavenLocal after every rebase/version bump.
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.appliedenergistics"
            artifactId = "appliedenergistics2-fabric"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
