import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Inject"""


plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  api(libs.guice)

  addKotlinTestDependencies(project)
}
