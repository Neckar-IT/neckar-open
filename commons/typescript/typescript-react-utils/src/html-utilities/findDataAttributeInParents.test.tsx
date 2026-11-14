/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { findDataAttributeInParents } from "./findDataAttributeInParents";

/**
 * Tests
 */

describe("findDataAttributeInParents", () => {
  it("should find data attribute from a parent element", () => {
    const parent = window.document.createElement("div");
    parent.setAttribute("data-test-id", "123");

    const child = window.document.createElement("span");
    parent.appendChild(child);

    const result = findDataAttributeInParents(child, "data-test-id");
    expect(result).toBe("123");
  });

  it("should return undefined if attribute is not found", () => {
    const parent = window.document.createElement("div");
    const child = window.document.createElement("span");
    parent.appendChild(child);

    const result = findDataAttributeInParents(child, "data-not-present");
    expect(result).toBeUndefined();
  });

  it("should find closest attribute when multiple ancestors have it", () => {
    const grandParent = window.document.createElement("div");
    grandParent.setAttribute("data-level", "grandparent");

    const parent = window.document.createElement("div");
    parent.setAttribute("data-level", "parent");

    const child = window.document.createElement("span");

    grandParent.appendChild(parent);
    parent.appendChild(child);

    const result = findDataAttributeInParents(child, "data-level");
    expect(result).toBe("parent"); // should pick the closest one
  });
});
