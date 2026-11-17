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

import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import it.neckar.runtime.context.ServiceHost
import kotlinx.coroutines.*

/**
 * Demo for Clikt interactive prompts with environment variable and system property fallbacks.
 *
 * Run directly from IDE.
 *
 * Priority order for values:
 * 1. CLI argument (e.g., --username foo)
 * 2. Environment variable (e.g., DEMO_USERNAME=foo)
 * 3. System property (e.g., -Ddemo.username=foo)
 * 4. Interactive prompt (asks user)
 *
 * Shows:
 * - Prompted options with env var fallback
 * - Prompted options with system property fallback
 * - Password prompts (hidden input)
 * - Options with defaults
 * - Flags
 */
fun main(args: Array<String>) {
  runBlocking {
    CliktPromptDemo().main(args)
  }
}

/**
 * Demo command showing Clikt prompt patterns with env/sysprop fallbacks.
 */
class CliktPromptDemo : BaseCommand<ServiceHost.Default>(
  name = "clikt-prompt-demo",
  serviceHostRegistry = ServiceHost.Default
) {

  // Prompt with environment variable fallback
  // Priority: CLI > DEMO_USERNAME env var > prompt
  private val username: String by option(
    "--username", "-u",
    help = "Username (or set DEMO_USERNAME env var)",
    envvar = "DEMO_USERNAME",
  ).prompt("Enter username")

  // Prompt with environment variable fallback
  // Priority: CLI > DEMO_EMAIL env var > prompt
  private val email: String by option(
    "--email", "-e",
    help = "Email address (or set DEMO_EMAIL env var)",
    envvar = "DEMO_EMAIL",
  ).prompt("Enter email address")

  // Prompt with system property as default value
  // Priority: CLI > prompt (with sysprop as pre-filled default)
  private val server: String by option(
    "--server", "-s",
    help = "Server URL (or set -Ddemo.server system property)",
  ).prompt("Enter server URL", default = System.getProperty("demo.server"))

  // Combined: env var fallback, system property as prompt default
  // Priority: CLI > DEMO_TOKEN env var > prompt (with sysprop as pre-filled default)
  private val token: String by option(
    "--token", "-t",
    help = "API token (or set DEMO_TOKEN env var or -Ddemo.token)",
    envvar = "DEMO_TOKEN",
  ).prompt("Enter API token", default = System.getProperty("demo.token"), hideInput = true)

  // Password prompt - input is hidden
  private val password: String by option(
    "--password", "-p",
    help = "Password (will be prompted securely if not provided)",
  ).prompt("Enter password", hideInput = true)

  // Optional with default (no prompt)
  private val count: Int by option(
    "--count", "-c",
    help = "Number of items to process",
  ).int().default(1)

  // Flag option
  private val verbose: Boolean by option(
    "--verbose", "-v",
    help = "Enable verbose output",
  ).flag(default = false)

  override suspend fun execute() {
    println()
    println("=== Captured Values ===")
    println()
    println("  Username: $username")
    println("  Email:    $email")
    println("  Server:   $server")
    println("  Token:    ${"*".repeat(token.length)} (${token.length} chars)")
    println("  Password: ${"*".repeat(password.length)} (${password.length} chars)")
    println("  Count:    $count")
    println("  Verbose:  $verbose")
    println()

    if (verbose) {
      println("=== Value Sources ===")
      println()
      println("  DEMO_USERNAME env: ${System.getenv("DEMO_USERNAME") ?: "(not set)"}")
      println("  DEMO_EMAIL env:    ${System.getenv("DEMO_EMAIL") ?: "(not set)"}")
      println("  DEMO_TOKEN env:    ${System.getenv("DEMO_TOKEN")?.let { "****" } ?: "(not set)"}")
      println("  demo.server prop:  ${System.getProperty("demo.server") ?: "(not set)"}")
      println("  demo.token prop:   ${System.getProperty("demo.token")?.let { "****" } ?: "(not set)"}")
      println()
      println("=== Runtime Info ===")
      println()
      println("  Execution Environment: $executionEnvironment")
      println("  Deployment Stage:      $deploymentStage")
      println("  Host:                  $host")
      println()
    }

    println("=== Demo Complete ===")
  }
}
