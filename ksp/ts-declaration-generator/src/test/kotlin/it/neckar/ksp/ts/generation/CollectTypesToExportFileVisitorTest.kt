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
package it.neckar.ksp.ts.generation

import assertk.*
import assertk.assertions.*
import it.neckar.gradle.ksp.generating.types.list.model.Class2ProjectMapping
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProject
import it.neckar.gradle.ksp.generating.types.list.model.TypesListForProjectCollection
import it.neckar.gradle.ksp.options.KspPluginOptions
import it.neckar.ksp.test.KspTestUtils
import it.neckar.ksp.ts.ConstObjectTsGenerator
import it.neckar.ksp.ts.TypeScriptDefinitionGenerationProcessorProvider
import it.neckar.ksp.ts.TypescriptDeclarationGenerator
import it.neckar.ksp.ts.generation.sample.ExampleClassWithLambdas
import it.neckar.ksp.ts.generation.sample.ExampleClassWithNestedValueClass
import it.neckar.ksp.ts.generation.sample.ExampleDataClass
import it.neckar.ksp.ts.generation.sample.ExampleTestClass
import it.neckar.ksp.ts.generation.sample.ExampleTestObject
import it.neckar.ksp.ts.model.CollectingContext
import it.neckar.ksp.ts.model.KotlinModulesGuesser
import it.neckar.ksp.ts.model.TsClass
import it.neckar.ksp.ts.model.TsEnumClass.TsEnumValue
import it.neckar.ksp.ts.model.TsModuleReference
import it.neckar.ksp.ts.model.TsModuleReference.KotlinNativeTypes
import it.neckar.ksp.ts.model.TsNamespace
import it.neckar.ksp.ts.model.TsType
import it.neckar.ksp.ts.model.TsValueClass
import it.neckar.ksp.ts.writeImports
import it.neckar.ksp.ts.writeTypescriptDefinition
import it.neckar.open.collections.fastForEach
import it.neckar.open.test.utils.SlowTest
import it.neckar.open.test.utils.isEqualIgnoringWhitespacesEmptyLines
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.StringWriter

/**
 *
 */
@OptIn(ExperimentalCompilerApi::class)
@SlowTest
class CollectTypesToExportFileVisitorTest {

  private val testClassFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleTestClass.kt")
  private val testDataClassFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleDataClass.kt")
  private val testObjectFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleTestObject.kt")
  private val testInterfaceFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleTestInterface.kt")
  private val testEnumFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleTestEnum.kt")
  private val testValueClassFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleValueClass.kt")
  private val testValueClassWithUuidFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleValueClassWithUuid.kt")
  private val testValueClassWithDerivedPropertyFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleValueClassWithDerivedProperty.kt")
  private val testSealedInterfaceFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleSealedInterface.kt")
  private val testGenericsFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleClassWithGenerics.kt")
  private val testInterfaceNestedFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleTestInterfaceWithNested.kt")
  private val testClassNestedFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleClassWithNested.kt")
  private val testClassWithNestedValueClassFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleClassWithNestedValueClass.kt")
  private val testClassWithLambdas = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleClassWithLambdas.kt")
  private val testClassWithJsName = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleClassWithJsName.kt")
  private val testDataClassWithJsName = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/ExampleDataClassWithJsName.kt")
  private val testBuiltinNameCollisionFile = File("src/test/kotlin/it/neckar/ksp/ts/generation/sample/collision/ExampleClassWithBuiltinNameCollision.kt")

  private val allSourceFiles = listOf(
    testObjectFile,
    testClassFile,
    testDataClassFile,
    testInterfaceFile,
    testEnumFile,
    testValueClassFile,
    testValueClassWithUuidFile,
    testValueClassWithDerivedPropertyFile,
    testSealedInterfaceFile,
    testGenericsFile,
    testInterfaceNestedFile,
    testClassNestedFile,
    testClassWithNestedValueClassFile,
    testClassWithJsName,
    testDataClassWithJsName,
    //testClassWithLambdas,
  )

