import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { PageSectionHeading } from "./PageSectionHeading";

describe("PageSectionHeading", () => {
  it("renders the primary level as h1 with 24 px below sm and 30 px from sm", () => {
    render(<PageSectionHeading level="primary">Jane Doe</PageSectionHeading>);

    const heading = screen.getByRole("heading", { level: 1, name: "Jane Doe" });
    expect(heading).toHaveClass("text-2xl font-bold sm:text-3xl", { exact: true });
  });

  it("renders the secondary level as h2 with 20 px below sm and 24 px from sm", () => {
    render(<PageSectionHeading level="secondary">Books</PageSectionHeading>);

    const heading = screen.getByRole("heading", { level: 2, name: "Books" });
    expect(heading).toHaveClass("text-xl font-semibold sm:text-2xl", { exact: true });
  });
});
