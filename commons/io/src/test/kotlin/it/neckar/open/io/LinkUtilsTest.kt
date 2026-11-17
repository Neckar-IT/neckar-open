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
package it.neckar.open.io

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

/**
 *
 */
class LinkUtilsTest {
  @Test
  fun testLinkTypes() {
    assertNotNull(LinkType.HARD)
    assertNotNull(LinkType.SYMBOLIC)
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  @Throws(Exception::class)
  fun testCreation(@TempDir directory: File) {
    // Use canonical path to avoid issues with /var -> /private/var symlink on macOS
    val target = directory.resolve("linked.to").canonicalFile.apply { createNewFile() }
    assertFalse(target.isSymbolicLink())
    val link = directory.resolve("dir").apply { mkdirs() }.resolve("link")
    assertFalse(link.exists())

    createSymbolicLink(target, link)
    assertTrue(link.exists())
    assertTrue(link.isSymbolicLink())
    assertTrue(target.exists())

    link.delete()
    assertTrue(target.exists())
    assertFalse(link.exists())
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  @Throws(Exception::class)
  fun `isSymLinkTo checks the actual target`(@TempDir directory: File) {
    val target = directory.resolve("linked.to").canonicalFile.apply { createNewFile() }
    val otherTarget = directory.resolve("other.to").canonicalFile.apply { createNewFile() }

    val link = directory.resolve("link")
    createSymbolicLink(target, link)

    //The link points at target, not otherTarget.
    assertTrue(link.isSymLinkTo(target))
    assertFalse(link.isSymLinkTo(otherTarget))

    link.delete()
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `the conflict message names the existing target and the requested one`(@TempDir directory: File) {
    val existingTarget = directory.resolve("existing.to").canonicalFile.apply { createNewFile() }
    val requestedTarget = directory.resolve("requested.to").canonicalFile.apply { createNewFile() }

    val link = directory.resolve("link")
    createSymbolicLink(existingTarget, link)

    val thrown = assertThrows<AlreadyExistsWithOtherTargetException> {
      createLink(requestedTarget, link, LinkType.SYMBOLIC)
    }

    //The path the link resolves to is what exists, the parameter is what was asked for - the message must not swap them.
    assertThat(thrown.message).isNotNull()
      .contains("points at <${existingTarget.canonicalPath}>", "requested target <${requestedTarget.canonicalPath}>")

    link.delete()
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `a hard link onto an existing file names the link file`(@TempDir directory: File) {
    val target = directory.resolve("linked.to").canonicalFile.apply { createNewFile() }

    val link = directory.resolve("link")
    assertTrue(link.createNewFile())

    val thrown = assertThrows<IOException> {
      createLink(target, link, LinkType.HARD)
    }

    assertThat(thrown.message).isEqualTo("Link file already exists, refusing to create a hard link over it: <${link.absolutePath}>")
  }
}
