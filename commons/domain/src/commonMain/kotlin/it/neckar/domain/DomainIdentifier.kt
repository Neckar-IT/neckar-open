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
package it.neckar.domain

import it.neckar.open.kotlin.lang.toKebabCase
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a domain identifier that categorizes items into business domains.
 *
 * A domain identifier is a simple string-based value that groups related entities.
 * Common examples include:
 * - "electro" for electrical components
 * - "heating" for heating systems
 * - "plumbing" for plumbing equipment
 * - "hvac" for heating, ventilation, and air conditioning
 *
 * This is a value class (inline class) for optimal performance - at runtime,
 * it's represented as a plain String without additional object allocation.
 */
@Serializable
@JvmInline
value class DomainIdentifier(
  /**
   * The string representation of the domain identifier
   */
  val value: String
) : HasDomainIdentifier {

  /**
   * Returns the domain identifier as a URL-friendly path segment (kebab-case).
   *
   * Use this for URL path construction to ensure consistent, URL-friendly paths.
   * Examples:
   * - "Electro" -> "electro"
   * - "PV" -> "pv"
   * - "SolarPanel" -> "solar-panel"
   * - "HVACSystem" -> "hvac-system"
   */
  val urlPath: String
    get() = value.toKebabCase()

  override fun toString(): String {
    return value
  }

  override val domainIdentifier: DomainIdentifier
    get() = this

  companion object {
    /**
     * A special domain identifier representing an unknown or unspecified domain.
     */
    val Unknown: DomainIdentifier = DomainIdentifier("unknown")
  }
}
