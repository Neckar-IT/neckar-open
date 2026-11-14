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
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.common.featureflags.FeatureFlagsSupport
import it.neckar.common.featureflags.loadFeatureFlagsFromUrlOrLocalStorage
import it.neckar.common.featureflags.writeToLocalStorage
import it.neckar.common.featureflags.writeToUrl
import it.neckar.commons.kotlin.js.exception.JsErrorHandler
import it.neckar.logging.Level
import it.neckar.logging.LogConfigurer
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.console.ConsoleLogFunctionsSupport
import it.neckar.logging.console.ConsoleLogPrefix
import it.neckar.open.version.VersionInformation
import react.*
import react.dom.client.*
import web.html.HTMLElement

private val logger: Logger = LoggerFactory.getLogger("KotlinReactDemo")


/**
 * Entry point for the application
 */
fun main() {
  JsErrorHandler.registerWindowErrorHandler()

  LogConfigurer.initializeFromLocalStorage(Level.DEBUG)
  ConsoleLogFunctionsSupport.init(ConsoleLogPrefix("reactDemo")) { VersionInformation.artifactIdentity }

  logger.info("Starting React Demo: ${VersionInformation.artifactIdentity}")

  val featureFlags = initializeFeatureFlags()

  val rootDiv: HTMLElement = web.dom.document.getElementById("root") ?: error("Element root not found")
  createRoot(rootDiv).render(createElement<Props> {
    DemoRoot {}
  })
}


private fun initializeFeatureFlags() {
  val loadedFeatureFlags: FeatureFlags = loadFeatureFlagsFromUrlOrLocalStorage() ?: FeatureFlags.empty
  logger.debug("loadedFeatureFlags: $loadedFeatureFlags")

  FeatureFlagsSupport.flags = loadedFeatureFlags
  FeatureFlagsSupport.flags.writeToUrl() //Update the URL (might be redundant)
  FeatureFlagsSupport.flags.writeToLocalStorage() //Update local storage (might be redundant)
}
