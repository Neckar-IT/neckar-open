package it.neckar.ksp.sample

/**
 * This class is read (as source) by the KSP processor in the unit test
 */
@ExportedInTest
class TestKClass {
  /**
   * The foo function doc
   */
  fun <T : Number?> foo(bar: String, baz: Int?): List<TestKClass?> {
    println("Hello, World!")
    return emptyList()
  }
}
