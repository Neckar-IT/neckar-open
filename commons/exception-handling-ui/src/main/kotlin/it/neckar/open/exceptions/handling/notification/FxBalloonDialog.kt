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
package it.neckar.open.exceptions.handling.notification

import it.neckar.open.resources.getResourceSafe
import it.neckar.open.exceptions.handling.Messages
import it.neckar.open.unit.other.px
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.Hyperlink
import javafx.scene.image.ImageView
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.StageStyle
import org.tbee.javafx.scene.layout.MigPane
import java.util.concurrent.Callable

/**
 * Java fx based notification balloon
 */
abstract class FxBalloonDialog(notification: Notification) : Dialog<ButtonType>() {
  init {
    initModality(Modality.NONE)
    initStyle(StageStyle.UNDECORATED)
    isResizable = true

    //Add a custom dialog pane to remove the button bar
    dialogPane = object : DialogPane() {
      override fun createButtonBar(): Node? {
        return null;
      }
    }
    dialogPane.stylesheets.add(javaClass.getResourceSafe("FxBalloonDialog.css").toExternalForm())
    dialogPane.styleClass.add("balloon-dialog")

    dialogPane.header = null

    val contentPane = MigPane("fill, wrap 2, insets 0", "[][grow,fill]", "[fill][fill,grow][fill]")
    contentPane.styleClass.add("pane")
    dialogPane.content = contentPane

    //Icon
    val imageView = ImageView()
    imageView.styleClass.add("icon")
    contentPane.add(imageView)

    //Headline
    val headline = Text(notification.title)
    headline.styleClass.add("headline")
    contentPane.getChildren().add(headline);

    contentPane.add(Text(notification.message), "span, wrap")

    //Details link
    val detailsCallback = notification.detailsCallback
    if (detailsCallback != null) {
      val detailsLink = Hyperlink(Messages.get("details"))
      detailsLink.styleClass.add("details-link")
      contentPane.add(detailsLink, "alignx right, span")

      detailsLink.layoutX = 20.0
      detailsLink.layoutY = 20.0

      detailsLink.onAction = EventHandler {
        closeBalloon()
        detailsCallback.detailsClicked(notification)
      }
    }

    //Close icon
    run {
      val iconView = FontAwesomeIconView(FontAwesomeIcon.TIMES_CIRCLE_ALT)
      iconView.styleClass.add("close-icon")

      iconView.isManaged = false
      iconView.glyphSize = 18
      contentPane.children.add(iconView)

      iconView.y = 0.0
      iconView.textOrigin = VPos.TOP
      @px val closeIconWidth = iconView.boundsInLocal.width
      iconView.xProperty().bind(Bindings.createDoubleBinding(Callable { dialogPane.width - closeIconWidth }, dialogPane.widthProperty()))

      iconView.onMouseClickedProperty().setValue(EventHandler { closeBalloon() })
    }
  }

  /**
   * Is called when the balloon should be closed
   */
  abstract fun closeBalloon()
}
