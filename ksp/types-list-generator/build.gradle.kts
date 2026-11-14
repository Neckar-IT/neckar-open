import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
}

description = "KSP processor that generates a runtime list of annotated Kotlin types."

// The released build platform; the root build sets it, the standalone export writes it.
val buildplatformVersion: String by project

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.symbol.processing.api)
  api(project(Projects.open.ksp.kspCommons))

  implementation("it.neckar.buildplatform:generated-file-marker:$buildplatformVersion")

  runtimeOnly(libs.kotlin.compiler.embeddable)

  testImplementation(project(Projects.open.ksp.kspTestUtils))
  testImplementation(project(Projects.open.commons.kotlinLang))
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.kotlinx.serialization.core)
}

