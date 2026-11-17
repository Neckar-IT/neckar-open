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

import it.neckar.react.common.form.*
import js.objects.Object
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.HTMLTag
import kotlinx.html.LABEL
import kotlinx.html.Tag
import kotlinx.html.classes
import react.*
import react.dom.*
import web.cssom.ClassName
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Shortcut for
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props) { key, value ->
 *         attrs[key] = value
 *     }
 * }
 * ```
 *
 * Allows to write `<MyComponent ...props/>` as
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props)
 * }
 * ```
 *
 * @param jsObject a JS object properties of which will be used
 */
fun <T : Tag> RDOMBuilder<T>.spread(jsObject: Any) {
  spread(jsObject) { key, value ->
    attrs[key] = value
  }
}

/**
 * Attempt to mimic `...` operator from ES6.
 * For example, equivalent of `<MyComponent ...props/>` would be
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props) { key, value ->
 *         attrs[key] = value
 *     }
 * }
 * ```
 *
 * @param jsObject a JS object which properties will be used
 * @param handler a handler for [jsObject]'s property names and values
 */
@Suppress("TYPE_ALIAS")
fun spread(jsObject: Any, handler: (key: String, value: Any) -> Unit) {
  Object.keys(jsObject).map {
    it to jsObject.asDynamic()[it] as Any
  }
    .forEach { (key, value) ->
      handler(key, value)
    }
}

/**
 * Returns the value
 */
val <T> StateInstance<T>.value: T
  get() {
    return this.component1()
  }

val <T> StateInstance<T>.setter: StateSetter<T>
  get() {
    return this.component2()
  }

/**
 * Writes the label target under the key React expects.
 *
 * kotlinx.html's [LABEL.htmlFor] stores the value under the raw HTML attribute name `for`, but React names that
 * prop `htmlFor`. The kotlinx.html property is therefore the wrong key here - this one writes `htmlFor` directly.
 *
 * Write only: the getter only exists because Kotlin requires one for a `var`, and is hidden for that reason.
 */
var RDOMBuilder<LABEL>.htmlForFixed: String
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return attrs["htmlFor"] as String
  }
  set(value) {
    attrs["htmlFor"] = value
  }


/**
 * Writes the label target under the key React expects - see the [RDOMBuilder] overload for the reason.
 */
var LABEL.htmlForFixed: String
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return attributes["htmlFor"] as String
  }
  set(value) {
    attributes["htmlFor"] = value
  }

/**
 * Sets the aria label
 */
var HTMLTag.ariaLabel: String
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return attributes["aria-label"] as String
  }
  set(value) {
    attributes["aria-label"] = value
  }

var HTMLTag.ariaValueRange: IntRange
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return throw UnsupportedOperationException("not supported")
  }
  set(value) {
    attributes["aria-valuemin"] = value.start.toString()
    attributes["aria-valuemax"] = value.endInclusive.toString()
  }

var HTMLTag.ariaValueNow: Int
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return throw UnsupportedOperationException("not supported")
  }
  set(value) {
    attributes["aria-valuenow"] = value.toString()
  }

var HTMLTag.roleHTML: String
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return throw UnsupportedOperationException("not supported")
  }
  set(value) {
    attributes["role"] = value
  }

/**
 * Sets the "aria-describedby" attribute
 */
var HTMLTag.ariaDescribedBy: String
  @Deprecated("write only", level = DeprecationLevel.HIDDEN)
  get() {
    return attributes["aria-describedby"] as String
  }
  set(value) {
    attributes["aria-describedby"] = value
  }

/**
 * Appends a new class
 */
fun CommonAttributeGroupFacade.addClass(newClass: String) {
  classes = classes + newClass
}

/**
 * Removes an old class
 */
fun CommonAttributeGroupFacade.removeClass(oldClass: String) {
  classes = classes - oldClass
}

/**
 * Adds the classes - separated by space
 */
fun CommonAttributeGroupFacade.addClasses(newClasses: String) {
  classes = classes + newClasses.split(' ')
}


/**
 * Converts a setter to an onChange listener.
 *
 * Unparsable input is swallowed - the state keeps its old value, so the field can be typed in freely
 * (`-`, `1e`) without the state jumping around.
 *
 * An emptied field falls back to `0`, because this state can not hold "no value at all". Use the
 * [StateInstance]<[Int]?> overload where an empty field must stay empty.
 */
