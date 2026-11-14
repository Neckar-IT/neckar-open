import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
plugins {
}

description = """BSON utilities"""

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.bson.kotlinx)
  api(project(Projects.open.commons.uuid))
}
