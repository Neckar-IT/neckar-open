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
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

/**
 * Reports `as?` to a type that untyped data hands out - a `JsonElement`, a `BsonValue`, a `YamlNode`,
 * `org.bson.Document`, or the `String`, `Boolean`, `Date`, `List`, `Set`, `Collection`, `Map` and
 * number types behind `Document.get`, `Map<String, Any?>` and `dynamic` - unless its null reaches a
 * throw: `?: throw`, `?: error(…)`, `?: TODO()`, `?: fail(…)`, also at the end of a fallback chain
 * `a as? T ?: b as? T ?: error(…)`, or the cast stands inside `requireNotNull(…)` / `checkNotNull(…)`
 * or carries `.requireNotNull { … }` / `.checkNotNull { … }`. Only these forms count: a `!!`, an
 * `as` to a non-null type or a cast inside an `if` branch is reported and gets the elvis or the
 * suppression.
 *
 * `as?` answers null for a value of another type, the same answer as for an absent key, so a reader
 * files a defect in the data under "nothing here" and every caller walks past it. A wrong type
 * throws, naming the path, the expected and the found type; absence alone is null
 * (`ai/guides/code-style.md`, "Reading an untyped source"). A cast whose null genuinely means
 * "another format" carries `@Suppress("UntypedValueSafeCast")` with the reason.
 *
 * Decided from the syntax tree alone: the right-hand type name is matched against [UntypedValueTypes]
 * by its simple name, so a qualifier, a type argument and a `?` do not change the verdict. A cast to an
 * own sealed case or to a subtype of a typed API (PSI node, Gradle event, exception) is not in the set
 * and stays unreported; `Document` is in it for `org.bson.Document`, so a cast to a DOM `Document`
 * carries the suppression.
 */
class UntypedValueSafeCast(config: Config) : Rule(
  config,
  description = "`as?` on a value read from untyped data answers null for a wrong type exactly as for an absent key. Follow it with `?: throw` or `?: error(…)` naming the path, the expected and the found type.",
) {

  override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
    super.visitBinaryWithTypeRHSExpression(expression)

    if (expression.operationReference.getReferencedNameElementType() != KtTokens.AS_SAFE) return
    val typeName: String = expression.right?.simpleTypeName() ?: return
    if (typeName !in UntypedValueTypes) return
    if (expression.throwsOnNull()) return

    report(
      Finding(
        Entity.from(expression),
        "`as? $typeName` answers null for a value of another type, the same as for an absent key, " +
          "so the caller files a defect in the data under \"nothing here\". Follow the cast with " +
          "`?: throw` or `?: error(…)` naming the path, the expected and the found type, or carry " +
          "`@Suppress(\"UntypedValueSafeCast\")` with the reason. See `ai/guides/code-style.md#errors-are-never-null`.",
      ),
    )
  }

  /** The simple name of the type, without qualifier, type arguments and `?`. */
  private fun KtTypeReference.simpleTypeName(): String? {
    val typeElement = when (val element = typeElement) {
      is KtNullableType -> element.innerType
      else -> element
    }
    return (typeElement as? KtUserType)?.referencedName
  }

  /**
   * Whether the expression's null reaches a `throw`: through parentheses, safe calls, a further `as`
   * or `as?` and an elvis chain whose intermediate right sides [passNullOn] a cast, up to an elvis
   * whose right side throws; or into a not-null assertion - as its argument or as the receiver of the
   * `.requireNotNull { … }` / `.checkNotNull { … }` extension. A `?.` skips the assertion on null and
   * any other `.` call turns the null into a value, so neither counts.
   */
  private fun KtExpression.throwsOnNull(): Boolean {
    var current: PsiElement = this
    while (true) {
      val parent: PsiElement = current.parent ?: return false
      when (parent) {
        is KtParenthesizedExpression -> Unit
        is KtBinaryExpressionWithTypeRHS -> if (parent.left !== current) return false
        is KtSafeQualifiedExpression -> if (parent.receiverExpression !== current) return false
        is KtDotQualifiedExpression -> return parent.receiverExpression === current && parent.selectorExpression.calleeName() in NotNullAssertions

        is KtBinaryExpression -> {
          if (parent.operationToken != KtTokens.ELVIS) return false
          if (parent.left === current) {
            if (parent.right.isThrow()) return true
            // A fallback value ends the read without a throw; only a further cast keeps the chain open.
            if (parent.right.passNullOn().not()) return false
          }
        }

        is KtValueArgument -> return parent.enclosingCallee() in NotNullAssertions
        else -> return false
      }
      current = parent
    }
  }

  /** Whether the expression ends the read: a `throw`, or a call to one of [ThrowingCallees], bare or qualified with one of [ThrowingPackages]. */
  private fun KtExpression?.isThrow(): Boolean = when (this) {
    is KtThrowExpression -> true
    is KtParenthesizedExpression -> expression.isThrow()
    is KtCallExpression -> calleeName() in ThrowingCallees
    is KtQualifiedExpression -> receiverExpression.text in ThrowingPackages && selectorExpression.calleeName() in ThrowingCallees
    else -> false
  }

  private fun KtExpression?.calleeName(): String? = (this as? KtCallExpression)?.calleeExpression?.text

  /**
   * Whether the expression can be null only through a cast or a safe call in it, with no non-null
   * fallback: an `as?`, a safe call or a parenthesized expression starting with one, or an elvis whose
   * both operands qualify. Such a right side of an elvis passes the null of the left side on.
   */
  private fun KtExpression?.passNullOn(): Boolean = when (this) {
    is KtBinaryExpressionWithTypeRHS -> operationReference.getReferencedNameElementType() == KtTokens.AS_SAFE
    is KtParenthesizedExpression -> expression.passNullOn()
    is KtSafeQualifiedExpression -> receiverExpression.passNullOn()
    is KtBinaryExpression -> operationToken == KtTokens.ELVIS && left.passNullOn() && right.passNullOn()
    else -> false
  }

  private fun KtValueArgument.enclosingCallee(): String? {
    val call: KtCallExpression = (parent as? KtValueArgumentList)?.parent as? KtCallExpression ?: return null
    return call.calleeExpression?.text
  }

  companion object {
    /** The simple names of the types untyped data hands out, by their source. */
    private val UntypedValueTypes: Set<String> = setOf(
      // kotlinx.serialization.json
      "JsonElement", "JsonObject", "JsonArray", "JsonPrimitive", "JsonNull",
      // org.bson
      "BsonValue", "BsonDocument", "BsonArray", "BsonString", "BsonNumber", "BsonInt32", "BsonInt64", "BsonDouble",
      "BsonDecimal128", "BsonBoolean", "BsonBinary", "BsonNull", "BsonDateTime", "BsonObjectId", "BsonTimestamp", "Document",
      // kaml
      "YamlNode", "YamlMap", "YamlList", "YamlScalar", "YamlNull", "YamlTaggedNode",
      // what Document.get, Map<String, Any?> and dynamic hand out
      "String", "Int", "Long", "Double", "Float", "Boolean", "Number", "Date", "List", "Map", "Set", "Collection",
    )

    /** Callees on the right of the elvis that end the read: `kotlin.error`, `kotlin.TODO` and `kotlin.test.fail`. */
    private val ThrowingCallees: Set<String> = setOf("error", "TODO", "fail")

    /** The packages a qualified call to one of [ThrowingCallees] may name. */
    private val ThrowingPackages: Set<String> = setOf("kotlin", "kotlin.test")

    /** Calls whose argument throws on null. */
    private val NotNullAssertions: Set<String> = setOf("requireNotNull", "checkNotNull")
  }
}
