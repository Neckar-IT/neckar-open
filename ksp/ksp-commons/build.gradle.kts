import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
}

description = "Shared helpers for writing Kotlin Symbol Processing (KSP) processors."

// The released build platform; the root build sets it, the standalone export writes it.
val buildplatformVersion: String by project

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.symbol.processing.api)
  // KspPluginOptions and the types-list model, shared with the KSP conventions of the build platform.
  api("it.neckar.buildplatform:ksp-contract:$buildplatformVersion")

  implementation(project(Projects.open.annotations))

  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.kotlin.compiler.embeddable)

  testImplementation(libs.dev.zacsweers.kctfork.core)
  testImplementation(libs.ksp) //zacsweers

}

