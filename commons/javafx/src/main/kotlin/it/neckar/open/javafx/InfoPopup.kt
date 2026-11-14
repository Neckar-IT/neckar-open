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

import it.neckar.open.resources.getResourceSafe
import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.effect.ColorAdjust
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Popup
import javax.annotation.Nonnull

/**
 * A popup that displays an information message.
 */
class InfoPopup private constructor(
  /**
   * The content of the info popup
   */
  content: Node,
  /**
   * The max width of the popup
   */
  maxWidth: Double
) : Popup() {
  init {
    isAutoHide = true
    isAutoFix = true

    val root = createContent(content, maxWidth)

    root.stylesheets.add(javaClass.getResourceSafe("InfoPopupService.css").toExternalForm())

    root.styleClass.add("sick-uiglv2")
    root.styleClass.add("info-popup")
    getContent().add(root)
  }

  /**
   * Show the popup for the given parent node
   */
  private fun show(@Nonnull parent: Node) {
    val bounds = parent.localToScreen(parent.layoutBounds)
    show(parent, bounds.minX + 5, bounds.minY + bounds.height + 5)
  }

  companion object {
    /**
     * The info icon
     */
    private val infoIcon = InfoPopup::class.java.getResourceSafe("info_16.png").toExternalForm()

    /**
     * Creates the content
     */
    private fun createContent(@Nonnull content: Node, maxWidth: Double): Parent {
      val root = VBox(content)
      root.maxWidth = maxWidth
      return root
    }

    /**
     * Shows the info popup
     */
    private fun showInfoPopup(parent: Node, content: Node, maxWidth: Double) {
      InfoPopup(content, maxWidth).show(parent)
    }

    @JvmStatic
    @JvmOverloads
    fun createInfoBox(@Nonnull message: String, maxWidth: Double = 500.0): ImageView {
      val text = Text(message)
      val textFlow = TextFlow(text)
      return createInfoBox(textFlow, maxWidth)
    }


    @JvmStatic
    @JvmOverloads
    fun createInfoBox(@Nonnull content: Node, maxWidth: Double = 500.0): ImageView {
      return infoBox(maxWidth) { content }
    }

    fun infoBox(maxWidth: Double = 500.0, @Nonnull content: () -> Node): ImageView {
      val imageView = ImageView(infoIcon)
      imageView.cursor = Cursor.HAND

      // make image brighter in case the image view is disabled
      imageView.effectProperty().bind(Bindings.createObjectBinding({
        if (imageView.isDisabled) {
          val brighter = ColorAdjust()
          brighter.brightness = 0.7
          brighter
        } else {
          null
        }
      }, imageView.disabledProperty()))

      imageView.onMouseClicked = EventHandler { showInfoPopup(imageView, content(), maxWidth) }
      return imageView
    }
  }
}
