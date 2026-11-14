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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.ktor.http.parametersOf
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.ParameterConversionException
import it.neckar.open.http.UrlParameterName
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class IdFromPathTest {
  private data class ThingId(val uuid: Uuid)

  private val thingId = UrlParameterName("thingId")

  @Test
  fun `valid uuid yields the typed id`() {
    val parameters = parametersOf("thingId", "6f1c9a3e-2b4d-4e8f-9a01-23456789abcd")

    assertThat(parameters.idFromPath(thingId, ::ThingId)).isEqualTo(ThingId(Uuid.parse("6f1c9a3e-2b4d-4e8f-9a01-23456789abcd")))
  }

  @Test
  fun `missing parameter throws MissingRequestParameterException`() {
    val parameters = parametersOf("otherId", "6f1c9a3e-2b4d-4e8f-9a01-23456789abcd")

    assertFailure { parameters.idFromPath(thingId, ::ThingId) }.isInstanceOf<MissingRequestParameterException>()
  }

  @Test
  fun `malformed uuid throws ParameterConversionException`() {
    val parameters = parametersOf("thingId", "not-a-uuid")

    assertFailure { parameters.idFromPath(thingId, ::ThingId) }.isInstanceOf<ParameterConversionException>()
  }
}
