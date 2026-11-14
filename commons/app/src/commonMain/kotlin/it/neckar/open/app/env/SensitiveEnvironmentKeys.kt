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
package it.neckar.open.app.env

/**
 * Decides which environment values must not reach a log.
 *
 * A substring match, not a list of known names: every `*_TOKEN`, `*_PASSWORD` and `*_SECRET` a
 * developer adds to an `.env` is covered without anyone maintaining a registry.
 */
object SensitiveEnvironmentKeys {
  /** What a masked value looks like in a log. */
  const val MaskedValue: String = "****"

  private val sensitivePatterns: List<String> = listOf("TOKEN", "PASSWORD", "SECRET", "KEY", "CERT")

  /** True when [key] names a credential. */
  fun isSensitive(key: String): Boolean = sensitivePatterns.any { key.contains(it, ignoreCase = true) }

  /** [value] as it may appear in a log: [MaskedValue] when [key] names a credential. */
  fun maskIfSensitive(key: String, value: String?): String? = if (isSensitive(key)) MaskedValue else value
}
