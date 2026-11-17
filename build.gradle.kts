import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import it.neckar.gradle.BuildplatformPlugins
import it.neckar.gradle.ansiConsole
import it.neckar.projects.Projects
import it.neckar.projects.isPublishedModule

val openProjectDirectory: File = projectDir

/** The public read-only mirror of every file git tracks under internal/open; `main` holds the newest release. */
val openMirrorUrl: String = "https://github.com/Neckar-IT/neckar-open"

/** MeisterCharts is licensed under Apache 2.0, every other module under GPLv3 with Classpath Exception: file headers and POMs alike. */
fun Project.isApacheLicensed(): Boolean = projectDir.startsWith(openProjectDirectory.resolve("meistercharts"))

allprojects {

  // The publish boundary is the directory, not a per-module opt-in: everything below this project
  // is exported and depends on nothing outside internal/open. Applied here rather than by each build
  // script, so a new module is fenced the moment it exists.
  apply(plugin = BuildplatformPlugins.OpenModule)

  // Skip meistercharts — has its own Spotless configuration
  if (isApacheLicensed().not()) {
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<SpotlessExtension> {
      isEnforceCheck = false // Don't fail build on formatting violations — nightly housekeeping fixes them automatically
      kotlin {
        target("src/**/*.kt")
        targetExclude("src/**/ksp/sample/**/*.kt")
        licenseHeaderFile(openProjectDirectory.resolve("config/license-headers/gplv3-with-classpath-exception.template"))
      }
    }
  }
}

// Collect published coordinates for the final summary
val publishedArtifacts = mutableListOf<String>()

// The Central Portal deployment id printed by publishToMavenCentral. Captured from the build output
// (see the output listener below) so the summary can hand it straight to poll-deployment.sh instead of
// leaving the reader to grep it out of the log.
val capturedDeploymentId = java.util.concurrent.atomic.AtomicReference<String?>(null)

subprojects {
  if (Projects.open.isPublishedModule(this).not()) {
    return@subprojects
  }

  val project = this
  val mirrorPath: String = projectDir.relativeTo(openProjectDirectory).invariantSeparatorsPath

  apply(plugin = "com.vanniktech.maven.publish")

  // The published modules are Kotlin — their javadoc JARs contain nothing but a manifest and only
  // exist because Maven Central requires the artifact. Modules without any Java source skip the task
  // as NO-SOURCE; a module whose only Java source is a package-private interop helper (meistercharts-fx:
  // GraphicsContextInterop) instead runs javadoc and fails with "No public or protected classes found
  // to document". Same empty JAR either way — do not fail the release over it.
  tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
  }

  // Force signing for all publications including snapshots. The tag pipeline signs with the in-memory key that
  // signAllPublications() reads from signingInMemoryKey; a workstation without it signs through the local GPG agent.
  val signsInMemory: Boolean = providers.gradleProperty("signingInMemoryKey").isPresent
  afterEvaluate {
    extensions.findByType<SigningExtension>()?.apply {
      if (signsInMemory.not()) {
        useGpgCmd()
      }
      isRequired = true
    }
  }

  // Track staged artifacts for the final summary
  tasks.withType<PublishToMavenRepository>().configureEach {
    notCompatibleWithConfigurationCache("adds its coordinates to publishedArtifacts of the build script, which publishingSummary prints")
    doLast {
      val coords = "${publication.groupId}:${publication.artifactId}:${publication.version}"
      synchronized(publishedArtifacts) {
        publishedArtifacts.add(coords)
      }
      val console = ansiConsole
      logger.lifecycle("  📦 Staged ${console.green(coords)}")
    }
  }
  tasks.withType<PublishToMavenLocal>().configureEach {
    notCompatibleWithConfigurationCache("adds its coordinates to publishedArtifacts of the build script, which publishingSummary prints")
    doLast {
      val coords = "${publication.groupId}:${publication.artifactId}:${publication.version}"
      synchronized(publishedArtifacts) {
        publishedArtifacts.add(coords)
      }
      val console = ansiConsole
      logger.lifecycle("  📦 Installed ${console.green(coords)} → ${console.cyan(repositories.mavenLocal().url.toString())}")
    }
  }

  // A publication that declares an artifact no task produces fails when its Gradle metadata is
  // generated - and that happens only when somebody publishes, which here is a manual release. Making
  // `build` generate the metadata moves the failure into every merge request pipeline. Measured free:
  // the artifacts are built by then anyway.
  tasks.named("build") {
    dependsOn(tasks.withType(GenerateModuleMetadata::class.java))
  }

  extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
    publishToMavenCentral()
    signAllPublications()

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
      name.set(project.name)
      // The description is maintained per module via `description = "..."` in its build.gradle.kts.
      // Read lazily so the module's own build script (which runs after this subprojects block) has
      // set it by pom-generation time, and fail the publish rather than shipping a placeholder when
      // it is missing. `verifyPublishedModuleDescriptions` catches the same gap in CI before a publish.
      description.set(project.provider {
        project.description?.takeIf { it.isNotBlank() }
          ?: error(
            "Published module '${project.path}' has no POM description. " +
              "Add `description = \"...\"` (one or two English sentences describing what the library does) to its build.gradle.kts."
          )
      })
      url.set("$openMirrorUrl/tree/main/$mirrorPath")

      licenses {
        if (isApacheLicensed()) {
          license {
            name.set("Apache License 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
          }
        } else {
          license {
            name.set("GPLv3 with Classpath Exception")
            url.set("https://www.gnu.org/software/classpath/license.html")
          }
        }
      }
      developers {
        developer {
          id.set("jschneider")
          name.set("Johannes Schneider")
          email.set("js@neckar.it")
        }
      }

      scm {
        connection.set("scm:git:$openMirrorUrl.git")
        developerConnection.set("scm:git:$openMirrorUrl.git")
        url.set(openMirrorUrl)
      }
    }
  }
}

