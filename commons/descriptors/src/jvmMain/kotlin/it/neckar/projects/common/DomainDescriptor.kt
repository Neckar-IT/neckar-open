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
 * One production domain of a product (e.g., "app.turboangebot.de" or "meistercharts.com") with
 * its localhost subdomain for local development (e.g., "turboangebot").
 *
 * Environment-specific domains are derived from the production domain:
 * - Dev: "dev.app.turboangebot.de"
 * - Demo: "demo.app.turboangebot.de"
 * - Localdev: "app.turboangebot.localdev"
 *
 * The `productDeclaration { }` of the product generates one implementation per domain into the
 * Kotlin/JVM project that declares the product and into every module whose `serviceDeclaration { }`
 * names it (ADL 0202, `docs/gradle/project-declaration.md`).
 */
interface DomainDescriptor {

  /**
   * The production domain of the product.
   * Examples: "app.turboangebot.de", "meistercharts.com", "auto-merge.neckar.it"
   */
  val domain: Domain

  /**
   * The subdomain used for localhost development.
   * Examples: "turboangebot", "meistercharts", "auto-merge"
   */
  val localhostSubdomain: String
    get() {
      //Derive the localhost subdomain from the domain by taking the first part
      return domain.value.substringBefore('.')
    }

  /**
   * The product id (e.g. "neckar-it-website", "auto-merge-dev"). Generated descriptors carry the
   * declared id; the hand-written Lizergy descriptors fall back to [localhostSubdomain].
   */
  val productId: ProductId
    get() = ProductId(localhostSubdomain)

  /**
   * The product's WorktreeDevPorts index (the dev-port slot). Every product has one — even
   * PV-Planner, which occupies slot 5 while keeping its legacy ports (see WorktreeDevPorts).
   */
  val productIndex: ProductIndex

  /**
   * Whether this product honors the neckar feature-flag system (`?featureFlags=` is read by its
   * Ktor backend and/or its React/Vite frontend). False for static Astro sites that ignore it.
   */
  val supportsFeatureFlags: Boolean
    get() = false

  /**
   * The UI served under this domain.
   * Null if the domain serves no UI.
   */
  val ui: UiDescriptor?
    get() = null

  /**
   * The full local domain.
   * Example: "app.turboangebot.localdev"
   */
  val localhostDomain: Domain
    get() = Domain(LocalDev.host(localhostSubdomain))

  /**
   * The dev environment domain.
   * Example: "dev.app.turboangebot.de"
   */
  val devDomain: Domain
    get() = Domain("dev.${domain.value}")

  /**
   * The demo environment domain.
   * Example: "demo.app.turboangebot.de"
   */
  val demoDomain: Domain
    get() = Domain("demo.${domain.value}")
}
