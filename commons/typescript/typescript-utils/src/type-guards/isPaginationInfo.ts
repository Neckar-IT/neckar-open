/**
 * Imports
 */

import { type PaginationInfo } from "../../build/generated/open-api/openapi-types.schemas";

/**
 * Type Guard to check if an object is a PaginationInfo
 */

export function isPaginationInfo(obj: unknown): obj is PaginationInfo {
  if (typeof obj !== "object" || obj === null) return false;

  const o = obj as Record<string, unknown>;

  return (
    typeof o.pageIndex === "number" &&
    typeof o.pageSize === "number" &&
    typeof o.pageCount === "number" &&
    typeof o.filteredElementsCount === "number" &&
    typeof o.totalElementsCount === "number"
  );
}