  @BeforeEach
  fun setUp(@TempDir directory: File) {
    val typesListFile = directory.resolve("types-list-for-project-collection.json")
    KotlinModulesGuesser.clear()

    allSourceFiles.fastForEach { sourceFile ->
      assertThat(sourceFile, sourceFile.absolutePath).exists()
    }

    val projectCollection = TypesListForProjectCollection(
      listOf(
        TypesListForProject(
          projectPath = "neckarIT-internal-open-commons-kotlin-lang",
          packageNames = listOf("it.neckar.ksp.ts.generation.sample"),
          classNames = listOf(
            "it.neckar.open.http.UrlPattern.Relative0"
          )
        )
      )
    )

    typesListFile.writeText(Json.encodeToString(projectCollection))

    unitTestOptions = mapOf(
      KspPluginOptions.TypeScriptDefinitionGeneration.TypeScriptDefinitionFileName to "index.kspunittest.d.ts",
      KspPluginOptions.TypeScriptDefinitionGeneration.TypesListForProjectCollectionFilePath to typesListFile.absolutePath,
    )
  }

  @AfterEach
  fun tearDown() {
    KotlinModulesGuesser.clear()
  }

  private lateinit var unitTestOptions: Map<String, String>

  @Disabled
  @Test
  fun testLambda() {
    assertThat(ExampleClassWithLambdas::class).isNotNull()

    val collectingContext = executeCollect(listOf(testClassWithLambdas))

    assertThat(collectingContext.classes()).hasSize(1)
    val classWithLambdas = collectingContext.classes()[0]

    assertThat(classWithLambdas.name).isEqualTo("ExampleClassWithLambdas")

    assertThat(classWithLambdas.functions).hasSize(2)

    classWithLambdas.functions[0].let { function ->
      assertThat(function.name).isEqualTo("withCallbackParameter")
      assertThat(function.parameters).hasSize(1)
      assertThat(function.parameters[0].type).isEqualTo(TsType.defaultClass("Unit", importedFrom = TsModuleReference.SameFile))
    }
  }

  @Test
  fun testDataClass() {
    assertThat(ExampleDataClass::class).isNotNull()

    val collectingContext = executeCollect(listOf(testDataClassFile))

    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)
    assertThat(collectingContext.valueClasses()).hasSize(0)

    val tsClass = collectingContext.classes().first()
    assertThat(tsClass.name).isEqualTo("ExampleDataClass")

