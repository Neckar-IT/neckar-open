import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
  kotlinxSerialization
}

description = "KSP processor that harvests class, property, and annotation specifications from source into a structured model."

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.symbol.processing.api)
  api(project(Projects.open.ksp.kspCommons))
  api(project(Projects.open.ksp.specHarvestModel))

  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.core)

  runtimeOnly(libs.kotlin.compiler.embeddable)

  testImplementation(project(Projects.open.ksp.kspTestUtils))
  testImplementation(project(Projects.open.commons.kotlinLang))
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.kotlinx.serialization.core)
}

