import it.neckar.gradle.declaresOrvalCodegen
import it.neckar.gradle.openapi.openapiSpecTask
import it.neckar.projects.Projects

description = """ common typescript utilities """


tasks.named("pnpmRunBuild") {
  dependsOn(Projects.open.rest.model.openapiSpecTask)
  dependsOn(Projects.open.authRestApi.openapiSpecTask)
  dependsOn(Projects.open.businessDomain.openapiSpecTask)
  dependsOn(Projects.open.customer.openapiSpecTask)
  // `openapi-merge.sh` joins all four specs before Orval runs, so all four are inputs. The Orval
  // frontends reach these types through this package's `dist` — a chain that only carries if this
  // package itself is invalidated.
  declaresOrvalCodegen(
    Projects.open.rest.model.path.path,
    Projects.open.authRestApi.path.path,
    Projects.open.businessDomain.path.path,
    Projects.open.customer.path.path,
  )
}
