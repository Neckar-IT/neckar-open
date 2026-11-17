/**
 * Generic Good Response Interface
 */
export type ExtractGoodProps<T = unknown> = T extends { type: "good"; data: infer D; paginationInfo: infer P }
  ? { data: D; paginationInfo: P }
  : T extends { type: "good"; data: infer D }
    ? { data: D }
    : never;

/**
 * Type Guard to check if an object is a GoodResponse
 */
export function isGoodResponse<T = unknown>(obj: unknown): obj is ExtractGoodProps<T> {
  return typeof obj === "object" && obj !== null && "type" in obj && obj.type === "good" && "data" in obj;
}
