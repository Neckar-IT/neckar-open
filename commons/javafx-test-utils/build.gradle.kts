import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.configureJavaFX

description = """JavaFX Test Utils"""


plugins {
  `java-library`
  kotlinJvm
}

configureJavaFX()

dependencies {
  addAnnotationDependencies(project)

  api(libs.testfx.junit5)
  api(libs.junit.jupiter.api)

  implementation(libs.javafx.graphics)

  runtimeOnly(libs.javafx.controls)

  addKotlinTestDependencies(project)
  api(project(Projects.open.commons.testUtils))
}
