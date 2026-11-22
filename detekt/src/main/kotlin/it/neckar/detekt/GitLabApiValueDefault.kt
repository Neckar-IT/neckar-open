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

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Reports a default on a value read from a gitlab4j model, in a file with an import from `org.gitlab4j`:
 *
 * - `mr.title.orEmpty()`
 * - `mr.title ?: ""`, also with a number, a boolean, `null` or an empty collection on the right
 * - `mr.iid ?: return null`, `?: continue`, `?: break`, also as the last statement of `?: run { … }`
 * - `mr.mergeCommitSha ?: mr.sha ?: ""`: the default counts for each read of the chain
 *
 * A missing value throws, naming the field and the object: `mr.title.requireNotNull { "!${mr.iid} carries no title" }`.
 * A value GitLab documents as absent is branched on with an explicit `if`, or the statement carries
 * `@Suppress("GitLabApiValueDefault")` with the reason (`.claude/rules/kotlin.md`).
 *
 * The syntax tree decides. A read ends in a property or a call without arguments; a `…OrNull()` lookup is none.
 * The root of the read tells whether it starts at a model:
 *
 * - `this`, an unbound name or a call without receiver and arguments that the file does not declare: a model
 *   inside an extension on an imported gitlab4j model.
 * - A name with a declared type: a model when the type or a type argument names a class imported from
 *   `org.gitlab4j.api.models`. A star import names no class.
 * - A name without a declared type, and every read behind a call with arguments or a `…OrNull()` lookup: a
 *   model, because the syntax tree does not tell the type.
 * - An index access, a class literal, a cast: none.
 */
