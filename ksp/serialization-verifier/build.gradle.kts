import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

plugins {
}

description = "KSP processor that verifies Kotlinx Serialization is configured correctly for annotated types."

dependencies {
  addAnnotationDependencies(project)

  api(libs.symbol.processing.api)

  implementation(project(Projects.open.ksp.kspCommons))
  implementation(libs.kotlinx.serialization.core)

  addKotlinTestDependencies(project)

  testImplementation(project(Projects.open.commons.domain))
  testImplementation(libs.dev.zacsweers.kctfork.core)
  testImplementation(libs.ksp)
  testImplementation(libs.kotlin.compiler.embeddable)
}
