import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
  `java-library`
  kotlinJvm
}

description = "JVM heap-dump support for diagnostics."

dependencies {
  addAnnotationDependencies(project)

  //Test stuff
  addKotlinTestDependencies(project)
}
