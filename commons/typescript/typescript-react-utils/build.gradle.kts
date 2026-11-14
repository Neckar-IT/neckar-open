import it.neckar.projects.Projects

description = """ common react utilities """

//Ensure the openapi files are generated first
tasks.named("pnpmRunBuild") {
  dependsOn(Projects.open.commons.typescript.typescriptUtils.buildTask)
}
