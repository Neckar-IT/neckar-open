import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
}

description = "ZIP archive creation and extraction utilities."

dependencies {
  addKotlinTestDependencies(project)
  addAnnotationDependencies(project)

  api(libs.commons.compress)

  testImplementation(project(Projects.open.commons.kotlinLang))
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.commons.io)
}
