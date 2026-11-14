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
package it.neckar.projects.common

/**
 * Represents a backend service.
 *
 * The domains a service is reached under are `DomainDescriptor`s — they live in jvmMain, out of
 * reach of this KDoc link, because only the build and the JVM services read them.
 */
interface ServiceDescriptor : RelationshipParticipant {

  /**
   * The name of the service - the docker-compose service name is derived from it
   */
  val serviceName: ServiceName

  /**
   * The API prefix of the service.
   * This is used to create the URL to the service.
   *
   * Usually, the first part is "api/"
   */
  val apiPrefix: String

  /**
   * The port of the service
   */
  val port: Port

  val apiReference: ApiReference
    get() = ApiReference.None

  /**
   * Root-relative URL of the interactive API reference, e.g. `/api/patterns/doc/api`. Null where
   * [apiReference] is [ApiReference.None].
   */
  val apiReferencePath: String?
    get() = when (apiReference) {
      ApiReference.Scalar -> "/$apiPrefix/${ApiReference.ScalarRoute}"
      ApiReference.None -> null
    }

  /**
   * Returns the [serviceName]
   */
  override val participantName: String
    get() = serviceName.value

  override val description: String?
    get() = null

  companion object {
    /**
     * Creates a new instance
     */
    operator fun invoke(
      serviceName: String,
      apiPrefix: String,
      port: Port,
      apiReference: ApiReference = ApiReference.None,
    ): ServiceDescriptor = SimpleServiceDescriptor(ServiceName(serviceName), apiPrefix, port, apiReference)

    /**
     * Creates a new instance; the form the generated service objects use.
     */
    operator fun invoke(
      serviceName: ServiceName,
      apiPrefix: String,
      port: Port,
      apiReference: ApiReference,
    ): ServiceDescriptor = SimpleServiceDescriptor(serviceName, apiPrefix, port, apiReference)
  }
}

/**
 * Plain immutable [ServiceDescriptor] implementation used by the
 * [ServiceDescriptor.Companion.invoke] factory.
 */
private data class SimpleServiceDescriptor(
  override val serviceName: ServiceName,
  override val apiPrefix: String,
  override val port: Port,
  override val apiReference: ApiReference,
) : ServiceDescriptor

