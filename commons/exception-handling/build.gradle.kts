import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Exception Handling stuff"""


plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  api(project(Projects.open.commons.exceptions))
  api(project(Projects.open.commons.logging))

  implementation(libs.slf4j.api)
  implementation(libs.guava)

  addKotlinTestDependencies(project)
  testImplementation(libs.guice)
}
