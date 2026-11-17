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
package it.neckar.open.properties

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1

/**
 *
 */
class PropertiesPathTest {
  @Test
  fun testNullable() {
    val path1: PropertiesPath<Child?> = Parent::child + Child::child
    val path2: PropertiesPath<String> = Parent::child + Child::name
    val path3: PropertiesPath<Child?> = Parent::nullableChild + Child::child
    val path4: PropertiesPath<Child?> = Child::child + Child::child

    val property: KProperty1<Child, Child?> = Child::child

    assertThat((Parent::child + Child::child + Child::child).asString()).isEqualTo("child.child.child")
    assertThat((Parent::nullableChild + Child::child + Child::child).asString()).isEqualTo("nullableChild.child.child")
    assertThat((Child::child + Child::child).asString()).isEqualTo("child.child")
  }

  @Test
  fun testOperator() {
    assertThat(Parent::child.plus(Child::name).properties()).hasSize(2)
    assertThat((Parent::child + Child::name).properties()).hasSize(2)

    assertThat((Parent::child + Child::leaf).plus(Leaf::value).properties()).hasSize(3)
    assertThat((Parent::child + Child::leaf + (Leaf::value)).properties()).hasSize(3)
    assertThat((Parent::child + Child::leaf + Leaf::value).properties()).hasSize(3)
  }

  @Test
  fun testBasicPath() {
    run {
      val builder: PropertiesPath<Child> = PropertiesPath(Parent::child)
      val builder2: PropertiesPath<String> = builder.add(Child::name)
      assertThat(builder2.properties()).hasSize(2)

      val path = builder2
      assertThat(path.properties()).hasSize(2)
    }

    run {
      val builder: PropertiesPath<Child> = PropertiesPath(Parent::child)
      val builder2: PropertiesPath<Child?> = builder.add(Child::child)
      val builder3: PropertiesPath<Child?> = builder2.add(Child::child)
      val builder4 = builder3.add(Child::leaf)
      val builder5 = builder4.add(Leaf::value)

      assertThat(builder5.properties()).hasSize(5)

      val path = builder5
      assertThat(path.properties()).hasSize(5)
    }


    run {
      val emptyBuilder: PropertiesPath<Any> = PropertiesPath()
      val builder1 = emptyBuilder.add(Parent::child)
      val builder2 = builder1.add(Child::name)
      assertThat(builder2.properties()).hasSize(2)
    }

    val path = PropertiesPath()
      .add(Parent::child)
      .add(Child::child)
      .add(Child::child)
      .add(Child::child)
      .add(Child::child)
      .add(Child::leaf)
      .add(Leaf::value)

    assertThat(path.properties()).hasSize(7)
    assertThat(path.asString()).isEqualTo("child.child.child.child.child.leaf.value")
  }

  @Test
  fun `a path walks into the elements of a list`() {
    assertThat(Parent::children.each(Child::name).asString()).isEqualTo("children.name")
    assertThat((Parent::children.each(Child::leaf) + Leaf::value).asString()).isEqualTo("children.leaf.value")
    assertThat((PropertiesPath(Parent::child) + Child::children).each(Leaf::value).asString())
      .isEqualTo("child.children.value")
  }

  private data class Parent(
    val name: String,
    val child: Child,
    val nullableChild: Child?,
    val children: List<Child>,
  )

  private data class Child(
    val name: String,
    val child: Child?,
    val leaf: Leaf?,
    val children: List<Leaf>,
  )

  private data class Leaf(
    val value: Int,
  )
}
