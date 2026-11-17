import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
  kotlinxSerialization
}

description = "KSP processor that generates TypeScript declaration files from Kotlin types."

// The released build platform; the root build sets it, the standalone export writes it.
val buildplatformVersion: String by project

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.symbol.processing.api)
  api(libs.kotlinx.serialization.core)

  implementation(project(Projects.open.ksp.kspCommons))
  implementation(project(Projects.open.annotations))
  implementation("it.neckar.buildplatform:generated-file-marker:$buildplatformVersion")
  implementation(libs.kotlinx.serialization.json)

  runtimeOnly(libs.kotlin.compiler.embeddable)

  testImplementation(project(Projects.open.ksp.kspTestUtils))
  testImplementation(project(Projects.open.commons.kotlinLang))
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(project(Projects.open.annotations))
  testImplementation(libs.dev.zacsweers.kctfork.core)
  testImplementation(libs.ksp)
  testImplementation(libs.kotlin.compiler.embeddable)
  testImplementation(libs.kotlinx.serialization.core)
}

