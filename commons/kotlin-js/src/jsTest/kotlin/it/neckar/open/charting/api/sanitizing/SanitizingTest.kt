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
package it.neckar.open.charting.api.sanitizing

import assertk.*
import assertk.assertions.*
import it.neckar.open.isUndefined
import kotlin.test.Test


external interface MyPerson {
  val name: String
  val age: Int
  val myDouble: Double
  val myBool: Boolean

  val type: PersonType
}

enum class PersonType {
  Option1,
  Option2,
}

class SanitizingTest {
  @Test
  fun testWithUndefined() {
    val jsObject = js("{}")
    assertThat<Boolean>(isUndefined(jsObject)).isFalse()

    val myPerson = jsObject.unsafeCast<MyPerson>()

    assertThat(myPerson.type.isUndefined()).isTrue()
    assertThat(myPerson.name.isUndefined()).isTrue()
    assertThat(myPerson.age.isUndefined()).isTrue()
    assertThat(myPerson.myDouble.isUndefined()).isTrue()
    assertThat(myPerson.myBool.isUndefined()).isTrue()

    //Undefined returned as null
    assertThat(myPerson.type).isEqualTo(null)
    assertThat(myPerson.name).isEqualTo(null)
    assertThat(myPerson.age).isEqualTo(null)
    assertThat(myPerson.myDouble).isEqualTo(null)
    assertThat(myPerson.myBool).isEqualTo(null)
  }

  @Test
  fun testWithUndefinedWithSanitization() {
    val jsObject = js("{}")
    assertThat(isUndefined(jsObject)).isFalse()

    val myPerson = jsObject.unsafeCast<MyPerson>()

    assertFailure {
      myPerson.type.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Possible values: Option1, Option2")
        contains("Enum")
        contains("undefined")
      }
    }

    assertFailure {
      myPerson.name.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("String")
        contains("undefined")
      }
    }

    assertFailure {
      myPerson.age.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Int")
        contains("undefined")
      }
    }

    assertFailure {
      myPerson.myBool.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Boolean")
        contains("undefined")
      }
    }

    assertFailure {
      myPerson.myDouble.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Double")
        contains("undefined")
      }
    }
  }


  @Test
  fun testWithWrongValuesWithSanitization() {
    val jsObject = js("""{ 'name': 17, 'age': "asdf", 'myBool': 18, 'myDouble': 'abc', 'type': 'Option7' }""")

    val myPerson = jsObject.unsafeCast<MyPerson>()

    assertFailure {
      myPerson.type.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Possible values: Option1, Option2")
        contains("Enum")
        contains("Option7")
      }
    }

    assertFailure {
      myPerson.name.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("String")
        contains("17")
      }
    }

    assertFailure {
      myPerson.age.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Int")
        contains("asdf")
      }
    }

    assertFailure {
      myPerson.myBool.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Boolean")
        contains("18")
      }
    }

    assertFailure {
      myPerson.myDouble.sanitize()
    }.all {
      isInstanceOf(SanitizingFailedException::class)
      message().isNotNull().all {
        contains("Double")
        contains("abc")
      }
    }
  }


  @Test
  fun testFilledObjectWithoutSanitization() {
    val jsObject = js("""{ 'name': 'MyName', 'age': 77, 'myDouble': 99.4, 'myBool': true, 'type': 'Option1' }""")

    val myPerson = jsObject.unsafeCast<MyPerson>()

    assertThat(myPerson.type).isEqualTo("Option1") //enums are returned as strings
    assertThat(myPerson.name).isEqualTo("MyName")
    assertThat(myPerson.age).isEqualTo(77)
    assertThat(myPerson.myDouble).isEqualTo(99.4)
    assertThat(myPerson.myBool).isTrue()
  }


  @Test
  fun testFilledObjectWithSanitization() {
    val jsObject = js("""{ 'name': 'MyName', 'age': 77, 'myDouble': 99.4, 'myBool': true, 'type': 'Option1' }""")

    val myPerson = jsObject.unsafeCast<MyPerson>()

    assertThat(myPerson.type.sanitize()).isEqualTo(PersonType.Option1) //enums are returned as strings
    assertThat(myPerson.name.sanitize()).isEqualTo("MyName")
    assertThat(myPerson.age.sanitize()).isEqualTo(77)
    assertThat(myPerson.myDouble).isEqualTo(99.4)
    assertThat(myPerson.myBool.sanitize()).isTrue()
  }
}

fun jsTypeOf(o: Any): String {
  return js("typeof o")
}

