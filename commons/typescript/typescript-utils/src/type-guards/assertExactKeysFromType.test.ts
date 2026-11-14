import { describe, expect, it } from "vitest";
import { assertExactKeysFromType } from "./assertExactKeysFromType";

describe("assertExactKeysFromType (compile-time helper)", () => {
  it("returns the same object at runtime", () => {
    const obj = { a: 1, b: "x" };
    const out = assertExactKeysFromType<typeof obj>()(obj);
    expect(out).toBe(obj);
  });

  it("accepts optional properties as undefined", () => {
    type T = { a: number; b?: string };
    const v = assertExactKeysFromType<T>()({ a: 1, b: undefined });
    expect(v.a).toBe(1);
  });

  // The following lines are compile-time expectations validated by the TypeScript
  // compiler when running `tsc` / project build. They ensure the helper rejects
  // objects with missing or extra keys.
  type T = { a: number; b?: string };

  // extra property `c` should be rejected at compile time
  // @ts-expect-error extra property 'c' should be rejected
  assertExactKeysFromType<T>()({ a: 1, c: 2 } as unknown);

  // missing required property `a` should be rejected at compile time
  // @ts-expect-error missing required property 'a' should be rejected
  assertExactKeysFromType<T>()({ b: "x" } as unknown);

  // valid usages (compile-time)
  assertExactKeysFromType<T>()({ a: 1, b: "ok" });
});