fun StateInstance<Int>.asOnChangeForInt(numberConstraint: NumberConstraint): (String) -> Unit {
  return {
    if (it.isEmpty().not()) {
      it.toIntOrNull()?.let { parsedInt ->
        setter(numberConstraint.constraint(parsedInt))
      }
    } else {
      setter(0)
    }
  }
}

/**
 * Converts a setter to an onChange listener.
 *
 * Unparsable input is swallowed - the state keeps its old value. An empty or `null` input sets the state to `null`.
 */
fun StateInstance<Int?>.asOnChangeForInt(numberConstraint: NumberConstraint): (String?) -> Unit {
  return {
    if (it.isNullOrEmpty()) {
      setter(null)
    } else {
      it.toIntOrNull()?.let { parsedInt ->
        setter(numberConstraint.constraint(parsedInt))
      }
    }
  }
}

/**
 * Converts a setter to an onChange listener.
 *
 * Unparsable input is swallowed - the state keeps its old value, so the field can be typed in freely
 * (`-`, `1.`, `1e`) without the state jumping around.
 *
 * An emptied field falls back to `0.0`, because this state can not hold "no value at all". Use the
 * [StateInstance]<[Double]?> overload where an empty field must stay empty.
 */
fun StateInstance<Double>.asOnChangeForDouble(numberConstraint: NumberConstraint): (String) -> Unit {
  return {
    if (it.isEmpty().not()) {
      it.toDoubleOrNull()?.let { parsedDouble ->
        setter(numberConstraint.constraint(parsedDouble))
      }
    } else {
      setter(0.0)
    }
  }
}

/**
 * Converts a setter to an onChange listener.
 *
 * Unparsable input is swallowed - the state keeps its old value. An empty or `null` input sets the state to `null`.
 */
fun StateInstance<Double?>.asOnChangeForDouble(numberConstraint: NumberConstraint): (String?) -> Unit {
  return {
    if (it.isNullOrEmpty()) {
      setter(null)
    } else {
      it.toDoubleOrNull()?.let { parsedDouble ->
        setter(numberConstraint.constraint(parsedDouble))
      }
    }
  }
}


/**
 * Converts a setter to an onChange listener.
 *
 * An empty input is ignored - the state keeps its old value. Every other input goes through [String.toBoolean],
 * so only `"true"` (ignoring case) sets `true`; anything else sets `false`.
 */
fun StateInstance<Boolean>.asOnChangeForBoolean(): (String) -> Unit {
  return {
    if (it.isEmpty().not()) {
      setter(it.toBoolean())
    }
  }
}

/**
 * Diagnostic helper: warns on the console when a component is re-rendered with a different callback instance than
 * before - the usual cause of a child re-rendering although nothing it displays has changed.
 *
 * It only reports, it never changes the callback that is used.
 *
 * Currently disabled: the body returns before the comparison, so calls are a no-op. Remove the early return to
 * switch the warnings back on.
 *
 * Must only be called from a functional component
 */
fun ensureSameCallback(currentCallback: Any, contextInformation: String) {
  val refToLastCallback = useRef<Any>()

  //Disable for now
  if (true) {
    return
  }

  val last = refToLastCallback.current
  if (last != null) {
    if (last != currentCallback) {
      console.warn("Callback change detected! in <$contextInformation>")
      console.warn("Was: ")
      console.dir(last)
      console.warn("But now:")
      console.dir(currentCallback)
    }
  }

  //Assign the current value
  refToLastCallback.current = currentCallback
}

/**
 * Adds the class `invalid` if [invalidCheck] returns true.
 *
 * `invalid` is the theme's own class (it colours the text red), *not* Bootstrap's form validation class `is-invalid`.
 * It therefore works on any element, not just on form controls inside a validated form. Use [validationStyle] where
 * the Bootstrap classes are wanted.
 */
inline fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.invalidIf(invalidCheck: () -> Boolean) {
  contract {
    callsInPlace(invalidCheck, InvocationKind.EXACTLY_ONCE)
  }

  addClassIf("invalid", invalidCheck)
}

