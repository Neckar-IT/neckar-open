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
import it.neckar.open.annotations.ForcePolymorphic
import it.neckar.open.serialization.roundTrip
import it.neckar.open.test.utils.isJsonEqualTo
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TypeSerializationTest {
  @Serializable
  private data class Wrapper(
    @ForcePolymorphic(Foo.SubFoo::class)
    val foo: Foo
  )

  @Serializable
  private sealed interface Foo {
    @Serializable
    @SerialName("subFoo")
    data class SubFoo(val id: String) : Foo
  }

  @Test
  fun testMoreSerialization() {
    roundTrip(Wrapper(Foo.SubFoo("id-123"))) {
      //language=JSON
      """
        {
          "foo" : {
            "type" : "subFoo",
            "id" : "id-123"
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun testTypeSerialization() {
    val json = Json {
      this.classDiscriminator = "foo" //The default is "type"
      this.classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS
    }

    val encoded = json.encodeToString(FooBar("test"))

    assertThat(encoded).isJsonEqualTo(
      """
        {
          "foo" : "CustomSerialName",
          "companyProfile" : "test"
        }
      """.trimIndent()
    )
  }

  @Test
  fun testTypeSerializationSealed() {
    val json = Json

    /**
     * Includes the "type" discriminator
     */
    assertThat(json.encodeToString(value = Project1ResponseWithSealedInterface(Project.Project1("id-1")))).isJsonEqualTo(
      """
      {
        "project1" : {
          "type" : "project1",
          "id" : "id-1"
        }
      }
      """.trimIndent()
    )

    /**
     * Does *NOT* include the "type" discriminator because we are using the specific class directly
     */
    assertThat(json.encodeToString(value = Project1ResponseWithConcreteClass(Project.Project1("id-1")))).isJsonEqualTo(
      """
      {
        "project1" : {
          "id" : "id-1"
        }
      }
      """.trimIndent()
    )


    assertThat(json.encodeToString(value = Project.Project1("id-1"))).isJsonEqualTo(
      """
      {
        "id" : "id-1"
      }
      """.trimIndent()
    )
  }

  @Serializable
  @SerialName("CustomSerialName")
  private data class FooBar(val companyProfile: String)

  /**
   * Overwriting the default "type" discriminator does *NOT* work, throws a conflict exception
   */
  @Serializable
  private sealed interface NotWorkingProject {
    @Serializable
    @SerialName("notWorkingProject1")
    data class Project1(val id: String, @EncodeDefault val type: String = "projectType") : Project
  }

  @Serializable
  private sealed interface Project {
    @Serializable
    @SerialName("project1")
    data class Project1(val id: String) : Project
  }

  /**
   * Includes the "type" discriminator because we are using Project and not Project1 directly
   * -> @ForcePolymorphic as marker
   */
  @Serializable
  private data class Project1ResponseWithSealedInterface(
    @ForcePolymorphic(Project.Project1::class)
    val project1: Project
  )

  @Serializable
  private data class Project1ResponseWithConcreteClass(val project1: Project.Project1)

}
