/*
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
package com.meistercharts.canvas

/**
 * DSL marker for the **Configuration** of a layer or gestalt.
 *
 * Config-object naming convention in meistercharts:
 * - Layer / Gestalt      -> nested `Configuration` + [ConfigurationDsl] — full surface (data, behaviour, appearance)
 * - Painter / Paintable  -> nested `Style`         + [StyleDsl]         — purely visual (colors, widths, fonts, gaps)
 *
 * A `Configuration` covers everything a layer/gestalt needs (data source, behaviour, appearance).
 * A painter's purely visual appearance uses [StyleDsl] instead.
 */
@DslMarker
annotation class ConfigurationDsl
