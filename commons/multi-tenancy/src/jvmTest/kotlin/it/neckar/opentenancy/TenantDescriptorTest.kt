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
package it.neckar.opentenancy

import assertk.*
import assertk.assertions.*
import it.neckar.open.tenancy.Slug
import it.neckar.open.tenancy.Tenant
import it.neckar.open.tenancy.TenantDescriptor
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 *
 */
class TenantDescriptorTest {
  @Test
  fun testSerializationToString() {
    val tenantDescriptor = TenantDescriptor(
      id = Tenant.Id(Uuid.parse("a1b45679-e0d3-42be-9fb7-fd40cc12ba0e")),
      slug = Slug("the-tenant-slug")
    )

    assertThat(tenantDescriptor.asString()).isEqualTo("the-tenant-slug@obRWeeDTQr6ft_1AzBK6Dg")
    assertThat(TenantDescriptor.parse(tenantDescriptor.asString())).isEqualTo(tenantDescriptor)
  }
}
