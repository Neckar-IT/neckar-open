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
import org.jetbrains.kotlin.psi.KtThrowExpression

/**
 * Flags `throw <StdlibException>()` constructor calls that omit a message argument.
 *
 * A bare `throw IllegalStateException()` reaches the browser error overlay with no
 * diagnostic information beyond the exception class name and stack-frame line — which is rarely
 * enough to identify the failing site in production. Every throw must carry a message that
 * explains *why* it fired.
 *
 * Detection is purely PSI-based: a `KtThrowExpression` whose thrown expression is a constructor
 * call (`KtCallExpression`) targeting one of the stdlib exception types AND has zero value
 * arguments.
 *
 * Idiomatic alternatives that the rule does **not** flag:
 * - `error("msg")` (sugar for `throw IllegalStateException("msg")`)
 * - `throw IllegalStateException("msg")` / `throw IllegalArgumentException("msg")` etc.
 * - Project-local exception types — only kotlin-stdlib runtime exceptions are checked.
 *
 * Test sources are excluded via `excludes` in `internal/buildplatform/config/detekt/detekt.yml`.
 */
class ThrowWithoutMessage(config: Config) : Rule(
  config,
  description = "`throw <StdlibException>()` without a message argument leaves the browser error overlay with only the exception class name — pass a diagnostic message string.",
) {

  override fun visitThrowExpression(expression: KtThrowExpression) {
    super.visitThrowExpression(expression)

    val constructorCall = expression.thrownExpression as? KtCallExpression ?: return
    val callee = constructorCall.calleeExpression ?: return
    val calleeName = callee.text
    if (calleeName !in TARGET_EXCEPTIONS) return

    if (constructorCall.valueArguments.isNotEmpty()) return
    if (constructorCall.lambdaArguments.isNotEmpty()) return

    report(
      Finding(
        Entity.from(callee),
        "`throw $calleeName()` without a message leaves the browser error overlay with only the exception class name — " +
          "pass a diagnostic message describing why this throws (Issue #1621).",
      ),
    )
  }

  companion object {
    /**
     * kotlin-stdlib runtime exception types whose generic constructor produces a message-less
     * stack trace. Project-local exceptions are intentionally not flagged — their authors set
     * the conventions for their own types.
     */
    private val TARGET_EXCEPTIONS = setOf(
      "IllegalStateException",
      "IllegalArgumentException",
      "NullPointerException",
      "RuntimeException",
      "UnsupportedOperationException",
      "Exception",
      "Throwable",
      "Error",
      "AssertionError",
    )
  }
}
