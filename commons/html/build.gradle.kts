import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """HTML — resource inlining for self-contained documents, Content Security Policy nonce"""


plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  addKotlinTestDependencies(project)
}
