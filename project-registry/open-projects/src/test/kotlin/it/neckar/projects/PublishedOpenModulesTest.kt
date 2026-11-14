package it.neckar.projects

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PublishedOpenModulesTest {
  @TempDir
  lateinit var projectDir: File

  @Test
  fun `a published open library is on Maven Central, an open demo, a pnpm package and a closed library are not`() {
    val open = MonorepoRegistry().open
    val root: Project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    val internal: Project = child(root, "internal")
    val openProject: Project = child(internal, "open")
    val commons: Project = child(openProject, "commons")
    val kotlinLang: Project = child(commons, "kotlin-lang")
    val demo: Project = child(child(openProject, "demos"), "kotlin-js-demo")
    val typescriptUtils: Project = child(child(commons, "typescript"), "typescript-utils")
    val closedLibrary: Project = child(child(child(internal, "closed"), "db"), "mongodb")

    assertThat(open.isPublishedModule(kotlinLang)).isTrue()
    assertThat(open.isPublishedModule(demo)).isFalse()
    assertThat(open.isPublishedModule(typescriptUtils)).isFalse()
    assertThat(open.isPublishedModule(commons)).isFalse()
    assertThat(open.isPublishedModule(closedLibrary)).isFalse()
  }

  @Test
  fun `a build rooted at internal open publishes the same library`() {
    val open = StandaloneRegistry().open
    val root: Project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    val kotlinLang: Project = child(child(root, "commons"), "kotlin-lang")

    assertThat(open.isPublishedModule(kotlinLang)).isTrue()
  }

  @Test
  fun `a project below the open projects the registry does not name fails`() {
    val open = StandaloneRegistry().open
    val root: Project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    val unregistered: Project = child(child(root, "commons"), "unregistered")

    assertFailure { open.isPublishedModule(unregistered) }
      .isInstanceOf<IllegalStateException>()
      .messageContains("Project :commons:unregistered is not registered below :")
  }

  private fun child(parent: Project, name: String): Project = ProjectBuilder.builder().withName(name).withParent(parent).build()
}
