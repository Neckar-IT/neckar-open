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
package it.neckar.ktor.mcp

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import it.neckar.projects.common.DomainDescriptor
import it.neckar.projects.common.Domain
import it.neckar.projects.common.ProductIndex
import org.junit.jupiter.api.Test

class McpAllowedHostsTest {
  @Test
  fun `the loopback hosts, the public domains and the local host of every worktree are allowed`() {
    assertThat(mcpAllowedHosts(ExampleDomain)).containsExactlyInAnyOrder(
      "localhost",
      "127.0.0.1",
      "[::1]",
      "example.neckar.it",
      "dev.example.neckar.it",
      "demo.example.neckar.it",
      "example.localdev",
      "example1.localdev",
      "example2.localdev",
      "example3.localdev",
      "example4.localdev",
      "example5.localdev",
      "example6.localdev",
      "example7.localdev",
      "example8.localdev",
    )
  }

  @Test
  fun `every allowed host except the loopback names has its MCP resource`() {
    assertThat(mcpResources(ExampleDomain).map { it.value }).containsExactlyInAnyOrder(
      "https://example.neckar.it/api/mcp",
      "https://dev.example.neckar.it/api/mcp",
      "https://demo.example.neckar.it/api/mcp",
      "https://example.localdev/api/mcp",
      "https://example1.localdev/api/mcp",
      "https://example2.localdev/api/mcp",
      "https://example3.localdev/api/mcp",
      "https://example4.localdev/api/mcp",
      "https://example5.localdev/api/mcp",
      "https://example6.localdev/api/mcp",
      "https://example7.localdev/api/mcp",
      "https://example8.localdev/api/mcp",
    )
  }
}

private object ExampleDomain : DomainDescriptor {
  override val domain: Domain = Domain("example.neckar.it")
  override val productIndex: ProductIndex = ProductIndex(1)
}
