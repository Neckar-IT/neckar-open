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
package it.neckar.ktor

import it.neckar.projects.common.Domain
import it.neckar.projects.common.DomainDescriptor
import it.neckar.projects.common.Port
import it.neckar.projects.common.ProductIndex
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.projects.common.UiDescriptor

/**
 * Test domain with a UI, served by [TestProductService].
 */
internal object TestProductDomain : DomainDescriptor {
  override val domain: Domain = Domain("test-product.neckar.it")
  override val productIndex: ProductIndex = ProductIndex(90)
  override val ui: UiDescriptor = UiDescriptor(devPort = Port(5173))
}

internal object TestProductService : ServiceDescriptor by ServiceDescriptor("TestProductService", "api/test-product", Port(8080))

/**
 * Test domain without UI (backend-only service), served by [TestProductNoUiService].
 */
internal object TestProductNoUiDomain : DomainDescriptor {
  override val domain: Domain = Domain("no-ui-product.neckar.it")
  override val productIndex: ProductIndex = ProductIndex(91)
}

internal object TestProductNoUiService : ServiceDescriptor by ServiceDescriptor("NoUiProductService", "api/no-ui-product", Port(8081))
