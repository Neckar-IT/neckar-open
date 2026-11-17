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
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

/**
 * Flags `gitLabApi.mergeRequestApi`, `gitLabApi.getMergeRequestApi()` and every `AbstractApi` subclass outside the package
 * `it.neckar.tools.gitlab.api`; a test fake that overrides an accessor declares it and passes.
 */
class GitLabAccessThroughClient(config: Config) : Rule(
  config,
  description = "Every GitLab request goes through an operation of `GitLabClient` in `gitlab-api`, the one place that calls the API classes of gitlab4j.",
) {

  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    super.visitQualifiedExpression(expression)
    if (expression.containingKtFile.isInClientPackage()) return

    val accessor: String = when (val selector = expression.selectorExpression) {
      is KtNameReferenceExpression -> selector.getReferencedName().takeIf { it in AccessorProperties }
      is KtCallExpression -> (selector.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
        ?.takeIf { it in AccessorGetters && selector.valueArguments.isEmpty() }
      else -> null
    } ?: return

    report(Finding(Entity.from(expression), "`$accessor` calls gitlab4j past `GitLabClient`. Add the operation to `GitLabClient` in `gitlab-api` and call it there."))
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)
    if (classOrObject.containingKtFile.isInClientPackage()) return

    val extendsAbstractApi: Boolean = classOrObject.superTypeListEntries.any { entry ->
      entry.typeReference?.text?.substringBefore('<')?.let { it == AbstractApiName || it == "$Gitlab4jPackage.$AbstractApiName" } == true
    }
    if (extendsAbstractApi.not()) return

    report(Finding(Entity.atName(classOrObject), "A raw REST call through `$AbstractApiName` belongs into an operation of `GitLabClient` in `gitlab-api`."))
  }

  private fun KtFile.isInClientPackage(): Boolean {
    val packageName: String = packageFqName.asString()
    return packageName == ClientPackage || packageName.startsWith("$ClientPackage.")
  }

  companion object {
    private const val ClientPackage = "it.neckar.tools.gitlab.api"
    private const val Gitlab4jPackage = "org.gitlab4j.api"
    private const val AbstractApiName = "AbstractApi"

    /** The property names of the API class accessors of `org.gitlab4j.api.GitLabApi` 6. */
    private val AccessorProperties: Set<String> = setOf(
      "applicationsApi",
      "applicationSettingsApi",
      "auditEventApi",
      "awardEmojiApi",
      "boardsApi",
      "commitsApi",
      "containerRegistryApi",
      "deployKeysApi",
      "deploymentsApi",
      "deployTokensApi",
      "discussionsApi",
      "environmentsApi",
      "epicsApi",
      "errorTrackingApi",
      "eventsApi",
      "externalStatusCheckApi",
      "gitLabCiYamlApi",
      "groupApi",
      "healthCheckApi",
      "importExportApi",
      "issuesApi",
      "jobApi",
      "keysApi",
      "labelsApi",
      "licenseApi",
      "licenseTemplatesApi",
      "markdownApi",
      "mergeRequestApi",
      "metadataApi",
      "milestonesApi",
      "namespaceApi",
      "notesApi",
      "notificationSettingsApi",
      "packagesApi",
      "personalAccessTokenApi",
      "pipelineApi",
      "projectApi",
      "protectedBranchesApi",
      "releaseLinksApi",
      "releasesApi",
      "repositoryApi",
      "repositoryFileApi",
      "repositorySubmodulesApi",
      "resourceLabelEventsApi",
      "resourceStateEventsApi",
      "runnersApi",
      "searchApi",
      "servicesApi",
      "snippetsApi",
      "systemHooksApi",
      "tagsApi",
      "todosApi",
      "topicsApi",
      "userApi",
      "wikisApi",
    )

    private val AccessorGetters: Set<String> = AccessorProperties.map { name -> "get" + name.replaceFirstChar { it.uppercase() } }.toSet()
  }
}
