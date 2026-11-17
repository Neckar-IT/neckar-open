/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Filter an array of strings or nested objects by a search term
 *
 * @param array The array to filter
 * @param searchTerm The search term to filter by
 */

export function filterArrayBySearchTerm<T extends string | Record<string, any>>(array: T[], searchTerm: string): T[] {
  const lowerCaseSearchTerm = searchTerm.toLowerCase();

  const includesSearchTerm = (item: any): boolean => {
    // directly check for string values
    if (typeof item === "string") {
      return item.toLowerCase().includes(lowerCaseSearchTerm);
    }

    // recursively check if any value in the (possibley nested) object contains the search term
    // typeof null === "object", so guard against null before calling Object.values (which throws on null)
    if (item !== null && typeof item === "object") {
      return Object.values(item).some((value) => includesSearchTerm(value));
    }

    // ignore any other types
    return false;
  };

  return array.filter(includesSearchTerm);
}
