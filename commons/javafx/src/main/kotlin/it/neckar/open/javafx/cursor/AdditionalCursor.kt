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
package it.neckar.open.javafx.cursor

import it.neckar.open.http.Url
import it.neckar.open.resources.getResourceSafe
import javafx.scene.ImageCursor
import javafx.scene.image.Image

/**
 * Additional cursor icons
 */
object AdditionalCursor {
  val NotAllowed: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/not-allowed.png")).toExternalForm()))
  val ZoomIn: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/zoom-in.png")).toExternalForm()), 22.0, 22.0)
  val ZoomOut: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/zoom-out.png")).toExternalForm()), 22.0, 22.0)
  val Help: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/help.png")).toExternalForm()))
  val ContextMenu: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/context-menu.png")).toExternalForm()))
  val Pointer: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/pointer.png")).toExternalForm()), 22.0, 5.0)
  val Progress: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/progress.png")).toExternalForm()))
  val Cell: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/cell.png")).toExternalForm()), 22.0, 22.0)
  val AllScroll: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/all-scroll.png")).toExternalForm()), 22.0, 22.0)
  val ColResize: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/col-resize.png")).toExternalForm()), 22.0, 22.0)
  val RowResize: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/row-resize.png")).toExternalForm()), 22.0, 22.0)
  val NoDrop: ImageCursor = ImageCursor(Image(AdditionalCursor::class.getResourceSafe(Url.rootRelative("/it/neckar/open/javafx/cursor/no-drop.png")).toExternalForm()))
}
