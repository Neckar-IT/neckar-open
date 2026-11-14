#!/usr/bin/env kscript
//DEPS com.google.guava:guava:28.1-jre

import com.google.common.io.Files
import java.io.File


println("Cloning kds repository")

val projectDir = File(".")
val buildDir = File(projectDir, "build")
val kdsImportDir = File(buildDir, "kds-import")
val kdsImportSourcesDir = File(kdsImportDir, "korge-korlibs/korlibs-datastructure/")
val commonMainKotlinDir = File(kdsImportSourcesDir, "src")


kdsImportDir.mkdirs()

val process = Runtime.getRuntime().exec(arrayOf("git", "clone", "https://github.com/korlibs/korge-korlibs.git"), arrayOf(), kdsImportDir)
println("Result: ${process.waitFor()}")

println(process.inputStream.readBytes().toString(Charsets.UTF_8))
println(process.errorStream.readBytes().toString(Charsets.UTF_8))


val targetPackageDir = File(projectDir, "src/commonMain/kotlin/it/neckar/open/collections")


val sourceFilesToCopy = listOf(
  "korlibs/datastructure/_Extensions.kt",
  //"korlibs/datastructure/internal/internal.kt",
  "korlibs/datastructure/Queue.kt",
  "korlibs/datastructure/ArrayDeque.kt",
  "korlibs/datastructure/IteratorExt.kt",
  "korlibs/datastructure/RingBuffer.kt",
  "korlibs/datastructure/Deque.kt",
  "korlibs/datastructure/BitSet.kt",
  "korlibs/datastructure/CacheMap.kt",
  "korlibs/datastructure/Pool.kt",
  "korlibs/datastructure/PriorityQueue.kt",
  "korlibs/datastructure/Stack.kt",
  "korlibs/datastructure/IntMap.kt",
  "korlibs/datastructure/ArrayList.kt",
  "korlibs/datastructure/ArrayListExt.kt",
  "korlibs/datastructure/Array2.kt",
  "korlibs/datastructure/_GenericSort.kt"
)

sourceFilesToCopy.forEach {
  println("Copy $it")

  val sourceFile = File(commonMainKotlinDir, it)
  val targetFile = File(targetPackageDir, sourceFile.name)


  val sourceContentOriginal: String = Files.asByteSource(sourceFile).read().toString(Charsets.UTF_8)
  val sourceContent: String = sourceContentOriginal.processForCedarsoft()

  println("Copying:")
  println("\tFROM ${sourceFile.absolutePath}")
  println("\tTO   ${targetFile.absolutePath}")

  Files.asByteSink(targetFile).write(sourceContent.toByteArray(Charsets.UTF_8))
}

fun String.processForCedarsoft(): String {
  return fixPackages()
}

fun String.fixPackages(): String {
  return replace("korlibs.datastructure.internal", "it.neckar.open.collections")
    .replace("korlibs.datastructure", "it.neckar.open.collections")
}
