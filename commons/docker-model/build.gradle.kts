description = """Docker model — value classes for Docker image descriptors, tags, digests, manifest references, registries, repositories, volumes"""

plugins {
  kotlinxSerialization
}

dependencies {
  api(libs.kotlinx.serialization.core)
}
