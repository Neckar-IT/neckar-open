import type { Primitive } from "../types";

/**
 * Returns the indices of duplicate elements in an array
 *
 * @param array The array to check for duplicates
 * @param selector Optional function to select the value to check for duplicates. if not provided the elements themselves are used.
 * @returns An array of indices of duplicate elements
 */
export function getDuplicateIndicesBy<T>(array: T[], selector?: (item: T) => Primitive): number[] {
  const map = new Map<T | Primitive, number[]>();

  array.forEach((item, index) => {
    const key = selector ? selector(item) : item;
    const indices = map.get(key) ?? [];
    indices.push(index);
    map.set(key, indices);
  });

  return [...map.values()].filter((indices) => indices.length > 1).flat();
}
