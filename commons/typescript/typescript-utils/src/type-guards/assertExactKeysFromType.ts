/**
 * Compile-time helper that asserts an object has exactly the keys of `T`.
 *
 * Ensures there are no missing or extra keys (excess property checks).
 * Optional properties on `T` may be present with the value `undefined`.
 *
 * Usage example:
 * const v = assertExactKeysFromType<{ a: number; b?: string }>()({ a: 1, b: undefined });
 */
export function assertExactKeysFromType<T>() {
  return <U extends { [K in keyof T]-?: unknown } & Record<Exclude<keyof U, keyof T>, never>>(obj: U) => obj as unknown as T;
}
