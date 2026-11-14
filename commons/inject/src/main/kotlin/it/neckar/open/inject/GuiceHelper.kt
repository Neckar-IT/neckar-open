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
package it.neckar.open.inject

import com.google.inject.Binder
import com.google.inject.Key
import com.google.inject.util.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 *
 * GuiceHelper class.
 *
 */
object GuiceHelper {
  /**
   *
   * superCollectionOf
   *
   * @param type a Type object.
   * @return a ParameterizedType object.
   */
  @JvmStatic
  fun superCollectionOf(type: Type): ParameterizedType {
    return Types.newParameterizedType(MutableCollection::class.java, Types.subtypeOf(type))
  }

  /**
   *
   * superListOf
   *
   * @param type a Type object.
   * @return a ParameterizedType object.
   */
  @JvmStatic
  fun superListOf(type: Type): ParameterizedType {
    return Types.newParameterizedType(MutableList::class.java, Types.subtypeOf(type))
  }

  /**
   * Binds a wildcard collection to the set of that type
   *
   * @param binder the binder
   * @param type   the type
   * @param <T>    a T object.
   */
  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  fun <T> bindWildcardCollectionForSet(binder: Binder, type: Type) {
    val key = Key.get(superCollectionOf(type)) as Key<Collection<T?>>
    val targetKey = Key.get(Types.setOf(type)) as Key<out Collection<T?>>
    binder.bind(key).to(targetKey)
  }
}
