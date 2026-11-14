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
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import it.neckar.open.test.utils.SlowTest
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Verifies that a value that cannot be deserialized names itself in the exception
 */
@SlowTest
class JacksonMillisAsIsoDateTimeDeserializerTest {
  @Test
  fun `an unparsable string names the value, the expected format and the property`() {
    //language=JSON
    val json = """{ "id" : 1, "createdAt" : "15.01.1970 06:56" }"""

    val exception = assertThrows<InvalidFormatException> {
      objectMapper().readValue(json, MyEntity::class.java)
    }

    assertThat(exception.value).isEqualTo("15.01.1970 06:56")
    assertThat(exception.message).isNotNull().all {
      contains("15.01.1970 06:56")
      contains("1970-01-15T06:56:07.890Z") //the expected format, given as an example
      contains("createdAt") //the reference chain Jackson prepends
    }
  }

  @Test
  fun `an unexpected token names the value and the expected shape`() {
    //language=JSON
    val json = """{ "id" : 1, "createdAt" : true }"""

    val exception = assertThrows<MismatchedInputException> {
      objectMapper().readValue(json, MyEntity::class.java)
    }

    assertThat(exception.message).isNotNull().all {
      contains("true") //the value - not just the token type
      contains("VALUE_TRUE")
      contains("createdAt")
    }
  }

  private fun objectMapper(): ObjectMapper {
    return ObjectMapper().apply {
      registerModule(kotlinModule())
      registerModule(Jdk8Module())
      registerModule(JavaTimeModule())
      registerModule(ParameterNamesModule())
      registerModule(JacksonMillisAsIsoDateTimeModule())
    }
  }

  private data class MyEntity(
    val id: Int,
    val createdAt: @ms Double,
  )
}
