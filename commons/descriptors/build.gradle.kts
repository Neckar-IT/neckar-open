description = """Descriptors — the value vocabulary of deployments, services and ports and the runtime context"""

plugins {
  kotlinxSerialization
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
