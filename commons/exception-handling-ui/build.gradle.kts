import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.configureJavaFX

description = """Exception Handling stuff"""

plugins {
  `java-library`
  kotlinJvm
}

configureJavaFX()

dependencies {
  addAnnotationDependencies(project)

  api(project(Projects.open.commons.exceptions))
  api(project(Projects.open.commons.exceptionHandling))

  implementation(project(Projects.open.commons.version))
  implementation(project(Projects.open.units.unit))
  implementation(project(Projects.open.commons.javafx))
  implementation(project(Projects.open.commons.kotlinLang))

  implementation(libs.miglayout.javafx)
  implementation(libs.fontawesomefx)
  implementation(libs.slf4j.api)
  implementation(libs.guava)
  implementation(libs.balloontip)

  implementation(libs.javafx.base)
  implementation(libs.javafx.graphics)

  runtimeOnly(libs.controlsfx)

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.testfx.junit5)
  testImplementation(libs.testfx.core)
  testImplementation(libs.controlsfx)
}
