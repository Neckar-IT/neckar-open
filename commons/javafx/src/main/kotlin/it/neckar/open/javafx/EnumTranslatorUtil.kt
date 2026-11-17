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

import it.neckar.open.annotations.JavaFriendly
import javax.annotation.Nonnull

typealias EnumTranslatorLambda = (item: Enum<*>) -> String?

/**
 * Enum translator
 */
object EnumTranslatorUtil {
  /**
   * Fallback enum translator
   */
  private val fallback: EnumTranslatorLambda = { item ->
    System.err.println("No enum translation found for <$item>")
    item.name
  }

  /**
   * The enum translators. The first one that returns a result will be used
   */
  @Nonnull
  private val enumTranslatorsChain = mutableListOf(fallback)

  /**
   * Adds an enum translator at *first* position.
   * The given enum translator is used *first*
   */
  @JvmStatic
  @JavaFriendly
  fun addEnumTranslator(enumTranslator: EnumTranslator) {
    enumTranslator(enumTranslator::translate)
  }

  /**
   * Adds an enum translator
   */
  fun enumTranslator(enumTranslator: EnumTranslatorLambda) {
    enumTranslatorsChain.add(0, enumTranslator)
  }

  /**
   * Resets the chain. Just adds the fallback enum translator
   */
  @JvmStatic
  fun reset() {
    enumTranslatorsChain.clear()
    enumTranslatorsChain.add(fallback)
  }

  /**
   * Translates the num
   */
  @JvmStatic
  fun translate(item: Enum<*>): String {
    return enumTranslatorsChain.firstNotNullOf { it(item) }
  }

  /**
   * The enum translator that uses the translator chain
   */
  @JvmStatic
  val enumTranslator: EnumTranslator = object : EnumTranslator {
    override fun translate(item: Enum<*>): String {
      return EnumTranslatorUtil.translate(item)
    }
  }
}
