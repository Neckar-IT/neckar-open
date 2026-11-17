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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * A MongoDB Access returns several results as `Flow<T>` or `Page<T>`, never as a collection: the caller reads the flow inside its
 * transaction as `internal/patterns/docs/kotlin/repository-access.md` describes, without an assumed upper bound.
 *
 * A MongoDB Access is a class named `*Access` or `*AccessAllTenants` whose primary constructor takes a collection, or still the
 * database handle [AccessTakesItsCollection] reports; an Access of another system, such as the GitLab API, is out of reach. The
 * rule recognises a collection parameter by its written type name and a database handle as [databaseHandleTypeOf] decides.
 */
class AccessReturnsNoCollection(config: Config) : Rule(
  config,
  description = "A MongoDB Access returns several results as Flow<T> (named query*) or Page<T>, never as List, Set, Collection, Iterable, Map, Sequence or Array.",
), RequiresAnalysisApi {

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    val name: String = klass.name ?: return
    if (name.endsWith(AccessTakesItsCollection.AccessSuffix).not() && name.endsWith(AccessTakesItsCollection.AccessAllTenantsSuffix).not()) return
    val takesMongoDbSource: Boolean = klass.primaryConstructorParameters.any { parameter ->
      parameter.typeReference?.nameAsWritten() in CollectionParameterNames || databaseHandleTypeOf(parameter) != null
    }
    if (takesMongoDbSource.not()) return

    klass.body?.functions.orEmpty()
      .filter { it.hasModifier(KtTokens.PRIVATE_KEYWORD).not() }
      .forEach { function -> reportCollection(name, function) }
  }

  private fun reportCollection(className: String, function: KtNamedFunction) {
    val returnType: String = function.typeReference?.nameAsWritten()?.takeIf { it in CollectionTypeNames } ?: return
    report(
      Finding(
        Entity.from(function.nameIdentifier ?: function),
        "$className.${function.name} returns $returnType. An Access returns several results as `Flow<T>`, named `query*`, or as `Page<T>` for a REST or UI table; " +
          "the caller reads the flow inside its transaction, see internal/patterns/docs/kotlin/repository-access.md#access-returns-no-collection.",
      ),
    )
  }

  companion object {
    val CollectionParameterNames: Set<String> = setOf("CollectionPerTenant", "MongoCollection", "TenantScopedCollection")

    val CollectionTypeNames: Set<String> = setOf(
      "List", "MutableList", "Set", "MutableSet", "Collection", "MutableCollection", "Iterable", "Map", "MutableMap", "Sequence", "Array",
    )
  }
}
