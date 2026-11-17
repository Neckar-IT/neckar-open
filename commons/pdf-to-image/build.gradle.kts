import it.neckar.gradle.addKotlinTestDependencies

description = """Renders pages of a PDF as images"""

plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  api(libs.kotlinx.coroutines.core)

  implementation(libs.pdfbox)

  addKotlinTestDependencies(project)
  testImplementation(libs.kotlinx.coroutines.test)
}
