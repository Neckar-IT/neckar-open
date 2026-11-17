import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { StorybookColorMode } from "./storybookPreviewParameters";
import { useStorybookColorModeClass } from "./useStorybookColorModeClass";

describe("useStorybookColorModeClass", () => {
  it("replaces the class of the previous color mode on the body", () => {
    const { rerender } = renderHook(({ colorMode }) => useStorybookColorModeClass(colorMode), {
      initialProps: { colorMode: "dark" as StorybookColorMode },
    });
    rerender({ colorMode: "light" });

    expect([...window.document.body.classList]).toEqual(["light"]);
  });
});
