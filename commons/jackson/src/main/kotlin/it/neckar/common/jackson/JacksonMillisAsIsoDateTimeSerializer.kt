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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import it.neckar.open.formatting.formatUtc
import it.neckar.open.formatting.parseUtc
import java.time.DateTimeException

/**
 *
 */
class JacksonMillisAsIsoDateTimeSerializer : JsonSerializer<Double>() {
  override fun serialize(value: Double, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeString(value.formatUtc())
  }
}

class JacksonMillisAsIsoDateTimeDeserializer : JsonDeserializer<Double>() {
  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Double? {
    if (parser.hasToken(JsonToken.VALUE_STRING)) {
      val unparsedDate = parser.valueAsString

      try {
        return parseUtc(unparsedDate)
      } catch (ignore: DateTimeException) {
        //The message of the DateTimeException ("Text '…' could not be parsed at index 0") names neither the expected
        //format nor the property. Reporting through the context yields an InvalidFormatException that carries the
        //offending value, the expected format and the reference chain to the property.
        //The DateTimeException is deliberately *not* attached as cause: Jackson replaces a thrown exception by its
        //root cause (BeanDeserializerBase.wrapAndThrow), which would restore exactly the message we improve here.
        return ctxt.handleWeirdStringValue(
          Double::class.javaObjectType,
          unparsedDate,
          "Expected millis as an ISO date time in UTC (e.g. \"1970-01-15T06:56:07.890Z\"), as \"NaN\" or as \"∞\"",
        ) as Double?
      }
    }

    if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT) || parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      //A number - integral (VALUE_NUMBER_INT, e.g. whole millis) or fractional (VALUE_NUMBER_FLOAT)
      return parser.doubleValue
    }

    return ctxt.handleUnexpectedToken(
      Double::class.javaObjectType,
      parser.currentToken,
      parser,
      "Expected millis as an ISO date time string or as a number but got %s: <%s>",
      parser.currentToken,
      parser.text,
    ) as Double?
  }
}
