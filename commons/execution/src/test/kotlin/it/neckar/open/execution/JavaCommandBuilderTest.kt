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
package it.neckar.open.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 *
 */
class JavaCommandBuilderTest {

  private lateinit var starter: JavaCommandBuilder

  @BeforeEach
  fun setUp() {
    starter = JavaCommandBuilder("mainClass")
  }

  @Test
  fun testClasspath() {
    val classPathElements = arrayOf("a", "b", "", "c")
    starter.setClassPathElements(*classPathElements)
    assertEquals(3, starter.getClassPathElements().size.toLong())
    assertEquals("a" + File.pathSeparator + "b" + File.pathSeparator + "c", starter.classPath)
    starter.addClassPathElement("d")
    assertEquals("a" + File.pathSeparator + "b" + File.pathSeparator + "c" + File.pathSeparator + "d", starter.classPath)
  }

  @Test
  fun testVmProperties() {
    starter.setVmProperties("key=value", "", "key2=value2")
    assertEquals(2, starter.getVmProperties().size.toLong())
    assertEquals("key=value", starter.getVmProperties()[0])
    assertEquals("key2=value2", starter.getVmProperties()[1])
    starter.addVmProperty("d=e")
    assertEquals("d=e", starter.getVmProperties()[2])
  }

  @Test
  fun testArguments() {
    starter.setArguments("a", "b", "", "c")
    assertEquals(3, starter.getArguments().size.toLong())
  }

  @Test
  fun testAll() {
    assertEquals("java mainClass", starter.commandLine)
    starter.setClassPathElements("a", "b")
    assertEquals("java -cp a" + File.pathSeparator + "b mainClass", starter.commandLine)
    starter.setArguments("arg", "arg1")
    assertEquals("java -cp a" + File.pathSeparator + "b mainClass arg arg1", starter.commandLine)
    starter.setVmProperties("prop0", "prop1")
    assertEquals("java -Dprop0 -Dprop1 -cp a" + File.pathSeparator + "b mainClass arg arg1", starter.commandLine)
    val elements = starter.commandLineElements
    assertEquals(8, elements.size.toLong())
    assertEquals("java", elements[0])
    assertEquals("-Dprop0", elements[1])
    assertEquals("-Dprop1", elements[2])
    assertEquals("-cp", elements[3])
    assertEquals("a" + File.pathSeparator + "b", elements[4])
    assertEquals("mainClass", elements[5])
    assertEquals("arg", elements[6])
    assertEquals("arg1", elements[7])
  }
}
