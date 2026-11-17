import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.configureJavaFX

description = """JavaFX 3D"""


plugins {
  `java-library`
  kotlinJvm
}

configureJavaFX()

dependencies {
  addAnnotationDependencies(project)

  api(libs.fxyz3d)
  api(libs.javafx.graphics)

  implementation(project(Projects.open.units.unit))
  implementation(project(Projects.open.commons.kotlinLang))
  implementation(libs.guava)
  implementation(libs.slf4j.api)
  implementation(libs.javafx.base)

  runtimeOnly(libs.javafx.controls)

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.javafx))
  testImplementation(libs.javafx.base)
  testImplementation(libs.javafx.controls)
  testImplementation(libs.javafx.graphics)
  testRuntimeOnly(libs.controlsfx)
  testRuntimeOnly(libs.jfxtras.all)
}
