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
package it.neckar.open.kotlin.serializers

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.defaultJsonConfiguration
import it.neckar.open.serialization.roundTrip
import it.neckar.open.test.utils.isJsonEqualTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 *
 */
class KotlinxSerializationBasicsTest {
  @Test
  fun testNonBackingFields() {
    roundTrip(Example(a = 3, b = 4)) {
      //language=JSON
      """
        {
          "a" : 3,
          "b" : 4,
          "the-sum" : 7,
          "sumNoAnnotation" : 7
        }
      """.trimIndent()
    }
  }

  @Test
  fun testOverwriteFieldWithInitializer() {
    val example = Json {
      this.serializersModule = serializersModule
      this.defaultJsonConfiguration(true)
    }.decodeFromString<Example>(
      //language=JSON
      """
        {
          "a" : 3,
          "b" : 4,
          "the-sum" : 79,
          "sumNoAnnotation" : 89
        }
      """.trimIndent()
    )

    assertThat(example.a).isEqualTo(3)
    assertThat(example.b).isEqualTo(4)
    assertThat(example.sum).isEqualTo(79)
    assertThat(example.sumNoAnnotation).isEqualTo(89)
  }

  @Serializable
  private data class Example(
    val a: Int,
    val b: Int,
  ) {
    @SerialName("the-sum")
    val sum: Int = a + b
    val sumNoAnnotation: Int = a + b
  }

  @Test
  fun testEncodeDefaults() {
    val json = Json {
      prettyPrint = true
      prettyPrintIndent = "  "
      encodeDefaults = true
      explicitNulls = false
    }

    //ATTENTION! Nickname is missing
    assertThat(json.encodeToString(UserProfile(id = "the-id", nickname = null))).isJsonEqualTo(
      //language=JSON
      """
        {
          "id" : "the-id",
          "country" : "DE",
          "avatarUrl" : "https://my-default-url"
        }
      """.trimIndent()
    )
  }

  @Disabled
  @Test
  fun testReproducerAllCases() {
    val pairs = listOf(
      true to true,
      true to false,
      false to true,
      false to false
    )

    val success = pairs.map { (encodeDefaults, explicitNulls) ->
      try {
        val json = Json {
          prettyPrint = true
          prettyPrintIndent = "  "
          this.encodeDefaults = encodeDefaults
          this.explicitNulls = explicitNulls
        }

        println("--------------------------------------------------------------------")
        println("----- encodeDefaults=$encodeDefaults, explicitNulls=$explicitNulls")
        println("--------------------------------------------------------------------")

        println("Minimal object - with nickname: null")
        simulate(
          json, UserProfile(
            id = "the-id",
            nickname = null,
          )
        )

        println("Minimal object - with nickname set")
        simulate(
          json,
          UserProfile(
            id = "the-id",
            nickname = "the-nickname",
          )
        )

        println("Minimal object - avatarUrl set to null")
        simulate(
          json,
          UserProfile(
            id = "the-id",
            nickname = "the-nickname",
            avatarUrl = null,
          )
        )

        println("Filled object - all values set")
        simulate(
          json,
          UserProfile(
            id = "the-id",
            nickname = "the-nickname",
            displayName = "the-display-name",
            country = "the-country",
            avatarUrl = "the-avatar-url",
          )
        )

        println("")
        println("")
        println("##############################################################################################")
        println("##############################################################################################")
        println("##############################################################################################")
        println("##############################################################################################")
        println("Success for encodeDefaults=$encodeDefaults, explicitNulls=$explicitNulls")
        println("##############################################################################################")
        println("##############################################################################################")
        println("##############################################################################################")
        println("##############################################################################################")
        println("")
        return@map true
      } catch (e: Throwable) {
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("!!!! Failed for for encodeDefaults=$encodeDefaults, explicitNulls=$explicitNulls: ${e.message}")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        return@map false
      }
    }

    //Ensure that at least one configuration was successful
    assertThat(success.any { it }).isTrue()
  }

  /**
   * Simulates serializing and parsing from TypeScript
   */
  private fun simulate(json: Json, profile: UserProfile) {
    val jsonString = json.encodeToString<UserProfile>(
      profile
    )

    println("Serialized JSON:")
    println(jsonString)

    //Simulate parsing in TS
    val jsonElement = Json.parseToJsonElement(jsonString)

    //
    // Id is required and non-nullable
    //
    assertThat(jsonElement.jsonObject.getValue("id").jsonPrimitive.isString).isTrue()

    //
    //Nickname is required! But might be null!
    //
    jsonElement.jsonObject.getValue("nickname").jsonPrimitive.let {
      //Is either string or null
      when (profile.nickname) {
        null -> {
          assertThat(it.isString).isFalse()
          assertThat(it.jsonNull).isNotNull()
        }

        else -> {
          assertThat(it.isString).isTrue()
        }
      }
    }

    //
    //Display name is optional and nullable in TS (might be missing)
    //
    if (profile.displayName == null) {
      //The property *might* exist (as null) - but does not have to

      if (jsonElement.jsonObject.containsKey("displayName")) {
        //ensure it is null
        assertThat(jsonElement.jsonObject.getValue("displayName").jsonNull).isNotNull()
      }
    } else {
      //The property must be required!
      assertThat(jsonElement.jsonObject.getValue("displayName").jsonPrimitive.isString).isTrue()
    }

    //
    // Country is optional and non-nullable in TS (might be missing in the JSON, but never null)
    //
    if (jsonElement.jsonObject.containsKey("country")) {
      //must be non-null
      assertThat(jsonElement.jsonObject.getValue("country").jsonPrimitive.isString).isTrue()
    }

    //
    // Avatar URL is optional and nullable - but has a non null default value
    //
    if (profile.avatarUrl != null) {
      //must be non-null
      assertThat(jsonElement.jsonObject.getValue("avatarUrl").jsonPrimitive.isString).isTrue()
    }
  }

  /**
   * Contains all possible combinations of nullable and default values
   *
   * Corresponding TS type:
   * ```ts
   * export interface UserProfile {
   *   id: string;
   *   nickname: string | null;
   *   displayName?: string | null;
   *   country?: string;
   *   avatarUrl?: string | null;
   * }
   * ```
   */
  @Serializable
  private data class UserProfile(
    val id: String,
    val nickname: String?,
    val displayName: String? = null,
    val country: String = "DE",
    val avatarUrl: String? = "https://my-default-url",
  )
}
