/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ErrorPageLayout, ErrorPageViewport } from "./ErrorPageLayout";

/**
 * Tests
 */

describe("ErrorPageLayout", () => {
  it("caps the width of the column every section sits in", () => {
    render(
      <ErrorPageLayout>
        <div data-testid="section">section</div>
      </ErrorPageLayout>,
    );

    const column = screen.getByTestId("section").parentElement;
    expect(column).toHaveClass("w-full", "max-w-screen-md");
  });

  it("keeps the content off the window edges", () => {
    render(
      <ErrorPageViewport>
        <div data-testid="content">content</div>
      </ErrorPageViewport>,
    );

    expect(screen.getByTestId("content").parentElement).toHaveClass("px-6", "py-12");
  });
});
