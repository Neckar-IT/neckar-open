package it.neckar.projects

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OpenProjectsTest {
  @TempDir
  lateinit var projectDir: File

  private fun subProject(parent: Project, name: String): Project =
    ProjectBuilder.builder().withName(name).withParent(parent).build()

  @Test
  fun `the monorepo resolves an entry below internal open`() {
    val registry = MonorepoRegistry()
    val root = ProjectBuilder.builder().withProjectDir(projectDir).build()
    val kotlinLang = subProject(subProject(subProject(subProject(root, "internal"), "open"), "commons"), "kotlin-lang")

    assertThat(registry.open.commons.kotlinLang.path).isEqualTo(GradleProjectPath(":internal:open:commons:kotlin-lang"))
    assertThat(root.project(registry.open.commons.kotlinLang)).isEqualTo(kotlinLang)
    assertThat(registry.findOrNull(kotlinLang)).isEqualTo(registry.open.commons.kotlinLang)
  }

  @Test
  fun `the monorepo resolves a MeisterCharts entry below internal open meistercharts`() {
    assertThat(MonorepoRegistry().open.meistercharts.meisterchartsHistory.meisterchartsHistoryCore.path)
      .isEqualTo(GradleProjectPath(":internal:open:meistercharts:meistercharts-history:meistercharts-history-core"))
  }

  @Test
  fun `a build rooted at internal open resolves the entry below its root`() {
    val registry = StandaloneRegistry()
    val root = ProjectBuilder.builder().withProjectDir(projectDir).build()
    val kotlinLang = subProject(subProject(root, "commons"), "kotlin-lang")

    assertThat(registry.open.commons.kotlinLang.path).isEqualTo(GradleProjectPath(":commons:kotlin-lang"))
    assertThat(root.project(registry.open.commons.kotlinLang)).isEqualTo(kotlinLang)
    assertThat(registry.findOrNull(kotlinLang)).isEqualTo(registry.open.commons.kotlinLang)
  }

  @Test
  fun `the export of MeisterCharts resolves a MeisterCharts entry below its root and an open entry below internal open`() {
    val registry = MeisterchartsExportRegistry()

    assertThat(registry.open.meistercharts.meisterchartsCore.path).isEqualTo(GradleProjectPath(":meistercharts-core"))
    assertThat(registry.open.meistercharts.path).isEqualTo(GradleProjectPath.Root)
    assertThat(registry.open.annotations.path).isEqualTo(GradleProjectPath(":internal:open:annotations"))
    assertThat(registry.findOrNull(":meistercharts-core")).isEqualTo(registry.open.meistercharts.meisterchartsCore)
    assertThat(registry.accessorNameViolations()).isEmpty()
  }

  @Test
  fun `every property is named after its directory`() {
    assertThat(StandaloneRegistry().accessorNameViolations()).isEmpty()
  }
}

internal class MonorepoRegistry : ProjectRoot() {
  val open: OpenProjects = OpenProjects(this, GradleProjectPath(":internal:open"))
}

internal class StandaloneRegistry : ProjectRoot() {
  val open: OpenProjects = OpenProjects(this, GradleProjectPath.Root)
}

internal class MeisterchartsExportRegistry : ProjectRoot() {
  val open: OpenProjects = OpenProjects(this, GradleProjectPath(":internal:open"), meisterchartsPath = GradleProjectPath.Root)
}
