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
package it.neckar.react.common.featureflags

import it.neckar.common.featureflags.FeatureFlag
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.react.common.*
import it.neckar.react.common.toast.*
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

/**
 * Renders and edits the feature flags from the [FeatureFlagsContext]
 */
val EditFeatureFlagsComponent: FC<FeatureFlagsComponentProps> = fc("FeatureFlagsComponent") { props ->
  val featureFlags =  props.specificFlags ?: FeatureFlag.available

  val featureFlagsContextContent = useFeatureFlagsContext()

  val featureFlagStates: Map<FeatureFlag, StateInstance<Boolean>> = featureFlags.associateWith { flag ->
    useState(featureFlagsContextContent.featureFlags.contains(flag))
  }


  div {
    FeatureFlagsComponentForm {
      attrs {
        this.flagsStates = featureFlagStates
      }
    }


    button(classes = "btn btn-primary") {
      +"Anwenden"

      attrs {
        onClickFunction = {
          val flags = featureFlagStates.toFeatureFlags()
          featureFlagsContextContent.updater(flags)
          notifySuccess("Feature Flags angewendet!")
        }
      }
    }
  }
}

/**
 * Converts the map to a feature flags object
 */
private fun Map<FeatureFlag, StateInstance<Boolean>>.toFeatureFlags(): FeatureFlags {
  val flags = this
    .asSequence()
    .filter { it.value.value }
    .map {
      it.key
    }.toSet()

  return FeatureFlags(flags)
}

external interface FeatureFlagsComponentProps : Props {
  var specificFlags: List<FeatureFlag>?

}
