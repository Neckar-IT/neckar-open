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
package it.neckar.react.common

import react.*
import react.dom.*

object FontAwesome {
  /**
   * Adds a sign-out span
   */
  fun RBuilder.faSignOut() {
    icon(FontAwesomeIcons.SignOut)
  }

  fun RBuilder.faBug() {
    icon(FontAwesomeIcons.Bug)
  }

  fun RBuilder.faHome() {
    icon(FontAwesomeIcons.Home)
  }

  fun RBuilder.faEdit() {
    icon(FontAwesomeIcons.Edit)
  }

  fun RBuilder.faSave() {
    i(classes = FontAwesomeIcons.Save) {}
  }

  fun RBuilder.faAdd() {
    i(classes = FontAwesomeIcons.Add) {}
  }

  fun RBuilder.faPhone() {
    i(classes = FontAwesomeIcons.Phone) {}
  }

  fun RBuilder.faReload() {
    i(classes = FontAwesomeIcons.Sync) {}
  }

  fun RBuilder.faCancel() {
    i(classes = FontAwesomeIcons.Ban) {}
  }

  fun RBuilder.faGift() {
    i(classes = FontAwesomeIcons.Gift) {}
  }

  fun RBuilder.faHorizontalArrows() {
    i(classes = FontAwesomeIcons.HorizontalArrows) {}
  }

  fun RBuilder.faVerticalArrows() {
    i(classes = FontAwesomeIcons.VerticalArrows) {}
  }

  fun RBuilder.faArrowRight() {
    i(classes = FontAwesomeIcons.ArrowRight) {}
  }

  fun RBuilder.faArrowLeft() {
    i(classes = FontAwesomeIcons.ArrowLeft) {}
  }

  fun RBuilder.faUser() {
    i(classes = FontAwesomeIcons.User) {}
  }

  fun RBuilder.faAddUser() {
    i(classes = FontAwesomeIcons.AddUser) {}
  }

  fun RBuilder.faInformation() {
    i(classes = FontAwesomeIcons.Information) {}
  }

  fun RBuilder.faCircleQuestion() {
    i(classes = FontAwesomeIcons.QuestionCircle) {}
  }

  fun RBuilder.faCircleExclamation() {
    i(classes = FontAwesomeIcons.ExclamationCircle) {}
  }

  fun RBuilder.faLockClosed() {
    i(classes = FontAwesomeIcons.Lock) {}
  }

  fun RBuilder.faLockOpen() {
    i(classes = FontAwesomeIcons.LockOpen) {}
  }

  fun RBuilder.faEye() {
    i(classes = FontAwesomeIcons.Eye) {}
  }

  fun RBuilder.faEyeSlash() {
    i(classes = FontAwesomeIcons.EyeSlash) {}
  }

  fun RBuilder.faCircleCheck() {
    i(classes = FontAwesomeIcons.CircleCheck) {}
  }

  fun RBuilder.faMinus() {
    i(classes = FontAwesomeIcons.Minus) {}
  }

  fun RBuilder.faInformationCircle() {
    i(classes = FontAwesomeIcons.InformationCircle) {}
  }

  fun RBuilder.faSpinner() {
    i(classes = FontAwesomeIcons.Busy) {}
  }

  fun RBuilder.faSort() {
    i(classes = FontAwesomeIcons.Sort) {}
  }

  fun RBuilder.faSortDown() {
    i(classes = FontAwesomeIcons.SortDown) {}
  }

  fun RBuilder.faSortUp() {
    i(classes = FontAwesomeIcons.SortUp) {}
  }

  fun RBuilder.faTrash() {
    i(classes = FontAwesomeIcons.Trash) {}
  }

  /**
   * Creates a new icon using "i" with the given icon class
   */
  fun RBuilder.icon(iconClass: String) {
    i(classes = iconClass) {}
  }
}
