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
package it.neckar.common.jackson

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import it.neckar.open.collections.fastForEach
import it.neckar.open.unit.si.ms

/**
 *
 */
class JacksonMillisAsIsoDateTimeModule(
  /**
   * The magic names that are automatically detected and serialized as ISO date time
   */
  val magicNames: Set<String> = setOf("createdAt", "updatedAt", "created", "updated"),

  ) : SimpleModule("JacksonMillisAsIsoDateTimeModule") {

  private val magicNamesGetter: Set<String> = magicNames.map { "get" + it.lowercase() }.toSet()

  init {
    addDeserializer(Double::class.java, JacksonMillisAsIsoDateTimeDeserializer())

    setSerializerModifier(object : BeanSerializerModifier() {
      override fun changeProperties(
        config: com.fasterxml.jackson.databind.SerializationConfig,
        beanDesc: com.fasterxml.jackson.databind.BeanDescription,
        beanProperties: MutableList<BeanPropertyWriter>,
      ): MutableList<BeanPropertyWriter> {

        beanProperties.fastForEach { propertyWriter ->
          val property = propertyWriter.member

          //Check if the property is of type Double
          if (property.type.rawClass != Double::class.java) {
            return@fastForEach
          }

          if (property.hasAnnotation(ms::class.java) || property.name.lowercase() in magicNamesGetter) {
            propertyWriter.assignSerializer(JacksonMillisAsIsoDateTimeSerializer() as JsonSerializer<Any>)
          }
        }

        return beanProperties
      }
    })
  }
}
