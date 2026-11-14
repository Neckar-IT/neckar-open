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

import com.github.ajalt.mordant.animation.progress.ThreadProgressTaskAnimator
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.speed
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Mordant evaluation demo — demonstrates tables, styling, and progress bars.
 *
 * Run directly from IDE to compare with the current [it.neckar.open.console.AnsiConsole] approach.
 *
 * Covers:
 * 1. **Tables** — equivalent to the manual ASCII construction in ContainerRegistryPrinter
 * 2. **Styling** — colors, bold, combinations as replacement for AnsiConsole
 * 3. **Progress Bar** — animated progress as replacement for AnsiConsole.loadingBar()
 */
fun main() {
  val terminal = Terminal()

  demoStyling(terminal)
  demoTable(terminal)
  demoProgressBar(terminal)

  terminal.println()
  terminal.println(bold("Demo complete."))
}


// --- 1. Styling ---

private fun demoStyling(terminal: Terminal) {
  terminal.println()
  terminal.println(bold("=== Mordant Styling Demo ==="))
  terminal.println()

  // Basic colors (equivalent to AnsiConsole.red(), .green(), etc.)
  terminal.println(red("Red text"))
  terminal.println(green("Green text"))
  terminal.println(blue("Blue text"))
  terminal.println(yellow("Yellow text"))
  terminal.println(magenta("Magenta text"))
  terminal.println(cyan("Cyan text"))
  terminal.println(white("White text"))
  terminal.println(gray("Gray text"))

  // Combinations (not possible with AnsiConsole without manual ANSI codes)
  terminal.println((bold + red)("Bold red"))
  terminal.println((bold + underline + blue)("Bold underline blue"))
  terminal.println((italic + green)("Italic green"))

  // Background colors
  terminal.println((white on red)("White on red background"))
  terminal.println((black on yellow)("Black on yellow background"))

  terminal.println()
}


// --- 2. Table ---

/**
 * Equivalent to [it.neckar.tools.gitlab.docker.cleanup.ContainerRegistryPrinter.formatTags],
 * but using Mordant's table DSL instead of manual padEnd/padStart with ANSI offset calculations.
 */
private fun demoTable(terminal: Terminal) {
  terminal.println(bold("=== Mordant Table Demo ==="))
  terminal.println()
  terminal.println("Simulated ContainerRegistryPrinter output:")
  terminal.println()

  // Simulated container tag data
  data class Tag(
    val path: String,
    val name: String,
    val sizeInMiB: Double,
    val age: kotlin.time.Duration,
    val repositoryId: Int,
    val isMainOrLatest: Boolean,
    val toBeDeleted: Boolean,
  )

  val tags = listOf(
    Tag("registry.neckar.it/neckarit/neckar-hub:latest", "latest", 342.7, 1.days + 4.hours + 12.minutes, 101, isMainOrLatest = true, toBeDeleted = false),
    Tag("registry.neckar.it/neckarit/neckar-hub:main", "main", 338.1, 1.days + 4.hours + 15.minutes, 102, isMainOrLatest = true, toBeDeleted = false),
    Tag("registry.neckar.it/neckarit/neckar-hub:feature-123", "feature-123", 341.2, 5.days + 2.hours + 30.minutes, 103, isMainOrLatest = false, toBeDeleted = true),
    Tag("registry.neckar.it/neckarit/neckar-hub:fix-456", "fix-456", 339.8, 8.days + 14.hours + 45.minutes, 104, isMainOrLatest = false, toBeDeleted = true),
    Tag("registry.neckar.it/neckarit/neckar-hub:release-2.1", "release-2.1", 345.0, 3.hours + 22.minutes, 105, isMainOrLatest = false, toBeDeleted = false),
  )

  val serverUrl = "https://git.neckar.it/neckarit/neckar-hub/container_registry"

  val mordantTable = table {
    header {
      row("Path", "Size in MiB", "Age", "Repo Url")
    }
    body {
      for (tag in tags.sortedBy { it.age }) {
        row {
          // Path: blue for main/latest, yellow for branches
          val coloredName = if (tag.isMainOrLatest) blue(tag.name) else yellow(tag.name)
          cell(tag.path.substringBeforeLast(":") + ":" + coloredName)

          cell(String.format(Locale.ROOT, "%.1f", tag.sizeInMiB))

          // Age: red for to-be-deleted, green for kept
          val ageFormatted = tag.age.toString()
          cell(if (tag.toBeDeleted) red(ageFormatted) else green(ageFormatted))

          cell("$serverUrl/${tag.repositoryId}")
        }
      }
    }
  }

  terminal.println(mordantTable)
  terminal.println()
}


// --- 3. Progress Bar ---

private fun demoProgressBar(terminal: Terminal) {
  terminal.println(bold("=== Mordant Progress Bar Demo ==="))
  terminal.println()

  val total = 25L
  terminal.println("Simulated cleanup of $total container tags:")
  terminal.println()

  val layout = progressBarLayout {
    text("Cleaning up tags")
    percentage()
    progressBar()
    completed()
    speed("tags/s")
    timeRemaining()
  }

  val animator: ThreadProgressTaskAnimator<Unit> = layout.animateOnThread(terminal, total = total)
  val future = animator.execute()

  for (i in 1..total) {
    Thread.sleep(80)
    animator.advance()
  }

  future.get()
  terminal.println()
}
