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
import it.neckar.open.test.utils.isEqualIgnoringWhitespacesEmptyLines
import org.junit.jupiter.api.Test

/**
 *
 */
class TsClassTest {
  @Test
  fun `test code generation`() {
    val myInterface = TsClass(
      name = "MyClass",
      doc = "This is my class",
      superTypes = listOf(TsType("MySuperType", classKind = TsType.ClassKind.Interface, importedFrom = TsModuleReference.SameFile)),
      typeParameters = listOf(
        TsTypeParameter("T", listOf()),
        TsTypeParameter("U", listOf(TsType("Foo", importedFrom = TsModuleReference.SameFile), TsType("Bar", importedFrom = TsModuleReference.SameFile))),
      ),
      kotlinSource = "it.neckar.MyInterface",
      properties = listOf(
        TsProperty("prop1", TsType.String, doc = "This is my first property"),
        TsProperty("prop2", TsType.Number, doc = "This is my second property"),
      ),
      constructors = listOf(
        TsConstructor(
          doc = "This is my constructor",
          parameters = listOf(
            TsFunctionParameter("param1", TsType.String),
            TsFunctionParameter("param2", TsType.Number),
          )
        )
      ),
      functions = listOf(
        TsFunctionSignature(
          name = "myFunction",
          doc = "This is my function",
          parameters = listOf(
            TsFunctionParameter("param1", TsType.String),
            TsFunctionParameter("param2", TsType.Number),
          ),
          returnType = TsType.String,
          typeParameters = listOf(TsTypeParameter("T", emptyList())),
        )
      ),
      companionObject = null,
    )

    assertThat(myInterface.toTypescriptString(Indentation.None).trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my class
         */
        export declare class MyClass<T, U extends Foo & Bar> implements MySuperType {
          /**
           * This is my first property
           */
          prop1: string;
          /**
           * This is my second property
           */
          prop2: number;

          constructor(param1: string, param2: number);

          /**
           * This is my function
           */
          myFunction<T>(param1: string, param2: number): string;


        }
      """.trimIndent()
    )
  }

  @Test
  fun testClassComplex() {
    val toCreate = TsClass(
      name = "MyClass",
      doc = "This is my class",
      kotlinSource = "it.neckar.MyClass",
      superTypes = listOf(TsType(name = "MySuperType", importedFrom = TsModuleReference.ProjectModuleReference("my-other-module"), classKind = TsType.ClassKind.Interface)),

      typeParameters = listOf(
        TsTypeParameter(name = "T", bounds = listOf()),
        TsTypeParameter(name = "U", bounds = listOf(TsType("Foo", importedFrom = TsModuleReference.SameFile), TsType("Bar", importedFrom = TsModuleReference.SameFile))),
      ),

      constructors = listOf(
        TsConstructor(
          doc = "This is my first constructor",
          parameters = listOf(
            TsFunctionParameter(name = "param1", type = TsType.String),
          )
        ),

        TsConstructor(
          doc = "This is my second constructor",
          parameters = listOf(
            TsFunctionParameter(name = "param1", type = TsType("FooBar", typeArguments = listOf(TsType.String), importedFrom = TsModuleReference.SameFile)),
            TsFunctionParameter(name = "param2", type = TsType.Number),
          )
        ),
      ),
      functions = listOf(
        TsFunctionSignature(
          name = "myFunction",
          doc = "This is my function",
          parameters = listOf(
            TsFunctionParameter(name = "param1", type = TsType.String),
            TsFunctionParameter(name = "param2", type = TsType.Number),
          ),
          returnType = TsType.String,
          typeParameters = listOf(TsTypeParameter("T", emptyList())),
        ),

        TsFunctionSignature(
          name = "myFunction",
          doc = "This is my function",
          parameters = listOf(
            TsFunctionParameter(name = "param1", type = TsType("FooBar", typeArguments = listOf(TsType.String), importedFrom = TsModuleReference.SameFile), isOptional = true),
            TsFunctionParameter(name = "param2", type = TsType.Number, isOptional = true),
          ),
          returnType = TsType.String(isNullable = true, isArray = true),
          typeParameters = listOf(TsTypeParameter("BarFoo", listOf(TsType.typeParameter("T")))),
        ),
      ),
      properties = listOf(
        TsProperty(name = "myProperty", type = TsType.String, isOptional = true, doc = "This is my property"),
        TsProperty(name = "myListProperty", type = TsType.String(isArray = true), doc = "This is list property"),
      ),
      companionObject = null,
    )

    val generatedCode = toCreate.toTypescriptString()

    assertThat(generatedCode.trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my class
         */
        export declare class MyClass<T, U extends Foo & Bar> implements MySuperType {
          /**
           * This is my property
           */
          myProperty?: string;
          /**
           * This is list property
           */
          myListProperty: string[];

          constructor(param1: string);
          constructor(param1: FooBar<string>, param2: number);

          /**
           * This is my function
           */
          myFunction<T>(param1: string, param2: number): string;
          /**
           * This is my function
           */
          myFunction<BarFoo extends T>(param1?: FooBar<string> | null | undefined, param2?: number | null | undefined): string[] | null | undefined;


        }
      """.trimIndent()
    )

    assertThat(toCreate.collectTypes()).hasSize(9)
    assertThat(toCreate.collectTypes()).contains(TsType.defaultInterface("MySuperType", importedFrom = TsModuleReference.ProjectModuleReference("my-other-module")))
    assertThat(toCreate.collectTypes()).contains(TsType.String)
  }

}
