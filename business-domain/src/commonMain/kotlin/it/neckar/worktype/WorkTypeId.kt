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
package it.neckar.worktype

import it.neckar.open.annotations.CrossLayer
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.ValueClassUuid
import it.neckar.uuid.randomForEnvironment
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * Identity of a work type (Arbeitsart) catalog entry — a named kind of installation work with its
 * own hourly rate.
 *
 * Opaque at this layer: the catalog itself (description + historized rate) lives in the product, so
 * trade-neutral code such as [it.neckar.price.ArticleConditions] can reference a work type
 * without knowing the catalog's shape. The product resolves the id to a rate.
 */
@CrossLayer("WorkTypeId — identical shape in Domain, Entity and REST")
@Serializable
@JvmInline
value class WorkTypeId(override val uuid: NativeUuid) : ValueClassUuid {
  override fun toString(): String = uuid.toString()

  companion object {
    fun random(): WorkTypeId = WorkTypeId(Uuid.randomForEnvironment())
  }
}
