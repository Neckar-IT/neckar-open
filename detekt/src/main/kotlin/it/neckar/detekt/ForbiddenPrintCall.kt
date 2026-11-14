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
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags calls to `kotlin.io.println` and `kotlin.io.print`.
 *
 * Lives in `neckar-rules` (instead of the bundled `style.ForbiddenMethodCall`) so it can carry an
 * own severity (`warning`) independent of the other forbidden calls. `ForbiddenMethodCall` only
 * supports a single rule-wide severity, so listing `kotlin.io.println` next to entries like
 * `kotlinx.coroutines.flow.toList` would force the entire rule to one severity.
 *
 * Use a logger (`mu` / `kotlin-logging`), clikt's `echo()`, or `File.writeText()` instead — never
 * raw stdout. Test sources are analysed as well; demo modules and the test sources whose result is
 * the console output are excluded via `excludes` in `internal/buildplatform/config/detekt/detekt.yml`.
 */
class ForbiddenPrintCall(config: Config) : Rule(
  config,
  description = "Calls to `kotlin.io.println` / `kotlin.io.print` reach raw stdout — use a logger, clikt's `echo()`, or `File.writeText()` instead.",
), RequiresAnalysisApi {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    val callee = expression.calleeExpression ?: return
    if (callee.text !in TARGET_NAMES) return

    val resolvedName = analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze null
      val symbol = call.symbol as? KaNamedFunctionSymbol ?: return@analyze null

      val callableId = symbol.callableId ?: return@analyze null
      if (callableId.classId != null) return@analyze null
      if (callableId.packageName.asString() != KotlinIoPackage) return@analyze null

      callableId.callableName.asString().takeIf { it in TARGET_NAMES }
    } ?: return

    report(
      Finding(
        Entity.from(callee),
        "Call to `kotlin.io.$resolvedName` reaches raw stdout. Use a logger, clikt's `echo()`, " +
          "or `File.writeText()` instead.",
      ),
    )
  }

  companion object {
    private const val KotlinIoPackage = "kotlin.io"
    private val TARGET_NAMES = setOf("println", "print")
  }
}
