import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Jackson extensions"""

plugins {
}

dependencies {
  addAnnotationDependencies(project)

  implementation(project(Projects.open.commons.kotlinLang))
  implementation(project(Projects.open.commons.i18n))

  api(libs.jackson.databind)
  api(libs.jackson.core)

  implementation(project(Projects.open.units.unit))

  runtimeOnly(libs.jackson.module.kotlin)
  runtimeOnly(libs.jackson.module.parameter.names)
  runtimeOnly(libs.jackson.datatype.jsr310)
  runtimeOnly(libs.jackson.datatype.jdk8)

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.jackson.module.kotlin)
  testImplementation(libs.jackson.module.parameter.names)
  testImplementation(libs.jackson.datatype.jsr310)
  testImplementation(libs.jackson.datatype.jdk8)
  testImplementation(project(Projects.open.units.unit))
}