// Fails if any published module lacks a POM description. Wired into `verifyMergeRequest` (root
// build.gradle.kts) so a new module without a description is caught in CI before a publish, and
// mirrors the fail-on-missing that the pom `description` provider enforces at publish time.
tasks.register("verifyPublishedModuleDescriptions") {
  group = "verification"
  description = "Fails if any published internal/open module lacks a POM description"

  val modulesWithoutDescription: Provider<List<String>> = provider {
    subprojects
      .filter { Projects.open.isPublishedModule(it) }
      .filter { it.description.isNullOrBlank() }
      .map { it.path }
      .sorted()
  }

  doLast {
    val missing: List<String> = modulesWithoutDescription.get()

    if (missing.isNotEmpty()) {
      error(
        buildString {
          appendLine("The following published modules under internal/open have no POM description:")
          missing.forEach { appendLine("  $it") }
          appendLine()
          append("Add `description = \"...\"` (one or two English sentences describing what the library does) to each module's build.gradle.kts.")
        }
      )
    }
  }
}

// Print a final summary after the build completes
val taskNames = gradle.startParameter.taskNames
val isMavenCentral = taskNames.any { it.contains("MavenCentral", ignoreCase = true) }
val isMavenLocal = taskNames.any { it.contains("MavenLocal", ignoreCase = true) }

