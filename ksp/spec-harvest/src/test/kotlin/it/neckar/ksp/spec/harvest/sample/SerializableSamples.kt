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
package it.neckar.ksp.spec.harvest.sample

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class Unit

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class Power

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class Name(val value: String)

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class Symbol(val value: String)

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class Definition(vararg val expressions: String)

@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class SiDerivedUnit(vararg val baseUnits: KClass<*>)

internal class Joule
internal class Second

@MustBeDocumented
@Unit
@Power
@Name("watt")
@SiDerivedUnit(Joule::class, Second::class)
@Symbol(W.Symbol)
@Definition("J/s", "N*m/s", "kg*m²/s³", "VA")
@Target(AnnotationTarget.TYPE)
internal annotation class W {
  companion object {
    const val Symbol: String = "W"
  }
}

/**
 * Represents a customer payload returned by the API.
 */
@Serializable
@Deprecated("Use CustomerPayloadV2")
internal data class CustomerPayload(
  /**
   * Unique customer identifier.
   */
  @SerialName("id_value")
  val id: String,

  /**
   * Optional customer age when known.
   */
  val age: @W Int? = null,

  /**
   * Tags describing the customer in more detail.
   */
  val tags: List<String> = emptyList(),
)

/**
 * Wrapper class that references another serializable type.
 */
@Serializable
internal class Wrapper(
  val payload: CustomerPayload,
)
