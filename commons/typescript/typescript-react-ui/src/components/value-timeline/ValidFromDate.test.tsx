import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ValidFromDate } from "./ValidFromDate";

describe("ValidFromDate", () => {
  it("renders the date without a time component", () => {
    render(<ValidFromDate isoInstant="2026-01-15T08:24:00Z" />);

    const rendered = screen.getByText(/2026/);
    expect(rendered).toHaveTextContent(/15|01/);
    expect(rendered.textContent).not.toMatch(/\d{2}:\d{2}/);
  });

  it("keeps the full date-and-time in a title tooltip for non-midnight instants", () => {
    render(<ValidFromDate isoInstant="2026-01-15T08:24:00Z" />);

    const rendered = screen.getByText(/2026/);
    expect(rendered.getAttribute("title")).toMatch(/\d{2}:\d{2}/);
  });

  it("omits the tooltip when the instant is local midnight", () => {
    render(<ValidFromDate isoInstant="2026-01-15T00:00:00" />);

    const rendered = screen.getByText(/2026/);
    expect(rendered.getAttribute("title")).toBeNull();
  });
});
