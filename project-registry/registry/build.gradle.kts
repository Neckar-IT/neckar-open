// The registry of a build rooted at internal/open, and the settings plugin of that build. The monorepo declares its own
// `Projects` in build-logic and never puts this module on a classpath.
plugins {
  `kotlin-dsl`
}

dependencies {
  api(project(":open-projects"))
  // The settings classpath is the parent of every build script classpath: the build scripts under internal/open apply the
  // convention plugins, Spotless and the Maven publishing plugin by id, without a version.
  api("it.neckar.buildplatform:conventions")
  api("com.diffplug.spotless:spotless-plugin-gradle:${libs.plugins.com.diffplug.spotless.get().version.requiredVersion}")
  api(libs.maven.publish.plugin)
}

gradlePlugin {
  plugins {
    register("OpenSettingsPlugin") {
      id = "it.neckar.open.settings"
      implementationClass = "it.neckar.projects.OpenSettingsPlugin"
    }
  }
}
