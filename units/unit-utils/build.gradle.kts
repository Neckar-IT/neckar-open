import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Unit Utils"""

plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)
  api(project(Projects.open.units.unit))

  implementation(project(Projects.open.commons.kotlinLang))

  addKotlinTestDependencies(project)
}
