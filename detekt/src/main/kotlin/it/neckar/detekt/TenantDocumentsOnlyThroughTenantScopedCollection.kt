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
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Enforces TENANT.2 and TENANT.3 from `docs/mongodb/filter-conventions.md`: outside the mongodb library, code marked
 * `@AllTenants` and `MongoDbMigrator`s, a `MongoCollection` of `BelongsToTenant` documents is reached only through
 * a `TenantScopedCollection` from `MongoCollections.tenantScoped` or `MongoCollections.perTenant`. TENANT.4 there lists the reported calls.
 */
class TenantDocumentsOnlyThroughTenantScopedCollection(config: Config) : Rule(
  config,
  description = "A MongoCollection of BelongsToTenant documents is used only through a TenantScopedCollection (MongoCollections.tenantScoped or perTenant), except in code marked @AllTenants and in migrations (TENANT.2, TENANT.3 in docs/mongodb/filter-conventions.md).",
), RequiresAnalysisApi {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    checkYieldsTenantCollection(expression)
    checkPassesTenantFilterWithoutTenant(expression)
    checkPagesAllTenants(expression)
    checkReadsTenantCollectionByName(expression)
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    super.visitReferenceExpression(expression)
    if (expression !is KtNameReferenceExpression) return
    // A callee is checked as its call; the referenced member of a callable reference is a function, not the collection.
    val call: KtCallExpression? = expression.parent as? KtCallExpression
    if (call?.calleeExpression == expression) return
    if ((expression.parent as? KtCallableReferenceExpression)?.callableReference == expression) return
    // A local variable carries what its initializer yields, and the initializer is reported already.
    val isLocalVariable: Boolean = analyze(expression) { expression.mainReference.resolveToSymbol() is KaLocalVariableSymbol }
    if (isLocalVariable) return
    checkYieldsTenantCollection(expression)
  }

  /**
   * Reports `database.getCollection<BsonDocument>(…)` whose name comes from the descriptor of a tenant collection.
   */
  private fun checkReadsTenantCollectionByName(expression: KtCallExpression) {
    if (isInMongoDbLibrary(expression)) return

    val documentType: String = analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze null
      val callableId = call.symbol.callableId ?: return@analyze null
      if (callableId.callableName.asString() != GetCollectionName) return@analyze null
      // A typed getCollection of tenant documents is already reported as the collection it yields.
      if (tenantDocumentTypeOfCollection(expression.expressionType) != null) return@analyze null

      expression.valueArguments
        .mapNotNull { it.getArgumentExpression() }
        .flatMap { argument -> listOf(argument) + argument.collectDescendantsOfType<KtExpression>() }
        .firstNotNullOfOrNull { tenantDocumentTypeOfDescriptor(it.expressionType) }
    } ?: return

    if (isAllTenantsContext(expression)) return

    report(
      Finding(
        Entity.from(expression),
        "`getCollection` reads the collection of $documentType, documents of every tenant, by its name. A per-tenant Access takes `collections.perTenant(descriptor)` in its constructor, other code uses `collections.tenantScoped<$documentType>(tenantId)`; " +
          "work across tenants belongs in code marked `@AllTenants(reason = …)` or a MongoDbMigrator. See TENANT.2 and TENANT.3 in docs/mongodb/filter-conventions.md.",
      ),
    )
  }

  private fun checkPagesAllTenants(expression: KtCallExpression) {
    if (isInMongoDbLibrary(expression)) return

    val pagesTenantDocuments: Boolean = analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze false
      val callableId = call.symbol.callableId ?: return@analyze false
      if (callableId.asSingleFqName().asString() != ForAllTenantsFqn) return@analyze false
      call.typeArgumentsMapping.values.any { isBelongsToTenant(it) }
    }
    if (pagesTenantDocuments.not()) return

    if (isAllTenantsContext(expression)) return

    report(
      Finding(
        Entity.from(expression),
        "`MongoPageQuery.forAllTenants` pages the documents of every tenant. Page through `MongoPageQuery.create(…, tenantId)`; " +
          "work across tenants belongs in code marked `@AllTenants(reason = …)` or a MongoDbMigrator. See TENANT.2 and TENANT.3 in docs/mongodb/filter-conventions.md.",
      ),
    )
  }

  private fun checkPassesTenantFilterWithoutTenant(expression: KtCallExpression) {
    if (isInMongoDbLibrary(expression)) return

    val functionName: String = analyze(expression) {
      val call = expression.resolveToCall()?.successfulCallOrNull<KaFunctionCall<*>>() ?: return@analyze null
      val callableId = call.symbol.callableId ?: return@analyze null
      // The receiver decides: a call through a subclass resolves to a fake override declared there.
      val receiverType: KaType = (call.partiallyAppliedSymbol.dispatchReceiver ?: call.partiallyAppliedSymbol.extensionReceiver)?.type ?: return@analyze null
      val onDatabaseAccess = receiverType.fqString() == MongoDatabaseConnectionFqn || receiverType.allSupertypes.any { it.fqString() == MongoDatabaseConnectionFqn }
      if (onDatabaseAccess.not()) return@analyze null

      val parameterTypes: List<KaType> = call.partiallyAppliedSymbol.signature.valueParameters.map { it.returnType }
      if (parameterTypes.any { it.fqString() == TenantIdFqn }) return@analyze null

      val takesTenantFilter = parameterTypes.any { type ->
        val filterType = type as? KaClassType ?: return@any false
        val isFilter = filterType.fqString() == MongoFilterFqn || filterType.allSupertypes.any { it.fqString() == MongoFilterFqn }
        isFilter && filterType.typeArguments.firstOrNull()?.type?.let { isBelongsToTenant(it) } == true
      }
      callableId.callableName.asString().takeIf { takesTenantFilter }
    } ?: return

    if (isAllTenantsContext(expression)) return

    report(
      Finding(
        Entity.from(expression),
        "`$functionName` runs the filter of a tenant's documents without a tenant. Use the overload that takes the tenant; a per-tenant Access takes `collections.perTenant(descriptor)` in its constructor, other code uses `collections.tenantScoped(tenantId)`; " +
          "work across tenants belongs in code marked `@AllTenants(reason = …)` or a MongoDbMigrator. See TENANT.2 and TENANT.3 in docs/mongodb/filter-conventions.md.",
      ),
    )
  }

  override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
    super.visitArrayAccessExpression(expression)
    checkYieldsTenantCollection(expression)
  }

  private fun isInMongoDbLibrary(expression: KtExpression): Boolean {
    return expression.containingKtFile.packageFqName.asString().let { it == MongodbLibraryPackage || it.startsWith("$MongodbLibraryPackage.") }
  }

  private fun checkYieldsTenantCollection(expression: KtExpression) {
    if (isInMongoDbLibrary(expression)) return

    val documentType: String = analyze(expression) {
      tenantDocumentTypeOfCollection(expression.expressionType)
    } ?: return

    if (isAllTenantsContext(expression)) return

    report(
      Finding(
        Entity.from(expression),
        "MongoCollection<$documentType> holds documents of every tenant. A per-tenant Access takes `collections.perTenant(descriptor)` in its constructor, other code uses `collections.tenantScoped<$documentType>(tenantId)`; " +
          "work across tenants belongs in code marked `@AllTenants(reason = …)` or a MongoDbMigrator, through `collections.allTenants`. " +
          "See TENANT.2 and TENANT.3 in docs/mongodb/filter-conventions.md.",
      ),
    )
  }

  /**
   * Returns the document type when [type] is a `MongoCollection` of tenant documents, null otherwise.
   */
  private fun KaSession.tenantDocumentTypeOfCollection(type: KaType?): String? {
    val collectionType = type as? KaClassType ?: return null
    if (collectionType.classId.asFqNameString() != MongoCollectionFqn) return null

    val documentType: KaType = collectionType.typeArguments.firstOrNull()?.type ?: return null
    return (documentType.fqString() ?: documentType.toString()).takeIf { isBelongsToTenant(documentType) }
  }

  /**
   * Returns the document type when [type] is a `MongoCollectionDescriptor` of tenant documents, null otherwise.
   */
  private fun KaSession.tenantDocumentTypeOfDescriptor(type: KaType?): String? {
    val descriptorType = type as? KaClassType ?: return null
    if (descriptorType.classId.asFqNameString() != MongoCollectionDescriptorFqn) return null

    val documentType: KaType = descriptorType.typeArguments.firstOrNull()?.type ?: return null
    return (documentType.fqString() ?: documentType.toString()).takeIf { isBelongsToTenant(documentType) }
  }

  private fun KaSession.isBelongsToTenant(type: KaType): Boolean {
    return type.fqString() == BelongsToTenantFqn || type.allSupertypes.any { it.fqString() == BelongsToTenantFqn }
  }

  private fun isAllTenantsContext(expression: KtExpression): Boolean {
    var enclosing: KtDeclaration? = expression.getStrictParentOfType()
    while (enclosing != null) {
      val current: KtDeclaration = enclosing
      val exempt: Boolean = when (current) {
        is KtClassOrObject -> analyze(current) {
          val symbol = current.symbol as? KaClassSymbol ?: return@analyze false
          symbol.annotations.contains(ALL_TENANTS_CLASS_ID) || symbol.superTypes.flatMap { listOf(it) + it.allSupertypes }.any { it.fqString() == MigratorFqn }
        }

        is KtNamedFunction -> analyze(current) { current.symbol.annotations.contains(ALL_TENANTS_CLASS_ID) }
        else -> false
      }
      if (exempt) return true
      enclosing = current.getStrictParentOfType()
    }
    return false
  }

  private fun KaType.fqString(): String? {
    return (this as? KaClassType)?.classId?.asFqNameString()
  }

  companion object {
    const val MongoCollectionFqn: String = "com.mongodb.kotlin.client.coroutine.MongoCollection"
    const val BelongsToTenantFqn: String = "it.neckar.open.tenancy.BelongsToTenant"
    const val MongoFilterFqn: String = "it.neckar.open.mongodb.MongoFilter"
    const val ForAllTenantsFqn: String = "it.neckar.open.mongodb.query.MongoPageQuery.Companion.forAllTenants"
    const val MongoDatabaseConnectionFqn: String = "it.neckar.open.mongodb.MongoDatabaseConnection"
    const val TenantIdFqn: String = "it.neckar.open.tenancy.Tenant.Id"
    const val MigratorFqn: String = "it.neckar.open.mongodb.migration.MongoDbMigrator"
    const val MongodbLibraryPackage: String = "it.neckar.open.mongodb"
    const val MongoCollectionDescriptorFqn: String = "it.neckar.open.mongodb.MongoCollectionDescriptor"
    const val GetCollectionName: String = "getCollection"
    val ALL_TENANTS_CLASS_ID: ClassId = ClassId.topLevel(FqName("it.neckar.open.mongodb.AllTenants"))
  }
}
