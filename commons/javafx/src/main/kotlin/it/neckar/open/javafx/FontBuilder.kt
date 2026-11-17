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

import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javax.annotation.Nonnull

/**
 * A builder for [Font]s.
 */
class FontBuilder @JvmOverloads constructor(font: Font = Font.getDefault()) {
  private var family: String
  private var weight: FontWeight
  private var posture: FontPosture
  private var size: Double

  init {
    family = font.family
    size = font.size
    weight = FontWeight.NORMAL
    posture = FontPosture.REGULAR
  }

  fun withFamily(@Nonnull family: String): FontBuilder {
    this.family = family
    return this
  }

  fun withWeight(@Nonnull weight: FontWeight): FontBuilder {
    this.weight = weight
    return this
  }

  fun withPosture(@Nonnull posture: FontPosture): FontBuilder {
    this.posture = posture
    return this
  }

  fun withSize(size: Double): FontBuilder {
    this.size = size
    return this
  }

  fun build(): Font {
    return Font.font(family, weight, posture, size)
  }

  companion object {
    @JvmStatic
    fun create(): FontBuilder {
      return FontBuilder()
    }

    @JvmStatic
    fun createWithFont(@Nonnull font: Font): FontBuilder {
      return FontBuilder(font)
    }
  }
}
