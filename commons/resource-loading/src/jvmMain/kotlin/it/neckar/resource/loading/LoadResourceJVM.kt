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
package it.neckar.resource.loading

import it.neckar.open.http.Url
import it.neckar.open.resources.getResourceAsStreamSafe
import java.io.InputStream

/**
 * Loads the resource into a byte array (usually with a root relative URL [Url.rootRelative])
 */
actual suspend fun loadResource(url: Url): ByteArray {
  return loadResourceFromClasspath(url)
}

/**
 * Loads a resource form the classpath (usually with a root relative URL [Url.rootRelative])
 */
fun loadResourceFromClasspath(url: Url): ByteArray {
  return ResourceLoaderJava.load(url)
}

fun loadResourceFromClasspathAsStream(url: Url): InputStream {
  return ResourceLoaderJava.loadStream(url)
}

object ResourceLoaderJava {
  fun load(url: Url): ByteArray {
    return javaClass.getResourceAsStreamSafe(url.value).use { it.readBytes() }
  }

  fun loadStream(url: Url): InputStream {
    return javaClass.getResourceAsStreamSafe(url.value)
  }
}

