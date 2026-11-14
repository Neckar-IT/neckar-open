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

import it.neckar.events.KeyStroke
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.collections.fastForEach
import it.neckar.react.common.router.key.*
import it.neckar.runtime.context.RuntimeContext
import js.objects.jso
import react.*
import react.dom.*
import react.router.*
import react.router.dom.*
import remix.run.router.Router

/**
 * Represents a navigation element
 */
@NavigationDsl
data class NavigationElement(
  /**
   * The path as it is used in the route.
   *
   * Attention: Contains query parameters
   */
  val pathFragment: String,

  /**
   * Provides the element
   */
  val element: () -> ReactElement<out Props>,

  /**
   * Reference to the parent
   */
  val parent: NavigationElement?,

  val index: Boolean?,

  /**
   * The (optional) keystroke for this navigation element
   */
  val keyStrokes: Set<KeyStroke> = emptySet(),

  /**
   * Contains the information about the breadcrumb - if there is any
   */
  val breadcrumbInfo: BreadcrumbInfo? = null,
) {

  /**
   * The routes are later set - as soon as they are build
   */
  lateinit var routes: List<NavigationElement>

  /**
   * Returns all routes recursively
   */
  fun routesRecursive(): List<NavigationElement> {
    return buildList {
      routes.fastForEach {
        add(it)
        addAll(it.routesRecursive())
      }
    }
  }


  /**
   * Returns the complete chain.
   * This is the *last* element within the returned list
   */
  fun getChain(stopAtAbsoluteElements: Boolean): List<NavigationElement> {
    return buildList {
      var currentElement: NavigationElement? = this@NavigationElement
      while (currentElement != null) {
        add(0, currentElement)

        if (stopAtAbsoluteElements && currentElement.isAbsolute()) {
          //Return immediately if this is an absolute URL
          return@buildList
        }
        currentElement = currentElement.parent
      }
    }
  }

  /**
   * Returns true if this element is absolute
   */
  fun isAbsolute(): Boolean {
    return pathFragment.startsWith("/")
  }

  fun completePath(): String {
    val chain = getChain(true)
    return chain.joinToString(separator = "/") { it.pathFragment }
  }


  @NavigationDsl
  class Builder(
    /**
     * The path fragment as it is used in the route
     */
    var pathFragment: String,
  ) {
    /**
     * Provides the element
     */
    var element: () -> ReactElement<out Props> = { Outlet.create() }

    var index: Boolean? = null

    /**
     * The routes - which may contain further notes
     */
    val routes: MutableList<Builder> = mutableListOf()

    var breadcrumbInfo: BreadcrumbInfo? = null

    /**
     * Sets an additional keystroke.
     * This var can be set multiple times
     */
    var keyStroke: KeyStroke
      @Deprecated("Only set supported", level = DeprecationLevel.ERROR)
      get() {
        throw UnsupportedOperationException("Only set supported")
      }
      set(value) {
        keyStrokes.add(value)
      }

    var keyStrokes: MutableSet<KeyStroke> = mutableSetOf()

    /**
     * Can be used to overwrite the parent URL.
     * This can be used when parallel routes are used to solve issues related to Outlets.
     *
     * The parent URL is then used to identify the parent for the breadcrumb url.
     *
     * ATTENTION: The order matters. The parent must be created *before* it is referenced.
     */
    var parentUrl: String? = null

    /**
     * Registers the route
     */
    @IgnorableReturnValue
    @NavigationDsl
    fun route(variable: RouterVar, handler: Builder.() -> Unit): Builder {
      return route(variable.toPathString(), handler)
    }

    @IgnorableReturnValue
    @NavigationDsl
    fun route(pathFragment: String, handler: Builder.() -> Unit): Builder {
      return Builder(pathFragment)
        .apply(handler)
        .also {
          routes.add(it)
        }
    }

    @IgnorableReturnValue
    @NavigationDsl
    fun index(handler: Builder.() -> Unit): Builder {
      return Builder("")
        .apply(handler)
        .also {
          it.index = true
          routes.add(it)
        }
    }

    @NavigationDsl
    fun breadcrumb(handler: BreadcrumbInfo.Builder.() -> Unit) {
      this.breadcrumbInfo = BreadcrumbInfo.Builder()
        .also(handler)
        .build()
    }

    /**
     * Creates a new navigation element - does *not* build the children
     */
    fun build(parent: NavigationElement?, context: NavigationBuildingContext): NavigationElement {
      if (parentUrl != null) {
        require(pathFragment.startsWith("/")) { "Use absolute URLs if the parent URL is configured" }
      }

      //Resolve the parent if necessary
      val parentToUse: NavigationElement? = this.parentUrl?.let {
        context.findElement(it)
      } ?: parent

      return NavigationElement(
        pathFragment = pathFragment,
        element = element,
        parent = parentToUse,
        index = index,
        keyStrokes = keyStrokes,
        breadcrumbInfo = breadcrumbInfo,
      )
    }

    fun buildRecursively(parent: NavigationElement?, context: NavigationBuildingContext): NavigationElement {
      val thisElement = build(parent, context)
      context.store(thisElement)

      val children = routes.map {
        it.buildRecursively(thisElement, context)
      }

      thisElement.routes = children
      return thisElement
    }
  }
}

/**
 * Builds the navigation
 */
