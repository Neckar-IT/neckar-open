/**
 * Find the value of a data attribute in the parent elements of the given element.
 */

export function findDataAttributeInParents(element: HTMLElement, dataAttribute: string): string | undefined {
  return element.closest(`[${dataAttribute}]`)?.getAttribute(dataAttribute) ?? undefined;
}
