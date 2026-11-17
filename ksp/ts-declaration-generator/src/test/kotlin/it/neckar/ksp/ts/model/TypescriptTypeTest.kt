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
package it.neckar.ksp.ts.model

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

/**
 *
 */
class TypescriptTypeTest {

  @Test
  fun testToTypescriptString() {
    assertThat(TsType(name = "string", isNullable = false, isArray = false, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string")
    assertThat(TsType(name = "string", isNullable = true, isArray = false, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string | null | undefined")
    assertThat(TsType(name = "string", isNullable = false, isArray = false, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string")
    assertThat(TsType(name = "string", isNullable = false, isArray = true, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string[]")
    assertThat(TsType(name = "string", isNullable = true, isArray = true, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string[] | null | undefined")
    assertThat(TsType(name = "string", isNullable = true, isArray = false, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string | null | undefined")
    assertThat(TsType(name = "string", isNullable = false, isArray = true, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string[]")
    assertThat(TsType(name = "string", isNullable = true, isArray = true, importedFrom = TsModuleReference.SameFile).toTypescriptString()).isEqualTo("string[] | null | undefined")
    assertThat(
      TsType(
        name = "string", isNullable = false, isArray = false,
        typeArguments = listOf(
          TsType.Number(isNullable = false, isArray = false)
        ),
        importedFrom = TsModuleReference.SameFile
      ).toTypescriptString()
    ).isEqualTo("string<number>")
    assertThat(
      TsType(
        name = "string", isNullable = false, isArray = false,
        typeArguments = listOf(TsType.Number(isNullable = false, isArray = false), TsType.Boolean(isNullable = false, isArray = false)),
        importedFrom = TsModuleReference.SameFile

      ).toTypescriptString()
    ).isEqualTo("string<number, boolean>")
  }

  @Test
  fun testStringType() {
    assertThat(TsType.String.toTypescriptString()).isEqualTo("string")
    assertThat(TsType.String(isNullable = true, isArray = false).toTypescriptString()).isEqualTo("string | null | undefined")
    assertThat(TsType.String(isNullable = false, isArray = false).toTypescriptString()).isEqualTo("string") //? at var
    assertThat(TsType.String(isNullable = false, isArray = true).toTypescriptString()).isEqualTo("string[]")
    assertThat(TsType.String(isNullable = true, isArray = true).toTypescriptString()).isEqualTo("string[] | null | undefined")
  }

  @Test
  fun testNumberType() {
    assertThat(TsType.Number.toTypescriptString()).isEqualTo("number")
    assertThat(TsType.Number(isNullable = true, isArray = false).toTypescriptString()).isEqualTo("number | null | undefined")
    assertThat(TsType.Number(isNullable = false, isArray = false).toTypescriptString()).isEqualTo("number")
    assertThat(TsType.Number(isNullable = false, isArray = true).toTypescriptString()).isEqualTo("number[]")
    assertThat(TsType.Number(isNullable = true, isArray = true).toTypescriptString()).isEqualTo("number[] | null | undefined")
  }

  @Test
  fun testBooleanType() {
    assertThat(TsType.Boolean.toTypescriptString()).isEqualTo("boolean")
    assertThat(TsType.Boolean(isNullable = true, isArray = false).toTypescriptString()).isEqualTo("boolean | null | undefined")
    assertThat(TsType.Boolean(isNullable = false, isArray = false).toTypescriptString()).isEqualTo("boolean")
    assertThat(TsType.Boolean(isNullable = false, isArray = true).toTypescriptString()).isEqualTo("boolean[]")
    assertThat(TsType.Boolean(isNullable = true, isArray = true).toTypescriptString()).isEqualTo("boolean[] | null | undefined")
  }

  @Test
  fun testAnyType() {
    assertThat(TsType.Any.toTypescriptString()).isEqualTo("any")
    assertThat(TsType.Any(isNullable = true, isArray = false).toTypescriptString()).isEqualTo("any | null | undefined")
    assertThat(TsType.Any(isNullable = false, isArray = false).toTypescriptString()).isEqualTo("any")
    assertThat(TsType.Any(isNullable = false, isArray = true).toTypescriptString()).isEqualTo("any[]")
    assertThat(TsType.Any(isNullable = true, isArray = true).toTypescriptString()).isEqualTo("any[] | null | undefined")
  }

  @Test
  fun testVoidType() {
    assertThat(TsType.Void.toTypescriptString()).isEqualTo("void")
  }
}
