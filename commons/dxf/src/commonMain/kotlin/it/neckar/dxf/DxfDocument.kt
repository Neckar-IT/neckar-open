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
 * The parsed content of a DXF file.
 */
data class DxfDocument(
  /**
   * The entities of the `ENTITIES` section in file order: the model space and the active paper space
   * ([DxfEntityProperties.paperSpace]).
   */
  val entities: List<DxfEntity>,
  val header: DxfHeader = DxfHeader(),
  val layerTable: List<DxfLayer> = emptyList(),
  val linetypes: List<DxfLinetype> = emptyList(),
  val blocks: List<DxfBlock> = emptyList(),
) {
  private val layersByName: Map<String, DxfLayer> by lazy { layerTable.associateBy { it.name.uppercase() } }
  private val linetypesByName: Map<String, DxfLinetype> by lazy { linetypes.associateBy { it.name.uppercase() } }
  private val blocksByName: Map<String, DxfBlock> by lazy { blocks.associateBy { it.name.uppercase() } }

  /**
   * The distinct layer names the entities use; [layerTable] holds the `LAYER` entries.
   */
  val usedLayerNames: Set<String>
    get() = entities.mapTo(mutableSetOf()) { it.layer }

  /**
   * The entities of the model space.
   */
  val modelSpaceEntities: List<DxfEntity>
    get() = entities.filter { it.properties.paperSpace.not() }

  /**
   * Returns all entities on the given [layer].
   */
  fun entitiesOnLayer(layer: String): List<DxfEntity> {
    return entities.filter { it.layer == layer }
  }

  /**
   * Returns all entities of the requested concrete type [T].
   */
  inline fun <reified T : DxfEntity> entitiesOfType(): List<T> {
    return entities.filterIsInstance<T>()
  }

  /**
   * The `LAYER` table entry of [name]; DXF compares table names without regard to case.
   */
  fun layer(name: String): DxfLayer? = layersByName[name.uppercase()]

  fun linetype(name: String): DxfLinetype? = linetypesByName[name.uppercase()]

  fun block(name: String): DxfBlock? = blocksByName[name.uppercase()]
}
