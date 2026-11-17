import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.GitProperty
import it.neckar.gradle.artifact.ArtifactKey
plugins {
}

description = """Version-Info for the repository"""

// Only stable values: the monorepo version, the coordinates of the fat-jar artifact key and GitProperty, so no commit enters compiled code.
val createVersionConstantsTask: TaskProvider<Task> = tasks.register("createVersionConstants") {
  group = "Build"
  description = "Create the version constants file (stable values only)"

  val monorepoVersion: String = version.toString()

  //Define inputs/outputs to support incremental builds
  inputs.property("version", monorepoVersion)

  val generatedSourcesDir = layout.buildDirectory.dir("generated/$name/main/kotlin")
  val targetFileProvider = layout.buildDirectory.file("generated/$name/main/kotlin/versionInfo/VersionConstants.kt")
  outputs.dir(generatedSourcesDir)

  doLast {
    val targetFile = targetFileProvider.get().asFile
    targetFile.parentFile.mkdirs()

    val gitPropertyEntries = GitProperty.entries.joinToString(",\n  ") { gitProperty ->
      """${gitProperty.name}("${gitProperty.propertyKey}", "${gitProperty.envVar}", "${gitProperty.systemProperty}")"""
    }

    targetFile.writeText(
      """
      package it.neckar.open.version

      /**
       * Contains stable version constants that rarely change.
       * Git info is resolved at runtime from the injected deploy metadata (see VersionInformation).
       */
      object VersionConstants {
        val monorepoVersion: String = "$monorepoVersion"

        /**
         * The fat jar entry the build writes the artifact key into.
         */
        const val ArtifactKeyResourcePath: String = "${ArtifactKey.ResourcePath}"

        /**
         * The property holding the artifact key in [ArtifactKeyResourcePath].
         */
        const val ArtifactKeyPropertyName: String = "${ArtifactKey.PropertyName}"

        /**
         * The prefix of every artifact key.
         */
        const val ArtifactKeyPrefix: String = "${ArtifactKey.Prefix}"
      }

      /**
       * The git properties resolvable at runtime, with their injection coordinates.
       * Generated 1:1 from the GitProperty enum of the build platform conventions (single source of
       * truth), so injection (compose env, serve-time HTML) and resolution
       * cannot drift.
       */
      enum class GitProperty(
        /**
         * Key in window.__APP_GIT_INFO__.
         */
        val propertyKey: String,
        /**
         * Environment variable a compose file sets to name the deployed commit.
         */
        val envVar: String,
        /**
         * JVM system property that overrides the environment variable (highest priority).
         */
        val systemProperty: String,
      ) {
        $gitPropertyEntries,
      }
      """.trimIndent() + "\n"
    )

    logger.info("Wrote version constants to: ${targetFile.absolutePath}")
  }
}

kotlin {
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      kotlin.srcDir(createVersionConstantsTask)

      dependencies {
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
      }
    }

    jsTest {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
      }
    }

    jvmTest {
      dependencies {
      }
    }
  }
}

//Ensure tasks are added as dependencies
tasks.findByName("jvmSourcesJar")?.dependsOn("createVersionConstants")
tasks.findByName("compileKotlinMetadata")?.dependsOn("createVersionConstants")
