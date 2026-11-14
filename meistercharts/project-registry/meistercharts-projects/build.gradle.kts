plugins {
  `kotlin-dsl`
}

dependencies {
  api("it.neckar.buildplatform:projects")

  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(libs.assertk.jvm)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform {
    includeEngines("junit-jupiter")
  }

  filter {
    includeTestsMatching("*Test")
    isFailOnNoMatchingTests = true
  }
}
