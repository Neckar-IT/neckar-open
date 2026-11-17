import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = """Logging"""

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
      }
    }

    commonTest {
      dependencies {
      }
    }

    webMain {
      dependencies {
        implementation(libs.kotlinx.browser)
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
        implementation(libs.kotlin.reflect)

        api(libs.slf4j.api)
        api(libs.logback.classic)
        api(libs.loki.logback.appender)
        api(libs.logstash.logback.encoder)
      }
    }

    jvmTest {
      dependencies {
      }
    }
  }
}
