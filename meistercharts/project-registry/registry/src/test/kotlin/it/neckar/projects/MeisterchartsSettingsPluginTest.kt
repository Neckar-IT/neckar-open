package it.neckar.projects

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import it.neckar.gradle.mavenGroupOf
import it.neckar.gradle.substituteNeckarCoordinatesWithSourceProjects
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MeisterchartsSettingsPluginTest {
  @TempDir
  lateinit var rootDir: File

  @Test
  fun `every project of the export has the group the monorepo gives it`() {
    assertThat(mavenGroupOf(MeisterchartsSettingsPlugin.GroupRootSegments, ":meistercharts-core")).isEqualTo(MeisterchartsSettingsPlugin.MeisterchartsGroup)
    assertThat(mavenGroupOf(MeisterchartsSettingsPlugin.GroupRootSegments, ":meistercharts-history:meistercharts-history-core")).isEqualTo(MeisterchartsSettingsPlugin.MeisterchartsGroup)
    assertThat(mavenGroupOf(listOf(), ":internal:open:meistercharts:meistercharts-history:meistercharts-history-core")).isEqualTo(MeisterchartsSettingsPlugin.MeisterchartsGroup)
  }

  @Test
  fun `a MeisterCharts coordinate resolves to the project of this build`() {
    val consumer = consumerOf(rootProject())

    consumer.dependencies.add("implementation", "it.neckar.open.meistercharts:meistercharts-core")

    assertThat(compileClasspathComponents(consumer)).contains("project ':meistercharts-core'")
  }

  @Test
  fun `an open coordinate resolves from the repositories`() {
    val consumer = consumerOf(rootProject())

    consumer.dependencies.add("implementation", "it.neckar.open:kotlin-lang:10.2.0")

    assertThat(unresolvedCoordinates(consumer)).containsExactly("it.neckar.open:kotlin-lang:10.2.0")
  }

  @Test
  fun `the accessor names the registry entry`() {
    assertThat(MeisterchartsSettingsPlugin.accessorOf(Projects.meistercharts.meisterchartsHistory.meisterchartsHistoryCore.path))
      .isEqualTo("Projects.meistercharts.meisterchartsHistory.meisterchartsHistoryCore")
  }

  private fun rootProject(): Project = ProjectBuilder.builder().withProjectDir(rootDir).build()

  /** `:meistercharts-canvas` next to its sibling `:meistercharts-core`, both with the group of the export. */
  private fun consumerOf(root: Project): Project {
    ProjectBuilder.builder().withName("meistercharts-core").withParent(root).build().also {
      it.group = MeisterchartsSettingsPlugin.MeisterchartsGroup
      it.plugins.apply("java-library")
    }
    return ProjectBuilder.builder().withName("meistercharts-canvas").withParent(root).build().also {
      it.group = MeisterchartsSettingsPlugin.MeisterchartsGroup
      it.plugins.apply("java-library")
      it.substituteNeckarCoordinatesWithSourceProjects { group -> group == MeisterchartsSettingsPlugin.MeisterchartsGroup }
    }
  }

  private fun compileClasspathComponents(consumer: Project): List<String> {
    return consumer.configurations.getByName("compileClasspath")
      .incoming.resolutionResult.allComponents
      .map(ResolvedComponentResult::getId)
      .map { it.displayName }
  }

  /** The coordinates the resolution left alone: this test declares no repository. */
  private fun unresolvedCoordinates(consumer: Project): List<String> {
    return consumer.configurations.getByName("compileClasspath")
      .incoming.resolutionResult.allDependencies
      .filterIsInstance<UnresolvedDependencyResult>()
      .map { it.requested.displayName }
  }
}
