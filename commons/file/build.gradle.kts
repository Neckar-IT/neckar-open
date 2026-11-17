import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """File related stuff"""


plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  api(project(Projects.open.commons.version))
  implementation(project(Projects.open.commons.kotlinLang))

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(libs.commons.io)
}
