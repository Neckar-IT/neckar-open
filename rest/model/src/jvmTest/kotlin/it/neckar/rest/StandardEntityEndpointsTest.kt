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
package it.neckar.rest

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.uuid.HasUuid
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class StandardEntityEndpointsTest {
  private data class ThingId(override val uuid: Uuid) : HasUuid {
    override fun toString(): String = "ThingId(not-the-uuid)"
  }

  private object ThingEndpoints : StandardEntityEndpoints<ThingId>(collectionSegment = "things", idParameterName = "thingId")

  private val id = ThingId(Uuid.parse("6f1c9a3e-2b4d-4e8f-9a01-23456789abcd"))

  @Test
  fun `patterns compose collection segment and id parameter`() {
    assertThat(ThingEndpoints.IdParameter.value).isEqualTo("thingId")
    assertThat(ThingEndpoints.BasePath.value).isEqualTo("api/things")
    assertThat(ThingEndpoints.TableEntries.value).isEqualTo("api/things/views/table-entries")
    assertThat(ThingEndpoints.New.value).isEqualTo("api/things/new")
    assertThat(ThingEndpoints.ById.value).isEqualTo("api/things/{thingId}")
    assertThat(ThingEndpoints.Preview.value).isEqualTo("api/things/{thingId}/preview")
    assertThat(ThingEndpoints.Archive.value).isEqualTo("api/things/{thingId}/archive")
    assertThat(ThingEndpoints.Unarchive.value).isEqualTo("api/things/{thingId}/unarchive")
  }

  @Test
  fun `urls resolve the uuid of the id`() {
    assertThat(ThingEndpoints.basePathUrl().value).isEqualTo("api/things")
    assertThat(ThingEndpoints.tableEntriesUrl().value).isEqualTo("api/things/views/table-entries")
    assertThat(ThingEndpoints.newUrl().value).isEqualTo("api/things/new")
    assertThat(ThingEndpoints.byIdUrl(id).value).isEqualTo("api/things/6f1c9a3e-2b4d-4e8f-9a01-23456789abcd")
    assertThat(ThingEndpoints.previewUrl(id).value).isEqualTo("api/things/6f1c9a3e-2b4d-4e8f-9a01-23456789abcd/preview")
    assertThat(ThingEndpoints.archiveUrl(id).value).isEqualTo("api/things/6f1c9a3e-2b4d-4e8f-9a01-23456789abcd/archive")
    assertThat(ThingEndpoints.unarchiveUrl(id).value).isEqualTo("api/things/6f1c9a3e-2b4d-4e8f-9a01-23456789abcd/unarchive")
  }
}
