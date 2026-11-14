import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.gradle.commandlinetool.CommandLineToolArtifact
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """nit-container: writes a container from the stdout of shell commands and lists or extracts its entries, as linuxX64 executable"""

plugins {
  kotlinxSerialization
  commandLineTool
}

commandLineTool {
  name = "nit-container"
  artifact = CommandLineToolArtifact.KotlinNativeExecutable
}

kotlin {
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  linuxX64 {
    binaries {
      executable {
        entryPoint = "it.neckar.open.container.cli.main"
        baseName = "nit-container"
      }
    }
  }

  sourceSets {
    val linuxX64Main by getting {
      dependencies {
        implementation(project(Projects.open.commons.container))
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
