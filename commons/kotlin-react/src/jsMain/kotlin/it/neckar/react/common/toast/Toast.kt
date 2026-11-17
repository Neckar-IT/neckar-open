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
package it.neckar.react.common.toast

import it.neckar.open.obj

/**
 * Toast message types.
 */
internal enum class ToastType(internal val type: String) {
  INFO("info"),
  SUCCESS("success"),
  WARNING("warning"),
  ERROR("error")
}

/**
 * Toast positions.
 */
enum class ToastPosition(internal val position: String) {
  TOPRIGHT("toast-top-right"),
  BOTTOMRIGHT("toast-bottom-right"),
  BOTTOMLEFT("toast-bottom-left"),
  TOPLEFT("toast-top-left"),
  TOPFULLWIDTH("toast-top-full-width"),
  BOTTOMFULLWIDTH("toast-bottom-full-width"),
  TOPCENTER("toast-top-center"),
  BOTTOMCENTER("toast-bottom-center")
}

/**
 * Toast easings.
 */
enum class ToastEasing(internal val easing: String) {
  SWING("swing"),
  LINEAR("linear")
}

/**
 * Toast animation methods.
 */
enum class ToastMethod(internal val method: String) {
  FADEIN("fadeIn"),
  FADEOUT("fadeOut"),
  SLIDEUP("slideUp"),
  SLIDEDOWN("slideDown"),
  SHOW("show"),
  HIDE("hide")
}

/**
 * Toast options.
 */
data class ToastOptions(
  val positionClass: ToastPosition? = null,
  val escapeHtml: Boolean? = null,
  val closeButton: Boolean? = null,
  val closeHtml: String? = null,
  val closeDuration: Int? = null,
  val newestOnTop: Boolean? = null,
  val showEasing: ToastEasing? = null,
  val hideEasing: ToastEasing? = null,
  val closeEasing: ToastEasing? = null,
  val showMethod: ToastMethod? = null,
  val hideMethod: ToastMethod? = null,
  val closeMethod: ToastMethod? = null,
  val preventDuplicates: Boolean? = null,
  val timeOut: Int? = null,
  val extendedTimeOut: Int? = null,
  val progressBar: Boolean? = null,
  val rtl: Boolean? = null,
  val onShown: (() -> Unit)? = null,
  val onHidden: (() -> Unit)? = null,
  val onClick: (() -> Unit)? = null,
  val onCloseClick: (() -> Unit)? = null,
)

internal fun ToastOptions.toJs(): dynamic {
  return obj {
    if (positionClass != null) this.positionClass = positionClass.position
    if (escapeHtml != null) this.escapeHtml = escapeHtml
    if (closeButton != null) this.closeButton = closeButton
    if (closeHtml != null) this.closeHtml = closeHtml
    if (closeDuration != null) this.closeDuration = closeDuration
    if (newestOnTop != null) this.newestOnTop = newestOnTop
    if (showEasing != null) this.showEasing = showEasing.easing
    if (hideEasing != null) this.hideEasing = hideEasing.easing
    if (closeEasing != null) this.closeEasing = closeEasing.easing
    if (showMethod != null) this.showMethod = showMethod.method
    if (hideMethod != null) this.hideMethod = hideMethod.method
    if (closeMethod != null) this.closeMethod = closeMethod.method
    if (preventDuplicates != null) this.preventDuplicates = preventDuplicates
    if (timeOut != null) this.timeOut = timeOut
    if (extendedTimeOut != null) this.extendedTimeOut = extendedTimeOut
    if (progressBar != null) this.progressBar = progressBar
    if (rtl != null) this.rtl = rtl
    if (onShown != null) this.onShown = onShown
    if (onHidden != null) this.onHidden = onHidden
    if (onClick != null) this.onclick = onClick
    if (onCloseClick != null) this.onCloseClick = onCloseClick
  }
}

/**
 * Toast component object.
 */
object Toast {
  init {
    //Load CSS file
    toastrStyles
  }

  /**
   * Shows a success toast.
   * @param message a toast message
   * @param title a toast title
   * @param options toast options
   */
  fun success(message: String, title: String? = null, options: ToastOptions? = null) {
    show(ToastType.SUCCESS, message, title, options)
  }

  /**
   * Shows an info toast.
   * @param message a toast message
   * @param title a toast title
   * @param options toast options
   */
  fun info(message: String, title: String? = null, options: ToastOptions? = null) {
    show(ToastType.INFO, message, title, options)
  }

  /**
   * Shows a warning toast.
   * @param message a toast message
   * @param title a toast title
   * @param options toast options
   */
  fun warning(message: String, title: String? = null, options: ToastOptions? = null) {
    show(ToastType.WARNING, message, title, options)
  }

  /**
   * Shows an error toast.
   * @param message a toast message
   * @param title a toast title
   * @param options toast options
   */
  fun error(message: String, title: String? = null, options: ToastOptions? = null) {
    show(ToastType.ERROR, message, title, options)
  }

  private fun show(type: ToastType, message: String, title: String? = null, options: ToastOptions? = null) {
    if (options != null) {
      toastr[type.type](message, title, options.toJs())
    } else {
      toastr[type.type](message, title)
    }
  }
}

//@Suppress("SpellCheckingInspection")
//@JsModule("toastr/build/toastr.min.css")
//external val toastrCss: dynamic
@JsModule("toastr/build/toastr.min.css")
@JsNonModule
external val toastrStyles: dynamic

@Suppress("SpellCheckingInspection")
@JsModule("toastr")
@JsNonModule
external val toastr: dynamic
