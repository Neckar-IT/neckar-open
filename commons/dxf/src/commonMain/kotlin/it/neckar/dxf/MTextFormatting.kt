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
package it.neckar.dxf

/** Private-use placeholders that temporarily stand in for escaped characters during cleanup. */
private const val PlaceholderBackslash: Char = ''
private const val PlaceholderBraceOpen: Char = ''
private const val PlaceholderBraceClose: Char = ''

private const val NonBreakingSpace: Char = ' '

/**
 * Matches an inline formatting code that takes an argument terminated by `;`,
 * e.g. `\fArial|b0|i0;`, `\H0.8x;`, `\pxql;`, `\C1;`, `\A1;`.
 */
private val FORMATTING_CODE_WITH_ARGUMENT = Regex("""\\[A-Za-z][^;\\}]*;""")

/**
 * Matches a remaining single-letter formatting code without an argument, e.g. `\L`, `\O`, `\K`.
 */
private val FORMATTING_CODE_BARE = Regex("""\\[A-Za-z]""")

/**
 * Strips AutoCAD MTEXT inline formatting from [raw] and returns plain text.
 *
 * Conversions:
 * - `\P` becomes a line break (`\n`)
 * - `\~` becomes a non-breaking space
 * - `^I` becomes a tab
 * - font/height/color/paragraph codes (`{\f...;}`, `\H...;`, `\C...;`, `\p...;`, ...) are removed
 * - grouping braces `{` `}` are removed, while escaped `\{` `\}` `\\` are kept as literal characters
 *
 * Limitation: stacked fractions (`\S1/2;`) are removed wholesale rather than rendered.
 */
fun cleanMTextFormatting(raw: String): String {
  var text = raw

  // Protect escaped characters so the formatting removal below does not touch them.
  text = text
    .replace("\\\\", PlaceholderBackslash.toString())
    .replace("\\{", PlaceholderBraceOpen.toString())
    .replace("\\}", PlaceholderBraceClose.toString())

  text = text
    .replace("\\P", "\n")
    .replace("\\~", NonBreakingSpace.toString())
    .replace("^I", "\t")

  text = FORMATTING_CODE_WITH_ARGUMENT.replace(text, "")
  text = text.replace("{", "").replace("}", "")
  text = FORMATTING_CODE_BARE.replace(text, "")

  // Restore the protected escaped characters as their literal form.
  text = text
    .replace(PlaceholderBackslash, '\\')
    .replace(PlaceholderBraceOpen, '{')
    .replace(PlaceholderBraceClose, '}')

  return text.trim()
}
