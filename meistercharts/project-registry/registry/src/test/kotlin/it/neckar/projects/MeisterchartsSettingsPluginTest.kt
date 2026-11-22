package it.neckar.projects

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.gradle.mavenGroupOf
import it.neckar.projects.MeisterchartsSettingsPlugin.Companion.useMonorepoGroup
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MeisterchartsSettingsPluginTest {
  @TempDir
  lateinit var rootDir: File

  @Test
  fun `every project of the export has the group the monorepo gives it`() {
    val root = ProjectBuilder.builder().withProjectDir(rootDir).build()
    val history = ProjectBuilder.builder().withName("meistercharts-history").withParent(root).build()
    val historyCore = ProjectBuilder.builder().withName("meistercharts-history-core").withParent(history).build()

    historyCore.useMonorepoGroup()

    assertThat(historyCore.group).isEqualTo(mavenGroupOf(listOf(), ":internal:open:meistercharts:meistercharts-history:meistercharts-history-core"))
  }

  @Test
  fun `the registry of the export names each MeisterCharts project below its root`() {
    assertThat(Projects.open.meistercharts.meisterchartsHistory.meisterchartsHistoryCore.path)
      .isEqualTo(GradleProjectPath(":meistercharts-history:meistercharts-history-core"))
  }

  @Test
  fun `the registry of the export names every other open project by its path in the monorepo`() {
    assertThat(Projects.open.commons.kotlinLang.path).isEqualTo(GradleProjectPath(":internal:open:commons:kotlin-lang"))
  }
}
