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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Flags every call of the constructor `org.gitlab4j.api.GitLabApi(…)` outside the file that declares the factory
 * `it.neckar.tools.gitlab.api.gitLabApi`.
 *
 * PSI-based, so it fires without type resolution. Reported are `GitLabApi(…)` behind an explicit or star import of
 * `org.gitlab4j.api`, behind an import alias, and `org.gitlab4j.api.GitLabApi(…)` fully qualified.
 */
class GitLabApiViaFactory(config: Config) : Rule(
  config,
  description = "A `GitLabApi` comes from `it.neckar.tools.gitlab.api.gitLabApi(…)`, which sets connect and read timeouts — the constructor leaves a blocking socket read waiting forever.",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    val callee = expression.calleeExpression as? KtNameReferenceExpression ?: return
    val qualifier = (expression.parent as? KtDotQualifiedExpression)?.takeIf { it.selectorExpression == expression }

    val constructsGitLabApi = if (qualifier == null) {
      callee.getReferencedName() in expression.containingKtFile.gitLabApiNames()
    } else {
      callee.getReferencedName() == GitLabApiName && qualifier.receiverExpression.qualifiedName() == Gitlab4jPackage
    }
    if (constructsGitLabApi.not()) return
    if (expression.containingKtFile.declaresFactory()) return

    report(
      Finding(
        Entity.from(qualifier ?: expression),
        "`GitLabApi(…)` sends requests without a read timeout. Use `gitLabApi(…)` from `it.neckar.tools.gitlab.api`.",
      ),
    )
  }

  /**
   * `org.gitlab4j.api` for the expression `org.gitlab4j.api`, or `null` when a segment is no plain name.
   */
  private fun KtExpression.qualifiedName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> {
      val receiver = receiverExpression.qualifiedName()
      val selector = (selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
      if (receiver == null || selector == null) null else "$receiver.$selector"
    }
    else -> null
  }

  /**
   * The simple names under which `org.gitlab4j.api.GitLabApi` is visible in this file: every import alias, and
   * `GitLabApi` when an explicit or star import brings it in and no explicit import binds that name to another class.
   */
  private fun KtFile.gitLabApiNames(): Set<String> {
    val aliases = importDirectives
      .filter { it.importedFqName?.asString() == GitLabApiFqName }
      .mapNotNull { it.aliasName }
      .toSet()
    val imported = importDirectives.any {
      (it.importedFqName?.asString() == GitLabApiFqName && it.aliasName == null) ||
        (it.isAllUnder && it.importedFqName?.asString() == Gitlab4jPackage)
    }
    val simpleNameTaken = importDirectives.any {
      it.importedName?.asString() == GitLabApiName && it.importedFqName?.asString() != GitLabApiFqName
    }
    return if (imported && simpleNameTaken.not()) aliases + GitLabApiName else aliases
  }

  private fun KtFile.declaresFactory(): Boolean {
    if (packageFqName.asString() != DeclaringPackage) return false
    return declarations.any { it is KtNamedFunction && it.name == DeclaredFunction }
  }

  companion object {
    private const val GitLabApiName = "GitLabApi"
    private const val Gitlab4jPackage = "org.gitlab4j.api"
    private const val GitLabApiFqName = "$Gitlab4jPackage.$GitLabApiName"
    private const val DeclaringPackage = "it.neckar.tools.gitlab.api"
    private const val DeclaredFunction = "gitLabApi"
  }
}
