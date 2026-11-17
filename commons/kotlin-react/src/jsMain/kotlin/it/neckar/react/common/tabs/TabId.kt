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
package it.neckar.react.common.tabs

import it.neckar.react.common.*
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.html.role
import react.*
import react.dom.*

/**
 * Represents the ID for a tab
 */
@Deprecated("Use react router tabs instead")
value class TabId(val value: String) {
  init {
    require(value.isNotBlank()) { "Invalid value <$value>" }
    require(value.first().isLetter()) { "Value must start with letter <$value>" }
  }

  override fun toString(): String {
    return value
  }
}

/**
 * Creates a tab button
 */
@Deprecated("Use react router tabs instead")
fun RBuilder.tabButton(name: String, id: TabId, active: Boolean = false) {
  li("nav-item") {
    attrs {
      role = "presentation"
    }
    button(classes = "nav-link", type = ButtonType.button) {
      attrs {
        this.id = "${id}Tab"
        role = "tab"
        if (active) addClass("active")
      }
      attrs["data-bs-toggle"] = "tab"
      attrs["data-bs-target"] = "#$id"
      attrs["aria-controls"] = id
      if (active) attrs["aria-selected"] = "true"

      +name
    }
  }
}

/**
 * Creates the tab content (for a tab button) - to be used with JavaScript
 */
@Deprecated("Use react router tabs instead")
fun RBuilder.tabContent(id: TabId, active: Boolean = false, content: RDOMBuilder<DIV>.() -> Unit) {
  div("tab-pane fade") {
    attrs {
      this.id = id.value
      role = "tabpanel"
      attrs["aria-labelledby"] = "$id-tab"
      if (active) addClass("show active")
    }

    content(this@div)
  }
}

@Deprecated("Use react router tabs instead")
fun RBuilder.tabContent(active: Boolean = false, content: RDOMBuilder<DIV>.() -> Unit) {
  div("tab-pane fade") {
    attrs {
      role = "tabpanel"
      if (active) addClass("show active") //TODO necessary(?)
    }

    content(this@div)
  }
}
