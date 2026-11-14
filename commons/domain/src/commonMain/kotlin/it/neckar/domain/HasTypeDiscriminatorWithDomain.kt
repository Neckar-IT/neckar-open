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

/**
 * Combines [HasTypeDiscriminator] and [HasDomainIdentifier] for the common case where
 * the domain identifier is derived from the type discriminator enum.
 *
 * This interface is useful when:
 * - You have a polymorphic type hierarchy (sealed class/interface)
 * - Each type maps to an enum value (the type discriminator)
 * - Each enum value belongs to a specific domain
 *
 * ## Example
 * ```kotlin
 * enum class ProjectType(override val domainIdentifier: DomainIdentifier) : HasDomainIdentifier {
 *   Electro(DomainIdentifier("electro")),
 *   PV(DomainIdentifier("pv"))
 * }
 *
 * @Serializable
 * sealed interface Project : HasTypeDiscriminatorWithDomain<ProjectType> {
 *   override val typeDiscriminator: ProjectType
 *     get() = when (this) {
 *       is ElectroProject -> ProjectType.Electro
 *       is PvProject -> ProjectType.PV
 *     }
 *   // domainIdentifier is automatically derived from typeDiscriminator
 * }
 * ```
 *
 * @param T The enum type used as the discriminator, which must implement [HasDomainIdentifier]
 */
interface HasTypeDiscriminatorWithDomain<T> : HasTypeDiscriminator<T>, HasDomainIdentifier
  where T : Enum<T>, T : HasDomainIdentifier {

  /**
   * The domain identifier, derived from the type discriminator.
   *
   * This default implementation delegates to [typeDiscriminator].[domainIdentifier][HasDomainIdentifier.domainIdentifier].
   */
  override val domainIdentifier: DomainIdentifier
    get() = typeDiscriminator.domainIdentifier
}
