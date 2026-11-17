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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface1
import it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface1.Child1
import it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface1.Child1.Child1Object
import it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface2
import it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface2.Child2
import it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface2.Child2.Child2Object
import it.neckar.open.kotlin.reflect.classForName
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf


/**
 *
 */
class ReflectExtKtTest {
  private interface WithArgs<T, U>

  private data class Foobar(
    val foo: String,
    val bar: Int,
  )

  private class WithTransientProperty {
    @Transient
    val transientProperty: String = "transient"

    val nonTransientProperty: String = "nonTransient"
  }

  @Test
  fun testPropertyTransient() {
    assertThat(WithTransientProperty::transientProperty.isTransient()).isTrue()
    assertThat(WithTransientProperty::nonTransientProperty.isTransient()).isFalse()
  }

  @Test
  fun testFindCallable() {
    val clazz = Foobar::class

    val callable = clazz.getProperty("bar")
    assertThat(callable.call(Foobar("test", 42))).isEqualTo(42)
  }

  @Test
  fun testFindCallableForConstructorParameter() {
    val clazz = Foobar::class

    val fooParameter = clazz.primaryConstructor!!.parameters.first()
    val callable = clazz.getBackingProperty(fooParameter)

    assertThat(callable).isNotNull()
    assertThat(callable.name).isEqualTo("foo")
    assertThat(callable.isAccessible).isTrue()

    assertThat(callable.call(Foobar("test", 42))).isEqualTo("test")
  }

  @Test
  fun testIsNothing() {
    assertThat(String::class.starProjectedType.isNothing()).isFalse()
    assertThat(Unit::class.starProjectedType.isNothing()).isFalse()
    assertThat(Nothing::class.starProjectedType.isNothing()).isTrue()

    assertThat(typeOf<String>().isNothing()).isFalse()
    assertThat(typeOf<Unit>().isNothing()).isFalse()

    assertThat(this::nothingVal.returnType.isNothing()).isTrue()
  }

  private val nothingVal: Nothing
    get() {
      throw IllegalStateException("This should never be called")
    }

  @Test
  fun testTypeArguments() {
    val type = typeOf<WithArgs<String, Int>>()
    assertThat(type.arguments).hasSize(2)
  }

  @Test
  fun testFindSupertype() {
    assertThat(SealedInterface1::class.findSupertype<SealedInterface>()).isEqualTo(SealedInterface::class.starProjectedType)
    assertThat(SealedInterface1::class.findSupertype<String>()).isNull()
    assertThat(SealedInterface1::class.findSupertype<Any>()).isEqualTo(Any::class.starProjectedType)
  }

