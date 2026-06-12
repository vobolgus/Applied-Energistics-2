import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.initialization.resolve.RulesMode

pluginManagement {
    repositories {
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
    plugins {
        id("com.diffplug.spotless") version "6.25.0"
        // https://projects.neoforged.net/neoforged/ModDevGradle
        id("net.neoforged.moddev") version "2.0.141"
        id("net.neoforged.moddev.repositories") version "2.0.141"
        // https://fabricmc.net/develop/
        id("net.fabricmc.fabric-loom") version "1.17.8"
    }
}

plugins {
    id("net.neoforged.moddev.repositories")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // fabric-loom injects project-level repositories (e.g. LoomLocalRemappedMods), which is
    // incompatible with FAIL_ON_PROJECT_REPOS. Project repos take precedence where declared;
    // everything else continues to resolve from the settings-level repositories below.
    repositoriesMode = RepositoriesMode.PREFER_PROJECT
    rulesMode = RulesMode.FAIL_ON_PROJECT_RULES
    repositories {
        maven {
            url = uri("https://maven.shedaniel.me/")
            content {
                includeGroup("me.shedaniel")
                includeGroup("me.shedaniel.cloth")
                includeGroup("dev.architectury")
            }
        }
        maven {
            url = uri("https://maven2.bai.lol")
            content {
                includeGroup("mcp.mobius.waila")
                includeGroup("lol.bai")
            }
        }
        maven {
            name = "TerraformersMC"
            url = uri("https://maven.terraformersmc.com/")
            content {
                includeGroup("dev.emi")
            }
        }
        maven {
            name = "cursemaven"
            url = uri("https://www.cursemaven.com")
            content {
                includeGroup("curse.maven")
            }
        }
        maven { // for TOP
            url = uri("https://maven.k-4u.nl/")
            content {
                includeGroup("mcjty.theoneprobe")
            }
        }
        maven {
            url = uri("https://maven.theillusivec4.top/")
            content {
                includeGroup("top.theillusivec4.curios")
            }
        }
        maven {
            url = uri("https://maven.blamejared.com/")
            content {
                includeGroup("mezz.jei")
            }
        }
        mavenLocal {
            content {
                includeModule("org.appliedenergistics", "guideme")
                includeModule("org.appliedenergistics", "guideme-fabric")
            }
        }
        maven {
            name = "GuideME Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                includeModule("org.appliedenergistics", "guideme")
            }
        }
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
            content {
                includeGroupAndSubgroups("net.fabricmc")
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
            name = "MojangLibraries"
            url = uri("https://libraries.minecraft.net/")
            content {
                includeGroup("com.mojang")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "ae2"

include(":neoforge")
project(":neoforge").projectDir = file("loader/neoforge")

// :fabric cannot even be *configured* without org.appliedenergistics:guideme-fabric (fabric-loom
// resolves mod dependencies eagerly during configuration), and that artifact only exists in
// mavenLocal until GuideME ships a Fabric build. CI jobs that never touch :fabric (NeoForge build,
// guide export, localization) pass -Pae2.skipFabric=true instead of building GuideME first.
if (providers.gradleProperty("ae2.skipFabric").orNull != "true") {
    include(":fabric")
    project(":fabric").projectDir = file("loader/fabric")
}
