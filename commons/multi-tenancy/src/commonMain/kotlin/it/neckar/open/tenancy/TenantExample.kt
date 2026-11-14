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
package it.neckar.open.tenancy

import it.neckar.open.time.TimeInstantExample
import kotlin.uuid.Uuid

/**
 * Contains example tenants.
 */
object TenantExample {
  /**
   * An example tenant that can be used for testing or as a default tenant.
   */
  val ExampleTenant: Tenant = DefaultTenant(
    id = Tenant.Id(Uuid.parse("669f7325-5e2d-4653-8a93-a232d4ccc878")),
    name = "Example Tenant",
    slug = Slug("example-tenant"),
    createdAt = TimeInstantExample._2023_11_15_12_00_00_000,
  )
}
