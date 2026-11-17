import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Kotlinx Serialization serializers"""


plugins {
  `java-library`
  kotlinJvm
  kotlinxSerialization
}

val protobufFeatureName: String = "protobuf"

// Create dedicated source set for protobuf feature (fixes Gradle 10 deprecation warning)
val protobufSourceSet: SourceSet = sourceSets.create(protobufFeatureName) {
  compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
  runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}

java {
  registerFeature(protobufFeatureName) {
    usingSourceSet(protobufSourceSet)
  }
}

dependencies {
  addAnnotationDependencies(project)

  api(project(Projects.open.commons.kotlinxSerializers))
  api(project(Projects.open.commons.kotlinLang))
  api(libs.kotlinx.serialization.json)

  api(libs.jackson.databind)
  api(libs.kotlinx.serialization.core)
  api(libs.bson)

  implementation(project(Projects.open.commons.bson))
  implementation(project(Projects.open.commons.testUtils))
  implementation(libs.bson.kotlinx)
  implementation(libs.commons.io)

  addKotlinTestDependencies(project)
  testImplementation(libs.kotlinx.serialization.core)
  testImplementation(libs.bson)
  testImplementation(libs.kotest.property.jvm)

  // Protobuf source set dependencies
  "${protobufFeatureName}Api"(libs.kotlinx.serialization.protobuf)
}
