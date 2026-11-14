package it.neckar.projects

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class MeisterchartsProjectsTest {
  @Test
  fun `the monorepo resolves an entry below internal open meistercharts`() {
    assertThat(MonorepoRegistry().meistercharts.meisterchartsHistory.meisterchartsHistoryCore.path)
      .isEqualTo(GradleProjectPath(":internal:open:meistercharts:meistercharts-history:meistercharts-history-core"))
  }

  @Test
  fun `the export of internal open meistercharts resolves the entry below its root`() {
    assertThat(StandaloneRegistry().meistercharts.meisterchartsHistory.meisterchartsHistoryCore.path)
      .isEqualTo(GradleProjectPath(":meistercharts-history:meistercharts-history-core"))
  }

  @Test
  fun `every property is named after its directory`() {
    assertThat(StandaloneRegistry().accessorNameViolations()).isEmpty()
  }
}

internal class MonorepoRegistry : ProjectRoot() {
  val meistercharts: MeisterchartsProjects = MeisterchartsProjects(this, GradleProjectPath(":internal:open:meistercharts"))
}

internal class StandaloneRegistry : ProjectRoot() {
  val meistercharts: MeisterchartsProjects = MeisterchartsProjects(this, GradleProjectPath.Root)
}
