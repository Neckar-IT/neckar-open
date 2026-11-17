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
package it.neckar.open.app.clikt

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.parameters.groups.MutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.OptionDelegate
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import it.neckar.open.app.clikt.model.GitlabAccessToken
import it.neckar.open.app.clikt.model.ProjectPath

/*
 * The credential and endpoint options every neckar.it command line shares — declared on
 * BaseCliktCommand, not on BaseCommand, so a Kotlin/Native binary gets the same flag names, env
 * vars and value classes as a JVM service. Options bound to the JVM (`Url.Absolute` endpoints,
 * the runtime context enums, the CA trust store) stay with BaseCommand in jvmMain.
 */

/**
 * Creates an option for the gitlab access token.
 *
 * This method loads the token from the `.env` file - if no parameter is provided.
 * Call `.required()` if the token is mandatory.
 */
fun BaseCliktCommand<*>.gitlabAccessTokenOption(): NullableOption<GitlabAccessToken, GitlabAccessToken> {
  return option(
    help = $$"Gitlab Access Token. Generate @ $SERVER_URL/-/user_settings/personal_access_tokens",
    envvar = "GITLAB_ACCESS_TOKEN",
    names = arrayOf("--gitlab-access-token"),
  ).convert { value ->
    GitlabAccessToken(value)
  }
}

/**
 * Creates an optional option for the GitLab Admin Token.
 *
 * This is separate from [gitlabAccessTokenOption] because admin operations
 * (like user creation) require elevated privileges that normal users don't have.
 *
 * Uses the env var `GITLAB_ADMIN_TOKEN` when present.
 */
fun BaseCliktCommand<*>.gitlabAdminTokenOption(): NullableOption<GitlabAccessToken, GitlabAccessToken> {
  return option(
    help = "GitLab Admin Token (requires admin scope for user management)",
    envvar = "GITLAB_ADMIN_TOKEN",
    names = arrayOf("--gitlab-admin-token"),
  ).convert { value ->
    GitlabAccessToken(value)
  }
}

/**
 * Creates an option for the GitLab project (ID or path).
 *
 * Uses the env var `GITLAB_PROJECT` when present.
 * Call `.required()` if the project is mandatory.
 */
fun BaseCliktCommand<*>.gitlabProjectOption(): NullableOption<ProjectPath, ProjectPath> {
  return option(
    help = "GitLab project (ID or full path, e.g. group/subgroup/project)",
    envvar = "GITLAB_PROJECT",
    names = arrayOf("--gitlab-project"),
  ).convert { value ->
    ProjectPath(value)
  }
}

/**
 * Creates the `--keycloak-realm-url` option for services that validate Keycloak-issued JWTs.
 *
 * Defaults to the standard neckar.it Keycloak realm — services that point at a different realm
 * (e.g. tests, alternate stages) can override via the CLI flag or the `KEYCLOAK_REALM_URL` env
 * var. Value stays a [String]; callers wrap into `Url.Absolute(...)` when constructing the
 * `KeycloakConfiguration`.
 */
fun BaseCliktCommand<*>.keycloakRealmUrlOption(): OptionWithValues<String, String, String> {
  return option(
    help = "Keycloak realm URL for JWT validation",
    envvar = "KEYCLOAK_REALM_URL",
    names = arrayOf("--keycloak-realm-url"),
  ).default("https://auth.neckar.it/realms/internal")
}

/**
 * Creates the `--keycloak-client-id` option for services that validate Keycloak-issued JWTs.
 *
 * No default — the client id is service-specific. Callers should chain `.default("foo.neckar.it")`
 * with the service's own client id. Uses the env var `KEYCLOAK_CLIENT_ID` when present.
 * Callers wrap into `JwtAudience(...)` when constructing the `KeycloakConfiguration`.
 */
fun BaseCliktCommand<*>.keycloakClientIdOption(): RawOption {
  return option(
    help = "Keycloak client ID (matches the `aud` claim on Keycloak-issued JWTs)",
    envvar = "KEYCLOAK_CLIENT_ID",
    names = arrayOf("--keycloak-client-id"),
  )
}

/**
 * Creates an optional `--http-port` option for overriding the Ktor server port.
 *
 * Used for worktree-scoped local development where each worktree
 * runs services on different ports to avoid conflicts.
 */
fun BaseCliktCommand<*>.httpPortOption(): NullableOption<Int, Int> {
  return option(
    "--http-port",
    envvar = "SERVICE_HTTP_PORT",
    help = "Override the HTTP port the Ktor server binds to. Used for worktree-scoped local dev.",
  ).int()
}

/**
 * Takes a secret from [file] when that option is given, on the command line or as variable, else from [value]: a Docker secret wins over
 * the variable that a `.env` of a local run or a published configuration example still sets. Both options convert their value, so an invalid
 * value fails the parse also where the file wins. Call `.required()` if the secret is mandatory.
 */
fun <T : Any> BaseCliktCommand<*>.secretFromFileOrValue(value: OptionDelegate<out T?>, file: OptionDelegate<out T?>): MutuallyExclusiveOptions<T, T?> {
  // mutuallyExclusiveOptions keeps the value of the last given option in declaration order.
  return mutuallyExclusiveOptions(value, file)
}
