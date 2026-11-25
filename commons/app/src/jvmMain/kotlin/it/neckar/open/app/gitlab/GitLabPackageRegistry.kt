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
package it.neckar.open.app.gitlab

import it.neckar.open.app.env.DotEnvSupport
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.requireNotNull

/**
 * The Generic Package Registry of one GitLab project.
 *
 * Builds the download URLs of generic packages, so no caller has to assemble such a URL by hand:
 * `<apiBaseUrl>/projects/<projectId>/packages/generic/<packageName>/<version>/<fileName>`.
 *
 * Authentication for the resulting URLs is provided by [GitLabTokenResolver].
 *
 * See: https://docs.gitlab.com/user/packages/generic_packages/
 */
data class GitLabPackageRegistry(
  /**
   * Base URL of the GitLab REST API, e.g. `https://git.neckar.it/api/v4`.
   */
  val apiBaseUrl: Url.Absolute,
  /**
   * The project that owns the packages.
   */
  val projectId: GitLabProjectId,
) {

  /**
   * The download URL of one file of a generic package.
   */
  fun downloadUrl(packageName: PackageName, version: PackageVersion, fileName: PackageFileName): Url.Absolute {
    return apiBaseUrl + "projects/$projectId/packages/generic/$packageName/$version/$fileName"
  }

  /**
   * The name of a generic package, e.g. `turboangebot-db-snapshot`.
   */
  @JvmInline
  value class PackageName(val value: String) {
    init {
      require(value.isNotBlank()) { "package name must not be blank" }
    }

    override fun toString(): String {
      return value
    }
  }

  /**
   * The version of a generic package. Either [Latest] or a version string such as `2.3.2-2026-06-08T01-03-28`.
   */
  @JvmInline
  value class PackageVersion(val value: String) {
    init {
      require(value.isNotBlank()) { "package version must not be blank" }
    }

    override fun toString(): String {
      return value
    }

    companion object {
      /**
       * The mutable version every upload overwrites — always the most recent upload.
       */
      val Latest: PackageVersion = PackageVersion("latest")
    }
  }

  /**
   * The name of one file within a generic package, e.g. `turboangebot-prod.mongodump.gz`.
   */
  @JvmInline
  value class PackageFileName(val value: String) {
    init {
      require(value.isNotBlank()) { "package file name must not be blank" }
    }

    override fun toString(): String {
      return value
    }
  }

  companion object {
    /**
     * The registry of the project the current CI job belongs to, resolved from the GitLab-provided
     * `CI_API_V4_URL` and `CI_PROJECT_ID` (via [DotEnvSupport], so an `.env` file works as well).
     *
     * Returns null when either variable is absent — outside of CI without a matching `.env` entry.
     */
    fun findFromEnvironment(): GitLabPackageRegistry? {
      val environment = DotEnvSupport.dotEnv

      val apiBaseUrl: String = environment["CI_API_V4_URL"] ?: return null
      val projectId: String = environment["CI_PROJECT_ID"] ?: return null

      return GitLabPackageRegistry(
        apiBaseUrl = Url.absolute(apiBaseUrl),
        projectId = GitLabProjectId(
          projectId.toIntOrNull().requireNotNull { "CI_PROJECT_ID must be numeric but was [$projectId]" },
        ),
      )
    }
  }
}
