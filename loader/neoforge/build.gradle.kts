/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

plugins {
    id("net.neoforged.moddev")
    `maven-publish`
    signing
}

apply<appengbuild.ProjectDefaultsPlugin>()

// Gradle property accessor (the Groovy script used the dynamic `project.foo` / `${foo}` lookup)
fun prop(name: String): String = providers.gradleProperty(name).get()

base {
    archivesName = "appliedenergistics2"
}

neoForge.enable {
    version = prop("neoforge_version")
    // Disable recompilation if the "CI" environment variable is set to true. It is automatically set by GitHub Actions.
    isDisableRecompilation = System.getenv("CI") == "true"
}

// Sources are shared with the :fabric loader project and live in the repository root.
sourceSets {
    named("main") {
        java {
            setSrcDirs(listOf(rootProject.file("src/main/java"), "src/main/java"))
            // TODO 1.21.11
            exclude("appeng/integration/modules/theoneprobe/**")
        }
        resources {
            setSrcDirs(listOf(rootProject.file("src/main/resources"), rootProject.file("src/generated/resources")))
        }
    }
    create("client") {
        java.setSrcDirs(listOf(rootProject.file("src/client/java"), "src/client/java"))
        resources.setSrcDirs(listOf(rootProject.file("src/client/resources")))

        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output

        // TODO 1.21.11
        java {
            exclude("appeng/client/integration/emi/**")
        }
    }
    named("test") {
        java.setSrcDirs(listOf(rootProject.file("src/test/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/test/resources")))

        compileClasspath += sourceSets["client"].output
        runtimeClasspath += sourceSets["client"].output
    }
    create("buildtools") {
        java.setSrcDirs(listOf(rootProject.file("src/buildtools/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/buildtools/resources")))
    }
}

val localRuntimeOnly = configurations.create("localRuntimeOnly")
configurations["buildtoolsImplementation"].extendsFrom(configurations["compileClasspath"])
configurations["runtimeClasspath"].extendsFrom(localRuntimeOnly)
configurations["clientRuntimeClasspath"].extendsFrom(localRuntimeOnly)

