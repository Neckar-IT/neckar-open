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

/**
 * A single DXF group code / value pair. A DXF file is a flat stream of such pairs: every two lines
 * form one pair - the first line is the numeric group code, the second line is its value.
 */
internal data class DxfGroupCode(
  val code: Int,
  val value: String,
) {
  /**
   * The value as a number. DXF writes numbers as text and pads them with whitespace; a value that is not a number
   * yields `null`.
   */
  val doubleValue: Double?
    get() = value.trim().toDoubleOrNull()

  val intValue: Int?
    get() = value.trim().toIntOrNull()
}
