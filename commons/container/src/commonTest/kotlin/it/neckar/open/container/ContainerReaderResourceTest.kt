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
package it.neckar.open.container

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlin.test.Test

class ContainerReaderResourceTest {
  @Test
  fun closesTheSourceWhenTheConstructorRejects() {
    val source = RecordingSource(rawContainer("meta/version" to "2.0.0"))

    assertFailure { ContainerReader(source.buffered(), backupExpectation) }.isInstanceOf<InvalidContainerException>()

    assertThat(source.closed).isTrue()
  }

  @Test
  fun closesTheSourceOnClose() {
    val source = RecordingSource(container { })

    ContainerReader(source.buffered(), backupExpectation).close()

    assertThat(source.closed).isTrue()
  }

  @Test
  fun refusesFurtherContentAfterClose() {
    val bytes = container { writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) } }
    val reader = ContainerReader(bytes.toSource(), backupExpectation)
    val entry = reader.nextEntry()!!
    assertThat(entry.content.readByteArray(10).size).isEqualTo(10)

    reader.close()

    assertFailure { entry.content.readByteArray() }.isInstanceOf<IllegalStateException>()
    assertFailure { reader.nextEntry() }.isInstanceOf<IllegalStateException>()
  }

  @Test
  fun refusesTheFirstEntryAfterCloseRightBehindTheConstructor() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    val reader = ContainerReader(bytes.toSource(), backupExpectation)

    reader.close()

    assertFailure { reader.nextEntry() }.isInstanceOf<IllegalStateException>()
  }

  @Test
  fun refusesAnEntryAfterTheNextOne() {
    val bytes = container {
      writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) }
      writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") }
    }

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      val users = reader.nextEntry()!!
      assertThat(reader.nextEntry()!!.versionedName.zipPath).isEqualTo("volume/2.1.0")

      assertFailure { users.content.readByteArray() }.isInstanceOf<IllegalStateException>().hasMessage("Entry <config/users/1.2.0> was followed by the next entry")
    }
  }

  /** Records [close]; a [Buffer] cannot tell whether it was closed. */
  private class RecordingSource(bytes: ByteArray) : RawSource {
    private val content = bytes.toSource()

    var closed: Boolean = false
      private set

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = content.readAtMostTo(sink, byteCount)

    override fun close() {
      closed = true
    }
  }
}