    assertThat(tsClass.functions).hasSize(3)
    assertThat(tsClass.functions[0].name).isEqualTo("foobar")
    assertThat(tsClass.functions[1].name).isEqualTo("copy")
    assertThat(tsClass.functions[2].name).isEqualTo("toString")
  }

  @Test
  fun testBuiltinNameCollision() {
    //A domain type named `List` (no type argument) must not hit the builtin `List` branch's `check(arguments.size == 1)`,
    //and a domain type named `String` must not be emitted as the TS `string` primitive. Matching builtins by qualified
    //name routes both through the user-type branch.
    val collectingContext = executeCollect(listOf(testBuiltinNameCollisionFile))

    assertThat(collectingContext.classes().map { it.name }).containsAll("String", "List", "ExampleClassWithBuiltinNameCollision")

    val holder = collectingContext.classes().first { it.name == "ExampleClassWithBuiltinNameCollision" }

    val constructor = holder.constructors.first()

    constructor.parameters.first { it.name == "collidingString" }.type.let { type ->
      assertThat(type.name).isEqualTo("String")
      assertThat(type.classKind).isEqualTo(TsType.ClassKind.Class)
      assertThat(type.importedFrom).isEqualTo(TsModuleReference.SameFile)
      assertThat(type).isNotEqualTo(TsType.String)
    }

    constructor.parameters.first { it.name == "collidingList" }.type.let { type ->
      assertThat(type.name).isEqualTo("List")
      assertThat(type.classKind).isEqualTo(TsType.ClassKind.Class)
      assertThat(type.importedFrom).isEqualTo(TsModuleReference.SameFile)
      assertThat(type.isArray).isFalse()
    }
  }

  @Test
  fun testNestedValueClass() {
    assertThat(ExampleClassWithNestedValueClass::class).isNotNull()

    val collectingContext = executeCollect(listOf(testClassWithNestedValueClassFile))

    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)
    assertThat(collectingContext.valueClasses()).hasSize(1)

    val tsClass = collectingContext.classes().first()
    assertThat(tsClass.name).isEqualTo("ExampleClassWithNestedValueClass")
    assertThat(tsClass.doc?.trim()).isEqualTo("Contains a nested value class")
    assertThat(tsClass.superTypes).isEmpty()

    collectingContext.valueClasses().first().let { valueClass ->
      assertThat(valueClass.name).isEqualTo("Id")
      assertThat(valueClass.namespace?.name).isEqualTo("ExampleClassWithNestedValueClass")
    }

    collectingContext.collectElementsWithNamespace().let { elementsWithNamespace ->
      assertThat(elementsWithNamespace).hasSize(1)
      assertThat(elementsWithNamespace.first()).isSameInstanceAs(collectingContext.valueClasses().first())
    }


    val writer = StringWriter()
    TypescriptDeclarationGenerator().generateDeclarationFile(collectingContext, writer)

    assertThat(writer.toString()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        import {Uuid} from "./neckarIT-kotlin-native-types";

        /**
         * Contains a nested value class
         */
        export declare class ExampleClassWithNestedValueClass {
        }

        export declare namespace ExampleClassWithNestedValueClass {

          export type Id = Uuid;
        }

      """.trimIndent()
    )
  }

  @Test
  fun testInterfaceNested() {
    val collectingContext = executeCollect(listOf(testInterfaceNestedFile))

    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(1)
    assertThat(collectingContext.constObjects()).hasSize(0)
    assertThat(collectingContext.valueClasses()).hasSize(0)

    val elementsWithNamespace = collectingContext.collectElementsWithNamespace()
    assertThat(elementsWithNamespace).hasSize(1)

    val tsInterface = collectingContext.interfaces().first()
    assertThat(tsInterface.name).isEqualTo("ExampleTestInterfaceWithNested")
    assertThat(tsInterface.doc?.trim()).isEqualTo("An interface containing a nested class")
    assertThat(tsInterface.superTypes).isEmpty()

    val nestedClass = elementsWithNamespace.first() as TsClass
    assertThat(nestedClass.namespace?.name).isEqualTo("ExampleTestInterfaceWithNested")
    assertThat(nestedClass.name).isEqualTo("MyInnerClass")
  }

  @Test
  fun testClassNested() {
    val collectingContext = executeCollect(listOf(testClassNestedFile))

    assertThat(collectingContext.classes()).hasSize(2)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)
    assertThat(collectingContext.valueClasses()).hasSize(0)

    val tsClass = collectingContext.classes().first()
    assertThat(tsClass.name).isEqualTo("ExampleClassWithNested")
    assertThat(tsClass.doc?.trim()).isEqualTo("A class containing a nested class")
    assertThat(tsClass.superTypes).isEmpty()
    assertThat(tsClass.functions).hasSize(1)
    tsClass.functions[0].let { tsFunction ->
      assertThat(tsFunction.name).isEqualTo("foobar")

      val returnType = tsFunction.returnType
      assertThat(returnType.namespace?.name).isEqualTo("ExampleClassWithNested")
      assertThat(returnType.name).isEqualTo("MyInnerClass")
      assertThat(returnType.classKind).isEqualTo(TsType.ClassKind.Class)
      assertThat(returnType.importedFrom).isEqualTo(TsModuleReference.SameFile)
    }

    val nestedClass = collectingContext.collectElementsWithNamespace().first() as TsClass
    assertThat(nestedClass.namespace?.name).isEqualTo("ExampleClassWithNested")
    assertThat(nestedClass.name).isEqualTo("MyInnerClass")

    assertThat(tsClass.collectTypes()).hasSize(1)
    assertThat(tsClass.collectTypes().first().name).isEqualTo("MyInnerClass")
  }

  @Test
  fun testValueClassWithUuid() {
    val collectingContext = executeCollect(listOf(testValueClassWithUuidFile))

    assertThat(collectingContext.valueClasses()).hasSize(1)
    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)

    val tsValueClass: TsValueClass = collectingContext.valueClasses().first()
    assertThat(tsValueClass).isNotNull()

    assertThat(tsValueClass.name).isEqualTo("ExampleValueClassWithUuid")
    assertThat(tsValueClass.doc?.trim()).isEqualTo("This is a value class that has a UUID as its value.")

    assertThat(tsValueClass.property.name).isEqualTo("value")
    assertThat(tsValueClass.property.type.name).isEqualTo("Uuid")

    assertThat(tsValueClass.property.type.importedFrom).isEqualTo(KotlinNativeTypes)


    val writer = StringWriter()
    writer.writeImports(tsValueClass.collectTypes())
    writer.writeTypescriptDefinition(listOf(tsValueClass))
    assertThat(writer.toString()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        import { Uuid } from "./neckarIT-kotlin-native-types";

        /**
         * This is a value class that has a UUID as its value.
         */
        export type ExampleValueClassWithUuid = Uuid;
      """.trimIndent()
    )
  }

  @Test
  fun testValueClassWithDerivedProperty() {
    //A value class may declare derived properties (get() without a backing field).
    //Only the single backing-field property counts, and only it is emitted in the TypeScript declaration.
    val collectingContext = executeCollect(listOf(testValueClassWithDerivedPropertyFile))

    assertThat(collectingContext.valueClasses()).hasSize(1)
    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)

    val tsValueClass: TsValueClass = collectingContext.valueClasses().first()
    assertThat(tsValueClass.name).isEqualTo("ExampleValueClassWithDerivedProperty")
    assertThat(tsValueClass.doc?.trim()).isEqualTo("A value class with a derived property that has no backing field.")

    assertThat(tsValueClass.property.name).isEqualTo("index")
    assertThat(tsValueClass.property.type).isEqualTo(TsType.Number)


    val writer = StringWriter()
    writer.writeImports(tsValueClass.collectTypes())
    writer.writeTypescriptDefinition(listOf(tsValueClass))
    assertThat(writer.toString()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        /**
         * A value class with a derived property that has no backing field.
         */
        export type ExampleValueClassWithDerivedProperty = number;
      """.trimIndent()
    )
  }

  @Test
  fun testClassWithGenerics() {
    val collectingContext = executeCollect(listOf(testGenericsFile))

    assertThat(collectingContext.valueClasses()).hasSize(0)
    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)

    val tsClass = collectingContext.classes().first()
    assertThat(tsClass).isNotNull()

    assertThat(tsClass.name).isEqualTo("ExampleClassWithGenerics")
    assertThat(tsClass.doc?.trim()).isEqualTo("Contains a class with generics")

    assertThat(tsClass.functions).hasSize(1)
    tsClass.functions[0].let { function ->
      assertThat(function.name).isEqualTo("doSomething")
      assertThat(function.returnType).isEqualTo(TsType.defaultClass("ExampleClassWithGenerics", typeArguments = listOf(TsType.String), importedFrom = TsModuleReference.SameFile))
      assertThat(function.parameters).isEmpty()
    }
  }

  @Test
  fun testClassWithJsName() {
    val collectingContext = executeCollect(listOf(testClassWithJsName))

    assertThat(collectingContext.valueClasses()).hasSize(0)
    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)

    val tsClass = collectingContext.classes().first()
    assertThat(tsClass).isNotNull()

    assertThat(tsClass.name).isEqualTo("ExampleClassWithJsNameFromAnnotation")
    assertThat(tsClass.doc?.trim()).isEqualTo("Contains a class @TsName annotations")

    assertThat(tsClass.functions).hasSize(1)
    tsClass.functions[0].let { function ->
      assertThat(function.name).isEqualTo("doSomethingFromAnnotation")
      assertThat(function.returnType).isEqualTo(TsType.defaultClass("ExampleClassWithJsNameFromAnnotation", typeArguments = listOf(TsType.String), importedFrom = TsModuleReference.SameFile))
      assertThat(function.parameters).isEmpty()
    }

    assertThat(tsClass.properties).hasSize(1)
    tsClass.properties[0].let { property ->
      assertThat(property.name).isEqualTo("valueFromAnnotation")
      assertThat(property.type).isEqualTo(TsType.String)
    }
  }

  @Test
  fun testDataClassWithJsName() {
    val collectingContext = executeCollect(listOf(testDataClassWithJsName))

    assertThat(collectingContext.valueClasses()).hasSize(0)
    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)

    val tsClass = collectingContext.classes().first()
    assertThat(tsClass).isNotNull()

    assertThat(tsClass.name).isEqualTo("ExampleClassWithJsNameFromAnnotation")
    assertThat(tsClass.doc?.trim()).isEqualTo("Contains a class @TsName annotations")

    assertThat(tsClass.functions).hasSize(2)

    assertThat(tsClass.properties).hasSize(1)
    tsClass.properties[0].let { property ->
      assertThat(property.name).isEqualTo("valueFromAnnotation")
      assertThat(property.type).isEqualTo(TsType.String)
    }

    tsClass.constructors[0].let { constructor ->
      assertThat(constructor.parameters).hasSize(1)
      //assertThat(constructor.parameters[0].name).isEqualTo("valueFromAnnotation") //TODO how to fix the parameter name?
      assertThat(constructor.parameters[0].name).isEqualTo("value")
      assertThat(constructor.parameters[0].type).isEqualTo(TsType.String)
    }
  }

  @Test
  fun testSealedInterface() {
    val collectingContext = executeCollect(listOf(testSealedInterfaceFile))

    assertThat(collectingContext.interfaces()).hasSize(1)
    assertThat(collectingContext.classes()).hasSize(2)
    assertThat(collectingContext.valueClasses()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)

    val tsInterface = collectingContext.interfaces()[0]

    assertThat(tsInterface.name).isEqualTo("ExampleSealedInterface")
    assertThat(tsInterface.doc?.trim()).isEqualTo("This is a sealed interface")
    assertThat(tsInterface.superTypes).isEmpty()
    assertThat(tsInterface.typeParameters).isEmpty()
    assertThat(tsInterface.properties).hasSize(1)

    collectingContext.interfaces().first().let { sealedInterface ->
      assertThat(sealedInterface.name).isEqualTo("ExampleSealedInterface")
    }

    collectingContext.classes()[0].let { sealedClass1 ->
      assertThat(sealedClass1.name).isEqualTo("SealedClass1")
      assertThat(sealedClass1.superTypes).hasSize(1)
      assertThat(sealedClass1.typeParameters).isEmpty()
      assertThat(sealedClass1.properties).hasSize(1)
    }
    collectingContext.classes()[1].let { sealedClass2 ->
      assertThat(sealedClass2.name).isEqualTo("SealedClass2")
      assertThat(sealedClass2.superTypes).hasSize(1)
      assertThat(sealedClass2.typeParameters).isEmpty()
      assertThat(sealedClass2.properties).hasSize(1)
    }

    //TODO find sealed classes references!
  }

  @Test
  fun testValueClass() {
    val collectingContext = executeCollect(listOf(testValueClassFile))

    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)
    assertThat(collectingContext.valueClasses()).hasSize(1)

    val tsValueClass = collectingContext.valueClasses().first()
    assertThat(tsValueClass).isNotNull()

    assertThat(tsValueClass.name).isEqualTo("ExampleValueClass")
    assertThat(tsValueClass.doc?.trim()).isEqualTo("This is a value class")

    assertThat(tsValueClass.property.name).isEqualTo("value")
    assertThat(tsValueClass.property.type).isEqualTo(TsType.String)
  }

  @Test
  fun testCollectObject() {
    //We only compile the one file - therefore, the origin of the other types is "KOTLIN_LIB"
    KotlinModulesGuesser.registerClass2ProjectMapping(
      Class2ProjectMapping(
        mapOf("it.neckar.ksp.ts.generation.sample.ExampleTestEnum" to "foobar-module"),
      )
    )

    assertThat(ExampleTestObject::class).isNotNull()

    val collectingContext = executeCollect(listOf(testObjectFile))

    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(1)

    val tsObject = collectingContext.constObjects().first()

    assertThat(tsObject.name).isEqualTo("ExampleTestObject")
    assertThat(tsObject.doc?.trim()).isEqualTo("")
    assertThat(tsObject.properties).hasSize(3)

    tsObject.properties[0].let { property ->
      assertThat(property.name).isEqualTo("Prop1")
      assertThat(property.type).isEqualTo(TsType.String)
    }

    tsObject.properties[1].let { property ->
      assertThat(property.name).isEqualTo("prop2")
      assertThat(property.type).isEqualTo(
        TsType.defaultClass(
          "ExampleId",
          namespace = TsNamespace("ExampleTestObject"),
          importedFrom = TsModuleReference.SameFile
        )
      )
    }

    tsObject.properties[2].let { property ->
      assertThat(property.name).isEqualTo("arrayProp")
      assertThat(property.type).isEqualTo(
        TsType.defaultClass(
          "ExampleTestEnum",
          isArray = true,
          importedFrom = TsModuleReference.ProjectModuleReference("foobar-module")
        )
      )
    }

    assertThat(tsObject.functions).hasSize(1)

    tsObject.functions[0].let { function ->
      assertThat(function.name).isEqualTo("function1")
      assertThat(function.returnType).isEqualTo(TsType.String(isArray = true))
      assertThat(function.parameters).hasSize(1)

      function.parameters[0].let { parameter ->
        assertThat(parameter.name).isEqualTo("param1")
        assertThat(parameter.type).isEqualTo(TsType.String)
        assertThat(parameter.isOptional).isEqualTo(false)
      }
    }

    tsObject.collectTypes().toList().let { typesToImport ->
      assertThat(typesToImport).hasSize(4)
      assertThat(typesToImport[0]).isEqualTo(TsType.String)

      assertThat(typesToImport[1].name).isEqualTo("ExampleId")

      assertThat(typesToImport[2]).isEqualTo(
        TsType.defaultClass(
          "ExampleTestEnum",
          isArray = true,
          importedFrom = TsModuleReference.ProjectModuleReference("foobar-module")
        )
      )
      assertThat(typesToImport[3]).isEqualTo(TsType.String(isArray = true))
    }


    val writer = StringWriter()
    ConstObjectTsGenerator().generateTsFile(tsObject, writer, "neckarIT-foobar-module")

    assertThat(writer.toString()).isEqualIgnoringWhitespacesEmptyLines(
      //language=TypeScript
      """
        import { ExampleTestObject as _ExampleTestObject } from './neckarIT-foobar-module';
        import { ExampleTestObject } from "./neckarIT-foobar-module";
        import { ExampleTestEnum } from "./neckarIT-foobar-module";

        const _instance = _ExampleTestObject.getInstance();
        export const ExampleTestObject: {
          Prop1: string,
          prop2: ExampleTestObject.ExampleId,
          arrayProp: ExampleTestEnum[],
          function1: (param1: string) => string[],
        } = {
          Prop1: _instance.Prop1,
          prop2: _instance.prop2,
          arrayProp: _instance.arrayProp,
          function1: (param1: string): string[] => _instance.function1(param1),
        };
      """.trimIndent()
    )
  }

  @Test
  fun testCollectEnum() {
    val collectingContext = executeCollect(listOf(testEnumFile))

    assertThat(collectingContext.enums()).hasSize(1)
    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)

    val tsEnum = collectingContext.enums().first()
    assertThat(tsEnum.name).isEqualTo("ExampleTestEnum")

    assertThat(tsEnum.properties).hasSize(1)
    tsEnum.properties[0].let { property ->
      assertThat(property.name).isEqualTo("testVal")
      assertThat(property.type).isEqualTo(TsType.String)
    }

    assertThat(tsEnum.functions).hasSize(1)
    tsEnum.functions[0].let { function ->
      assertThat(function.name).isEqualTo("foo")
      assertThat(function.returnType).isEqualTo(TsType.Void)
      assertThat(function.parameters).hasSize(0)
    }

    assertThat(tsEnum.values).containsExactly(
      TsEnumValue("Foo"),
      TsEnumValue("Bar"),
      TsEnumValue("Baz"),
    )
  }

  @Test
  fun testCollectClass() {
    //First instantiate the class to verify it exists
    assertThat(ExampleTestClass<String>("doo", emptyList())).isNotNull()
    assertThat(ExampleTestClass<String?>("doo", emptyList())).isNotNull()

    //We only compile the class - therefore, the interface origin is "KOTLIN_LIB"
    KotlinModulesGuesser.registerClass2ProjectMapping(
      Class2ProjectMapping(
        mapOf("it.neckar.ksp.ts.generation.sample.ExampleTestInterface" to "foobar-module"),
      )
    )

    val collectingContext = executeCollect(listOf(testClassFile))

    assertThat(collectingContext.classes()).hasSize(1)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(0)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)

    val tsClass = collectingContext.classes().first()

    assertThat(tsClass.name).isEqualTo("ExampleTestClass")
    assertThat(tsClass.doc?.trim()).isEqualTo("Is read as a plain source by unit test".trimIndent())

    assertThat(tsClass.typeParameters.map { it.name }).isEqualTo(listOf("T"))
    assertThat(tsClass.typeParameters.flatMap { it.bounds }).isEqualTo(emptyList())

    assertThat(tsClass.properties).hasSize(5)

    tsClass.properties.first().let { property ->
      assertThat(property.name).isEqualTo("doo")
      assertThat(property.type).isEqualTo(TsType.String)
      assertThat(property.doc?.trim()).isEqualTo("This is the doo property".trim())
    }

    tsClass.properties[4].let { property ->
      assertThat(property.name).isEqualTo("myProp")
      assertThat(property.type).isEqualTo(TsType.String.asNullable())
    }

    tsClass.functions[0].let { function ->
      assertThat(function.name).isEqualTo("url")
      assertThat(function.doc?.trim()).isEqualTo("Returns a URL pattern".trim())
      assertThat(function.returnType).isEqualTo(TsType(name = "Relative0", namespace = TsNamespace("UrlPattern"), classKind = TsType.ClassKind.Class, importedFrom = TsModuleReference.ProjectModuleReference("neckarIT-internal-open-commons-kotlin-lang")))
      assertThat(function.parameters).hasSize(0)
    }

    tsClass.functions[1].let { function ->
      assertThat(function.name).isEqualTo("fooBar")
      assertThat(function.doc?.trim()).isEqualTo("The foo function doc".trim())
      assertThat(function.returnType).isEqualTo(
        TsType(
          name = "ExampleTestClass",
          isNullable = true,
          isArray = true,
          typeArguments = listOf(TsType.typeParameter("T")),
          classKind = TsType.ClassKind.Class,
          importedFrom = TsModuleReference.SameFile,
        )
      )
      assertThat(function.parameters).hasSize(2)

      function.parameters[0].let { parameter ->
        assertThat(parameter.name).isEqualTo("bar")
        assertThat(parameter.type).isEqualTo(TsType.String)
        assertThat(parameter.isOptional).isEqualTo(false)
      }

      function.parameters[1].let { parameter ->
        assertThat(parameter.name).isEqualTo("baz")
        assertThat(parameter.type).isEqualTo(TsType.Number.withNullable(true))
        assertThat(parameter.isOptional).isEqualTo(false)
      }
    }

    tsClass.functions[2].let { function ->
      assertThat(function.name).isEqualTo("fooBar2")
      assertThat(function.returnType).isEqualTo(
        TsType(
          name = "ExampleTestClass",
          isNullable = true,
          isArray = true,
          typeArguments = listOf(TsType.typeParameter("T")),
          classKind = TsType.ClassKind.Class,
          importedFrom = TsModuleReference.SameFile,
        )
      )
      assertThat(function.parameters).hasSize(2)

      function.parameters[0].let { parameter ->
        assertThat(parameter.name).isEqualTo("bar")
        assertThat(parameter.type).isEqualTo(TsType.String)
        assertThat(parameter.isOptional).isEqualTo(true)
      }

      function.parameters[1].let { parameter ->
        assertThat(parameter.name).isEqualTo("baz")
        assertThat(parameter.type).isEqualTo(TsType.Number.withNullable(true))
        assertThat(parameter.isOptional).isEqualTo(true)
      }
    }

    assertThat(tsClass.superTypes).hasSize(1)

    tsClass.superTypes[0].let { superType ->
      assertThat(superType.name).isEqualTo("ExampleTestInterface")
      assertThat(superType.classKind).isEqualTo(TsType.ClassKind.Interface)
      assertThat(superType.typeArguments).hasSize(2)
      assertThat(superType.typeArguments[0]).isEqualTo(TsType.String)
      assertThat(superType.typeArguments[1]).isEqualTo(TsType.Number.asNullable())
    }
  }

  @Test
  fun testCollectInterface() {
    //We only compile the one file - therefore, the origin of the other types is "KOTLIN_LIB"
    KotlinModulesGuesser.registerClass2ProjectMapping(
      Class2ProjectMapping(
        mapOf("it.neckar.ksp.ts.generation.sample.ExampleTestClass" to "foobar-module"),
      )
    )


    val collectingContext = executeCollect(listOf(testInterfaceFile))

    assertThat(collectingContext.classes()).hasSize(0)
    assertThat(collectingContext.enums()).hasSize(0)
    assertThat(collectingContext.interfaces()).hasSize(1)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(0)

    val tsInterface = collectingContext.interfaces().first()

    assertThat(tsInterface.name).isEqualTo("ExampleTestInterface")
    assertThat(tsInterface.doc?.trim()).isEqualTo("")

    assertThat(tsInterface.typeParameters.map { it.name }).isEqualTo(listOf("T", "S"))
    assertThat(tsInterface.typeParameters.size).isEqualTo(2)
    assertThat(tsInterface.typeParameters[0].bounds).isEmpty() //we skip Any
    assertThat(tsInterface.typeParameters[1].bounds).isEmpty()

    assertThat(tsInterface.properties).hasSize(2)

    tsInterface.properties[0].let { property ->
      assertThat(property.name).isEqualTo("myProp")
      assertThat(property.type).isEqualTo(TsType.String.asNullable())
    }

    tsInterface.properties[1].let { property ->
      assertThat(property.name).isEqualTo("myPropWithDefaultImpl")
      assertThat(property.type).isEqualTo(TsType.String)
    }

    tsInterface.functions[0].let { function ->
      assertThat(function.name).isEqualTo("fooBar")
      assertThat(function.doc?.trim()).isEqualTo("The foo function doc".trim())
      assertThat(function.returnType).isEqualTo(
        TsType(name = "ExampleTestClass", isNullable = true, isArray = true, typeArguments = listOf(TsType.typeParameter("T")), classKind = TsType.ClassKind.Class, importedFrom = TsModuleReference.ProjectModuleReference("foobar-module"))
      )
      assertThat(function.parameters).hasSize(2)

      function.parameters[0].let { parameter ->
        assertThat(parameter.name).isEqualTo("bar")
        assertThat(parameter.type).isEqualTo(TsType.String)
        assertThat(parameter.isOptional).isEqualTo(false)
      }

      function.parameters[1].let { parameter ->
        assertThat(parameter.name).isEqualTo("baz")
        assertThat(parameter.type).isEqualTo(TsType.Number.withNullable(true))
        assertThat(parameter.isOptional).isEqualTo(false)
      }
    }

    tsInterface.functions[1].let { function ->
      assertThat(function.name).isEqualTo("withImpl")
      assertThat(function.returnType).isEqualTo(TsType.Void)
      assertThat(function.parameters).hasSize(1)

      function.parameters[0].let { parameter ->
        assertThat(parameter.name).isEqualTo("value")
        assertThat(parameter.type).isEqualTo(TsType.typeParameter("S"))
        assertThat(parameter.isOptional).isFalse()
      }
    }

    assertThat(tsInterface.superTypes).hasSize(0) //do not export comparable!
  }

  @Test
  fun testCollectAll() {
    val collectingContext = executeCollect(allSourceFiles)

    assertThat(collectingContext.enums()).hasSize(1)
    assertThat(collectingContext.classes()).hasSize(11)
    assertThat(collectingContext.interfaces()).hasSize(3)
    assertThat(collectingContext.topLevelFunctions()).hasSize(0)
    assertThat(collectingContext.constObjects()).hasSize(1)
    assertThat(collectingContext.collectTopLevelElements()).hasSize(15)
    assertThat(collectingContext.collectElementsWithNamespace()).hasSize(5)
  }

  private fun executeCollect(sourceFiles: List<File>): CollectingContext {
    val symbolProcessorProvider = TypeScriptDefinitionGenerationProcessorProvider()

    KspTestUtils.compileSourceFiles(
      sourceFiles = sourceFiles,
      symbolProcessorProvider = symbolProcessorProvider,
      options = unitTestOptions,
    )

    return symbolProcessorProvider.collectingContext
  }
}
