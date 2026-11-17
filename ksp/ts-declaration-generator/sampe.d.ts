declare namespace Example {
  /**
   * Entspricht grob dem Konzept von CharSequence in Kotlin.
   * Diese Definition ist vereinfacht; bei Bedarf können weitere Methoden ergänzt werden.
   */
  interface CharSequence {
    length: number;

    charAt(index: number): string;

    substring(start: number, end?: number): string;
  }

  /**
   * Entspricht dem Konzept von Comparable aus Kotlin.
   * Eine Instanz sollte mithilfe von compareTo ein anderes Objekt
   * mit demselben Typ vergleichen können.
   */
  interface Comparable<T> {
    /**
     * Vergleicht die aktuelle Instanz mit 'other'.
     * @returns Ein negativer Wert, falls this < other,
     *          0, falls this == other,
     *          ein positiver Wert, falls this > other.
     */
    compareTo(other: T): number;
  }

  /**
   * Repräsentation der Klasse TextComparator aus Kotlin.
   * Der generische Typ-Parameter T muss sowohl ein CharSequence als auch ein Comparable sein.
   */
  class TextComparator<T extends CharSequence & Comparable<T>> {
    findLongestOrMax(a: T, b: T): T;

    printResult(a: T, b: T): void;
  }
}

interface MyInterface<T, U extends string & (Foo | null)> extends MySuperType, MyOtherSuperType {
}


interface Foo {

}

interface MySuperType {

}

interface MyOtherSuperType {

}
