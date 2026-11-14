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
package it.neckar.common.jackson

import assertk.*
import assertk.assertions.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import it.neckar.open.test.utils.SlowTest
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test

/**
 *
 */
@SlowTest
class JacksonDateAsIsoSerializerTest {
  @Test
  fun testIt() {
    val myEntity = MyEntity(id = 1, createdAt = 1234567890.0)

    val objectMapper = ObjectMapper()
    objectMapper.registerModule(kotlinModule())
    objectMapper.registerModule(Jdk8Module())
    objectMapper.registerModule(JavaTimeModule())
    objectMapper.registerModule(ParameterNamesModule())
    objectMapper.registerModule(JacksonMillisAsIsoDateTimeModule())

    val json = objectMapper.writeValueAsString(myEntity)
    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "id" : 1,
          "createdAt" : "1970-01-15T06:56:07.890Z",
          "normalDouble" : 123.7
        }
      """.trimIndent()
    )

    assertThat(objectMapper.readValue(json, MyEntity::class.java)).isEqualTo(myEntity)
  }

  @Test
  fun `deserializes an integral number for a double field`() {
    val objectMapper = ObjectMapper()
    objectMapper.registerModule(kotlinModule())
    objectMapper.registerModule(Jdk8Module())
    objectMapper.registerModule(JavaTimeModule())
    objectMapper.registerModule(ParameterNamesModule())
    objectMapper.registerModule(JacksonMillisAsIsoDateTimeModule())

    // normalDouble arrives as a JSON integer (no decimal point) → token VALUE_NUMBER_INT.
    //language=JSON
    val json = """{ "id" : 1, "createdAt" : "1970-01-15T06:56:07.890Z", "normalDouble" : 124 }"""

    val parsed = objectMapper.readValue(json, MyEntity::class.java)
    assertThat(parsed.normalDouble).isEqualTo(124.0)
  }

  private data class MyEntity(
    val id: Int,
    val createdAt: @ms Double,
    val normalDouble: Double = 123.7,
  )
}
