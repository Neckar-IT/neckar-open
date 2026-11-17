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
 * The variables of the `HEADER` section a renderer needs.
 */
data class DxfHeader(
  /**
   * `$INSUNITS`: the unit of the drawing, null when the header does not set it.
   */
  val insertionUnits: Int? = null,
  /**
   * `$LTSCALE`: the global factor on every dash length.
   */
  val linetypeScale: Double = 1.0,
  /**
   * `$PDMODE`: the shape a `POINT` is drawn as.
   */
  val pointDisplayMode: Int = 0,
  /**
   * `$PDSIZE`: the size of a `POINT` in drawing units if positive, a percentage of the view height if negative.
   */
  val pointDisplaySize: Double = 0.0,
)
