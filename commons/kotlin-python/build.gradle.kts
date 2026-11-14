import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Kotlin Python runner"""

plugins {
  kotlinJvm
  kotlinxSerialization
}

dependencies {
  addAnnotationDependencies(project)

  api(libs.kotlinx.serialization.core)
  api(libs.kotlinx.serialization.json)

  implementation(project(Projects.open.commons.kotlinLang))
  implementation(project(Projects.open.units.unit))
  implementation(project(Projects.open.commons.logging))
  implementation(libs.slf4j.api)

  runtimeOnly(libs.logback.classic)

  addKotlinTestDependencies(project)
}

