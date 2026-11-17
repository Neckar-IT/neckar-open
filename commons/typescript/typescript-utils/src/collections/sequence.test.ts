import { describe, expect, test } from "vitest";
import { asSequence } from "./sequence";

describe("sequence", () => {
  describe("filter", () => {
    test("filter", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .filter((i) => i > 2)
        .toArray();
      expect(arr).toEqual([3, 4, 5]);
    });

    test("multiple filters", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .filter((i) => i > 2)
        .filter((i) => i % 2 === 0)
        .toArray();
      expect(arr).toEqual([4]);
    });

    test("empty filter result", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .filter((i) => i > 2)
        .filter((i) => i < 2)
        .toArray();
      expect(arr).toEqual([]);
    });
  });

  describe("map", () => {
    test("map", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 2)
        .toArray();
      expect(arr).toEqual([2, 4, 6, 8, 10]);
    });

    test("multiple maps", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 2)
        .map((i) => i * 1.5)
        .toArray();
      expect(arr).toEqual([3, 6, 9, 12, 15]);
    });

    test("chain filter and map", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .filter((i) => i % 2 === 0)
        .map((i) => i * 1.5)
        .toArray();
      expect(arr).toEqual([3, 6]);
    });

    test("chain map and filter", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 3)
        .filter((i) => i % 2 === 0)
        .toArray();
      expect(arr).toEqual([6, 12]);
    });
  });

  describe("find", () => {
    test("successful find", () => {
      const found = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 3)
        .find((i) => i % 2 === 0);
      expect(found).toEqual(6);
    });

    test("failed find", () => {
      const found = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 3)
        .find((i) => i < 0);
      expect(found).toBeUndefined();
    });
  });

  describe("findIndex", () => {
    test("successful findIndex", () => {
      const found = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 3)
        .findIndex((i) => i % 2 === 0);
      expect(found).toEqual(1);
    });

    test("failed findIndex", () => {
      const found = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 3)
        .findIndex((i) => i < 0);
      expect(found).toEqual(-1);
    });
  });

  describe("every", () => {
    test("successful every", () => {
      const result = asSequence([1, 2, 3, 4, 5]).every((i) => i > 0);
      expect(result).toEqual(true);
    });

    test("failed every", () => {
      const result = asSequence([1, 2, 3, 4, 5]).every((i) => i > 1);
      expect(result).toEqual(false);
    });

    test("every with empty array", () => {
      const result = asSequence([]).every((i) => i > 0);
      expect(result).toEqual(true);
    });

    test("every with filter", () => {
      const result = asSequence([1, 2, 3, 4, 5])
        .filter((i) => i > 2)
        .every((i) => i > 2);
      expect(result).toEqual(true);
    });

    test("chained every", () => {
      const result = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 2)
        .filter((i) => i < 3)
        .every((i) => i < 3);
      expect(result).toEqual(true);
    });

    test("chained every with empty array", () => {
      const result = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 2)
        .filter((i) => i < 0)
        .every((i) => i < 3);
      expect(result).toEqual(true);
    });
  });

  describe("some", () => {
    test("successful some", () => {
      const result = asSequence([1, 2, 3, 4, 5]).some((i) => i === 3);
      expect(result).toEqual(true);
    });

    test("failed some", () => {
      const result = asSequence([1, 2, 3, 4, 5]).some((i) => i === 6);
      expect(result).toEqual(false);
    });

    test("some with empty array", () => {
      const result = asSequence([]).some((i) => i > 0);
      expect(result).toEqual(false);
    });

    test("chained some", () => {
      const result = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 2)
        .filter((i) => i < 3)
        .some((i) => i < 3);
      expect(result).toEqual(true);
    });

    test("chained some with empty array", () => {
      const result = asSequence([1, 2, 3, 4, 5])
        .map((i) => i * 2)
        .filter((i) => i < 0)
        .some((i) => i < 3);
      expect(result).toEqual(false);
    });
  });

  describe("chains of three or more operations", () => {
    test("three maps apply every operation in order", () => {
      const arr = asSequence([1])
        .map((x) => x + 1)
        .map((x) => x * 10)
        .map((x) => x + 2)
        .toArray();
      // ((1 + 1) * 10) + 2 === 22
      expect(arr).toEqual([22]);
    });

    test("three filters apply every operation", () => {
      const arr = asSequence([1, 2, 3, 4, 5])
        .filter((i) => i !== 4)
        .filter((i) => i > 1)
        .filter((i) => i > 2)
        .toArray();
      expect(arr).toEqual([3, 5]);
    });
  });
});
