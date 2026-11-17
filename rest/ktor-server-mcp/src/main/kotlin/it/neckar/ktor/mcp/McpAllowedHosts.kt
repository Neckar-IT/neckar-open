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

import it.neckar.projects.common.Domain
import it.neckar.projects.common.DomainDescriptor
import it.neckar.projects.common.McpResource
import it.neckar.projects.common.WorktreeDevPorts
import it.neckar.rest.jwt.JwtAudience

/**
 * The `Host` header values [McpRouting] accepts for [domains]: the loopback names, the public domains of each domain descriptor
 * and its local Traefik host of every worktree (`patterns.localdev`, `patterns1.localdev` … `patterns8.localdev`).
 */
fun mcpAllowedHosts(vararg domains: DomainDescriptor): List<String> {
  return (LoopbackHosts + domains.flatMap { it.mcpHosts() }.map { it.value }).distinct()
}

/**
 * [McpResource] of every host of [mcpAllowedHosts] except the loopback names, which no authorization server issues a token for.
 */
fun mcpResources(vararg domains: DomainDescriptor): Set<JwtAudience> {
  return domains.flatMap { it.mcpHosts() }.map { JwtAudience(McpResource.of(it)) }.toSet()
}

private fun DomainDescriptor.mcpHosts(): List<Domain> {
  return listOf(domain, devDomain, demoDomain) + WorktreeDevPorts.localhostHosts(localhostSubdomain)
}

/**
 * A client on the same machine may address the service port directly, past Traefik.
 */
private val LoopbackHosts: List<String> = listOf("localhost", "127.0.0.1", "[::1]")
