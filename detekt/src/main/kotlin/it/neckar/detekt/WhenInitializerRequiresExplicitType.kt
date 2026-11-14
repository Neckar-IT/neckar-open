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
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtWhenExpression

/**
 * Forbids `val x = when (...) { ... }` declarations without an explicit type annotation, unless
 * the inferred type is one of the Kotlin primitive(-equivalent) types `String`, `Int`, `Long`,
 * `Boolean`, `Double`, `Float`, `Short`, `Byte`, `Char`.
 *
 * Why: Ktor's `call.respond(message)` is `inline fun <reified T> respond(T)`. The polymorphic
 * serializer (and the OpenAPI schema) is selected over the **static** type of the argument. With
 * `val response = when (...) { ... }`, the compiler infers the type from the branch results. A
 * refactor that adds a branch returning `Unit`, a concrete subtype, or anything that widens to
 * `Any` silently changes the static type — no compile error, but the polymorphic `type`
 * discriminator vanishes on the wire and the frontend `switch (response.type)` falls through
 * silently.
 *
 * Forcing the explicit type on the `val` makes the response contract load-bearing: the sealed
 * parent (e.g. `UpsertBookResponse`) is named at the binding site, and the moment any branch
 * fails to return that type the build breaks at the binding, not later at the call site.
 *
 * Primitives are exempted because their inference is unambiguous and refactoring-safe: a `when`
 * that yields `String` branches will stay `String` across refactors, and an explicit `: String`
 * adds noise without preventing any class of bug. (`String` is on the JVM a reference type, not a
 * `PrimitiveType` in the compiler sense — included here because it is a primitive-equivalent for
 * everyday Kotlin code.)
 *
 * Companion rules: [RestResponseConstructorMustUseFactory] and [RespondWithSealedSubclass] —
 * together the three form a defense-in-depth around the `call.respond` discriminator-drop bug.
 *
 * See `internal/patterns/docs/kotlin/rest-response.md`.
 */
class WhenInitializerRequiresExplicitType(config: Config) : Rule(
  config,
  description = "`val x = when (...) { ... }` without explicit type lets the compiler silently infer a subtype on refactor. Add an explicit `: SealedParent` annotation (primitive types are exempt).",
), RequiresAnalysisApi {

  override fun visitProperty(property: KtProperty) {
    super.visitProperty(property)

    if (property.typeReference != null) return
    val whenExpression = property.initializer as? KtWhenExpression ?: return

    analyze(property) {
      val inferredType = whenExpression.expressionType ?: return@analyze
      if (inferredType is KaTypeParameterType) return@analyze
      val fqn = inferredType.classIdFqString() ?: return@analyze
      if (fqn == NothingFqn) return@analyze
      if (fqn in KOTLIN_PRIMITIVE_FQNS) return@analyze

      val typeName = fqn.substringAfterLast('.')
      val propertyName = property.name ?: "<anonymous>"
      report(
        Finding(
          Entity.from(property.nameIdentifier ?: property),
          "`val $propertyName = when (...) { ... }` lets the compiler infer the type silently — a " +
            "refactor that widens any branch's result type (e.g. to a concrete leaf subclass or to " +
            "`Any`) will not fail the build, but breaks polymorphic serialization at `call.respond` " +
            "(the `type` discriminator vanishes). Bind explicitly: `val $propertyName: $typeName = " +
            "when (...) { ... }`. See `internal/patterns/docs/kotlin/rest-response.md`.",
        ),
      )
    }
  }

  companion object {
    private const val NothingFqn: String = "kotlin.Nothing"

    private val KOTLIN_PRIMITIVE_FQNS: Set<String> = setOf(
      "kotlin.String",
      "kotlin.Int",
      "kotlin.Long",
      "kotlin.Boolean",
      "kotlin.Double",
      "kotlin.Float",
      "kotlin.Short",
      "kotlin.Byte",
      "kotlin.Char",
    )
  }
}
