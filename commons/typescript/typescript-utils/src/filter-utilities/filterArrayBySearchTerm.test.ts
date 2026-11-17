/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { filterArrayBySearchTerm } from "./filterArrayBySearchTerm";

/**
 * Tests
 */

describe("filterArrayBySearchTerm", () => {
  it("should filter an array of strings by a search term", () => {
    const fruits = ["apple", "banana", "cherry", "date", "elderberry", "pineapple"];
    const result = filterArrayBySearchTerm(fruits, "apple");
    expect(result).toEqual(["apple", "pineapple"]);
  });

  it("should filter an arrayof objects by a search term", () => {
    const people = [
      { firstName: "John", lastName: "Doe" },
      { firstName: "Jane", lastName: "Doe" },
      { firstName: "John", lastName: "Smith" },
      { firstName: "Michael", lastName: "Jackson" },
      { firstName: "Brian", lastName: "Taylor" },
    ];

    const result = filterArrayBySearchTerm(people, "ja");
    expect(result).toEqual([
      { firstName: "Jane", lastName: "Doe" },
      { firstName: "Michael", lastName: "Jackson" },
    ]);
  });

  it("should filter an array of nested objects by a search term", () => {
    const fruits = [
      { name: "apple", attributes: { color: "red", taste: "sweet" } },
      { name: "banana", attributes: { color: "yellow", taste: "sweet" } },
      { name: "cherry", attributes: { color: "red", taste: "sour" } },
      { name: "date", attributes: { color: "brown", taste: "sweet" } },
      { name: "elderberry", attributes: { color: "black", taste: "bitter" } },
      { name: "pineapple", attributes: { color: "yellow", taste: "sweet" } },
    ];

    const result = filterArrayBySearchTerm(fruits, "red");
    expect(result).toEqual([
      { name: "apple", attributes: { color: "red", taste: "sweet" } },
      { name: "cherry", attributes: { color: "red", taste: "sour" } },
    ]);
  });

  it("should filter an array of objects where some properties are arrays", () => {
    const people = [
      { name: "John", hobbies: ["reading", "swimming", "hiking"] },
      { name: "Jane", hobbies: ["reading", "swimming", "biking"] },
      { name: "Michael", hobbies: ["reading", "swimming", "running"] },
      { name: "Brian", hobbies: ["reading", "climbing", "hiking"] },
    ];

    const result = filterArrayBySearchTerm(people, "hik");
    expect(result).toEqual([
      { name: "John", hobbies: ["reading", "swimming", "hiking"] },
      { name: "Brian", hobbies: ["reading", "climbing", "hiking"] },
    ]);
  });

  it("should not crash on objects with null property values", () => {
    const people = [
      { name: "apple", note: null },
      { name: "banana", note: "yellow" },
    ];

    expect(filterArrayBySearchTerm(people, "yellow")).toEqual([{ name: "banana", note: "yellow" }]);
    expect(filterArrayBySearchTerm(people, "xxx")).toEqual([]);
  });
});
