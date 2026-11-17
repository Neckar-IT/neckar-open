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
 * Forbids passing a leaf instance of a `@Serializable sealed RestResponse` directly into
 * `ApplicationCall.respond(message)` (or `respondNullable`).
 *
 * `call.respond(x)` resolves the kotlinx-serialization serializer over `x`'s **runtime type**. If
 * the runtime type is a leaf subclass of a `@Serializable sealed` parent, the serializer is the
 * leaf-specific one — and that one omits the polymorphic `type` discriminator. The frontend
 * `switch (response.type)` then falls through silently; the UI hangs on a never-arriving toast or
 * navigation event. HTTP-Status stays `200` the whole time.
 *
 * The rule fires when the **static type** of the argument expression at the call site is a leaf
 * subclass of a sealed `RestResponse`. When the argument is typed as the sealed parent itself
 * (variable with explicit sealed-parent type, factory return type, etc.), no finding — the
 * runtime type may still be a leaf, but kotlinx-serialization picks the polymorphic serializer
 * via the declared variable type and emits the discriminator correctly.
 *
 * Companion: [RestResponseConstructorMustUseFactory] — catches the same bug at the construction
 * site, so leaf-typed values rarely reach a `respond` call in the first place.
 *
 * Sharp edge: a smart-cast block (`if (response is MyResponse.Good) call.respond(response)`) types
 * `response` as the leaf at the call site, so this rule fires. That is the correct behaviour — the
 * bug really exists there. Fix: extract `val r: MyResponse = response` before the `respond` call,
 * or upcast `response as MyResponse`.
 *
 * See `internal/patterns/docs/kotlin/rest-response.md`.
 */
class RespondWithSealedSubclass(config: Config) : Rule(
  config,
  description = "`call.respond(<leaf-of-@Serializable-sealed-RestResponse>)` drops the polymorphic discriminator. Pass the value typed as the sealed parent instead.",
), RequiresAnalysisApi {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    val argumentExpression = expression.valueArguments.firstOrNull()?.getArgumentExpression() ?: return

    analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze
      val symbol = call.symbol as? KaNamedFunctionSymbol ?: return@analyze
      val callableId = symbol.callableId ?: return@analyze
      if (callableId.packageName.asString() != KtorResponsePackage) return@analyze
      val calleeName = callableId.callableName.asString()
      if (calleeName !in respondFunctionNames) return@analyze

      val argType = argumentExpression.expressionType ?: return@analyze
      val sealedParent = findSealedRestResponseParent(argType) ?: return@analyze

      val leafName = argType.classIdFqString()?.substringAfterLast('.') ?: "subclass"
      val parentName = sealedParent.shortName()

      report(
        Finding(
          Entity.from(expression.calleeExpression ?: expression),
          "`call.$calleeName($leafName(...))` serializes via the runtime type and drops the polymorphic " +
            "`type` discriminator (the frontend `switch (response.type)` falls through silently). " +
            "Bind the value to a `val response: $parentName = …` first, or use a companion factory whose " +
            "return type is `$parentName`. " +
            "See `internal/patterns/docs/kotlin/rest-response.md`.",
        ),
      )
    }
  }

  companion object {
    private const val KtorResponsePackage: String = "io.ktor.server.response"
    private val respondFunctionNames: Set<String> = setOf("respond", "respondNullable")
  }
}
