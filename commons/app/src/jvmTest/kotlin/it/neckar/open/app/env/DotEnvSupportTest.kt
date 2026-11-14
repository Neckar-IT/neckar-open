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
package it.neckar.open.app.env

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Reads the real `.env-for-unit-test` next to this module through the real file system — the
 * part the [FakeFileSystemAccess]-driven common tests cannot cover.
 */
class DotEnvSupportTest {
  @Test
  fun `reads the unit test env file next to the module`() {
    val dotEnv = DotEnvSupport.dotEnvForUnitTest

    assertThat(dotEnv["foo"]).isNull()
    assertThat(dotEnv["TEST_ENTRY"]).isEqualTo("test-value")
    assertThat(dotEnv["TEST_NUMBER_VALUE_ENTRY"]).isEqualTo("17")
  }

  @Test
  fun `the env file of the checkout a tools bin entry point names is read from any working directory`(@TempDir checkout: File) {
    checkout.resolve(".env").writeText("CHECKOUT_ONLY_ENTRY=from-checkout\n")

    val dotEnv = DotEnvSupport.loadDefault { name -> if (name == DotEnvSupport.RepositoryRootVariable) checkout.path else null }

    assertThat(dotEnv["CHECKOUT_ONLY_ENTRY"]).isEqualTo("from-checkout")
  }
}
