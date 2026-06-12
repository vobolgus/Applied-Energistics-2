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

// Root project: shared conventions only. The mod itself is built per-loader:
//   :neoforge (loader/neoforge) — ModDevGradle
//   :fabric   (loader/fabric)   — fabric-loom
// Both compile the shared sources in /src/main/java and /src/client/java.

plugins {
    id("com.diffplug.spotless")
}

apply<appengbuild.CrowdinPlugin>()

/////////////
// Spotless
spotless {

    java {
        target("src/*/java/appeng/**/*.java", "loader/*/src/*/java/appeng/**/*.java")

        endWithNewline()
        indentWithSpaces()
        removeUnusedImports()
        toggleOffOn()
        eclipse().configFile("codeformat/codeformat.xml")
        importOrderFile("codeformat/ae2.importorder")

        // courtesy of diffplug/spotless#240
        // https://github.com/diffplug/spotless/issues/240#issuecomment-385206606
        custom("noWildcardImports") { content ->
            if (content.contains("*;\n")) {
                throw Error("No wildcard imports allowed")
            }

            content
        }
        bumpThisNumberIfACustomStepChanges(1)
    }

    json {
        target("src/*/resources/**/*.json", "loader/*/src/*/resources/**/*.json")
        targetExclude("src/generated/resources/**")
        // Was (Groovy): `it.new JsonExtension.BiomeJson(null)` + protected addStep(createStep()),
        // with downloadDir wrapped in try/catch (Groovy could call the protected members; the
        // catch swallowed the replaceStep failure that occurred because the step was not added
        // yet). The public biome() API yields the identical end state: one biome step with the
        // download dir pinned inside the repo.
        val biomeConfig = biome()
        try {
            biomeConfig.downloadDir(File(rootDir, ".gradle/biome").absolutePath)
        } catch (ignored: Exception) {
        }
        indentWithSpaces(2)
        endWithNewline()
    }
}
