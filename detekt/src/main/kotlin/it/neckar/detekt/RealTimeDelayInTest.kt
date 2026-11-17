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
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Flags `kotlinx.coroutines.delay` in a test or lifecycle method unless a `runTest { … }` encloses the call.
 *
 * PSI-based, in the resolution order of Kotlin: a `delay` of an enclosing class or an earlier one of an enclosing block, then an
 * explicit or aliased import, then the file's top-level `delay`, then the star import; an own `delay` counts by name alone.
 * Unseen are a `delay` in a helper the test calls and one that `withContext` or `runBlocking` moves out of virtual time.
 */
class RealTimeDelayInTest(config: Config) : Rule(
  config,
  description = "A `delay` outside `runTest` waits in real time: a fixed wait is too short on a loaded runner and too long everywhere else.",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    if (expression.callsCoroutinesDelay().not()) return
    if (expression.waitsInRealTimeInTest().not()) return

    report(
      Finding(
        Entity.from(expression),
        "delay outside runTest waits in real time: poll with awaitCondition or use runTest",
      ),
    )
  }

  private fun KtCallExpression.callsCoroutinesDelay(): Boolean {
    val callee = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return false
    val qualified = parent as? KtQualifiedExpression
    if (qualified != null && qualified.selectorExpression == this) {
      return qualified is KtDotQualifiedExpression && callee == DelayName && qualified.receiverExpression.dottedName() == CoroutinesPackage
    }
    if (seesEnclosingFunction(callee)) return false

    val file = containingKtFile
    val explicit = file.importDirectives.firstOrNull { it.isAllUnder.not() && it.importedName?.asString() == callee }
    if (explicit != null) return explicit.importedFqName?.asString() == DelayFqName
    if (callee != DelayName) return false
    if (file.declarations.any { (it as? KtNamedFunction)?.name == DelayName }) return false
    // An alias hides the original name from the star import of its package.
    if (file.importDirectives.any { it.isAllUnder.not() && it.importedFqName?.asString() == DelayFqName }) return false
    return file.importDirectives.any { it.isAllUnder && it.importedFqName?.asString() == CoroutinesPackage }
  }

  /**
   * Whether a function named [name] in an enclosing class or companion, or declared before the call in an enclosing block, takes
   * precedence over every import
   */
  private fun KtCallExpression.seesEnclosingFunction(name: String): Boolean {
    val callOffset = textOffset
    return parents.flatMap { parent ->
      when (parent) {
        is KtClassOrObject -> parent.declarations.asSequence() + parent.companionObjects.flatMap { it.declarations }
        is KtBlockExpression -> parent.statements.asSequence().filter { it.textOffset < callOffset }
        else -> emptySequence()
      }
    }.any { (it as? KtNamedFunction)?.name == name }
  }

  /**
   * Whether the call sits in a test or lifecycle method, at any lambda depth, without a `runTest` call in between
   */
  private fun KtCallExpression.waitsInRealTimeInTest(): Boolean {
    for (ancestor in parents) {
      if (ancestor is KtCallExpression && (ancestor.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == RunTestName) return false
      if (ancestor is KtNamedFunction && ancestor.annotationEntries.any { it.shortName?.asString() in TestAnnotations }) return true
    }
    return false
  }

  /**
   * `kotlinx.coroutines` for the expression `kotlinx.coroutines`, or `null` when a segment is no plain name
   */
  private fun KtExpression.dottedName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> {
      val receiver = receiverExpression.dottedName()
      val selector = (selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
      if (receiver == null || selector == null) null else "$receiver.$selector"
    }
    else -> null
  }

  companion object {
    private const val DelayName: String = "delay"
    private const val CoroutinesPackage: String = "kotlinx.coroutines"
    private const val DelayFqName: String = "$CoroutinesPackage.$DelayName"
    private const val RunTestName: String = "runTest"

    private val TestAnnotations: Set<String> = setOf(
      "Test",
      "ParameterizedTest",
      "BeforeEach",
      "AfterEach",
      "BeforeAll",
      "AfterAll",
      "BeforeTest",
      "AfterTest",
    )
  }
}
