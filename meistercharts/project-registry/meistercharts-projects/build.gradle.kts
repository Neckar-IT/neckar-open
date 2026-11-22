plugins {
  `kotlin-dsl`
}

description = "The registry of the MeisterCharts projects: their Gradle paths, project types, roles and Kotlin targets."

dependencies {
  api("it.neckar.buildplatform:projects")
}
