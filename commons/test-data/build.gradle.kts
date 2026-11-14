import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = "Reusable sample data fixtures shared across tests."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))

        api(project(Projects.open.commons.resourceLoading))
        api(libs.kotlinx.coroutines.core)
      }
    }

    commonTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
      }
    }

    jsMain {
      dependencies {
      }
    }

    jsTest {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.pdfbox)
      }
    }
  }
}

tasks.register<JavaExec>("writeSamplePdfs") {
  description = "Rewrites the committed sample PDFs in sample-pdfs"
  group = "test data"
  val mainCompilation = kotlin.jvm().compilations.getByName("main")
  classpath = files(mainCompilation.output.allOutputs, mainCompilation.runtimeDependencyFiles)
  mainClass.set("it.neckar.test.data.WriteSamplePdfsKt")
  val outputDirectory = layout.projectDirectory.dir("sample-pdfs")
  argumentProviders.add(CommandLineArgumentProvider { listOf(outputDirectory.asFile.absolutePath) })
}
