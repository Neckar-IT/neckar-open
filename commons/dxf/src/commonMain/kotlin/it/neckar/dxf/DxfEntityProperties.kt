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
 * The properties every graphical entity carries besides its layer.
 */
data class DxfEntityProperties(
  /**
   * The linetype name (group code 6); `BYLAYER` and `BYBLOCK` take the linetype of the layer or of the block reference.
   */
  val linetype: String = ByLayer,
  /**
   * The entity's factor on the dash lengths of its linetype (group code 48), applied on top of `$LTSCALE`.
   */
  val linetypeScale: Double = 1.0,
  val lineweight: DxfLineweight = DxfLineweight.ByLayer,
  /**
   * Group code 60: an invisible entity draws nothing.
   */
  val invisible: Boolean = false,
  /**
   * Group code 67: the entity belongs to the paper space, not to the model space.
   */
  val paperSpace: Boolean = false,
  /**
   * The normal of the object coordinate system (group code 210/220/230) of a planar entity.
   */
  val extrusion: DxfPoint = DefaultExtrusion,
) {
  companion object {
    const val ByLayer: String = "BYLAYER"
    const val ByBlock: String = "BYBLOCK"

    val DefaultExtrusion: DxfPoint = DxfPoint(0.0, 0.0, 1.0)

    val Default: DxfEntityProperties = DxfEntityProperties()
  }
}