@NavigationDsl
fun buildNavigation(block: NavigationRoot.Builder.() -> Unit): NavigationRoot {
  val builder = NavigationRoot.Builder()
    .apply(block)

  return builder.buildRecursively(NavigationBuildingContext())
}

/**
 * Helper class that is used to build the navigation.
 *
 * This object is used to resolve parents
 */
class NavigationBuildingContext {
  private val url2element = mutableMapOf<String, NavigationElement>()

  fun store(element: NavigationElement) {
    url2element[element.completePath()] = element
  }

  /**
   * Returns the navigation element for the given URL
   */
  fun findElement(url: String): NavigationElement {
    return url2element[url] ?: findElementIncludingStar(url) ?: throw IllegalArgumentException("No element found for <$url> in NavigationBuildingContext with url2element pathFragments:\n${url2element.keys}")
  }

  private fun findElementIncludingStar(url: String): NavigationElement? {
    val cleanUrl = url.substring(0, url.lastIndexOf('/'))
    val genericUrl = "$cleanUrl/*"
    return url2element[genericUrl] ?: url2element[cleanUrl]
  }
}

@NavigationDsl
data class NavigationRoot(
  /**
   * The (direct) routes - which may contain further routes
   */
  val directChildren: List<NavigationElement>,
  /**
   * Contains all routes - flattened list
   */
  val allRoutes: List<NavigationElement> = buildList {
    directChildren.fastForEach {
      add(it)
      addAll(it.routesRecursive())
    }
  },
) {
  class Builder {
    /**
     * The routes - which may contain further notes
     */
    val children: MutableList<NavigationElement.Builder> = mutableListOf()

    /**
     * Registers the route
     */
    @IgnorableReturnValue
    @NavigationDsl
    fun route(pathFragment: String, handler: NavigationElement.Builder.() -> Unit): NavigationElement.Builder {
      check(pathFragment != "/") {
        "Instead of a single / use empty string"
      }

      return NavigationElement.Builder(pathFragment)
        .apply(handler)
        .also {
          children.add(it)
        }
    }

    @NavigationDsl
    fun buildRecursively(context: NavigationBuildingContext): NavigationRoot {
      val children = children.map {
        it.buildRecursively(null, context)
      }

      return NavigationRoot(children)
    }
  }
}


@DslMarker
annotation class NavigationDsl {
}


/**
 * Generates the router
 */
fun NavigationRoot.generateRouter(): Router {
  return createBrowserRouter(
    directChildren.map {
      generateRouteRecursively(it)
    }.toTypedArray()
  ).also {
    if (RuntimeContext.deploymentStage.isDev()) {
      val allRoutes = allRoutes
      console.log("---------- Added ${allRoutes.size} routes ---------")
      allRoutes.fastForEach {
        console.log(it.completePath())
      }
    }
  }
}

private fun NavigationRoot.generateRouteRecursively(navigationElement: NavigationElement): RouteObject {
  return jso {
    path = navigationElement.pathFragment
    element = navigationElement.element()

    navigationElement.index?.let {
      index = it
    }

    //Recursive calls
    navigationElement.routes.map {
      this@generateRouteRecursively.generateRouteRecursively(it)
    }.let {
      if (it.isNotEmpty()) children = it.toTypedArray()
    }
  }
}

/**
 * Generates a key handler for the navigation
 */
fun NavigationRoot.generateKeyHandler(): NavigationKeyStrokeHandler {
  logger.debug("generateKeyHandler")

  return NavigationKeyStrokeHandler().also { navigationKeyHandler ->

    //Contains all elements that must be registered
    val remaining = this.directChildren.toMutableList()

    while (remaining.isNotEmpty()) {
      val current = remaining.removeFirst()

      current.keyStrokes.forEach { keyStroke ->
        logger.trace("Trying to register key stroke [${keyStroke}] for navigation element [${current.completePath()}]. Remaining: ${remaining.size}")
        navigationKeyHandler.registerKeyStroke(current, keyStroke)
      }

      //add all children
      remaining.addAll(current.routes)
    }
  }
}

private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.router.Navigation")

/**
 * The breadcrumb info for this navigation element
 */
data class BreadcrumbInfo(val linkContent: RBuilder.() -> Unit) {
  @NavigationDsl
  class Builder {
    /**
     * If the link content is set directly, [label] and [icon] are ignored!
     *
     * Attention: To avoid hook problems, do *not* use hooks directly in the provided lambda.
     * Instead, create new functional components that call hooks.
     */
    var linkContent: (RBuilder.() -> Unit)? = null

    /**
     * This value is only used if [linkContent] is *not* set
     */
    var label: String? = null

    /**
     * This value is only used if [linkContent] is *not* set
     */
    var icon: String? = null

    fun build(): BreadcrumbInfo {
      return BreadcrumbInfo(
        createLinkContent(),
      )
    }

    private fun createLinkContent(): RBuilder.() -> Unit {
      this.linkContent?.let {
        //Is configured manually
        return it
      }

      //Fallback that uses the [label] and [icon]
      require(label != null || icon != null) {
        "If no link content is configured, a label and/or an icon must be set"
      }

      return {
        LinkContentComponent {
          icon?.let {
            i(classes = "$it px-1") {}
          }

          label?.let {
            +it
          }
        }
      }
    }
  }
}

val LinkContentComponent: FC<PropsWithChildren> = fc("linkContentComponent") { props ->
  props.children()
}
