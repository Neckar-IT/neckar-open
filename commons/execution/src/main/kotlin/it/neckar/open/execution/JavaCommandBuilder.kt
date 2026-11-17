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


import java.io.File
import java.util.Collections
import java.util.Locale

/**
 * Class that is able to start a new java process
 *
 */
@Deprecated("No longer used!?")
class JavaCommandBuilder(
  val mainClass: String,
) {

  private val classPathElements = ArrayList<String>()
  private val vmProperties = ArrayList<String>()
  private val arguments = ArrayList<String>()

  val classPath: String?
    get() {
      if (classPathElements.isEmpty()) {
        return null
      }
      val stringBuilder = StringBuilder()
      val it = classPathElements.iterator()
      while (it.hasNext()) {
        val classPathElement = it.next()
        if (classPathElement.isEmpty()) {
          continue
        }
        stringBuilder.append(classPathElement)
        if (it.hasNext()) {
          stringBuilder.append(File.pathSeparator)
        }
      }

      val classPath = stringBuilder.toString()

      return if (classPath.contains(" ")) {
        if (System.getProperty("os.name").lowercase(Locale.ENGLISH).contains("windows").not()) {
          System.err.println("WARNING: Be careful! Added \" to the classpath. May be incompatible!")
        }
        '\"'.toString() + classPath + '\"'.toString()
      } else {
        classPath
      }
    }

  val commandLineElements: List<String>
    get() {
      val elements = ArrayList<String>()
      elements.add(JavaBin)

      for (property in getVmProperties()) {
        elements.add("-D$property")
      }

      val classPath = classPath
      if (classPath != null) {
        elements.add("-cp")
        elements.add(classPath)
      }

      elements.add(mainClass)

      for (argument in getArguments()) {
        elements.add(argument)
      }

      return elements
    }

  /**
   * Returns the command line as string. This method should only be used
   * for debugging purposes.
   * For creating a process use [commandLineElements] instead.
   *
   * @see [commandLineElements]]
   */
  val commandLine: String
    get() {
      val stringBuilder = StringBuilder()
      for (element in commandLineElements) {
        stringBuilder.append(element)
        stringBuilder.append(' ')
      }

      return stringBuilder.toString().trim { it <= ' ' }
    }

  /**
   *
   * Getter for the field `classPathElements`.
   *
   * @return a List object.
   */
  fun getClassPathElements(): List<String> {
    return Collections.unmodifiableList(classPathElements)
  }

  /**
   *
   * Setter for the field `classPathElements`.
   *
   * @param classPathElements a String object.
   */
  fun setClassPathElements(vararg classPathElements: String) {
    this.classPathElements.clear()
    for (element in classPathElements) {
      if (element.isEmpty()) {
        continue
      }
      this.classPathElements.add(element)
    }
  }

  /**
   *
   * addClassPathElement
   *
   * @param classPathElement a String object.
   */
  fun addClassPathElement(classPathElement: String) {
    classPathElements.add(classPathElement)
  }

  /**
   *
   * Getter for the field `vmProperties`.
   *
   * @return a List object.
   */
  fun getVmProperties(): List<String> {
    return Collections.unmodifiableList(vmProperties)
  }

  /**
   *
   * Setter for the field `vmProperties`.
   *
   * @param vmProperties a String object.
   */
  fun setVmProperties(vararg vmProperties: String) {
    this.vmProperties.clear()
    for (vmProperty in vmProperties) {
      if (vmProperty.isEmpty()) {
        continue
      }
      this.vmProperties.add(vmProperty)
    }
  }

  /**
   *
   * addVmProperty
   *
   * @param vmProperty a String object.
   */
  fun addVmProperty(vmProperty: String) {
    vmProperties.add(vmProperty)
  }

  /**
   *
   * addArgument
   *
   * @param argument a String object.
   */
  fun addArgument(argument: String) {
    this.arguments.add(argument)
  }

  /**
   *
   * Setter for the field `arguments`.
   *
   * @param arguments a String object.
   */
  fun setArguments(vararg arguments: String) {
    this.arguments.clear()
    for (argument in arguments) {
      if (argument.isEmpty()) {
        continue
      }
      this.arguments.add(argument)
    }
  }

  /**
   *
   * Getter for the field `arguments`.
   *
   * @return a List object.
   */
  fun getArguments(): List<String> {
    return Collections.unmodifiableList(arguments)
  }

  companion object {
    const val JavaBin: String = "java"
  }
}
