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
 * Forbids calls to a watched set of `reified T`-parameterized serialization entry points without an
 * explicit `<T>` type argument at the call site. Covers Ktor server (`respond` / `receive`),
 * Ktor client (`setBody` / `body`), and direct kotlinx-serialization (`Json.encodeToString` /
 * `decodeFromString` / `encodeToJsonElement` / `decodeFromJsonElement`).
 *
 * Why: every watched function picks the kotlinx-serialization serializer (or deserializer) over the
 * reified `T`. `T` is the **only** input that decides whether the polymorphic `type` discriminator
 * ends up on the wire (or whether the discriminator-aware parser is used on the way back). The
 * static type of the value argument helps the compiler infer `T`, but inference is fragile: rename
 * the variable, inline the expression, smart-cast into a branch, change the val type from sealed
 * parent to leaf, and the inferred `T` shifts without any compile-time hint. The frontend
 * `switch (response.type)` then falls through silently; an incoming leaf-typed deserialize fails
 * to handle the polymorphic case. HTTP-Status stays `200` the whole time.
 *
 * The rule has exactly one OK path: the call site names `<T>` explicitly. Argument-typing alone is
 * not enough — that is what [RespondWithSealedSubclass] (catches leaf-typed arguments at the
 * respond site) and [WhenInitializerRequiresExplicitType] (catches inferred-from-when `val`s) are
 * for. This rule makes the contract sit where kotlinx-serialization actually reads it.
 *
 * Pairs with the OpenAPI doc block: `responding<UpsertResponse>(…)` carries the same `<T>` —
 * having both `responding<T>` and `respond<T>` at the handler keeps the response type visible at
 * both ends of the contract and find-references reaches both sites.
 *
 * Scope: watched callable IDs are listed in [WATCHED]. Adjust the map to expand or contract the
 * scope rather than changing the rule logic. Functions outside the watchlist are not flagged.
 *
 * See `internal/patterns/docs/kotlin/rest-response.md`.
 */
class PolymorphicSerializationCallRequiresExplicitType(config: Config) : Rule(
  config,
  description = "Calls to reified-T serialization entry points (`call.respond`, `call.receive`, `setBody`, `Json.encodeToString`, `Json.decodeFromString`, …) must carry an explicit `<T>` type argument. Inferred `T` shifts silently on refactor and drops the polymorphic discriminator.",
), RequiresAnalysisApi {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    if (expression.typeArgumentList?.arguments?.isNotEmpty() == true) return

    analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze
      val symbol = call.symbol as? KaNamedFunctionSymbol ?: return@analyze
      val callableId = symbol.callableId ?: return@analyze
      val packageName = callableId.packageName.asString()
      val calleeName = callableId.callableName.asString()
      val brokenContract = WATCHED["$packageName.$calleeName"] ?: return@analyze

      report(
        Finding(
          Entity.from(expression.calleeExpression ?: expression),
          "`$calleeName(...)` without `<T>` lets the compiler infer the reified type from the " +
            "argument or assignment target — inference is fragile and may pick a leaf subtype, " +
            "breaking the $brokenContract (the polymorphic `type` discriminator vanishes on the wire). " +
            "Name the type explicitly at the call site: `$calleeName<SealedParent>(...)`. " +
            "See `internal/patterns/docs/kotlin/rest-response.md`.",
        ),
      )
    }
  }

  companion object {
    /**
     * Watched callable IDs (`package.functionName`) mapped to the contract that breaks when `T` is
     * inferred — the label goes into the finding message. Add entries here to extend the rule's reach.
     */
    private val WATCHED: Map<String, String> = mapOf(
      "io.ktor.server.response.respond" to "polymorphic serializer for the response",
      "io.ktor.server.response.respondNullable" to "polymorphic serializer for the response",
      "io.ktor.server.request.receive" to "polymorphic deserializer for the request",
      "io.ktor.server.request.receiveNullable" to "polymorphic deserializer for the request",
      "io.ktor.client.request.setBody" to "polymorphic serializer for the outgoing request",
      "io.ktor.client.call.body" to "polymorphic deserializer for the client response",
      "kotlinx.serialization.encodeToString" to "polymorphic serializer for JSON encoding",
      "kotlinx.serialization.decodeFromString" to "polymorphic deserializer for JSON decoding",
      "kotlinx.serialization.json.encodeToJsonElement" to "polymorphic serializer for JsonElement encoding",
      "kotlinx.serialization.json.decodeFromJsonElement" to "polymorphic deserializer for JsonElement decoding",
    )
  }
}
