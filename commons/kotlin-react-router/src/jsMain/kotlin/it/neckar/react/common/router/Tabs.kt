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
package it.neckar.react.common.router

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.open.collections.fastForEach
import it.neckar.react.common.*
import it.neckar.react.common.router.TabButtonStatus.*
import kotlinx.html.UL
import kotlinx.html.role
import react.*
import react.dom.*
import react.dom.aria.*
import react.router.dom.*

enum class TabButtonStatus {
  Normal,
  Disabled,
}

fun Boolean.toTabButtonStatus(): TabButtonStatus {
  return if (this) Normal else Disabled
}

/**
 * The tab pane should be used as container for [tabButton] calls
 */
fun RBuilder.tabButtonsPane(block: RDOMBuilder<UL>.() -> Unit) {
  ul("nav nav-tabs") {
    block()
  }
}

/**
 * Creates a tab button - using react router.
 * Should be placed within a [tabButtonsPane].
 */
fun RBuilder.tabButton(url: NavigateUrl, name: String, tabButtonStatus: TabButtonStatus = Normal, end: Boolean = true) {
  li("nav-item") {
    attrs {
      role = "presentation"
    }

    NavLink {
      +name

      attrs {
        addClass("nav-link")
        toUrl = url

        this.end = end
        role = AriaRole.tab

        if (tabButtonStatus == Disabled) {
          addClass("disabled")
        }
      }
    }
  }
}


/**
 * Creates Router Tabs
 */
fun RBuilder.routerTabs(withTabBar: Boolean = true, block: RouterTabsConfig.Builder.() -> Unit) {
  val config = RouterTabsConfig.Builder()
    .also(block)
    .build(withTabBar)

  TabRoutes {
    attrs {
      this.routerTabsConfig = config
      this.builder = this@routerTabs
    }
  }
}

/**
 * Contains the configuration for Router tabs
 */
class RouterTabsConfig(
  val tabs: List<RouterTabConfig>,
  val withTabBar: Boolean,
) {
  class Builder {
    val tabs: MutableList<RouterTabConfig> = mutableListOf()

    /**
     * Creates a new tab
     */
    fun tab(
      path: NavigateUrl,
      tabTitle: String,
      disabled: TabButtonStatus = Normal,
      element: (() -> ReactElement<out Props>),
    ) {
      this.tabs.add(RouterTabConfig(tabTitle, path, disabled, element))
    }

    fun build(withTabBar: Boolean): RouterTabsConfig {
      return RouterTabsConfig(
        tabs = tabs.toList(),
        withTabBar = withTabBar,
      )
    }
  }
}

/**
 * Configuration for a router tab
 */
class RouterTabConfig(
  /**
   * The title for the tab - visible to the user
   */
  val tabTitle: String,
  /**
   * The navigation path (usually relative) that is used to activate the tab
   */
  val path: NavigateUrl,
  /**
   * If the tab button is disabled
   */
  val disabled: TabButtonStatus,
  /**
   * Provides the content of the tab
   */
  val tabContent: (() -> ReactElement<out Props>),
)


val TabRoutes: FC<TabRoutesProps> = FC { props ->
  val builder = props::builder.safeGet()
  val routerTabsConfig = props::routerTabsConfig.safeGet()

  with(builder) {
    if (routerTabsConfig.withTabBar) {
      tabButtonsPane {
        routerTabsConfig.tabs.fastForEach {
          tabButton(it.path, it.tabTitle, it.disabled)
        }
      }
    }
  }
}

external interface TabRoutesProps : Props {
  var routerTabsConfig: RouterTabsConfig
  var builder: RBuilder
}
