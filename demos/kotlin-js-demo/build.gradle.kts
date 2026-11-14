import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

import it.neckar.gradle.JsTargetType
import it.neckar.gradle.JsTestMode
import it.neckar.gradle.WebpackModuleType
import it.neckar.gradle.executableJsApplication

plugins {
  kotlinMultiPlatform
  kotlinxSerialization
}

kotlin {
  applyDefaultHierarchyTemplate()

  js {
    executableJsApplication(
      varName = "kotlinJsDemo",
      jsTargetType = JsTargetType.ES2015,
      webpackModuleType = WebpackModuleType.ModernModule,
      testMode = JsTestMode.NodeJs
    )
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.serialization.json)
        //implementation(libs.kotlinx.serialization.json)
      }
    }
    val jsMain by getting {
      dependencies {
        //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:_")
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }

}

tasks.withType<KotlinJsCompile>().configureEach {
  compilerOptions {
    // https://kotlinlang.org/docs/js-project-setup.html#support-for-es2015-features
    target = "es2015"
    useEsClasses = true
  }
}
