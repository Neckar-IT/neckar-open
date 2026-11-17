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
package it.neckar.ktor.client.advanced

import assertk.*
import assertk.assertions.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.reflect.*
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.test.utils.isJsonEqualTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType

/**
 * Reproduces a bug related to the serialization of polymorphic interfaces.
 *
 * workaround: dependency=libs:ktor:3.6.0 observed-on=2026-10-07 upstream=https://youtrack.jetbrains.com/issue/KTOR-7106
 */
class KtorClientSerializationTest {

  private interface FooBarInterface

  @Serializable
  private sealed interface FooBarInterfaceSealed

  @Serializable
  @SerialName("FooBar")
  private data class FooBar(val foo: String, val bar: Int) : FooBarInterface, FooBarInterfaceSealed

  private val json = Json {
  }

  /**
   * Contains the polymorphic mapping for [FooBarInterface]
   */
  private val jsonWithPolymorphic = Json {
    this.serializersModule = SerializersModule {
      polymorphic(FooBarInterface::class) {
        subclass(FooBar::class)
      }
    }
  }

  @Test
  fun testIt() {
    val typeInfo = typeInfo<FooBarInterface>()

    val serializersModule = jsonWithPolymorphic.serializersModule
    val type = typeInfo.type
    assertThat(type).isEqualTo(FooBarInterface::class)

    assertThat(typeInfo.kotlinType).isNotNull()
    assertThat(typeInfo.kotlinType).isEqualTo(FooBarInterface::class.starProjectedType)

    serializersModule.serializerOrNull(typeInfo.kotlinType!!).let {
      assertThat(it).isNotNull()
      it.requireNotNull()
      assertThat(it.descriptor.serialName).isEqualTo("kotlinx.serialization.Polymorphic<FooBarInterface>")
    }
  }

  @OptIn(InternalSerializationApi::class)
  @Test
  fun testSerializer() {
    assertFailure {
      json.serializersModule.serializerForTypeInfo(typeInfo<FooBarInterface>())
    }.all {
      messageContains("Serializer for class 'FooBarInterface' is not found")
      isInstanceOf(kotlinx.serialization.SerializationException::class)
    }

    assertThat(json.serializersModule.serializerForTypeInfo(typeInfo<FooBar>()).descriptor.serialName).isEqualTo(FooBar::class.simpleName)
    assertThat(json.serializersModule.serializerForTypeInfo(typeInfo<FooBarInterfaceSealed>()).descriptor.serialName).isEqualTo(FooBarInterfaceSealed::class.qualifiedName)
  }

  @Test
  suspend fun testSerializationForTypeInfoNoSerializerModule() {
    verifySerialization(
      typeInfo<FooBarInterface>(),
      FooBar("foo", 42),
      json
    ) {
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """
    }

    verifySerialization(
      typeInfo<FooBarInterface>(),
      FooBar("foo", 42),
      json
    ) {
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : 42
        }
       """
    }

    verifySerialization(
      typeInfo<FooBarInterfaceSealed>(),
      FooBar("foo", 42),
      json
    ) {
      //language=JSON
      """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
       """
    }
  }

  @Test
  suspend fun testSerializationForTypeInfoWithSerializerModuleWithPolymorphic() {
    verifySerialization(
      typeInfo<FooBarInterface>(),
      FooBar("foo", 42),
      jsonWithPolymorphic
    ) {
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """
    }

    verifySerialization(
      typeInfo<FooBarInterface>(),
      FooBar("foo", 42),
      jsonWithPolymorphic
    ) {
      //language=JSON
      """
        {
          "foo" : "foo",
          "bar" : 42
        }
       """
    }

    verifySerialization(
      typeInfo<FooBarInterfaceSealed>(),
      FooBar("foo", 42),
      jsonWithPolymorphic
    ) {
      //language=JSON
      """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
       """
    }
  }

  @Test
  fun testSerializationDirectlyJsonWithoutModule() {
    assertFailure {
      json.encodeToString(serializer<FooBarInterface>(), FooBar("foo", 42))
    }.all {
      messageContains("Serializer for subclass 'FooBar' is not found in the polymorphic scope of 'FooBarInterface'")
      isInstanceOf(kotlinx.serialization.SerializationException::class)
    }


    json.encodeToString(FooBar.serializer(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    json.encodeToString(serializer<FooBar>(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    json.encodeToString(FooBarInterfaceSealed.serializer(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    json.encodeToString(serializer<FooBarInterfaceSealed>(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }
  }

  @Test
  fun testSerializationDirectlyJsonWithSerializerModule() {
    jsonWithPolymorphic.encodeToString(serializer<FooBarInterface>(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    jsonWithPolymorphic.encodeToString(FooBar.serializer(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    jsonWithPolymorphic.encodeToString(serializer<FooBar>(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    jsonWithPolymorphic.encodeToString(FooBarInterfaceSealed.serializer(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }

    jsonWithPolymorphic.encodeToString(serializer<FooBarInterfaceSealed>(), FooBar("foo", 42)).let {
      assertThat(it).isJsonEqualTo(
        //language=JSON
        """
        {
          "type" : "FooBar",
          "foo" : "foo",
          "bar" : 42
        }
      """
      )
    }
  }

  private suspend fun verifySerialization(typeInfo: TypeInfo, objectToSerialize: FooBar, json: Json, expectedJson: () -> String) {
    val contentType = ContentType.Application.Json
    val result = KotlinxSerializationConverter(json).serialize(
      contentType = contentType,
      charset = contentType.charset() ?: Charsets.UTF_8,
      typeInfo = typeInfo,
      value = objectToSerialize
    ) as TextContent

    assertThat(result.text).isJsonEqualTo(
      expectedJson()
    )
  }
}
