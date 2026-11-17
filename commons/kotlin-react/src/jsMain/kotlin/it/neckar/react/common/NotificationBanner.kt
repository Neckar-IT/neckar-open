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

import it.neckar.react.common.BannerPlacement.*
import kotlinx.html.DIV
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

/**
 * Generates the app footer
 */
fun RBuilder.notificationBanner(
  title: String,
  bannerType: BannerType,
  bannerPlacement: BannerPlacement,
  /**
   * Classes of nested wrappers around the content, outermost first; the alert padding is removed when given.
   */
  contentWrapperClasses: List<String> = emptyList(),
  onClose: (() -> Unit)? = null,
  block: RDOMBuilder<DIV>.() -> Unit,
): Unit = child(NotificationBanner) {
  attrs {
    this.title = title
    this.bannerType = bannerType
    this.bannerPlacement = bannerPlacement
    this.contentWrapperClasses = contentWrapperClasses
    this.onClose = onClose
    this.block = block
  }
}

val NotificationBanner: FC<NotificationBannerProps> = fc("NotificationBanner") { props ->

  div(classes = "alert alert-dismissible") {
    attrs {
      if (props.contentWrapperClasses.isNotEmpty()) {
        addClass("px-0")
      }

      when (props.bannerType) {
        BannerType.Info -> addClass("alert-info")
        BannerType.Warning -> addClass("alert-warning")
        BannerType.Danger -> addClass("alert-danger")
        BannerType.Success -> addClass("alert-success")
      }

      when (props.bannerPlacement) {
        Top -> addClass("fixed-top")
        Bottom -> {
          addClass("fixed-bottom")
          addClass("mb-0") //the alert margin would leave a gap below the banner
        }
      }
    }

    val renderContent: RDOMBuilder<DIV>.() -> Unit = {
      div("mb-2") {
        when (props.bannerType) {
          BannerType.Info -> i(classes = "fas fa-info-circle") {}
          BannerType.Warning -> i(classes = "fas fa-exclamation-triangle") {}
          BannerType.Danger -> i(classes = "fas fa-exclamation-circle") {}
          BannerType.Success -> i(classes = "fas fa-check-circle") {}
        }

        strong(classes = "mx-2 alert-heading") {
          +props.title
        }
      }

      props.onClose?.let { onClose ->
        button(classes = "btn-close") {
          attrs {
            onClickFunction = { onClose() }
          }
        }
      }

      props.block(this)
    }

    val wrappedContent: RDOMBuilder<DIV>.() -> Unit = props.contentWrapperClasses.foldRight(renderContent) { wrapperClasses, inner ->
      { div(wrapperClasses) { inner() } }
    }
    wrappedContent()
  }

}

enum class BannerType {
  Info,
  Warning,
  Danger,
  Success,
}

enum class BannerPlacement {
  Top,
  Bottom,
}


external interface NotificationBannerProps : Props {
  var title: String
  var bannerType: BannerType
  var bannerPlacement: BannerPlacement
  var contentWrapperClasses: List<String>
  var onClose: (() -> Unit)?
  var block: (RDOMBuilder<DIV>) -> Unit
}
