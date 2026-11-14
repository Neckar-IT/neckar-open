import it.neckar.projects.project

import it.neckar.gradle.ksp.generating.ts.generateTypeScriptDefinitions

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKtorServerDependencies

description = """REST"""


plugins {
  kotlinxSerialization
  kspSerialization
  generateTypesList
  generateTsDeclaration
  openapiGenerationConfig
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)
  addKotlinDependencies(project)
  addKtorServerDependencies(project, Scope.Main)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))
        api(project(Projects.open.commons.adjustable))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.multiTenancy))
        api(project(Projects.open.commons.kotlinxSerializers))

        api(libs.kotlinx.serialization.core)
        api(libs.ktor.http)

        api(project(Projects.open.commons.uuid))
        api(project(Projects.open.commons.kotlinJs))
        api(project(Projects.open.contact))
        api(project(Projects.open.commons.exceptions))
        api(project(Projects.open.commons.version))
        api(project(Projects.open.commons.multiTenancy))

        api(project(Projects.open.versionInfo))
        api(project(Projects.open.rest.modelHealth))
        api(project(Projects.open.rest.modelPaging))
      }
    }

    commonTest {
      dependencies {
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
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
        implementation(project(Projects.open.rest.openapiGenerator))
        implementation(project(Projects.open.commons.applicationError))
        implementation(project(Projects.open.commons.featureFlags))
        implementation(project(Projects.open.commons.money))
      }
    }
  }
}

generateTypeScriptDefinitions {
}

