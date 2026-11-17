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

import it.neckar.open.collections.fastForEach
import it.neckar.react.common.annotations.*
import js.array.ReadonlyArray
import js.objects.Object
import react.*
import react.dom.*
import react.router.*
import react.router.dom.*
import remix.run.router.generatePath


/**
 * Creates a breadcrumb bar
 */
val BreadcrumbBar: FC<BreadcrumbBarProps> = fc("breadcrumbBar") { props ->
  check(useInRouterContext()) {
    "Must only be used in router context"
  }

  val navigation = requireNotNull(props.navigation) { "attrs.navigation required" }
  val matchingNavigationElement = navigation.useFindBestRoute()

  val location = useLocation()
  checkNotNull(matchingNavigationElement) {
    "No navigation element found for ${location.pathname}"
  }

  //Render the breadcrumb
  nav {
    ol("breadcrumb") {

      BreadcrumbElements {
        attrs {
          this.navigationElement = matchingNavigationElement
        }
      }
    }
  }
}

external interface BreadcrumbBarProps : Props {
  var navigation: NavigationRoot?
}


/**
 * Renders the breadcrumb elements
 */
val BreadcrumbElements: FC<BreadcrumbElementsProps> = fc("breadcrumbElements") { props ->
  props.navigationElement.getChain(false).fastForEach { navigationElement ->
    BreadCrumbElement {
      attrs {
        this.navigationElement = navigationElement
      }
    }
  }
}

external interface BreadcrumbElementsProps : Props {
  var navigationElement: NavigationElement
}

/**
 * Creates a single breadcrumb element ([li] containing a [NavLink])
 */
val BreadCrumbElement: FC<BreadCrumbElementProps> = fc("breadCrumbElement") { props ->
  val navigationElement = requireNotNull(props.navigationElement) { "props.navigationElement required" }
  val breadcrumbInfo = navigationElement.breadcrumbInfo

  val linkContent = breadcrumbInfo?.linkContent ?: {
    span {
      //No breadcrumb info - we use the path fragment
      +navigationElement.pathFragment
    }
  }


  li("breadcrumb-item") {
    NavLink {
      linkContent()

      attrs {
        to = generatePath(navigationElement.completePath(), useParams())
        end = true
      }
    }
  }
}

external interface BreadCrumbElementProps : Props {
  var navigationElement: NavigationElement?
}


/**
 * Find the best navigation element for the current route.
 *
 * ATTENTION: We have to use filter + iterate over *all* routes to keep the number of hooks constant
 */
@UsesHooks
fun NavigationRoot.useFindBestRoute(): NavigationElement? {
  @Suppress("SimplifiableCallChain")
  val matches = useMatches()
  return matches.lastOrNull()?.let { match ->
    val params = buildMap {
      match.params.getOwnPropertyNames().fastForEach { param ->
        put(param, match.params[param] ?: throw NoSuchElementException("No parameter found for key $param"))
      }
    }

    this.allRoutes.lastOrNull { navigationElement ->
      routePathMatches(navigationElement.completePath(), match.pathname, params)
    }
  }
}

fun Any.asJsObject(): Object = unsafeCast<Object>()

fun Any.getOwnPropertyNames(): ReadonlyArray<String> =
  Object.getOwnPropertyNames(this)
