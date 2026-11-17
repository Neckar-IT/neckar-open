import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = """Playwright Support"""


dependencies {
  addAnnotationDependencies(project)
  addKotlinDependencies()

  api(libs.playwright)
  api(project(Projects.open.commons.disposable))
  api(project(Projects.open.commons.docker))
  api(project(Projects.open.commons.geometry))
  api(project(Projects.open.commons.kotlinLang))
  api(libs.image.comparison)
  api(libs.kotlinx.html)

  api(project(Projects.open.commons.uuid))

  implementation(project(Projects.open.commons.concurrent))
  implementation(project(Projects.open.commons.logging))
  implementation(project(Projects.open.commons.datetimeMinimal))
  implementation(project(Projects.open.commons.i18n))
  implementation(project(Projects.open.commons.kotlinCollections))
  implementation(libs.guava)
  implementation(libs.slf4j.api)

  addKotlinTestDependencies(project)
}
