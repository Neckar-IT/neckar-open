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

class TsEnumTest {
  @Test
  fun `test generate simple enum`() {
    {
      val toCreate = TsEnumClass(
        name = "MyEnum",
        doc = "This is my enum",
        kotlinSource = "it.neckar.MyEnum",
        values = listOf(
          TsEnumClass.TsEnumValue("Value1", "This is value 1"),
          TsEnumClass.TsEnumValue("Value2", "This is value 2"),
        ),
        properties = emptyList(),
        functions = emptyList(),
        constructors = emptyList(),
        superTypes = emptyList(),
        typeParameters = emptyList(),
        companionObject = null,
      )

      val generatedCode = toCreate.toTypescriptString()

      assertThat(generatedCode.trim()).isEqualTo(
        //language=TypeScript
        """
          /**
           * This is my enum
           */
          abstract class MyEnum {
            /**
             * This is value 1
             */
            static get Value1(): MyEnum & {
              get name(): string;
              get ordinal(): number;
            };

            /**
             * This is value 2
             */
            static get Value2(): MyEnum & {
              get name(): string;
              get ordinal(): number;
            };

            get name(): "Value1" | "Value2";
            get ordinal(): 0 | 1;
            static values(): Array<MyEnum>;
            static valueOf(name: string): MyEnum;
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun `test generate complex enum`() {
    val toCreate = TsEnumClass(
      name = "MyEnum",
      doc = "This is my enum",
      kotlinSource = "it.neckar.MyEnum",
      values = listOf(
        TsEnumClass.TsEnumValue("Value1", "This is value 1"),
        TsEnumClass.TsEnumValue("Value2", "This is value 2"),
      ),
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
          typeParameters = emptyList(),
        )
      ),
      constructors = listOf(
        TsConstructor(
          parameters = listOf(
            TsFunctionParameter("param1", TsType.String),
            TsFunctionParameter("param2", TsType.Number),
          ),
          doc = "This is my constructor"
        )
      ),
      superTypes = emptyList(),
      typeParameters = emptyList(),
      companionObject = null,
    )

    val generatedCode = toCreate.toTypescriptString()

    assertThat(generatedCode.trim()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * This is my enum
         */
        export declare abstract class MyEnum {
          /**
           * This is value 1
           */
          static get Value1(): MyEnum & {

            get name(): "Value1";

            get ordinal(): 0;

          };
          /**
           * This is value 2
           */
          static get Value2(): MyEnum & {

            get name(): "Value2";

            get ordinal(): 1;

          };
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
          myFunction(param1: string, param2: number): string;
          get name(): "Value1" | "Value2";
          get ordinal(): 0 | 1;
          static values(): MyEnum[];
          static valueOf(name: string): MyEnum;

        }
      """.trimIndent()
    )
  }
}