  @Test
  fun testsimpleNameWithEnclosing() {
    assertThat(SealedInterface::class.simpleName).isEqualTo("SealedInterface")
    assertThat(SealedInterface::class.simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.SealedInterface")
    assertThat(SealedInterface1::class.simpleName).isEqualTo("SealedInterface1")
    assertThat(SealedInterface1::class.simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.SealedInterface.SealedInterface1")
    assertThat(Child1::class.simpleName).isEqualTo("Child1")
    assertThat(Child1::class.simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.SealedInterface.SealedInterface1.Child1")
    assertThat(Child1Object::class.simpleName).isEqualTo("Child1Object")
    assertThat(Child1Object::class.simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.SealedInterface.SealedInterface1.Child1.Child1Object")
  }

  @Test
  fun testTypeSimpleNameWithEnclosing() {
    assertThat(Child1::class.starProjectedType.simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.SealedInterface.SealedInterface1.Child1")
    assertThat(typeOf<List<Child1Object?>>().simpleNameWithEnclosing).isEqualTo("List<ReflectExtKtTest.SealedInterface.SealedInterface1.Child1.Child1Object?>")
    assertThat(typeOf<Map<in Child1, out List<Child2Object>>>().simpleNameWithEnclosing).isEqualTo(
      "Map<ReflectExtKtTest.SealedInterface.SealedInterface1.Child1, List<ReflectExtKtTest.SealedInterface.SealedInterface2.Child2.Child2Object>>",
    )

    assertThat(typeOf<Foo>().simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.Foo")
    assertThat(typeOf<Foo.Bar<Int>>().simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.Foo.Bar<Int>")
    assertThat(typeOf<Foo.Bar<Foo>>().simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.Foo.Bar<ReflectExtKtTest.Foo>")
  }

  @Test
  fun testGetSealedSubclasses() {
    assertThat(Sealed::class.sealedSubclasses).containsExactly(Sealed.SealedImpl1::class)
    assertThat(Sealed::class.getAllSealedSubclasses()).containsExactly(Sealed.SealedImpl1::class)
  }

  private sealed interface Sealed {
    data class SealedImpl1(val value: Int) : Sealed
  }

  @Test
  fun testSealedSubclasses() {
    assertThat(SealedInterface::class.sealedSubclasses).containsExactly(SealedInterface1::class, SealedInterface2::class)

    assertThat(SealedInterface::class.getAllSealedSubclasses()).hasSize(2)
    assertThat(SealedInterface::class.getAllSealedSubclasses().toSet()).containsOnly(
      Child1Object::class,
      Child2Object::class,
    )
  }

  @Test
  fun testAncestors() {
    assertThat(Child1Object::class.getAllAncestors()).containsOnly(
      SealedInterface::class.starProjectedType,
      SealedInterface1::class.starProjectedType,
      Child1::class.starProjectedType,
      NotASealedInterface::class.starProjectedType,
      Any::class.starProjectedType,
    )

    assertThat(Child1Object::class.findSealedInterfacesAncestors()).containsOnly(
      SealedInterface::class.starProjectedType,
      SealedInterface1::class.starProjectedType,
      Child1::class.starProjectedType,
    )
  }

  @Test
  fun testSealedInterfaces() {
    assertThat(Child1Object::class.isSealed).isFalse()
    assertThat(Child1Object::class.getSealedInterface()).isEqualTo(Child1::class)
    assertThat(Child1::class.simpleName).isEqualTo("Child1")
    assertThat(Child1::class.simpleNameWithEnclosing).isEqualTo("ReflectExtKtTest.SealedInterface.SealedInterface1.Child1")
    assertThat(Child1::class.supertypes).containsExactly(SealedInterface1::class.starProjectedType, Any::class.starProjectedType)
    assertThat(Child1::class.getSealedInterface()).isEqualTo(SealedInterface1::class)

    assertThat(Child1Object::class.hasCommonSealedAncestorWith(Child1Object::class)).isTrue()
    assertThat(Child1Object::class.hasCommonSealedAncestorWith(Child2Object::class)).isTrue()
    assertThat(Child1Object::class.hasCommonSealedAncestorWith(Child1::class)).isTrue()
    assertThat(Child1Object::class.hasCommonSealedAncestorWith(Child2::class)).isTrue()
    assertThat(Child1Object::class.hasCommonSealedAncestorWith(SealedInterface1::class)).isTrue()
    assertThat(Child1Object::class.hasCommonSealedAncestorWith(SealedInterface2::class)).isTrue()
    assertThat(Child1Object::class.hasCommonSealedAncestorWith(SealedInterface::class)).isFalse()

    assertThat(Child2Object::class.hasCommonSealedAncestorWith(Child1Object::class)).isTrue()
    assertThat(Child2Object::class.hasCommonSealedAncestorWith(Child2Object::class)).isTrue()
    assertThat(Child2Object::class.hasCommonSealedAncestorWith(Child1::class)).isTrue()
    assertThat(Child2Object::class.hasCommonSealedAncestorWith(Child2::class)).isTrue()
    assertThat(Child2Object::class.hasCommonSealedAncestorWith(SealedInterface1::class)).isTrue()
    assertThat(Child2Object::class.hasCommonSealedAncestorWith(SealedInterface2::class)).isTrue()
    assertThat(Child2Object::class.hasCommonSealedAncestorWith(SealedInterface::class)).isFalse()

    assertThat(Child1::class.hasCommonSealedAncestorWith(Child1Object::class)).isTrue()
    assertThat(Child1::class.hasCommonSealedAncestorWith(Child2Object::class)).isTrue()
    assertThat(Child1::class.hasCommonSealedAncestorWith(Child1::class)).isTrue()
    assertThat(Child1::class.hasCommonSealedAncestorWith(Child2::class)).isTrue()
    assertThat(Child1::class.hasCommonSealedAncestorWith(SealedInterface1::class)).isTrue()
    assertThat(Child1::class.hasCommonSealedAncestorWith(SealedInterface2::class)).isTrue()
    assertThat(Child1::class.hasCommonSealedAncestorWith(SealedInterface::class)).isFalse()

    assertThat(Child2::class.hasCommonSealedAncestorWith(Child1Object::class)).isTrue()
    assertThat(Child2::class.hasCommonSealedAncestorWith(Child2Object::class)).isTrue()
    assertThat(Child2::class.hasCommonSealedAncestorWith(Child1::class)).isTrue()
    assertThat(Child2::class.hasCommonSealedAncestorWith(Child2::class)).isTrue()
    assertThat(Child2::class.hasCommonSealedAncestorWith(SealedInterface1::class)).isTrue()
    assertThat(Child2::class.hasCommonSealedAncestorWith(SealedInterface2::class)).isTrue()
    assertThat(Child2::class.hasCommonSealedAncestorWith(SealedInterface::class)).isFalse()

    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(Child1Object::class)).isTrue()
    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(Child2Object::class)).isTrue()
    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(Child1::class)).isTrue()
    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(Child2::class)).isTrue()
    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(SealedInterface1::class)).isTrue()
    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(SealedInterface2::class)).isTrue()
    assertThat(SealedInterface1::class.hasCommonSealedAncestorWith(SealedInterface::class)).isFalse()

    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(Child1Object::class)).isTrue()
    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(Child2Object::class)).isTrue()
    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(Child1::class)).isTrue()
    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(Child2::class)).isTrue()
    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(SealedInterface1::class)).isTrue()
    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(SealedInterface2::class)).isTrue()
    assertThat(SealedInterface2::class.hasCommonSealedAncestorWith(SealedInterface::class)).isFalse()

  }

  @Test
  fun `generic children with different type arguments share their sealed ancestor`() {
    assertThat(GenericGood::class.hasCommonSealedAncestorWith(GenericBad::class)).isTrue()
    assertThat(GenericBad::class.hasCommonSealedAncestorWith(GenericGood::class)).isTrue()
    assertThat(GenericGood::class.hasCommonSealedAncestorWith(Child1Object::class)).isFalse()
  }

  @Test
  fun testHasSuperclass() {
    assertThat(SealedInterface::class.hasSupertype<SealedInterface>()).isTrue()
    assertThat(SealedInterface1::class.hasSupertype<SealedInterface>()).isTrue()
    assertThat(Child1::class.hasSupertype<SealedInterface>()).isTrue()
    assertThat(Child1Object::class.hasSupertype<SealedInterface>()).isTrue()

    assertThat(SealedInterface::class.isSubclassOf(SealedInterface::class)).isTrue()
    assertThat(SealedInterface1::class.isSubclassOf(SealedInterface::class)).isTrue()
    assertThat(Child1::class.isSubclassOf(SealedInterface::class)).isTrue()
    assertThat(Child1Object::class.isSubclassOf(SealedInterface::class)).isTrue()
  }

  @Test
  fun testIsEnum() {
    assertThat(FooEnum::class.isEnum()).isTrue()
    assertThat(FooEnum.FOO::class).isEqualTo(FooEnum::class)

    val enumEntriesKnownType: Array<FooEnum> = FooEnum::class.enumEntries
    assertThat(enumEntriesKnownType).hasSize(2)
    //Ensure `name` can be called
    assertThat(enumEntriesKnownType.first().name).isEqualTo(FooEnum.FOO.name)

    //now without known class type
    val enumEntriesUnknownType = (FooEnum::class as KClass<*>).asEnumClass().enumEntries
    assertThat(enumEntriesUnknownType).hasSize(2)
    assertThat(enumEntriesUnknownType.first().name).isEqualTo(FooEnum.FOO.name)
  }

  @Test
  fun testClassForName() {
    assertThat(classForName("kotlin.String")).isEqualTo(String::class)
    assertThat(FooEnum::class.qualifiedName).isEqualTo("it.neckar.open.kotlin.lang.ReflectExtKtTest.FooEnum")
    assertThat(classForName(FooEnum::class.qualifiedName!!)).isEqualTo(FooEnum::class)
    // Kotlin collection aliases should resolve to their Kotlin interfaces
    assertThat(classForName("kotlin.collections.ArrayList")).isEqualTo(List::class)
    assertThat(classForName("kotlin.collection.ArrayList")).isEqualTo(List::class)
    assertThat(classForName("kotlin.collections.MutableList")).isEqualTo(MutableList::class)
  }

  @Test
  fun testFindKClass() {
    assertThat(typeOf<String>().findKClass()).isEqualTo(String::class)
    assertThat(WithArgs::class.typeParameters.first().createType().findKClass()).isNull()
  }

  @Test
  fun testAsKClassFailsForATypeParameter() {
    assertFailure {
      WithArgs::class.typeParameters.first().createType().asKClass()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<T> has no class as classifier")
  }

  @Test
  fun testGetSupertype() {
    assertThat(SealedInterface1::class.getSupertype<SealedInterface>()).isEqualTo(SealedInterface::class.starProjectedType)
  }

  @Test
  fun testGetSupertypeFailsForAnUnrelatedType() {
    assertFailure {
      SealedInterface1::class.getSupertype<String>()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedInterface.SealedInterface1> has no direct supertype <kotlin.String>")
  }

  @Test
  fun testValueClassWrappedType() {
    assertThat(Meters::class.valueClassWrappedType()).isEqualTo(typeOf<Double>())
  }

  @Test
  fun testValueClassWrappedTypeFailsForAnyOtherClass() {
    assertFailure {
      Foobar::class.valueClassWrappedType()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<it.neckar.open.kotlin.lang.ReflectExtKtTest.Foobar> is not a value class")
  }

  @Test
  fun testPrimaryConstructorParameterNames() {
    assertThat(Foobar::class.primaryConstructorParameterNames()).isEqualTo(setOf("foo", "bar"))
    assertThat(NotASealedInterface::class.primaryConstructorParameterNames()).isEmpty()
  }

  @Test
  fun testGetAllSealedObjectInstances() {
    assertThat(SealedInterface::class.getAllSealedObjectInstances()).containsExactlyInAnyOrder(Child1Object, Child2Object)
  }

  @Test
  fun testGetAllSealedObjectInstancesFailsForAClass() {
    assertFailure {
      SealedWithClass::class.getAllSealedObjectInstances()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("Every subclass of <it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedWithClass> must be an object but <it.neckar.open.kotlin.lang.ReflectExtKtTest.SealedWithClass.Instance> is not")
  }

  @JvmInline
  private value class Meters(val value: Double)

  private sealed interface SealedWithClass {
    data object Singleton : SealedWithClass
    data class Instance(val value: Int) : SealedWithClass
  }

  private enum class FooEnum {
    FOO, BAR
  }

  private interface NotASealedInterface

  private sealed interface GenericSealed<out T>

  private data class GenericGood<out T>(val value: T) : GenericSealed<T>

  private data object GenericBad : GenericSealed<Nothing>

  private sealed interface SealedInterface : NotASealedInterface {
    sealed interface SealedInterface1 : SealedInterface, NotASealedInterface {
      sealed interface Child1 : SealedInterface1 {
        data object Child1Object : Child1
      }
    }

    sealed interface SealedInterface2 : SealedInterface, NotASealedInterface {
      sealed interface Child2 : SealedInterface2 {
        data object Child2Object : Child2
      }
    }
  }

  private object Foo {
    interface Bar<T>
  }
}
