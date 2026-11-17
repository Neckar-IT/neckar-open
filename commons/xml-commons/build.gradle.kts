import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
  `java-library`
}

description = "XML helper utilities."

dependencies {
  addAnnotationDependencies(project)

  //Test stuff
  addKotlinTestDependencies(project)
}
