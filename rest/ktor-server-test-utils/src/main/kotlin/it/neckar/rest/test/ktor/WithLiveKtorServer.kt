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
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.reflect.KClass

/**
 * Starts a real Ktor server before the test and stops it afterwards.
 *
 * Apply this annotation to your test class or test method and request a [RunningKtorServer],
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ExtendWith(LiveKtorServerExtension::class)
annotation class WithLiveKtorServer(
  /**
   * The Ktor service that should be started for the test.
   *
   * The service class must provide a zero-argument constructor.
   */
  val service: KClass<out KtorService>,

  /**
   * The optional service factory that will be used to instantiate the service.
   */
  val serviceFactory: KClass<out KtorServiceFactory<out KtorService>> = KtorServiceFactory.DefaultFactory::class,

  /**
   * Port strategy that should be used
   */
  val portStrategy: PortStrategy = PortStrategy.Random,
) {

  /**
   * Defines the port strategy that should be used to start the server
   */
  enum class PortStrategy {
    /**
     * Use the port from the service descriptor
     */
    FromServiceDescriptor,

    /**
     * Use a random free port
     */
    Random,
  }
}
