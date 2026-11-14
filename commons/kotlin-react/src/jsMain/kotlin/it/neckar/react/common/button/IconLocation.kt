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
package it.neckar.react.common.button

import kotlin.contracts.contract

/**
 * The location of a button
 */
enum class IconLocation {
  Left,
  Center,
  Right,
}

/**
 * Represents a button icon with icon location
 */
sealed interface ButtonIcon {
  val icon: String
  val iconLocation: IconLocation

  companion object {
    fun centered(icon: String): ButtonIconCentered = ButtonIconCentered(icon)

    /**
     * Creates an icon on the left
     */
    fun left(icon: String): ButtonIconLeft = ButtonIconLeft(icon)

    /**
     * Creates an icon on the right
     */
    fun right(icon: String): ButtonIconRight = ButtonIconRight(icon)
  }
}


/**
 * Represents the icon for a button
 */
data class ButtonIconLeft(
  override val icon: String,
) : ButtonIcon {
  override val iconLocation: IconLocation
    get() = IconLocation.Left
}

data class ButtonIconRight(
  override val icon: String,
) : ButtonIcon {
  override val iconLocation: IconLocation
    get() = IconLocation.Right
}

data class ButtonIconCentered(
  override val icon: String,
) : ButtonIcon {
  override val iconLocation: IconLocation
    get() = IconLocation.Center
}


/**
 * Returns true if there is an icon on the left side of the button.
 *
 * Return false if this is null or the icon is on the right side.
 */
fun ButtonIcon?.isLeft(): Boolean {
  contract {
    returns(true) implies (this@isLeft != null)
  }

  if (this == null) {
    return false
  }

  return this.iconLocation == IconLocation.Left
}

/**
 * Returns true if there is an icon in the center of the button (without a label).
 *
 */
fun ButtonIcon?.isCenter(): Boolean {
  contract {
    returns(true) implies (this@isCenter != null)
  }

  if (this == null) {
    return false
  }

  return this.iconLocation == IconLocation.Center
}

/**
 * Returns true if there is an icon on the right side of the button.
 */
fun ButtonIcon?.isRight(): Boolean {
  contract {
    returns(true) implies (this@isRight != null)
  }

  if (this == null) {
    return false
  }

  return this.iconLocation == IconLocation.Right
}
