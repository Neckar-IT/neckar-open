package it.neckar.projects

import it.neckar.gradle.NeckarBasePlugin
import it.neckar.gradle.NeckarOpenVersionProperty
import it.neckar.gradle.ProjectRegistration
import it.neckar.gradle.configureNodeJsRootExtension
import it.neckar.gradle.configureRegisteredKotlinModules
import it.neckar.gradle.fence.forbidDependenciesOnFrontendProjects
import it.neckar.gradle.limitParallelNodeTasksAndKotlinJsLinks
import it.neckar.gradle.mavenGroupOf
import it.neckar.gradle.pnpm.PnpmModuleSettings
import it.neckar.gradle.pnpm.configurePnpmModule
import it.neckar.gradle.pnpm.configurePnpmToolchainOwner
import it.neckar.gradle.substituteNeckarCoordinatesWithSourceProjects
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Applied in `internal/open/settings.gradle.kts`, which the export of `internal/open` alone reads. Sets for every project what
 * the monorepo sets for the projects under `internal/open`:
 * - [NeckarBasePlugin], the group `it.neckar.open…` and the version `neckarOpenVersion` of `gradle.properties`
 * - the sources of this build for its own coordinates
 * - the Node.js of the Kotlin/JS targets and the limits of the node tasks and Kotlin/JS links
 * - the conventions of each Kotlin and pnpm module the settings include, and the fence against a dependency on a frontend project
 */
class OpenSettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    val neckarOpenVersion: String = settings.providers.gradleProperty(NeckarOpenVersionProperty).get()
    settings.gradle.allprojects {
      pluginManager.apply(NeckarBasePlugin::class.java)
      group = mavenGroupOf(listOf("open"), path)
      version = neckarOpenVersion
      // The conventions add the Detekt rules and the KSP processors as it.neckar.open coordinates, which this build produces itself.
      substituteNeckarCoordinatesWithSourceProjects()
    }

    settings.gradle.rootProject {
      configureNodeJsRootExtension()
      limitParallelNodeTasksAndKotlinJsLinks()

      // After the root project: the conventions read its version catalog, which a project has once its evaluation starts. Gradle evaluates
      // the root project before its subprojects, so their build scripts still see the Kotlin extensions.
      afterEvaluate {
        // Read once the include lines of the settings ran: the registry also lists the modules the export leaves out, such as MeisterCharts.
        val includedKotlinModules: List<ConfiguredProject> = Projects.open.selfAndDescendants
          .filter { it.type in kotlinModuleTypes }
          .filter { settings.findProject(it.path.path) != null }

        configureRegisteredKotlinModules(
          baseProject = this,
          projects = includedKotlinModules,
          kspProcessorPathPrefix = Projects.open.ksp.path.path,
          allowedKspDependencyPaths = Projects.open.allowedDependenciesForKspProcessingProjects.map { it.path.path }.toSet(),
          registrationOf = { configuredProject -> ProjectRegistration(file = OpenProjects.RegistryFile, accessor = accessorOf(configuredProject.path)) },
        )

        val frontendModulePaths: List<String> = Projects.open.selfAndDescendants
          .filter { it.role == ProjectRole.Frontend }
          .filter { settings.findProject(it.path.path) != null }
          .map { it.path.path }
        includedKotlinModules.forEach { module -> project(module.path.path).forbidDependenciesOnFrontendProjects(frontendModulePaths) }

        val includedPnpmModules: List<GradleProjectPath> = Projects.open.selfAndDescendants
          .filter { it.type is ProjectType.Pnpm }
          .filter { settings.findProject(it.path.path) != null }
          .map { it.path }

        configurePnpmToolchainOwner()
        includedPnpmModules.forEach { modulePath ->
          project(modulePath.path).configurePnpmModule(includedPnpmModules, ExportPnpmModules)
        }
      }
    }
  }

  private companion object {
    /** The shared TypeScript configs of the export are the packages below `:config:typescript`. */
    val ExportPnpmModules: PnpmModuleSettings = PnpmModuleSettings(
      sharedTypescriptConfigDirectory = { project -> project.rootProject.project(Projects.open.config.typescript.path.path).projectDir },
    )

    val kotlinModuleTypes: Set<ProjectType> = setOf(ProjectType.KotlinMultiplatform, ProjectType.KotlinJvm, ProjectType.KspProcessor)

    /** `:commons:kotlin-lang` → `Projects.open.commons.kotlinLang`. */
    fun accessorOf(path: GradleProjectPath): String {
      val directories: List<String> = path.path.split(':').filter { it.isNotEmpty() }
      return (listOf("Projects", "open") + directories.map { accessorName(it) }).joinToString(".")
    }
  }
}
