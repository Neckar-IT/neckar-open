/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Flags a test that locates the repository by its own position: a root-walking helper, `System.getProperty("user.dir")`,
 * `rootDir.parentFile`, `File("")` and a `..` literal handed to a path constructor or `resolve`. A test reads its classpath or
 * what `SharedFiles` hands its task. PSI-based; `internal/buildplatform/config/detekt/detekt.yml` applies it to test sources.
 */
class RepositoryReadInTest(config: Config) : Rule(
  config,
  description = "A test reads its classpath or the files SharedFiles hands its task, never a file located by walking up from its working directory.",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    val callee: String = expression.calleeExpression?.text ?: return
    val arguments: List<String?> = expression.valueArguments.map { (it.getArgumentExpression() as? KtStringTemplateExpression)?.literalValue() }

    when {
      callee in RootWalkerNames -> report(expression, "`$callee()` walks up to the repository root.")
      callee == "getProperty" && expression.isQualifiedBy("System") && arguments.firstOrNull() == "user.dir" ->
        report(expression, "`System.getProperty(\"user.dir\")` is the working directory Gradle runs the test in.")
      expression.isPathConstructor(callee) && arguments.size == 1 && arguments.single() in WorkingDirectoryLiterals ->
        report(expression, "`${expression.qualifiedCallee()}(\"${arguments.single()}\")` is the working directory Gradle runs the test in.")
      (expression.isPathConstructor(callee) || callee in ResolveNames) && arguments.any { it != null && (it == ".." || it.startsWith("../")) } ->
        report(expression, "`${expression.qualifiedCallee()}(\"../…\")` addresses a file by the working directory Gradle runs the test in.")
    }
  }

  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    super.visitQualifiedExpression(expression)
    if (expression.selectorExpression?.text == "parentFile" && expression.receiverExpression.lastNameOrNull() in DirectoryReceivers) {
      report(expression, "`${expression.text}` walks up from the module directory.")
    }
  }

  /** `File(…)`, `Path(…)`, `Path.of(…)` and `Paths.get(…)`; a nested `Path` of another type and an `of` or `get` of another receiver build no path. */
  private fun KtCallExpression.isPathConstructor(callee: String): Boolean = when (callee) {
    "File", "Path" -> qualifierOrNull()?.first()?.isLowerCase() ?: true
    "of", "get" -> isQualifiedBy("Path") || isQualifiedBy("Paths")
    else -> false
  }

  /** The name in front of the call, `Paths` for `java.nio.file.Paths.get(…)` and `io` for `java.io.File(…)`; null without a receiver or behind a call. */
  private fun KtCallExpression.qualifierOrNull(): String? {
    val qualified = parent as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression != this) return null
    return qualified.receiverExpression.lastNameOrNull()
  }

  /** The last name of a reference, `rootDir` for `project.rootDir` and `rootDir!!`; null for a call or a literal. */
  private fun KtExpression.lastNameOrNull(): String? = when (this) {
    is KtQualifiedExpression -> selectorExpression?.lastNameOrNull()
    is KtPostfixExpression -> baseExpression?.lastNameOrNull()
    is KtNameReferenceExpression -> getReferencedName()
    else -> null
  }

  private fun KtCallExpression.isQualifiedBy(receiver: String): Boolean = qualifierOrNull() == receiver

  /** `Paths.get` for a call on a type, the bare name for a constructor below a package. */
  private fun KtCallExpression.qualifiedCallee(): String =
    listOfNotNull(qualifierOrNull()?.takeIf { it.first().isUpperCase() }, calleeExpression?.text).joinToString(".")

  /** The literal of a template without entries; null for one with an interpolation. */
  private fun KtStringTemplateExpression.literalValue(): String? {
    if (entries.any { it.expression != null }) return null
    return entries.joinToString("") { it.text }
  }

  private fun report(element: KtElement, finding: String) {
    report(
      Finding(
        Entity.from(element),
        "$finding A test reads only its classpath: the producer publishes the file with publishFiles(SharedFiles.…) (internal/buildplatform/conventions), " +
          "the consumer resolves it with resolvedFiles(SharedFiles.…) and copies it onto the test classpath with processTestResources { from(configuration) }.",
      ),
    )
  }

  companion object {
    private val RootWalkerNames: Set<String> = setOf("findRepoRoot", "resolveRepoRoot", "resolveRepositoryRoot", "repositoryRoot", "repoRoot", "findRepositoryRoot")
    private val ResolveNames: Set<String> = setOf("resolve", "resolveSibling")
    private val WorkingDirectoryLiterals: Set<String> = setOf("")
    private val DirectoryReceivers: Set<String> = setOf("rootDir", "projectDir")
  }
}
