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

/**
 * Resolves a GitLab access token from environment variables or `.env` file.
 *
 * Resolution order (via [DotEnvSupport]):
 * 1. `CI_JOB_TOKEN` — CI pipelines (returns [GitLabAuthHeader.JobToken])
 * 2. `GITLAB_ACCESS_TOKEN` — `.env` file or environment (returns [GitLabAuthHeader.AccessToken])
 */
object GitLabTokenResolver {

  /**
   * Resolves a GitLab token and returns the appropriate [GitLabAuthHeader].
   *
   * @throws IllegalStateException if no token can be resolved
   */
  fun resolve(): GitLabAuthHeader {
    val env = DotEnvSupport.dotEnv

    env["CI_JOB_TOKEN"]?.let {
      return GitLabAuthHeader.JobToken(it)
    }

    env["GITLAB_ACCESS_TOKEN"]?.let {
      return GitLabAuthHeader.AccessToken(it)
    }

    throw IllegalStateException(
      "No GitLab token available. Set GITLAB_ACCESS_TOKEN in .env or environment.",
    )
  }
}

/**
 * Type-safe representation of a GitLab API authentication header.
 *
 * GitLab requires **different HTTP header names** depending on the token type:
 * - CI job tokens → `JOB-TOKEN` header (only supported auth method for job tokens)
 * - Personal/project access tokens → `PRIVATE-TOKEN` header
 *
 * Using the wrong header name results in HTTP 401. `Authorization: Bearer` is NOT
 * supported for job tokens.
 *
 * See: https://docs.gitlab.com/api/rest/authentication/
 * See: https://docs.gitlab.com/ci/jobs/ci_job_token/
 */
sealed interface GitLabAuthHeader {
  val headerName: String
  val headerValue: String

  /** CI job token — authenticated via `JOB-TOKEN` header. */
  data class JobToken(override val headerValue: String) : GitLabAuthHeader {
    override val headerName: String = "JOB-TOKEN"
  }

  /** Personal or project access token — authenticated via `PRIVATE-TOKEN` header. */
  data class AccessToken(override val headerValue: String) : GitLabAuthHeader {
    override val headerName: String = "PRIVATE-TOKEN"
  }
}
