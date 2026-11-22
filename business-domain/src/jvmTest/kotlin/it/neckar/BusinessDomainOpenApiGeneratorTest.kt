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
package it.neckar

import assertk.*
import assertk.assertions.*
import it.neckar.price.ArticleConditions
import it.neckar.price.rest.messages.ArticleConditionsPriceSummaryRequest
import it.neckar.price.rest.messages.ArticleConditionsPriceSummaryResponse
import it.neckar.price.rest.messages.PriceTimelineEntriesFilterCriteria
import it.neckar.price.rest.messages.PriceTimelineEntriesSortCriteria
import it.neckar.price.rest.messages.UpsertPriceTimelineEntryRequest
import it.neckar.price.rest.messages.ValueTimelineEntryStatus
import it.neckar.price.rest.views.ArticleConditionsLabourPriceRow
import it.neckar.price.rest.views.ArticleConditionsPriceRow
import it.neckar.price.rest.views.ArticleConditionsPriceSummary
import it.neckar.price.rest.views.PriceTimelineEntryView
import it.neckar.price.rest.views.PriceTimelineSummary
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.quote.rest.views.FlatQuoteDetails
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.test.utils.LocalResourceGenerationSupport
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.tag.Tag
import it.neckar.tag.rest.TagsResponse
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KClass

/**
 * Generates `openapi-business-domain.json` — the whole `business-domain` surface the frontend
 * consumes: tags, prices, article conditions and their timelines, and the flat quote.
 *
 * These types hang on no route of this module; the services that expose them live elsewhere. They
 * are listed as `additionalTypes` so they reach the shared spec.
 *
 * A type belongs in the list below as soon as the frontend imports it by name. Leaving it to a
 * transitive reference works until the message carrying it is restructured — then the exported
 * TypeScript type silently disappears.
 *
 * [ArticleConditions] is an open interface, so nothing derives its implementations the way a sealed
 * hierarchy is walked. They are discovered from the compiled package instead of listed by hand — see
 * [shippedArticleConditionsImplementations] — and registered before the interface, whose base schema
 * is built from the properties of the implementations already seen. The types that are generic over
 * the implementation — the timeline entry view, the summary and the upsert request — are not listed
 * here at all; a product registers the instantiation its article type uses, and for the pattern
 * service that happens transitively through the routes carrying them.
 *
 * See `internal/patterns/docs/kotlin/openapi-generation.md` for the other three specs this one is
 * merged with and for the step that joins them.
 */
class BusinessDomainOpenApiGeneratorTest {
  @Test
  fun testGenerate() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.usingHarvestedSpecs())

    val articleConditionsImplementations = shippedArticleConditionsImplementations()

    val generated = generator.generate(
      routes = emptyList(),
      additionalTypes = buildList {
        add(SchemaDescriptor.of(Tag::class))
        add(SchemaDescriptor.of(TagsResponse::class))
        add(SchemaDescriptor.of(PriceWithProfit::class))
        add(SchemaDescriptor.of(FlatQuoteDetails::class))
        articleConditionsImplementations.forEach { add(SchemaDescriptor.of(it)) }
        add(SchemaDescriptor.of(ArticleConditions::class))
        add(SchemaDescriptor.of(PriceTimelineSummary::class))
        add(SchemaDescriptor.of(PriceTimelineEntryView::class))
        add(SchemaDescriptor.of(UpsertPriceTimelineEntryRequest::class))
        add(SchemaDescriptor.of(PriceTimelineEntriesSortCriteria::class))
        add(SchemaDescriptor.of(PriceTimelineEntriesFilterCriteria.State::class))
        add(SchemaDescriptor.of(ValueTimelineEntryStatus::class))
        add(SchemaDescriptor.of(ArticleConditionsPriceSummaryRequest::class))
        add(SchemaDescriptor.of(ArticleConditionsPriceSummaryResponse::class))
        add(SchemaDescriptor.of(ArticleConditionsPriceSummary::class))
        add(SchemaDescriptor.of(ArticleConditionsPriceRow::class))
        add(SchemaDescriptor.of(ArticleConditionsLabourPriceRow::class))
      },
      metaData = OpenApiMetaData("Application Errors", "Contains application errors that might be provided with 4xx and 5xx status codes")
    )

    val schemaNames = generated.jsonObject.getValue("components").jsonObject.getValue("schemas").jsonObject.keys

    assertThat(schemaNames, "generated schemas").contains(ArticleConditions::class.simpleName)
    articleConditionsImplementations.forEach { implementation ->
      assertThat(schemaNames, "generated schemas").contains(implementation.simpleName)
    }

    LocalResourceGenerationSupport(
      target = File("build/generated/openapi/main/openapi-business-domain.json"),
    ).generate(generated.toStringPretty())
  }

  /**
   * Every [ArticleConditions] implementation this module ships, read off the compiled package rather
   * than listed by hand.
   *
   * A sealed hierarchy tells the generator its own members; an open interface does not, so a
   * hand-written list would let a new implementation reach production without a schema and without a
   * TypeScript type — silently, because nothing else references it by name. Discovery makes adding
   * an implementation to the package enough.
   *
   * A product module's own implementation lives outside this package and is registered by that
   * product's own spec.
   */
  private fun shippedArticleConditionsImplementations(): List<KClass<out ArticleConditions>> {
    val packageName = ArticleConditions::class.java.packageName
    val packageDirectories = javaClass.classLoader.getResources(packageName.replace('.', '/'))
      .toList()
      .filter { it.protocol == "file" }
      .map { File(it.toURI()) }

    require(packageDirectories.isNotEmpty()) { "Found no compiled directory for package <$packageName>" }

    return packageDirectories
      .flatMap { it.listFiles().orEmpty().asIterable() }
      .map { it.name }
      .filter { it.endsWith(".class") && it.contains('$').not() }
      .map { Class.forName("$packageName.${it.removeSuffix(".class")}") }
      .filter { ArticleConditions::class.java.isAssignableFrom(it) && it.isInterface.not() }
      .map { it.kotlin as KClass<out ArticleConditions> }
      .sortedBy { it.simpleName }
  }
}
