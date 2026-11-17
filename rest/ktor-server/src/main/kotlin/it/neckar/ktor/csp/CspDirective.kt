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
package it.neckar.ktor.csp

/**
 * A Content Security Policy directive — the kind of resource a list of [CspSource]s applies to.
 */
enum class CspDirective(
  /**
   * The token as it appears in the policy header.
   */
  val directiveName: String,
) {
  /**
   * Fallback for every fetch directive that is not set explicitly.
   */
  DefaultSrc("default-src"),

  /**
   * Where scripts may come from. Governs inline `<script>` blocks — those need [CspSource.Nonce].
   */
  ScriptSrc("script-src"),

  /**
   * Where stylesheets may come from. Governs inline `<style>` blocks — those need [CspSource.Nonce].
   */
  StyleSrc("style-src"),

  /**
   * Where images may come from. Inlined images need [CspSource.Data].
   */
  ImgSrc("img-src"),

  /**
   * Where fonts may come from.
   */
  FontSrc("font-src"),

  /**
   * Where `fetch` / `XMLHttpRequest` / WebSocket may connect to.
   */
  ConnectSrc("connect-src"),

  /**
   * Where `<object>` / `<embed>` may load plugin content from. Falls back to [DefaultSrc], which
   * makes `'self'` the effective value unless set — and same-origin plugin content can reach script
   * context, so the default is an empty source list ("nothing allowed").
   */
  ObjectSrc("object-src"),

  /**
   * Who may embed this page in a frame. Against clickjacking.
   */
  FrameAncestors("frame-ancestors"),

  /**
   * What the document may set its `<base href>` to. Against base-tag injection.
   */
  BaseUri("base-uri"),

  /**
   * Where forms may submit to.
   */
  FormAction("form-action"),
}
