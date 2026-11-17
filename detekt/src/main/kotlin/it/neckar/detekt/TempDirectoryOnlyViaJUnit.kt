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
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * PSI-based: a qualified name always counts, a receiver `File`/`Files` or a bare call only through an import of the file.
 * Unseen are the default import of `kotlin.io` and declarations of the same package in other files.
 */
class TempDirectoryOnlyViaJUnit(config: Config) : Rule(
  config,
  description = "A temporary file created by hand outlives the test; JUnit `@TempDir` deletes it after the test.",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    val callee = expression.calleeExpression.referencedNameOrNull() ?: return
    val qualified = expression.parent as? KtDotQualifiedExpression
    val calledFunction = if (qualified != null && qualified.selectorExpression == expression) {
      val receiver = expression.containingKtFile.resolveReceiver(qualified.receiverExpression) ?: return
      "$receiver.$callee"
    } else {
      expression.containingKtFile.resolveUnqualifiedCall(callee) ?: return
    }

    if (calledFunction in TempFileFunctions) {
      report(
        Finding(
          Entity.from(expression),
          "`$calledFunction` creates a temporary path by hand. A temporary file or directory in a test is a `@TempDir` or a path resolved in it, which JUnit deletes after the test.",
        ),
      )
    }
  }

  /**
   * `java.io.File` for the receiver `File` bound by `import java.io.File`, the written name for a fully qualified receiver,
   * `null` for any other receiver
   */
  private fun KtFile.resolveReceiver(receiver: KtExpression): String? {
    val written = receiver.dottedName() ?: return null
    if (written.contains('.')) return written
    return resolveSimpleName(written, TempFileReceivers)
  }

  /**
   * The function an unqualified call names through an explicit, aliased or star import, or `null` when no import binds it
   */
  private fun KtFile.resolveUnqualifiedCall(callee: String): String? {
    val explicit = importDirectives.firstOrNull { it.isAllUnder.not() && it.importedName?.asString() == callee }
    if (explicit != null) return explicit.importedFqName?.asString()
    if (declaresFunction(callee)) return null
    return importDirectives
      .filter { it.isAllUnder }
      .mapNotNull { it.importedFqName?.asString() }
      .map { "$it.$callee" }
      .firstOrNull { it in TempFileFunctions }
  }

  /**
   * The fully qualified class of [simpleName] among [candidates], bound by an explicit or aliased import or by a star import
   * of its package. A class of the same name declared in the file takes precedence over the star import.
   */
  private fun KtFile.resolveSimpleName(simpleName: String, candidates: Set<String>): String? {
    val explicit = importDirectives.firstOrNull { it.isAllUnder.not() && it.importedName?.asString() == simpleName }
    if (explicit != null) return explicit.importedFqName?.asString()?.takeIf { it in candidates }
    if (declaresClass(simpleName)) return null
    val starPackages = importDirectives.filter { it.isAllUnder }.mapNotNull { it.importedFqName?.asString() }
    return candidates.firstOrNull { candidate -> candidate.substringAfterLast('.') == simpleName && candidate.substringBeforeLast('.') in starPackages }
  }

  private fun KtFile.declaresClass(name: String): Boolean = collectDescendantsOfType<KtClassOrObject> { it.name == name }.isNotEmpty()

  private fun KtFile.declaresFunction(name: String): Boolean = collectDescendantsOfType<KtNamedFunction> { it.name == name }.isNotEmpty()

  /**
   * `java.nio.file.Files` for the expression `java.nio.file.Files`, or `null` when a segment is no plain name
   */
  private fun KtExpression.dottedName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> {
      val receiver = receiverExpression.dottedName()
      val selector = selectorExpression.referencedNameOrNull()
      if (receiver == null || selector == null) null else "$receiver.$selector"
    }
    else -> null
  }

  private fun KtExpression?.referencedNameOrNull(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    else -> null
  }

  companion object {
    private val TempFileReceivers: Set<String> = setOf("java.io.File", "java.nio.file.Files")

    private val TempFileFunctions: Set<String> = setOf(
      "java.io.File.createTempFile",
      "java.nio.file.Files.createTempFile",
      "java.nio.file.Files.createTempDirectory",
      "kotlin.io.path.createTempFile",
      "kotlin.io.path.createTempDirectory",
      "kotlin.io.createTempFile",
      "kotlin.io.createTempDir",
    )
  }
}
