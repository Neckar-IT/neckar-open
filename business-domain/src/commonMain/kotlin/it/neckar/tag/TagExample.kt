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
package it.neckar.tag

import kotlin.uuid.Uuid

@Suppress("SpellCheckingInspection")
object TagExample {
  val steckdose: Tag = Tag(Tag.Id(Uuid.parse("7770631c-c6df-45dd-b063-1cc6df55ddcd")), "Steckdose")

  val schalter: Tag = Tag(Tag.Id(Uuid.parse("cd6e4e2c-a9a6-4fda-ae4e-2ca9a67fdae1")), "Lichtschalter")

  val thermostat: Tag = Tag(Tag.Id(Uuid.parse("5ad41440-6aac-4df8-9414-406aac2df83f")), "Thermostat")

  val aufputz: Tag = Tag(Tag.Id(Uuid.parse("67b94f27-7f21-489f-b94f-277f21689fc9")), "Aufputz")

  val beleuchtung: Tag = Tag(Tag.Id(Uuid.parse("b1e2c3d4-e5f6-7890-b1e2-c3d4e5f67890")), "Beleuchtung")

  val tags: List<Tag> = listOf(
    steckdose,
    schalter,
    thermostat,
    aufputz,
    beleuchtung
  )
}
