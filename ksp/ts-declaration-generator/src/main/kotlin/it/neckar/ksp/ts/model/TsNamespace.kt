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
package it.neckar.ksp.ts.model

/**
 * Represents a namespace in TypeScript.
 *
 * Namespaces are used to represent nested classes
 */
data class TsNamespace(val parent: TsNamespace?, val name: String) : Comparable<TsNamespace> {

  constructor(name: String) : this(null, name)

  override fun compareTo(other: TsNamespace): Int {
    return if (parent == null) {
      if (other.parent == null) {
        name.compareTo(other.name)
      } else {
        -1
      }
    } else {
      if (other.parent == null) {
        1
      } else {
        parent.compareTo(other.parent)
      }
    }
  }

  override fun toString(): String {
    return "TsNamespace(parent=$parent, name='$name')"
  }


  companion object {
    fun of(name: String?): TsNamespace? {
      if (name == null) {
        return null
      }

      return TsNamespace(name)
    }
  }
}
