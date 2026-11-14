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
package it.neckar.open.javafx

import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Labeled
import javafx.scene.control.TextInputControl
import javafx.scene.layout.Region


/**
 * Calls the [callback] for each node. Visits all children
 */
fun Node.visitSceneGraph(callback: (node: Node, level: Int) -> Unit) {
  this.visitSceneGraph(0, callback)
}

/**
 * Calls the [callback] for each node. Visits all children
 */
fun Node.visitSceneGraph(level: Int = 0, callback: (node: Node, level: Int) -> Unit) {
  callback(this, level)

  if (this is Parent) {
    this.childrenUnmodifiable.forEach { it.visitSceneGraph(level + 1, callback) }
  }
}

/**
 * Dumps the scene graph as string
 */
fun Node.dumpSceneGraph(): String {
  return buildString {
    visitSceneGraph { node, level ->
      val indents = " ".repeat(level)

      val propertiesAsString = node.properties
        .filter { it.key != "javafx.scene.control.Tooltip" }
        .map { "${it.key}: ${it.value}" }.joinToString(", ")

      append("$indents${node.javaClass.name}" +
        "\n$indents -StyleClass: ${node.styleClass}" +
        "\n$indents -Style: ${node.style}" +
        "\n$indents -LayoutBounds: ${node.layoutBounds}" +
        "\n$indents -LayoutBoundsInParent: ${node.boundsInParent}")

      if (node is Region) {
        val insets = node.insets
        val padding = node.padding
        append("\n$indents -Insets: \n$insets -Padding: $padding")
      }

      if (node is Labeled) {
        append("\n$indents -Text: ${node.text}")
        append("\n$indents -Font: ${node.font}")
      }

      if(node is TextInputControl){
        append("\n$indents -Text: ${node.text}")
        append("\n$indents -Font: ${node.font}")
      }
      append("\n$indents -Properties: $propertiesAsString")
      appendLine()
    }
  }
}