/**
 * Adds Bootstrap's `is-valid` class if [invalidCheck] returns true.
 *
 * The parameter name is a leftover from [invalidIf]: the lambda must return true when the value is *valid*.
 *
 * Note the asymmetry to [invalidIf], which adds the theme's `invalid` rather than Bootstrap's `is-invalid`.
 */
inline fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.validIf(invalidCheck: () -> Boolean) {
  contract {
    callsInPlace(invalidCheck, InvocationKind.EXACTLY_ONCE)
  }

  addClassIf("is-valid", invalidCheck)
}

/**
 * Sets the bootstrap validation related classes:
 * * [ValidationState.Valid] -> `is-valid`
 * * [ValidationState.Invalid] -> `is-invalid`
 * * [ValidationState.NotValidated] -> no class at all, so an untouched field looks neutral instead of wrong
 */
inline fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.validationStyle(validationState: () -> ValidationState) {
  contract {
    callsInPlace(validationState, InvocationKind.EXACTLY_ONCE)
  }

  attrs {
    when (validationState()) {
      ValidationState.Valid -> addClass("is-valid")
      ValidationState.Invalid -> addClass("is-invalid")
      else -> {}
    }
  }
}

/**
 * Adds the given class
 */
fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.addClass(className: String?) {
  attrs {
    className?.let { this.addClass(it) }
  }
}

/**
 * Adds the given class if the given check returns true
 */
inline fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.addClassIf(className: String, check: () -> Boolean?) {
  contract {
    callsInPlace(check, InvocationKind.EXACTLY_ONCE)
  }

  addClassIf(className, check())
}

fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.addClassIf(className: String, addClass: Boolean?) {
  if (addClass == true) {
    addClass(className)
  }
}

/**
 * Removes the given class if the given check returns true
 */
inline fun <T : CommonAttributeGroupFacade> RDOMBuilder<T>.removeClassIf(className: String, check: () -> Boolean?) {
  if (check() == true) {
    attrs {
      removeClass(className)
    }
  }
}


/**
 * Returns this if in the list of available options, else returns the first element of the list.
 *
 * This can be used to automatically update a value to the first entry of a select list
 *
 * Despite the `OrNull` suffix the result is never null: a value that is missing from [available] is replaced by the
 * first option rather than dropped.
 *
 * @throws NoSuchElementException if [available] is empty and there is no first option to fall back to. Callers must
 * not render a select without options.
 */
fun <T> T?.orFirstIfNotInListOrNull(available: List<T>): T {
  if (this != null && available.contains(this)) {
    return this
  }

  return available.first()
}


/**
 * Must only be called inside a [fc].
 *
 * Checks the dependencies using *==*.
 * Attention. This method *might* be slow(er)
 *
 * This is the reason it exists: React's own `useMemo` compares its dependencies by identity, so a dependency that is
 * recreated on every render - a data class rebuilt from props, a freshly mapped list - never hits the cache. Here
 * such a dependency does hit the cache, at the price of calling [equals] on every dependency of every render.
 *
 * The cache holds exactly one entry per component instance: a value alternating between two sets of dependencies is
 * recomputed every time.
 */
inline fun <T : Any> useMemoCompare(
  vararg dependencies: Any,
  noinline callback: () -> T,
): T {

  /**
   * Reference to the previous value
   */
  val previousDepsRef = useRef<Array<*>>()

  /**
   * Reference to the previous value
   */
  val previousValueRef = useRef<T>()

  val previousDeps = previousDepsRef.current
  val previousValue = previousValueRef.current


  if (previousValue != null && previousDeps.contentEquals(dependencies)) {
    //previous value should be fine
    return previousValue
  }

  //previous value has to be recalculated, since the deps have been updated
  return callback().also { updatedValue ->
    previousDepsRef.current = dependencies
    previousValueRef.current = updatedValue
  }
}

enum class ValidationState {
  /**
   * Has not been validated (e.g. is empty)
   */
  NotValidated,
  Valid,
  Invalid,
}


/**
 * adds the class
 */
fun PropsWithClassName.addClass(newClass: String) {
  className = className?.let {
    ClassName("$it $newClass")
  } ?: ClassName(newClass)
}
