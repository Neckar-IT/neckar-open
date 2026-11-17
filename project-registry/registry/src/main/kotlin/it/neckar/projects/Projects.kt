package it.neckar.projects

/**
 * The registry of a build rooted at `internal/open`: `Projects.open.commons.kotlinLang` is
 * `:commons:kotlin-lang`.
 */
object Projects : ProjectRoot() {
  val open: OpenProjects = OpenProjects(this, GradleProjectPath.Root)
}
