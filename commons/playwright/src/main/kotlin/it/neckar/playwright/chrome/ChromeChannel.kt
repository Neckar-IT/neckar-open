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
package it.neckar.playwright.chrome

/**
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * The chrome channel to use for playwright tests.
 */
enum class ChromeChannel(
  /**
   * The channel name that is used to start the browser
   */
  val channelName: String,
  /**
   * The major version prefix that is expected for this channel
   */
  @Deprecated("Do not check for version numbers anymore - they change too often")
  val majorVersion: Int
) {
  /**
   * Uses the bundled chrome provided by playwright itself
   * The channel name was set to 'chromium' to use the new chrome headless: https://playwright.dev/java/docs/release-notes#version-149
   */
  ChromeBundled("chromium", 140),

  /**
   * Uses the preinstalled chrome stable
   */
  ChromeStable("chrome", 142),

  /**
   * Uses the preinstalled Chrome beta
   */
  ChromeBeta("chrome-beta", 142),

  /**
   * Uses the preinstalled Chrome dev
   */
  @Deprecated("Currently not used")
  ChromeDev("chrome-dev", 128),
  ;
}
