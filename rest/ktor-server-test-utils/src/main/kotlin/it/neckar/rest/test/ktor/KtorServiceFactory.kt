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
package it.neckar.rest.test.ktor

import it.neckar.ktor.KtorService
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.projects.common.Port
import kotlin.reflect.KClass

/**
 * Factory for creating Ktor services.
 *
 * Must provide a no-arg constructor.
 */
interface KtorServiceFactory<T : KtorService> {
  /**
   * Creates an instance of the given service class.
   */
  fun create(
    /**
     * The service class to create an instance of.
     */
    serviceClass: KClass<out KtorService>,
    /**
     * If provided, the service should use this port instead of the one defined in the service descriptor.
     */
    portOverride: Port?
  ): T

  /**
   * Default factory that uses the no-arg constructor
   */
  object DefaultFactory : KtorServiceFactory<KtorService> {
    override fun create(serviceClass: KClass<out KtorService>, portOverride: Port?): KtorService {
      val noArgsConstructor = serviceClass.constructors.firstOrNull { it.parameters.isEmpty() }.requireNotNull {
        "Service class [$serviceClass] must provide a zero-argument constructor"
      }
      return noArgsConstructor.call()
    }
  }
}
