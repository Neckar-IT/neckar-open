import it.neckar.gradle.declaresOrvalCodegen
import it.neckar.gradle.openapi.openapiSpecTask
import it.neckar.projects.Projects
import it.neckar.projects.project
import it.neckar.projects.sharing.OpenSharedFiles
import it.neckar.projects.sharing.resolvedFiles

description = """ common typescript utilities """

// The Vitest of `src/payment-formats` reads the corpora of the Kotlin value classes it mirrors.
val paymentFormatCases by resolvedFiles(OpenSharedFiles.PaymentFormatCases)

dependencies {
  paymentFormatCases(project(Projects.open.customer))
  paymentFormatCases(project(Projects.open.commons.payment))
}

tasks.named("pnpmRunBuild") {
  inputs.files(paymentFormatCases).withPropertyName("paymentFormatCases").withPathSensitivity(PathSensitivity.NAME_ONLY)
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
