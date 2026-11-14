import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
plugins {
}

description = "KSP processor that verifies value classes are not boxed at annotated call sites."

dependencies {
  addAnnotationDependencies(project)

  api(libs.symbol.processing.api)

  implementation(project(Projects.open.ksp.kspCommons))
  implementation(project(Projects.open.annotations))

  addKotlinTestDependencies(project)
}
