/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { HighlightSource } from "./HighlightContext";
import { HighlightText } from "./HighlightText";

/**
 * Tests
 *
 * `react-highlight-words` wraps matched terms in `<mark>` elements, which the assertions query.
 */

describe("HighlightText", () => {
  const marks = (container: HTMLElement): string[] => Array.from(container.querySelectorAll("mark")).map((mark) => mark.textContent ?? "");

  it("renders the text without marks when no search term is given", () => {
    const { container } = render(<HighlightText text="hello world" />);

    expect(screen.getByText("hello world")).toBeInTheDocument();
    expect(marks(container)).toEqual([]);
  });

  it("highlights a matching search term", () => {
    const { container } = render(<HighlightText text="hello world" search="world" />);

    expect(marks(container)).toEqual(["world"]);
  });

  it("drops search tokens shorter than the default token limit of 2", () => {
    const { container } = render(<HighlightText text="a ab abc" search="a" />);

    expect(marks(container)).toEqual([]);
  });

  it("highlights only the tokens that meet the token limit", () => {
    const { container } = render(<HighlightText text="a world" search={["a", "world"]} />);

    expect(marks(container)).toEqual(["world"]);
  });

  it("honors a custom token limit", () => {
    const { container } = render(<HighlightText text="ab abc" search={["ab", "abc"]} highlightTokenLimit={3} />);

    expect(marks(container)).toEqual(["abc"]);
  });

  it("falls back to the enclosing HighlightSource when no search prop is set", () => {
    const { container } = render(
      <HighlightSource tokens="world">
        <HighlightText text="hello world" />
      </HighlightSource>,
    );

    expect(marks(container)).toEqual(["world"]);
  });

  it("lets an explicit search prop override the HighlightSource", () => {
    const { container } = render(
      <HighlightSource tokens="world">
        <HighlightText text="hello world" search="hello" />
      </HighlightSource>,
    );

    expect(marks(container)).toEqual(["hello"]);
  });

  it("renders the no-value fallback when the text is null", () => {
    render(<HighlightText text={null} fallback="missing" />);

    expect(screen.getByText("missing")).toBeInTheDocument();
  });

  it("sets the full text as the title attribute for hover tooltips", () => {
    const { container } = render(<HighlightText text="hello world" search="world" />);

    expect(container.querySelector('[title="hello world"]')).toBeInTheDocument();
  });

  it("marks a multiline text and keeps its search hits", () => {
    const { container } = render(<HighlightText text={"hello\nworld"} search="world" multiline />);

    expect(container.querySelector("[data-multiline-text]")).toHaveClass("whitespace-pre-wrap");
    expect(marks(container)).toEqual(["world"]);
  });

  it("leaves a text without multiline unmarked", () => {
    const { container } = render(<HighlightText text="hello world" />);

    expect(container.querySelector("[data-multiline-text]")).toBeNull();
  });
});
