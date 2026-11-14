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
package it.neckar.rest.openapi

import assertk.assertFailure
import assertk.assertions.hasMessage
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

/**
 * A reference exists only for a type the schema does not inline, a primitive schema only for a class serialized as a
 * primitive.
 */
class SchemaDescriptorReferenceTest {
  @Serializable(with = ArticleCode.ArticleCodeSerializer::class)
  class ArticleCode(val code: String) {
    object ArticleCodeSerializer : KSerializer<ArticleCode> {
      override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ArticleCode", PrimitiveKind.STRING)
      override fun serialize(encoder: Encoder, value: ArticleCode): Unit = encoder.encodeString(value.code)
      override fun deserialize(decoder: Decoder): ArticleCode = ArticleCode(decoder.decodeString())
    }
  }

  @JvmInline
  value class Quantity(val value: Int)

  @Serializable
  data class Article(val name: String)

  @Test
  fun `an inlined type has no reference`() {
    assertFailure { SchemaDescriptor.reference(typeOf<String>()) }
      .hasMessage("Cannot create a reference for [kotlin.String]: it is a native type")
    assertFailure { SchemaDescriptor.reference(typeOf<Quantity>()) }
      .hasMessage("Cannot create a reference for [it.neckar.rest.openapi.SchemaDescriptorReferenceTest.Quantity]: it is a value class")
    assertFailure { SchemaDescriptor.reference(typeOf<ArticleCode>()) }
      .hasMessage("Cannot create a reference for [it.neckar.rest.openapi.SchemaDescriptorReferenceTest.ArticleCode]: it is serialized as a primitive")
    assertFailure { SchemaDescriptor.referenceObject(typeOf<ArticleCode>()) }
      .hasMessage("Cannot create a reference schema for [it.neckar.rest.openapi.SchemaDescriptorReferenceTest.ArticleCode]: it is serialized as a primitive")
  }

  @Test
  fun `only a class serialized as a primitive has a primitive schema`() {
    assertFailure { SchemaDescriptor.SerializedAsPrimitiveSchema(typeOf<Article>()) }
      .hasMessage("Cannot create a primitive schema for [it.neckar.rest.openapi.SchemaDescriptorReferenceTest.Article]: its serialized form is Class")
  }
}
