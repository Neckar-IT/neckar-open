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
package it.neckar.kotlin.reflect

import assertk.*
import assertk.assertions.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

class ReflectFullExtTest {
  @Test
  fun testIt() {
    assertThat(FooBarInterface::foo.javaField).isNull()
    assertThat(FooBarInterface::foo.javaGetter).isNotNull()

    assertThat(FooBarInterface::foo.annotations).hasSize(1)
    assertThat(FooBarInterface::foo.javaGetter).isNotNull()
    assertThat(FooBarInterface::foo.javaField).isNull()
    assertThat(FooBarInterface::foo.javaGetter!!.name).isEqualTo("getFoo")
  }

  @Test
  fun testSameSignature() {
    assertThat(FooBarImpl::bar.javaGetter!!.hasSameSignatureAs(FooBarImpl::bar.javaGetter!!)).isTrue()
    assertThat(FooBarImpl::foo.javaGetter!!.hasSameSignatureAs(FooBarImpl::foo.javaGetter!!)).isTrue()
    assertThat(FooBarImpl::foo.javaGetter!!.hasSameSignatureAs(FooBarImpl::bar.javaGetter!!)).isFalse()
    assertThat(FooBarImpl::bar.javaGetter!!.hasSameSignatureAs(FooBarImpl::foo.javaGetter!!)).isFalse()
  }

  @Test
  fun testSameSignature3() {
    assertThat(FooBarInterface::bar.javaGetter!!.hasSameSignatureAs(FooBarImpl::bar.javaGetter!!)).isTrue()
    assertThat(FooBarInterface::foo.javaGetter!!.hasSameSignatureAs(FooBarImpl::foo.javaGetter!!)).isTrue()
    assertThat(FooBarInterface::foo.javaGetter!!.hasSameSignatureAs(FooBarImpl::bar.javaGetter!!)).isFalse()
    assertThat(FooBarInterface::bar.javaGetter!!.hasSameSignatureAs(FooBarImpl::foo.javaGetter!!)).isFalse()
  }

  @Test
  fun testSameSignature2() {
    assertThat(FooBarImpl::bar.javaGetter!!.hasSameSignatureAs(FooBarInterface::bar.javaGetter!!)).isTrue()
    assertThat(FooBarImpl::foo.javaGetter!!.hasSameSignatureAs(FooBarInterface::foo.javaGetter!!)).isTrue()
    assertThat(FooBarImpl::foo.javaGetter!!.hasSameSignatureAs(FooBarInterface::bar.javaGetter!!)).isFalse()
    assertThat(FooBarImpl::bar.javaGetter!!.hasSameSignatureAs(FooBarInterface::foo.javaGetter!!)).isFalse()
  }

  @Test
  fun testSuper() {
    assertThat(FooBarImpl::class.java.superClassesAndInterfaces).hasSize(2)
    assertThat(FooBarImpl::class.java.superClassesAndInterfaces).containsExactly(FooBarInterface::class.java, Object::class.java)
  }

  @Test
  fun testFindAnnotationsSuper() {
    assertThat(FooBarImpl::foo.findAnnotations(Transient::class)).isEmpty()
    assertThat(FooBarImpl::foo.findAnnotationsSuper(Transient::class)).hasSize(1)
  }

  @Test
  fun testFindAnnotationsIface() {
    assertThat(FooBarInterface::foo.findAnnotations(Transient::class)).hasSize(1)
    assertThat(FooBarInterface::foo.findAnnotationsSuper(Transient::class)).hasSize(1)
  }

  @Test
  fun `findAnnotationsSuper resolves an annotation via the getter for a computed override`() {
    // computed has no backing field in the implementation, so resolution must go through the
    // getter branch of findAnnotationsSuper, not the field branch.
    assertThat(ComputedImpl::computed.javaField).isNull()
    assertThat(ComputedImpl::computed.findAnnotations(Transient::class)).isEmpty()
    assertThat(ComputedImpl::computed.findAnnotationsSuper(Transient::class)).hasSize(1)
  }

  interface ComputedInterface {
    @Transient
    val computed: String
  }

  class ComputedImpl : ComputedInterface {
    override val computed: String
      get() = "value"
  }

  interface FooBarInterface {
    @Transient
    val foo: String
    val bar: String
  }

  class FooBarImpl(
    override val foo: String,
    override val bar: String,
  ) : FooBarInterface


  interface RestResponseInTest<DataType> {
    /**
     * Returns true if the result is a success
     */
    @Transient
    val isSuccess: Boolean

    /**
     * Returns true if the result is a failure
     */
    @Transient
    val isFailure: Boolean
      get() = isSuccess.not()

    /**
     * Casts this to a success object or throws an exception
     */
    fun asSuccess(): GoodResponse<DataType>

    /**
     * Represents a success response
     */
    interface GoodResponse<DataType> : RestResponseInTest<DataType> {

      /**
       * The data that is provided by success
       */
      val data: DataType
    }

    /**
     * Tagging interface for failure responses
     */
    interface BadResponse<DataType> : RestResponseInTest<DataType> {
    }
  }

  /**
   * Response for
   */
  @Serializable
  sealed interface FooBarResponse : RestResponseInTest<String> {
    override val isSuccess: Boolean
      get() = this is Good

    override fun asSuccess(): Good {
      return this as Good
    }

    @Serializable
    @SerialName("good")
    data class Good(override val data: String) : FooBarResponse, RestResponseInTest.GoodResponse<String>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooBarResponse, RestResponseInTest.BadResponse<String>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: String): FooBarResponse = Good(data)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): FooBarResponse = Failed

      /**
       * Creates a success method for non-null results, failure for null results
       */
      fun createFor(data: String?): FooBarResponse {
        return if (data != null) {
          good(data)
        } else {
          bad()
        }
      }
    }
  }

}
