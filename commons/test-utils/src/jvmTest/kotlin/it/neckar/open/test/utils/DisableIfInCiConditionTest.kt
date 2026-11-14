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
package it.neckar.open.test.utils

import assertk.*
import assertk.assertions.*
import io.mockk.every
import io.mockk.mockk
import it.neckar.open.lang.orNull
import it.neckar.runtime.context.DeploymentStage
import it.neckar.runtime.context.ExecutionEnvironment
import it.neckar.runtime.context.RuntimeContext
import it.neckar.runtime.context.ServiceHost
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.Optional
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@SkipInCi
class DisableIfInCiConditionTest {
  private lateinit var originalRuntimeContext: RuntimeContext<*>

  @BeforeTest
  fun rememberRuntimeContext() {
    originalRuntimeContext = RuntimeContext.current
  }

  @AfterTest
  fun restoreRuntimeContext() {
    RuntimeContext.initialize(originalRuntimeContext)
  }

  @Test
  fun disablesInCiWithDefaultReason() {
    setRuntimeContext(ExecutionEnvironment.CI)
    val result = evaluateConditionFor(SampleCiSkippedTest::class.java)

    assertThat(result.isDisabled).isTrue()
    assertThat(result.reason.orNull()).isEqualTo("Disabled because running inside CI")
  }

  @Test
  fun usesCustomReasonWhenProvided() {
    setRuntimeContext(ExecutionEnvironment.CI)
    val result = evaluateConditionFor(SampleCiSkippedWithReason::class.java)

    assertThat(result.isDisabled).isTrue()
    assertThat(result.reason.orNull()).isEqualTo("custom reason")
  }

  @Test
  fun fallsBackToDefaultReasonForBlankReason() {
    setRuntimeContext(ExecutionEnvironment.CI)
    val result = evaluateConditionFor(SampleCiSkippedWithBlankReason::class.java)

    assertThat(result.isDisabled).isTrue()
    assertThat(result.reason.orNull()).isEqualTo(null)
  }

  @Test
  fun enablesOutsideCi() {
    setRuntimeContext(ExecutionEnvironment.LocalDev)
    val result = evaluateConditionFor(SampleCiSkippedTest::class.java)

    assertThat(result.isDisabled).isFalse()
    assertThat(result.reason.orElse(null)).isEqualTo("Enabled because not running inside CI")
  }

  private fun evaluateConditionFor(element: Class<*>): org.junit.jupiter.api.extension.ConditionEvaluationResult {
    val context = mockk<ExtensionContext>(relaxed = true)
    every { context.element } returns Optional.of(element)

    return DisableIfInCiCondition().evaluateExecutionCondition(context)
  }

  private fun setRuntimeContext(environment: ExecutionEnvironment) {
    RuntimeContext.initialize(
      RuntimeContext(
        executionEnvironment = environment,
        stage = DeploymentStage.Development,
        host = ServiceHost.Default,
        inUnitTest = true,
        debugMode = false,
        initialValue = false,
      )
    )
  }

  @SkipInCi
  private class SampleCiSkippedTest

  @SkipInCi(reason = "custom reason")
  private class SampleCiSkippedWithReason

  @SkipInCi(reason = "")
  private class SampleCiSkippedWithBlankReason
}
