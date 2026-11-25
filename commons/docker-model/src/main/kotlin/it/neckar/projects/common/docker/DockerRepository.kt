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
package it.neckar.projects.common.docker

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents the repository part of the image name.
 * ATTENTION: This is *not* the [DockerRegistry] part of the image name, but the part after the registry.
 */
@Serializable
@JvmInline
value class DockerRepository(val value: String) : Comparable<DockerRepository> {

  init {
    require(value.isNotBlank()) { "Docker repository name cannot be blank." }
    require(value.startsWith("/").not() && value.endsWith("/").not()) { "Docker repository name cannot start or end with a slash." }
  }

  /**
   * The first path segment: `turboangebot.de/turboangebot-ui` → `turboangebot`, `mea.neckar.it` →
   * `mea.neckar.it`. In this build's registry that segment is the product — a DNS name or the
   * product directory the module sits in.
   */
  val firstSegment: String
    get() = value.substringBefore('/')

  override fun toString(): String {
    return value
  }

  /** Repositories are listed to humans — in an error message, in a plan — so they sort by name. */
  override fun compareTo(other: DockerRepository): Int {
    return value.compareTo(other.value)
  }

  /**
   * Returns true if the repository contains a slash (`/`), meaning it belongs to a user or organization.
   * Official Docker images (e.g., `nginx`, `mongo`) do **not** contain a slash.
   */
  fun containsSlash(): Boolean {
    return value.contains("/")
  }

  /**
   * Returns true if the repository is an official Docker image repository (i.e., does not contain a `/`).
   * Official images exist under `library/` in DockerHub but are referenced without it (e.g., `mongo` instead of `library/mongo`).
   */
  fun isOfficialImageRepository(): Boolean {
    return containsSlash().not()
  }
}
