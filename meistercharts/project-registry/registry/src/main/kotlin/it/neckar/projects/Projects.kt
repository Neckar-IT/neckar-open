package it.neckar.projects

/**
 * The registry of a build rooted at `internal/open/meistercharts`, with the paths of the monorepo for every open project outside
 * MeisterCharts: `Projects.open.meistercharts.meisterchartsCore` is `:meistercharts-core`, `Projects.open.annotations` is
 * `:internal:open:annotations`, which the build lacks and `project(ConfiguredProject)` resolves as `it.neckar.open:annotations`.
 */
object Projects : ProjectRoot() {
  val open: OpenProjects = OpenProjects(this, GradleProjectPath(":internal:open"), meisterchartsPath = GradleProjectPath.Root)
}
