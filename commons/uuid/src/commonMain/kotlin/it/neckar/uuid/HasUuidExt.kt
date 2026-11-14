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
package it.neckar.uuid

import it.neckar.open.http.Url
import it.neckar.open.http.UrlPattern
import kotlin.uuid.Uuid


/**
 * Returns a new list with the replaced module
 */
fun <T : HasUuid> List<T>.withReplaced(updatedElement: T): List<T> {
  return withReplaced(updatedElement.uuid, updatedElement)
}

/**
 * Returns a new list with the replaced elements.
 * Elements whose UUID is not present in the receiver are appended at the end.
 */
fun <T : HasUuid> List<T>.withReplaced(updatedElements: List<T>): List<T> {
  val mutableList = this.toMutableList()
  updatedElements.forEach { updatedElement ->
    val index = mutableList.indexOfFirst { it.uuid == updatedElement.uuid }
    if (index < 0) {
      mutableList.add(updatedElement)
    } else {
      mutableList[index] = updatedElement
    }
  }
  return mutableList
}

/**
 * Returns a new list with the element with the given UUID replaced.
 * If no element with [uuid] is present, [updatedElement] is appended.
 */
fun <T : HasUuid> List<T>.withReplaced(uuid: Uuid, updatedElement: T): List<T> {
  val mutableList = this.toMutableList()
  val index = mutableList.indexOfFirst { it.uuid == uuid }
  if (index < 0) {
    mutableList.add(updatedElement)
  } else {
    mutableList[index] = updatedElement
  }
  return mutableList
}

fun <T : HasUuid> List<T>.withAdded(addedElement: T): List<T> {
  return this.toMutableList() + addedElement
}

/**
 * Returns a new list without the element(s) described by the given UUID
 */
fun <T : HasUuid> List<T>.withRemoved(removedElement: T): List<T> {
  return filter {
    it.uuid != removedElement.uuid
  }
}


/**
 * Appends the provided ID to the URL.
 */
operator fun Url.Relative.plus(toAppend: HasUuid): Url.Relative {
  return this + toAppend.uuid
}

/**
 * Resolves the URL template.
 * Must provide the correct number of parameters.
 */
fun UrlPattern.RelativeMulti.resolve(vararg parameterValues: HasUuid): Url.Relative {
  return resolve(parameterValues.map { it.uuid.toHexDashString() })
}

/**
 * Resolves the URL template with an uuid parameter value
 */
fun UrlPattern.Relative1.resolve(parameterValue: HasUuid): Url.Relative {
  return resolve(parameterValue.uuid)
}

/**
 * Resolves the URL template with two uuid parameter values
 */
fun UrlPattern.Relative2.resolve(parameterValue1: HasUuid, parameterValue2: HasUuid): Url.Relative {
  return resolve(parameterValue1.uuid, parameterValue2.uuid)
}

/**
 * Resolves the URL template with three uuid parameter values
 */
fun UrlPattern.Relative3.resolve(parameterValue1: HasUuid, parameterValue2: HasUuid, parameterValue3: HasUuid): Url.Relative {
  return resolve(parameterValue1.uuid, parameterValue2.uuid, parameterValue3.uuid)
}
