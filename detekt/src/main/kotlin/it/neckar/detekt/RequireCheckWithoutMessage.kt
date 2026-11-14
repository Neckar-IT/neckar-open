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

/**
 * Flags `require(...)` / `check(...)` / `requireNotNull(...)` / `checkNotNull(...)` calls that
 * omit a message-providing lambda.
 *
 * Without `{ "..." }`, the kotlin-stdlib functions throw with generic strings
 * (`"Failed requirement."`, `"Check failed."`, `"Required value was null."`). In the browser
 * error overlay, where dozens of throw-sites collapse into the same line, the
 * absence of a message makes the failing site impossible to identify. The rule forces every
 * bare assertion to carry a diagnostic message.
 *
 * Detection is purely PSI-based — no symbol resolution. A call is flagged when:
 * - the callee identifier is `require`, `check`, `requireNotNull`, or `checkNotNull`,
 * - the call has exactly one value argument, and
 * - the call has no trailing lambda argument.
 *
 * The two-argument overloads (`require(cond, lazyMessage)` / `require(cond) { "msg" }`) are not
 * flagged. Qualified calls like `obj.require(cond)` are also skipped — they cannot resolve to the
 * stdlib functions and any project-local extension is the project author's concern. In
 * particular, the `value.requireNotNull { "msg" }` extension form (preferred per `ai/guides/code-style.md`)
 * is a qualified call and therefore not flagged.
 *
 * Test sources are excluded via `excludes` in `internal/buildplatform/config/detekt/detekt.yml` — bare `require(cond)`
 * in tests is acceptable because the failure site is the test name, which is already diagnostic.
 */
class RequireCheckWithoutMessage(config: Config) : Rule(
  config,
  description = "`require` / `check` / `requireNotNull` / `checkNotNull` without a `{ \"...\" }` message produce generic stdlib messages that are useless in the browser error overlay — pass a diagnostic message lambda.",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    val callee = expression.calleeExpression ?: return
    val calleeName = callee.text
    if (calleeName !in TARGET_CALLEES) return

    if (expression.parent is KtDotQualifiedExpression) return

    if (expression.lambdaArguments.isNotEmpty()) return
    if (expression.valueArguments.size != 1) return

    report(
      Finding(
        Entity.from(callee),
        "`$calleeName(...)` without a message lambda produces a generic stdlib message — " +
          "add `{ \"...\" }` describing why the assertion holds (Issue #1621).",
      ),
    )
  }

  companion object {
    private val TARGET_CALLEES = setOf("require", "check", "requireNotNull", "checkNotNull")
  }
}