class GitLabApiValueDefault(config: Config) : Rule(
  config,
  description = "A value missing in a GitLab API response throws, naming the field and the object. `orEmpty()` and an elvis default hide the defect in the response.",
) {

  override fun visitBinaryExpression(expression: KtBinaryExpression) {
    super.visitBinaryExpression(expression)

    if (expression.operationToken != KtTokens.ELVIS) return
    if (expression.right.isDefault().not()) return
    val read: KtExpression = expression.left ?: return
    if (read.readsGitLabValue().not()) return

    reportDefault(expression, read)
  }

  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    super.visitQualifiedExpression(expression)

    val call: KtCallExpression = expression.selectorExpression as? KtCallExpression ?: return
    if (call.calleeExpression?.text != OrEmpty || call.valueArguments.isNotEmpty()) return
    val read: KtExpression = expression.receiverExpression
    if (read.readsGitLabValue().not()) return

    reportDefault(expression, read)
  }

  private fun reportDefault(expression: KtExpression, read: KtExpression) {
    report(
      Finding(
        Entity.from(expression),
        "`${expression.text}` answers a default for a value missing in the GitLab response. Throw instead, naming the field " +
          "and the object: `${read.text}.requireNotNull { \"… carries no …\" }`. Branch on a value GitLab documents as absent " +
          "with an explicit `if`, or carry `@Suppress(\"GitLabApiValueDefault\")` with the reason. See `.claude/rules/kotlin.md`.",
      ),
    )
  }

  /** Whether the right side of an elvis answers a default or leaves the surrounding code without a throw. */
  private fun KtExpression?.isDefault(): Boolean = when (this) {
    is KtParenthesizedExpression -> expression.isDefault()
    is KtPrefixExpression -> baseExpression is KtConstantExpression
    is KtConstantExpression, is KtStringTemplateExpression -> true
    is KtReturnExpression, is KtContinueExpression, is KtBreakExpression -> true
    is KtCallExpression -> (calleeExpression?.text in EmptyCollectionCallees && valueArguments.isEmpty()) || endsInJump()
    else -> false
  }

  /** `run { …; return }`: the block leaves the surrounding code like a bare `return`. */
  private fun KtCallExpression.endsInJump(): Boolean {
    if (calleeExpression?.text != Run) return false
    return when (lambdaArguments.singleOrNull()?.getLambdaExpression()?.bodyExpression?.statements?.lastOrNull()) {
      is KtReturnExpression, is KtContinueExpression, is KtBreakExpression -> true
      else -> false
    }
  }

  /** Whether the expression ends in a read and that read starts at a gitlab4j model. */
  private fun KtExpression.readsGitLabValue(): Boolean {
    if (containingKtFile.importsGitlab4j().not()) return false
    val modelNames: Set<String> = containingKtFile.importedModelNames()

    var current: KtExpression = withoutParentheses()
    if (current is KtBinaryExpression && current.operationToken == KtTokens.ELVIS) {
      return listOfNotNull(current.left, current.right).any { it.readsGitLabValue() }
    }
    if (current is KtQualifiedExpression && current.selectorExpression.isRead().not()) return false

    var selectors: Int = 0
    var behindCall: Boolean = false
    while (current is KtQualifiedExpression) {
      selectors++
      if (current.selectorExpression.isRead().not()) behindCall = true
      current = current.receiverExpression.withoutParentheses()
    }

    return when (val root: KtExpression = current) {
      is KtThisExpression -> behindCall || (selectors > 0 && root.hasModelReceiver(modelNames))

      is KtNameReferenceExpression -> {
        val binding: Binding? = root.findBinding()
        when {
          behindCall -> true
          binding == null -> root.hasModelReceiver(modelNames)
          binding.type == null -> selectors > 0
          else -> selectors > 0 && binding.type.namesAnyOf(modelNames)
        }
      }

      // Without arguments a getter of the extension's model; with arguments it hands out a value of unknown type.
      is KtCallExpression -> when {
        behindCall -> true
        root.isRead() -> root.isDeclaredInFile().not() && root.hasModelReceiver(modelNames)
        else -> selectors > 0
      }

      else -> false
    }
  }

  /** A property, or a call without arguments that is no `…OrNull()` lookup. */
  private fun KtExpression?.isRead(): Boolean = when (this) {
    is KtNameReferenceExpression -> true
    is KtCallExpression -> valueArguments.isEmpty() && calleeExpression?.text.orEmpty().endsWith(OrNullSuffix).not()
    else -> false
  }

  private fun KtCallExpression.isDeclaredInFile(): Boolean {
    val callee: String? = calleeExpression?.text
    return containingKtFile.collectDescendantsOfType<KtNamedFunction>().any { it.name == callee }
  }

  private fun KtExpression.withoutParentheses(): KtExpression {
    var current: KtExpression = this
    while (current is KtParenthesizedExpression) {
      current = current.expression ?: return current
    }
    return current
  }

  /** Whether an enclosing extension function or extension property has an imported gitlab4j model as receiver. */
  private fun KtExpression.hasModelReceiver(modelNames: Set<String>): Boolean {
    return generateSequence(parent) { it.parent }
      .filterIsInstance<KtCallableDeclaration>()
      .mapNotNull { it.receiverTypeReference }
      .any { receiver -> (receiver.typeElement as? KtUserType)?.referencedName in modelNames }
  }

  private fun KtTypeReference.namesAnyOf(modelNames: Set<String>): Boolean {
    return collectDescendantsOfType<KtUserType>().any { it.referencedName in modelNames }
  }

  /** The declaration in this file that binds the name at this position, innermost scope first. Null for a name declared elsewhere. */
  private fun KtNameReferenceExpression.findBinding(): Binding? {
    val name: String = getReferencedName()
    val position: Int = textOffset
    var child: PsiElement = this
    while (true) {
      val scope: PsiElement = child.parent ?: return null
      val binding: Binding? = when (scope) {
        is KtFunctionLiteral -> scope.valueParameters.bindingOf(name) ?: Binding(type = null).takeIf { name == ImplicitLambdaParameter && scope.hasParameterSpecification().not() }
        is KtFunction -> scope.valueParameters.bindingOf(name)
        is KtForExpression -> listOfNotNull(scope.loopParameter).bindingOf(name)
        is KtCatchClause -> listOfNotNull(scope.catchParameter).bindingOf(name)
        is KtWhenExpression -> scope.subjectVariable?.takeIf { it.name == name }?.let { Binding(it.typeReference) }
        is KtBlockExpression -> scope.statements.filter { it.textOffset < position }.bindingOfDeclarations(name)
        is KtClassOrObject -> scope.primaryConstructorParameters.bindingOf(name) ?: scope.declarations.bindingOfDeclarations(name)
        is KtFile -> return scope.declarations.bindingOfDeclarations(name)
        else -> null
      }
      if (binding != null) return binding
      child = scope
    }
  }

  private fun List<KtParameter>.bindingOf(name: String): Binding? {
    for (parameter in this) {
      if (parameter.name == name) return Binding(parameter.typeReference)
      parameter.destructuringDeclaration?.bindingOf(name)?.let { return it }
    }
    return null
  }

  private fun List<PsiElement>.bindingOfDeclarations(name: String): Binding? {
    for (declaration in this) {
      when (declaration) {
        is KtProperty -> if (declaration.name == name) return Binding(declaration.typeReference)
        is KtDestructuringDeclaration -> declaration.bindingOf(name)?.let { return it }
      }
    }
    return null
  }

  private fun KtDestructuringDeclaration.bindingOf(name: String): Binding? {
    return entries.firstOrNull { it.name == name }?.let { Binding(it.typeReference) }
  }

  private fun KtFile.importsGitlab4j(): Boolean {
    return importDirectives.any { it.importedFqName?.asString().orEmpty().startsWith("$Gitlab4jPackage.") }
  }

  /** The simple names under which this file imports a class of `org.gitlab4j.api.models`. */
  private fun KtFile.importedModelNames(): Set<String> {
    return importDirectives
      .filter { it.isAllUnder.not() && it.importedFqName?.parent()?.asString() == ModelsPackage }
      .mapNotNull { it.importedName?.asString() }
      .toSet()
  }

  /** A declaration binding a name; [type] is null where the declaration states no type. */
  private class Binding(val type: KtTypeReference?)

  companion object {
    private const val Gitlab4jPackage = "org.gitlab4j"
    private const val ModelsPackage = "$Gitlab4jPackage.api.models"
    private const val OrEmpty = "orEmpty"
    private const val Run = "run"
    private const val OrNullSuffix = "OrNull"
    private const val ImplicitLambdaParameter = "it"

    /** Calls that answer an empty collection when called without arguments. */
    private val EmptyCollectionCallees: Set<String> = setOf("emptyList", "emptySet", "emptyMap", "listOf", "setOf", "mapOf")
  }
}
