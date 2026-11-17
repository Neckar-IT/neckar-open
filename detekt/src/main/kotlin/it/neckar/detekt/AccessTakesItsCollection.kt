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
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * A class named `*Access` or `*AccessAllTenants` takes the collection it reads in its primary constructor, never the
 * database handle (a subclass of `MongoConnection`) or the `MongoCollections` it would take the collection or a
 * transaction from, and implements no `TransactionScope`: the wiring shows every collection a class reaches and
 * replaces each one on its own.
 */
class AccessTakesItsCollection(config: Config) : Rule(
  config,
  description = "An Access takes its CollectionPerTenant or MongoCollection in the primary constructor, never a database handle (MongoConnection) or MongoCollections, and implements no TransactionScope; a companion `of(collections, …)` derives the collection.",
), RequiresAnalysisApi {

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    val name: String = klass.name ?: return
    if (name.endsWith(AccessSuffix).not() && name.endsWith(AccessAllTenantsSuffix).not()) return

    klass.primaryConstructorParameters.forEach { parameter ->
      val handleType: String = databaseHandleTypeOf(parameter) ?: return@forEach
      report(
        Finding(
          Entity.from(parameter),
          "$name takes `$handleType` in its primary constructor. An Access takes the collection it reads: `CollectionPerTenant<T>` for the documents of a tenant, " +
            "`MongoCollection<T>` for documents without tenant or, marked `@AllTenants`, of every tenant; a companion `fun of(collections: MongoCollections, …)` derives it " +
            "(`collections.perTenant(Descriptor)`, `collections[Descriptor]`, `collections.allTenants(Descriptor)`). A Support takes the `TransactionScope`.",
        ),
      )
    }

    val namesTransactionScope: Boolean = klass.superTypeListEntries.any { it.typeReference?.nameAsWritten() == TransactionScopeName }
    val implementsTransactionScope: Boolean = namesTransactionScope || analyze(klass) {
      val symbol = klass.symbol as? KaClassSymbol ?: return@analyze false
      symbol.superTypes.flatMap { listOf(it) + it.allSupertypes }.any { it.fqString() == TransactionScopeFqn }
    }
    if (implementsTransactionScope) {
      report(
        Finding(
          Entity.from(klass.nameIdentifier ?: klass),
          "$name implements TransactionScope. An Access reads and writes inside the transaction of its caller; the wiring hands `database.transactionScope` to the Support that opens it.",
        ),
      )
    }
  }

  companion object {
    const val AccessSuffix: String = "Access"
    const val AccessAllTenantsSuffix: String = "AccessAllTenants"
    const val TransactionScopeFqn: String = "it.neckar.open.mongodb.TransactionScope"
    const val TransactionScopeName: String = "TransactionScope"

    /**
     * The base classes of the database handles and the collection registry, by written name.
     */
    val DatabaseHandleNames: Set<String> = setOf("MongoConnection", "MongoDatabaseConnection", "MongoCollections")

    /**
     * The suffix of every database handle of a service, such as `PatternsServiceDatabase`.
     */
    const val DatabaseSuffix: String = "Database"

    /**
     * The database type of the MongoDB driver: it ends in [DatabaseSuffix] and is no database handle.
     */
    const val DriverDatabaseName: String = "MongoDatabase"

    /**
     * Every database handle extends `MongoConnection`, whatever its name; `MongoCollections` is the collection registry.
     */
    val DatabaseHandleFqns: Set<String> = setOf("it.neckar.open.mongodb.MongoConnection", "it.neckar.open.mongodb.MongoCollections")

    /**
     * Whether the written type name [name] is a database handle or the collection registry.
     */
    fun isDatabaseHandleName(name: String): Boolean {
      return name in DatabaseHandleNames || (name.endsWith(DatabaseSuffix) && name != DriverDatabaseName)
    }
  }
}

/**
 * The short name of the type of [parameter] when it is a database handle or the collection registry, `null` otherwise.
 *
 * A resolved handle among the type and its supertypes decides; otherwise the written name decides where the type or
 * one of its supertypes does not resolve, as happens while type resolution fails on this codebase
 * (`internal/open/detekt/README.md`, "Analysis-API rules are silent right now").
 */
internal fun databaseHandleTypeOf(parameter: KtParameter): String? {
  val writtenName: String? = parameter.typeReference?.nameAsWritten()
  val resolvedVerdict: HandleVerdict = analyze(parameter) {
    val type: KaType = parameter.returnType
    val typeAndSupertypes: List<KaType> = listOf(type) + type.allSupertypes
    when {
      typeAndSupertypes.any { it.fqString() in AccessTakesItsCollection.DatabaseHandleFqns } -> HandleVerdict.Handle((type as? KaClassType)?.classId?.shortClassName?.asString() ?: writtenName ?: type.toString())
      typeAndSupertypes.any { it is KaErrorType } -> HandleVerdict.Unresolved
      else -> HandleVerdict.NoHandle
    }
  }
  return when (resolvedVerdict) {
    is HandleVerdict.Handle -> resolvedVerdict.typeName
    HandleVerdict.NoHandle -> null
    HandleVerdict.Unresolved -> writtenName?.takeIf { AccessTakesItsCollection.isDatabaseHandleName(it) }
  }
}

/**
 * The fully qualified name of a class type, `null` for every other type.
 */
internal fun KaType.fqString(): String? {
  return (this as? KaClassType)?.classId?.asFqNameString()
}

/**
 * What the resolved type of a constructor parameter says about it.
 */
private sealed interface HandleVerdict {
  /** The type or one of its supertypes is `MongoConnection` or `MongoCollections`. */
  data class Handle(val typeName: String) : HandleVerdict

  /** The type resolves and is neither. */
  data object NoHandle : HandleVerdict

  /** The type or one of its supertypes does not resolve; the written name decides. */
  data object Unresolved : HandleVerdict
}

/**
 * The type name as written, without nullability.
 */
internal fun KtTypeReference.nameAsWritten(): String? {
  val element = typeElement
  val userType: KtUserType? = (element as? KtNullableType)?.innerType as? KtUserType ?: element as? KtUserType
  return userType?.referencedName
}
