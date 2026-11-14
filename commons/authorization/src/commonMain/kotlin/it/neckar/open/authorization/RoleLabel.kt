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
package it.neckar.open.authorization

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Human-readable label for a user's role within a tenant. Predefined [Role]s
 * use stable slug-style values ("owner", "admin") that the UI can map to i18n
 * keys; tenants may later expose this field for custom labels chosen by
 * end users, hence the free-form storage with minimal validation.
 *
 * The label is purely informational. Authorization decisions never inspect it;
 * they consult [UserCapabilities] instead.
 */
@JvmInline
@Serializable
value class RoleLabel(val value: String) {
  init {
    require(value.isNotBlank()) { "RoleLabel must not be blank" }
    require(value.length <= MaxLength) {
      "RoleLabel [$value] exceeds maximum length of $MaxLength"
    }
  }

  override fun toString(): String = value

  companion object {
    const val MaxLength: Int = 64
  }
}