if (isMavenCentral || isMavenLocal) {
  // Capture the Central Portal deployment id from the build output — whichever task the Vanniktech
  // plugin logs it from — so the summary can print a ready-to-run poll command. The id line
  // ("... deployment id: <uuid>") is not exposed as a task property, so a build-wide output listener is
  // the reliable hook. The publish tasks and publishingSummary are not compatible with the configuration cache.
  if (isMavenCentral) {
    val deploymentIdRegex = Regex("""deployment id:\s*([0-9a-fA-F-]+)""")
    val outputBuffer = StringBuilder()
    val loggingOutput = (gradle as org.gradle.api.internal.GradleInternal).services
      .get(org.gradle.internal.logging.LoggingOutputInternal::class.java)
    loggingOutput.addStandardOutputListener(org.gradle.api.logging.StandardOutputListener { output ->
      outputBuffer.append(output)
      deploymentIdRegex.find(outputBuffer)?.let {
        capturedDeploymentId.set(it.groupValues[1])
        outputBuffer.setLength(0)
      }
      // Bound the buffer so a long build does not accumulate all output while waiting for the id line;
      // keep a small tail so a match split across two chunks is still found.
      if (outputBuffer.length > 4096) outputBuffer.delete(0, outputBuffer.length - 512)
    })
  }

  gradle.projectsEvaluated {
    rootProject.tasks.register("publishingSummary") {
      description = "Prints a summary of all published artifacts"
      notCompatibleWithConfigurationCache("collects the published coordinates across tasks and reads the build output through GradleInternal")
      doLast {
        if (publishedArtifacts.isEmpty()) return@doLast

        val console = ansiConsole
        val version = publishedArtifacts.first().substringAfterLast(':')
        val uniqueArtifacts = publishedArtifacts.distinct().sorted()
        val isSnapshot = version.endsWith("-SNAPSHOT")

        logger.lifecycle("")
        logger.lifecycle("════════════════════════════════════════════════════════════════")
        logger.lifecycle(console.green("  Publishing Summary"))
        logger.lifecycle("════════════════════════════════════════════════════════════════")
        logger.lifecycle("  Version:    ${console.orange(version)}")
        logger.lifecycle("  Artifacts:  ${console.orange("${uniqueArtifacts.size}")}")
        logger.lifecycle("")
        uniqueArtifacts.forEach { coords ->
          logger.lifecycle("    ${console.gray(coords)}")
        }
        logger.lifecycle("")

        if (isMavenLocal) {
          logger.lifecycle(console.green("  ✅ Installed to local Maven repository (~/.m2/repository/)"))
        }

        if (isMavenCentral) {
          if (isSnapshot) {
            logger.lifecycle(console.green("  ✅ Snapshots uploaded to Sonatype Snapshots"))
            logger.lifecycle("")
            logger.lifecycle("  Verify:")
            logger.lifecycle("    ${console.cyan("https://central.sonatype.com/repository/maven-snapshots/it/neckar/open/")}")
          } else {
            logger.lifecycle(console.green("  ✅ Staged on Central Portal (not yet released)"))
            logger.lifecycle("")
            val deploymentId = capturedDeploymentId.get()
            if (deploymentId != null) {
              logger.lifecycle("  Deployment id: ${console.orange(deploymentId)}")
              logger.lifecycle("")
            }
            logger.lifecycle("  Next steps:")
            logger.lifecycle("    1. Release the deployment once it is validated:")
            if (deploymentId != null) {
              logger.lifecycle("       ${console.cyan("tools/bin/nit-maven-central poll --publish $deploymentId")}")
            } else {
              logger.lifecycle("       ${console.cyan("tools/bin/nit-maven-central poll --publish <deployment-id>")}")
              logger.lifecycle("       ${console.gray("(deployment id not captured — copy the 'deployment id:' line from the log above)")}")
            }
            logger.lifecycle("       ${console.gray("The job 📦 Publish open to Maven Central of the tag pipeline runs this for a release tag open-$version.")}")
            logger.lifecycle("    2. Artifacts visible after ~10-30 min:")
            logger.lifecycle("       ${console.cyan("https://central.sonatype.com/search?q=it.neckar.open")}")
          }
        }

        logger.lifecycle("════════════════════════════════════════════════════════════════")
        logger.lifecycle("")
      }
    }

    // Make all publish tasks finalizedBy the summary task
    allprojects {
      tasks.configureEach {
        if (name == "publishToMavenCentral" || name == "publishToMavenLocal") {
          finalizedBy(rootProject.tasks.named("publishingSummary"))
        }
      }
    }
  }
}
