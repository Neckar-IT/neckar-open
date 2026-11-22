// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import "./vitest-jsdom-setup.js";

describe("vitest-jsdom-setup", () => {
  it("stubs ResizeObserver", () => {
    expect(() => new window.ResizeObserver(() => {}).observe(document.body)).not.toThrow();
  });

  it("stubs scrollIntoView and the pointer capture", () => {
    expect(() => document.body.hasPointerCapture(1)).not.toThrow();
    expect(() => document.body.releasePointerCapture(1)).not.toThrow();
    expect(() => document.body.scrollIntoView()).not.toThrow();
  });
});
