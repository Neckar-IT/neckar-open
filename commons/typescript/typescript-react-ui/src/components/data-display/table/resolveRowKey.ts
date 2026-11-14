/**
 * Resolves a unique key for a given row item.
 * In most cases every item has an id property, so this id is used.
 * Uses the index from the data array as fallback.
 */
export function resolveRowKey(item: unknown, index: number): string {
  if (typeof item === "object" && item !== null && "id" in item && typeof item.id === "string") {
    return item.id;
  }

  return String(index);
}
