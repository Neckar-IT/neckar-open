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
package it.neckar.rest

import it.neckar.open.http.Url
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPattern
import it.neckar.uuid.HasUuid
import it.neckar.uuid.resolve

/**
 * The standard URL patterns of an entity collection; an `<Entity>Endpoints` object extends it and declares its extra paths.
 * `("authors", "authorId")` yields `api/authors`, `…/views/table-entries`, `…/new`, `api/authors/{authorId}` and below it `…/preview`, `…/archive`, `…/unarchive`.
 */
abstract class StandardEntityEndpoints<Id : HasUuid>(collectionSegment: String, idParameterName: String) {
  val IdParameter: UrlParameterName = UrlParameterName(idParameterName)

  val BasePath: UrlPattern.Relative0 = Url.pattern("api") + collectionSegment
  val TableEntries: UrlPattern.Relative0 = BasePath + "views" + "table-entries"
  val New: UrlPattern.Relative0 = BasePath + "new"
  val ById: UrlPattern.Relative1 = BasePath + IdParameter
  val Preview: UrlPattern.Relative1 = ById + "preview"
  val Archive: UrlPattern.Relative1 = ById + "archive"
  val Unarchive: UrlPattern.Relative1 = ById + "unarchive"

  fun basePathUrl(): Url.Relative = BasePath.resolve()
  fun tableEntriesUrl(): Url.Relative = TableEntries.resolve()
  fun newUrl(): Url.Relative = New.resolve()
  fun byIdUrl(id: Id): Url.Relative = ById.resolve(id)
  fun previewUrl(id: Id): Url.Relative = Preview.resolve(id)
  fun archiveUrl(id: Id): Url.Relative = Archive.resolve(id)
  fun unarchiveUrl(id: Id): Url.Relative = Unarchive.resolve(id)
}
