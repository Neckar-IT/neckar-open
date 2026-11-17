/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { TextLines } from "./TextLines";

/**
 * Tests
 */

describe("TextLines", () => {
  it("shows one line per entry", () => {
    render(<TextLines texts={["first", "second"]} />);

    expect(screen.getByText("first")).toBeInTheDocument();
    expect(screen.getByText("second")).toBeInTheDocument();
  });

  it("renders nothing for an empty list", () => {
    const { container } = render(<TextLines texts={[]} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing for an absent list", () => {
    const { container } = render(<TextLines texts={null} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("shows both entries where two hold the same text", () => {
    render(<TextLines texts={["same", "same"]} />);

    expect(screen.getAllByText("same")).toHaveLength(2);
  });
});
