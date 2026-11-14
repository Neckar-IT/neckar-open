import it.neckar.projects.project

import org.openjfx.gradle.JavaFXModule

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.configureJavaFX

description = """JavaFX"""

plugins {
  `java-library`
  kotlinJvm
}

configureJavaFX(modules = listOf(JavaFXModule.CONTROLS, JavaFXModule.SWING))

dependencies {
  addAnnotationDependencies(project)

  api(project(Projects.open.commons.concurrent))
  api(project(Projects.open.commons.disposable))
  api(project(Projects.open.commons.observable))
  api(project(Projects.open.commons.provider))
  api(project(Projects.open.commons.i18n))
  api(libs.kotlinx.coroutines.core)

  api(libs.javafx.base)
  api(libs.javafx.graphics)

  implementation(project(Projects.open.commons.kotlinCollections))
  implementation(project(Projects.open.commons.logging))
  implementation(project(Projects.open.commons.time))
  implementation(project(Projects.open.units.unit))
  implementation(project(Projects.open.commons.kotlinLang))
  implementation(libs.slf4j.api)
  implementation(libs.guava)
  implementation(libs.commons.lang3)

  runtimeOnly(libs.kotlinx.coroutines.javafx)

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.javafxTestUtils))
  testImplementation(libs.miglayout.javafx)
  testImplementation(libs.jfxtras.all)
  testImplementation(libs.fxgraphics2d)
  testImplementation(libs.jfoenix)
  testImplementation(libs.fontawesomefx)
  testImplementation(libs.controlsfx)
  testImplementation(libs.vworkflows.fx)
  testImplementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.kotlinx.coroutines.javafx)
  testImplementation(libs.miglayout.core)
  testImplementation(libs.vworkflows.core)
  testImplementation(libs.jfxtras.common)
  testImplementation(libs.javafx.base)
  testImplementation(libs.javafx.graphics)
  testImplementation(project(Projects.open.commons.kotlinLang))
}
