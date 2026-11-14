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
package it.neckar.react.common

import it.neckar.commons.kotlin.js.debug
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import js.objects.jso
import react.*
import react.dom.*

private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.ErrorBoundary")

/**
 * Provides the error component for a given throwable and error info (which is usually null)
 */
typealias ErrorComponentProvider = (Throwable?, ErrorInfo?) -> ReactNode


external interface ErrorBoundaryState : State {
  /**
   * Is set to true if an error has been detected
   */
  var hasError: Boolean?

  /**
   * The throwable that has been caught (updated by react magic in RStatics)
   */
  var throwable: Throwable?

  /**
   * The error info that has been caught (updated by react magic in RStatics)
   */
  var errorInfo: ErrorInfo?
}

external interface ErrorBoundaryProps : PropsWithChildren {
  var fallbackComponent: ErrorComponentProvider
}

/**
 * A React error boundary (https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary).
 *
 * ## Two workarounds for kotlin-react-legacy on ES2015
 *
 * Under ES2015 Kotlin/JS codegen, class- and companion-init are emitted into static factory methods
 * (`new_..._ErrorBoundary_..._k$`) rather than the native JS `constructor`. React creates components
 * via `new ErrorBoundary(props)` directly and bypasses those factories — so neither `RComponent`'s
 * `state = jso { init() }` nor the companion's `init { getDerivedStateFromError = ... }` block runs
 * on React's render path. Two consequences and the two corresponding workarounds in this file:
 *
 * 1. **`this.state` stays `undefined` on the initial render**, despite being typed `S` (non-null) by
 *    [react.Component]. [render] reads it via [unsafeCast] to a nullable view and treats absence as
 *    "no error captured yet".
 *
 * 2. **The companion's `RStatics`-wiring never fires automatically.** [errorBoundary] touches
 *    [ErrorBoundary.Companion] before `child(...)`, which loads the companion via Kotlin's standard
 *    lazy-singleton path and runs its `init` block — wiring `getDerivedStateFromError` onto the JS
 *    class through `RStatics`'s property delegation (`by unsafeCast(klazz.js)`). Without this touch
 *    React warns "Error boundaries should implement getDerivedStateFromError()" and the boundary
 *    stays empty.
 *
 * A cleaner fix would drop kotlin-react-legacy here in favour of the `react-error-boundary` NPM
 * package or a direct static-method assignment onto the JS class. Not tracked as a follow-up:
 * kotlin-react is no longer strategically used — frontend work happens in React/TypeScript.
 *
 * workaround: dependency=libs:org-jetbrains-kotlin-wrappers:2025.1.6 observed-on=2026-10-07
 */
class ErrorBoundary : RComponent<ErrorBoundaryProps, ErrorBoundaryState>() {
  companion object : RStatics<ErrorBoundaryProps, ErrorBoundaryState, ErrorBoundary, Nothing>(ErrorBoundary::class) {
    init {
      getDerivedStateFromError = {
        jso {
          hasError = true
          throwable = it
          errorInfo = null
        }
      }
    }
  }

  override fun componentDidCatch(error: Throwable, info: ErrorInfo) {
    //noop
  }

  override fun RBuilder.render() {
    //`state` is typed non-null but may be `undefined` at runtime under ES2015 codegen — see class KDoc.
    val state: ErrorBoundaryState? = state.unsafeCast<ErrorBoundaryState?>()

    if (state?.hasError == true) {
      props.fallbackComponent.invoke(state.throwable, state.errorInfo).let {
        child(it)
      }
    } else {
      props.children()
    }
  }
}

/**
 * Mounts an [ErrorBoundary] around [handler].
 *
 * Touches [ErrorBoundary.Companion] before `child(...)` to force companion-object init under ES2015
 * codegen — see [ErrorBoundary] class KDoc.
 */
fun RBuilder.errorBoundary(
  errorComponentComponent: ErrorComponentProvider = { throwable, errorInfo ->
    ErrorFallbackComponent.create {
      this.throwable = throwable
      this.errorInfo = errorInfo
    }
  },
  handler: RHandler<Props>,
): Unit {
  ErrorBoundary.Companion

  child(ErrorBoundary::class) {
    attrs {
      this.fallbackComponent = errorComponentComponent
    }
    handler()
  }
}

/**
 * Very simple component that visualizes an error. Usually a (visually) better component should be provided
 */
val ErrorFallbackComponent: FC<ErrorFallbackComponentProps> = fc("ErrorFallbackComponent") { props ->
  val throwable: Throwable? = props.throwable
  val errorInfo: ErrorInfo? = props.errorInfo

  div {
    h3 {
      +"Error detected: ${throwable?.message}"
    }
    throwable?.let {
      p {
        +"Throwable class: ${it::class}"
      }
    }
    throwable?.let {
      p {
        +"Throwable message: ${it.message}"
      }
    }
    errorInfo?.let {
      p {
        +"Error Info component stack: ${it.componentStack}"
      }
    }
  }
}

external interface ErrorFallbackComponentProps : Props {
  var throwable: Throwable?
  var errorInfo: ErrorInfo?
}