dependencies {
    // Dependencies only used for the guide export, but not shipped
    localRuntimeOnly("org.bytedeco:ffmpeg-platform:${prop("ffmpeg_version")}")

    implementation("org.appliedenergistics:guideme:${prop("guideme_version")}")
    "clientImplementation"("org.appliedenergistics:guideme:${prop("guideme_version")}")

    // compile against provided APIs
    "clientCompileOnly"("dev.emi:emi-neoforge:${prop("emi_version")}:api")
    "clientCompileOnly"("me.shedaniel:RoughlyEnoughItems-neoforge:${prop("rei_version")}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${prop("rei_version")}")
    compileOnly("mcp.mobius.waila:wthit-api:neo-${prop("wthit_version")}")
    compileOnly("curse.maven:jade-324717:${prop("jade_file_id")}")
    "clientCompileOnly"("mezz.jei:jei-${prop("jei_minecraft_version")}-neoforge-api:${prop("jei_version")}")

    when (prop("runtime_itemlist_mod")) {
        "emi" -> localRuntimeOnly("dev.emi:emi-neoforge:${prop("emi_version")}")
        "rei" -> localRuntimeOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${prop("rei_version")}")
        "jei" -> localRuntimeOnly("mezz.jei:jei-${prop("jei_minecraft_version")}-neoforge:${prop("jei_version")}")
    }

    when (prop("runtime_tooltip_mod")) {
        "wthit" -> localRuntimeOnly("mcp.mobius.waila:wthit:neo-${prop("wthit_version")}")
        "jade" -> localRuntimeOnly("curse.maven:jade-324717:${prop("jade_file_id")}")
    }

    if (prop("runtime_curio") == "true") {
        localRuntimeOnly("top.theillusivec4.curios:curios-neoforge:${prop("curios_version")}")
    }

    // Athena
    if (prop("runtime_athena") == "true") {
        localRuntimeOnly("curse.maven:athena-841890:${prop("athena_file_id")}")
    }

    // unit test dependencies
    testImplementation(platform("org.junit:junit-bom:${prop("junit_version")}"))
    testImplementation(platform("org.assertj:assertj-bom:${prop("assertj_version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.google.guava:guava-testlib:21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:${prop("mockito_version")}")
    testImplementation("net.neoforged:testframework:${prop("neoforge_version")}")
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    // Might not need this anymore...
    systemProperty("guideme.ae2.guide.sources", rootProject.file("guidebook").absolutePath)
}

dependencies {
    "buildtoolsImplementation"("de.siegmar:fastcsv:2.1.0")
    "buildtoolsImplementation"("com.google.code.gson:gson:2.8.9")
}

neoForge {
    validateAccessTransformers = true

    addModdingDependenciesTo(sourceSets["client"])

    mods {
        create("ae2") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runs {
        configureEach {
            gameDirectory.set(rootProject.file("run"))
            systemProperty("appeng.tests", "true")
            // systemProperty("mixin.debug.export", "true")
            logLevel.set(org.slf4j.event.Level.INFO)
            systemProperty("guideme.ae2.guide.sources", rootProject.file("guidebook").absolutePath)
        }
        create("client") {
            client()
            systemProperty("appeng.tests", "true")
            sourceSet.set(sourceSets["client"])
        }
        create("gametestWorld") {
            client()
            programArguments.set(listOf(
                "--username", "AE2Dev", "--quickPlaySingleplayer", "GametestWorld"
            ))
            systemProperty("appeng.tests", "true")
            sourceSet.set(sourceSets["client"])
        }
        create("guide") {
            client()

            systemProperty("guideme.showOnStartup", "ae2:guide")
            sourceSet.set(sourceSets["client"])
        }
        create("server") {
            server()
        }
        create("data") {
            clientData()
            programArguments.set(listOf(
                "--mod", "ae2",
                "--all",
                "--output", rootProject.file("src/generated/resources/").absolutePath,
                "--existing", rootProject.file("src/main/resources").absolutePath
            ))
            sourceSet.set(sourceSets["client"])
        }
        create("guideexport") {
            client()
            systemProperty("guideme.exportOnStartupAndExit", "ae2:guide")
            systemProperty("guideme.exportDestination.ae2.guide", file("build/guide").absolutePath)
            systemProperty("guideme.exportModVersion.ae2.guide", project.version.toString())
            sourceSet.set(sourceSets["client"])
        }
        // Use to run the tests
        create("gametest") {
            type.set("gameTestServer")
            gameDirectory.set(project.file("build/gametest"))
        }
    }

    unitTest {
        enable()
        testedMod.set(mods["ae2"])
    }
}

//////////////
// Artifacts
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    group = "build"
    from(rootProject.file("src/main/neoforge.mods.toml")) {
        rename("(.*)", "META-INF/\$1")
    }
    into("build/generated/modMetadata")

    // Exposed project properties
    val projectProperties: Map<String, String> = listOf(
        "version",
        "minecraft_version",
        "neoforge_version_range",
        "jade_version_range",
        "jei_version",
        "guideme_version"
    ).associateWith { project.property(it).toString() }

    // Ensure the resources get re-evaluate when the version changes
    inputs.properties(projectProperties)
    expand(projectProperties)
}
sourceSets["main"].resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.named<Jar>("jar") {
    from(sourceSets["client"].output)
    exclude("/.cache")
    from(rootProject.file("guidebook")) {
        into("assets/ae2/ae2guide")
    }
}

// was: static void publicApiIncludePatterns(PatternFilterable spec)
fun publicApiIncludePatterns(spec: PatternFilterable) {
    spec.exclude("**/*Internal.*")
    spec.exclude("**/*Internal\$*.*")
    spec.include("appeng/api/**")
    spec.include("appeng/client/api/**")
}

tasks.named<Javadoc>("javadoc") {
    setSource(sourceSets["main"].allJava)
    source(sourceSets["client"].allJava)
    classpath = sourceSets["client"].compileClasspath + sourceSets["client"].output

    val docletOptions = options as StandardJavadocDocletOptions
    docletOptions.addStringOption("Xdoclint:none", "-quiet")
    docletOptions.encoding = "UTF-8"
    docletOptions.charSet = "UTF-8"
    publicApiIncludePatterns(this)
}

tasks.named<Jar>("sourcesJar") {
    from(sourceSets["client"].allSource)
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api"
    // api jar ist just a development aid and serves as both a binary and source jar simultaneously
    from(sourceSets["main"].output)
    from(sourceSets["main"].allJava)
    from(sourceSets["client"].output)
    from(sourceSets["client"].allJava)
    publicApiIncludePatterns(this)
}

//////////////////
// Maven publish
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "appliedenergistics2"
            version = project.version.toString()
            pom {
                name = "Applied Energistics 2"
                description = "A Minecraft mod about Matter, Energy and using them to conquer the world..."
                url = "https://appliedenergistics.org"
                scm {
                    connection = "scm:git:git://github.com/AppliedEnergistics/Applied-Energistics-2.git"
                    developerConnection = "scm:git:ssh://github.com:AppliedEnergistics/Applied-Energistics-2.git"
                    url = "https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/main"
                }
                licenses {
                    license {
                        name = "LGPLv3, MIT, CC BY-NC-SA 3.0"
                        url = "https://github.com/AppliedEnergistics/Applied-Energistics-2?tab=readme-ov-file#license"
                    }
                }
                developers {
                    developer {
                        name = "Technici4n"
                        email = "team@appliedenergistics.org"
                        organization = "Applied Energistics"
                        organizationUrl = "https://github.com/AppliedEnergistics/"
                    }
                    developer {
                        name = "shartte"
                        email = "team@appliedenergistics.org"
                        organization = "Applied Energistics"
                        organizationUrl = "https://github.com/AppliedEnergistics/"
                    }
                }
            }

            from(components["java"])
            artifact(apiJar)
        }
    }
    repositories {
        maven {
            name = "Local"
            url = file("build/repo").toURI()
        }
    }
}

//////////////// Add static site export
tasks.named("runGuideexport") {
    outputs.dir("build/guide")
}

tasks.register<JavaExec>("createStaticSite") {
    inputs.dir("build/guide")
    outputs.dir("build/website")
    group = "mod development"
    // dependsOn(runGuideexport)
    classpath(sourceSets["client"].runtimeClasspath)
    mainClass = "guideme.internal.web.StaticSiteGenerator"
    args("--data", "build/guide", "--output", "build/website")
}

////////////////
val validateResources = tasks.register<JavaExec>("validateResources") {
    group = "verification"
    classpath = sourceSets["buildtools"].runtimeClasspath
    mainClass = "ValidateResourceIds"
    workingDir = rootProject.projectDir
    args("guidebook")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}
tasks.named("check") {
    dependsOn(validateResources)
}

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    if (!signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
