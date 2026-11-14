package it.neckar.projects

import org.gradle.api.Project

/**
 * True for a project below these open projects that Maven Central carries: no parent of subprojects, no intermediate, other or pnpm
 * project, no demo. The publish configuration, `verifyPublishedModuleDescriptions` and `neckarHubPackagesModules` ask it alike.
 */
fun OpenProjects.isPublishedModule(project: Project): Boolean {
  val projectPath = GradleProjectPath(project.path)
  if (path.relativizeOrNull(projectPath) == null) return false
  if (project.subprojects.isNotEmpty()) return false
  val configuredProject = selfAndDescendants.firstOrNull { it.path == projectPath }
    ?: throw IllegalStateException("Project $projectPath is not registered below $path")
  val type = configuredProject.type
  if (type == ProjectType.Intermediate || type == ProjectType.Other || type is ProjectType.Pnpm) return false
  if (project.path.contains(":demos:") || project.name.contains("demo", ignoreCase = true)) return false
  return true
}
