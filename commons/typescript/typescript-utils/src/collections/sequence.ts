import { Step } from "./step";

/**
 * A sequence of elements that can be filtered, mapped, and searched.
 */
export interface Sequence<T> {
  /**
   * Filters all elements using the given predicate.
   * @see Array.prototype.filter
   */
  filter(predicate: (value: T, index: number) => boolean): Sequence<T>;

  /**
   * Maps each element using the given transform.
   * @see Array.prototype.map
   */
  map<U>(transform: (value: T, index: number) => U): Sequence<U>;

  /**
   * Finds a value in the sequence.
   * @see Array.prototype.find
   */
  find(predicate: (value: T, index: number) => boolean): T | undefined;

  /**
   * Finds a value in the sequence and returns its index.
   * @see Array.prototype.findIndex
   */
  findIndex(predicate: (value: T, index: number) => boolean): number;

  /**
   * Tests whether all elements in the sequence pass the test implemented by the provided function.
   * @see Array.prototype.every
   */
  every(predicate: (value: T, index: number) => boolean): boolean;

  /**
   * Tests whether at least one element in the sequence passes the test implemented by the provided function.
   * @see Array.prototype.some
   */
  some(predicate: (value: T, index: number) => boolean): boolean;

  /**
   * Returns a new array containing all elements in the sequence.
   */
  toArray(): T[];
}

export function asSequence<T>(source: T[]): Sequence<T> {
  return new Step(source);
}
