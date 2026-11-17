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

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import it.neckar.ktor.KtorService
import it.neckar.ktor.server
import it.neckar.open.kotlin.lang.isObject
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.test.utils.AbstractResourceProvidingExtension
import it.neckar.projects.common.Port
import it.neckar.rest.test.ktor.WithLiveKtorServer.PortStrategy
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.Parameter
import java.net.ServerSocket
import kotlin.reflect.KClass

/**
 * JUnit extension that starts and stops a real Ktor server for tests.
 */
class LiveKtorServerExtension : AbstractResourceProvidingExtension<RunningKtorServer>(RunningKtorServer::class.java) {

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    val parameterType = parameterContext.parameter.type
    val annotation = findAnnotation(extensionContext)

    val matchesFactory = annotation.serviceFactory.java.isAssignableFrom(parameterType) || KtorServiceFactory::class.java.isAssignableFrom(parameterType)

    return matchesFactory || super.supportsParameter(parameterContext, extensionContext)
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
    val parameterType = parameterContext.parameter.type
    val annotation = findAnnotation(extensionContext)

    if (annotation.serviceFactory.java.isAssignableFrom(parameterType) || KtorServiceFactory::class.java.isAssignableFrom(parameterType)) {
      return getOrCreateServiceFactory(annotation, extensionContext)
    }

    return super.resolveParameter(parameterContext, extensionContext)
  }

  override fun createResource(extensionContext: ExtensionContext): RunningKtorServer {
    val annotation = findAnnotation(extensionContext)

    val serviceFactory = getOrCreateServiceFactory(annotation, extensionContext)

    // Determine the port override - based on the strategy
    val portOverride = when (annotation.portStrategy) {
      PortStrategy.FromServiceDescriptor -> null
      PortStrategy.Random -> findFreePort()
    }

    // Now create the actual service instance with the port override set
    val service = instantiateService(annotation.service, serviceFactory, portOverride)
    val port = portOverride ?: service.serviceDescriptor.port

    require(service.baseUrl.port() == port) {
      "The service's base URL port [${service.baseUrl.port()}] does not match the expected port [$port]"
    }

    val server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> = server(
      service = service,
      port = port,
    )

    //Start the server in the background
    server.start(wait = false)

    return RunningKtorServer(
      service = service,
      baseUrl = service.baseUrl,
      port = port,
      server = server,
    )
  }

  override fun convertResourceForParameter(parameter: Parameter, resource: RunningKtorServer): Any {
    //Not required here
    throw UnsupportedOperationException("Not required")
  }

  override fun cleanup(resource: RunningKtorServer) {
    resource.stop()
  }

  /**
   * Finds a free port by opening a server socket on port 0
   */
  private fun findFreePort(): Port {
    ServerSocket(0).use { return Port(it.localPort) }
  }

  /**
   * Finds the [WithLiveKtorServer] annotation either on the test method or the test class
   */
  private fun findAnnotation(extensionContext: ExtensionContext): WithLiveKtorServer {
    val fromElement = extensionContext.element
      .flatMap { element -> AnnotationSupport.findAnnotation(element, WithLiveKtorServer::class.java) }

    if (fromElement.isPresent) {
      return fromElement.get()
    }

    val fromClass = AnnotationSupport.findAnnotation(extensionContext.requiredTestClass, WithLiveKtorServer::class.java)
    if (fromClass.isPresent) {
      return fromClass.get()
    }

    throw IllegalStateException("@WithRealKtorServer must be present on the test class or method")
  }

  /**
   * Instantiates the service class
   */
  private fun instantiateService(serviceClass: KClass<out KtorService>, serviceFactory: KtorServiceFactory<out KtorService>, portOverride: Port?): KtorService {
    return serviceFactory.create(serviceClass, portOverride)
  }

  /**
   * Returns the service factory instance for the current test context, creating it lazily if necessary.
   */
  private fun getOrCreateServiceFactory(annotation: WithLiveKtorServer, extensionContext: ExtensionContext): KtorServiceFactory<out KtorService> {
    val store = getStore(extensionContext)

    return store.computeIfAbsent(ServiceFactoryKey(annotation.serviceFactory)) {
      instantiateServiceFactory(annotation.serviceFactory)
    } as KtorServiceFactory<out KtorService>
  }

  /**
   * Instantiates the configured service factory
   */
  private fun instantiateServiceFactory(serviceFactoryClass: KClass<out KtorServiceFactory<out KtorService>>): KtorServiceFactory<out KtorService> {
    return when {
      //Special handling for object factories
      serviceFactoryClass.isObject -> serviceFactoryClass.objectInstance.requireNotNull { "Service factory $serviceFactoryClass is an object but the instance is null" }
      else -> {
        serviceFactoryClass.constructors.find { it.parameters.isEmpty() }
          ?.call()
          ?: throw IllegalArgumentException("Service factory $serviceFactoryClass must provide a zero-argument constructor")
      }
    }
  }

  private data class ServiceFactoryKey(
    val serviceFactoryClass: KClass<out KtorServiceFactory<out KtorService>>,
  )
}
