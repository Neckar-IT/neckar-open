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
import it.neckar.open.test.utils.isEqualIgnoringWhitespacesEmptyLines
import org.junit.jupiter.api.Test

/**
 *
 */
class TsInterfaceTest {
  @Test
  fun testInterfaceSimple() {
    val toCreate = TsInterface(
      name = "MyInterface",
      doc = "This is my interface",
      kotlinSource = "it.neckar.MyInterface",
      superTypes = listOf(),
      functions = listOf(
        TsFunctionSignature(
          name = "myFunction",
          doc = "This is my function",
          parameters = listOf(
            TsFunctionParameter("param1", TsType.String),
            TsFunctionParameter("param2", TsType.Number),
          ),
          returnType = TsType.Void,
          typeParameters = emptyList(),
        ),
      ),
      properties = emptyList(),
      typeParameters = listOf(),
      companionObject = null,
    )

    val generatedCode = toCreate.toTypescriptString()

    assertThat(generatedCode.trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my interface
         */
        export declare interface MyInterface {
          /**
           * This is my function
           */
          myFunction(param1: string, param2: number): void;

        }
      """.trimIndent()
    )
  }

  @Test
  fun testInterfaceWithComparable() {
    val toCreate = TsInterface(
      name = "MyInterface",
      doc = "This is my interface",
      kotlinSource = "it.neckar.MyInterface",
      superTypes = listOf(
        TsType.defaultInterface("MyComparable", typeArguments = listOf(TsType("MyInterface", importedFrom = TsModuleReference.SameFile)), importedFrom = TsModuleReference.SameFile),
      ),
      functions = listOf(
        TsFunctionSignature(
          name = "myFunction",
          doc = "This is my function",
          parameters = listOf(
            TsFunctionParameter("param1", TsType.String),
            TsFunctionParameter("param2", TsType.Number),
          ),
          returnType = TsType.Void,
          typeParameters = emptyList(),
        ),
      ),
      properties = emptyList(),
      typeParameters = listOf(),
      companionObject = null,
    )

    val generatedCode = toCreate.toTypescriptString()

    assertThat(generatedCode.trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my interface
         */
        export declare interface MyInterface extends MyComparable<MyInterface> {
          /**
           * This is my function
           */
          myFunction(param1: string, param2: number): void;

        }
      """.trimIndent()
    )
  }


  @Test
  fun testInterfaceWithClassAndInterfaceSuperType() {
    val toCreate = TsInterface(
      name = "MyInterface",
      doc = "This is my interface",
      kotlinSource = "it.neckar.MyInterface",
      superTypes = listOf(
        TsType.defaultClass("MyClassSuperType", importedFrom = TsModuleReference.SameFile),
        TsType.defaultInterface("MyInterfaceSuperType", importedFrom = TsModuleReference.SameFile),
      ),
      functions = emptyList(),
      properties = emptyList(),
      typeParameters = listOf(),
      companionObject = null,
    )

    val generatedCode = toCreate.toTypescriptString()

    //A class super type and an interface super type must be comma-separated, not concatenated
    assertThat(generatedCode.trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my interface
         */
        export declare interface MyInterface extends MyClassSuperType, MyInterfaceSuperType {
        }
      """.trimIndent()
    )
  }

  @Test
  fun `test code generation`() {
    val myInterface = TsInterface(
      name = "MyInterface",
      doc = "This is my interface",
      superTypes = listOf(TsType("MySuperType", importedFrom = TsModuleReference.SameFile)),
      typeParameters = listOf(
        TsTypeParameter("T", listOf()),
        TsTypeParameter("U", listOf(TsType("Foo", importedFrom = TsModuleReference.SameFile), TsType("Bar", importedFrom = TsModuleReference.SameFile))),
      ),
      kotlinSource = "it.neckar.MyInterface",
      properties = listOf(
        TsProperty("prop1", TsType.String, doc = "This is my first property"),
        TsProperty("prop2", TsType.Number, doc = "This is my second property"),
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
         * This is my interface
         */
        export declare interface MyInterface<T, U extends Foo & Bar> {
          /**
           * This is my first property
           */
          prop1: string;
          /**
           * This is my second property
           */
          prop2: number;
          /**
           * This is my function
           */
          myFunction<T>(param1: string, param2: number): string;

        }
      """.trimIndent()
    )
  }

  @Test
  fun testInterfaceComplex() {
    val toCreate = TsInterface(
      name = "MyInterface",
      doc = "This is my interface",
      kotlinSource = "it.neckar.MyInterface",
      superTypes = listOf(TsType("MySuperType", importedFrom = TsModuleReference.SameFile), TsType("MyOtherSuperType", importedFrom = TsModuleReference.SameFile)),
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
        ),

        TsFunctionSignature(
          name = "myFunction",
          doc = "This is my function",
          parameters = listOf(
            TsFunctionParameter("param1", TsType("FooBar", typeArguments = listOf(TsType.String), importedFrom = TsModuleReference.SameFile)),
            TsFunctionParameter("param2", TsType.Number),
          ),
          returnType = TsType.String(isNullable = true, isArray = true),
          typeParameters = emptyList(),
        ),
      ),

      properties = emptyList(),

      typeParameters = listOf(
        TsTypeParameter("T", listOf()),
        TsTypeParameter("U", listOf(TsType("Foo", importedFrom = TsModuleReference.SameFile), TsType("Bar", importedFrom = TsModuleReference.SameFile))),
      ),
      companionObject = null,
    )

    val generatedCode = toCreate.toTypescriptString()

    assertThat(generatedCode.trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my interface
         */
        export declare interface MyInterface<T, U extends Foo & Bar> {
          /**
           * This is my function
           */
          myFunction<T>(param1: string, param2: number): string;
          /**
           * This is my function
           */
          myFunction(param1: FooBar<string>, param2: number): string[] | null | undefined;

        }
      """.trimIndent()
    )
  }

}
