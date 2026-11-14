/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { MemoryRouter } from "react-router";
import { describe, expect, it } from "vitest";
import { TimelineNav } from "./TimelineNav";
import type { TimelineNavData } from "./TimelineNavTypes";

/**
 * Tests
 */

const data: TimelineNavData = [
  { title: "Setup", status: "completed", leafs: [{ type: "leaf", title: "Step A", status: "completed", href: "/a" }] },
  { title: "Review", status: "in-progress", leafs: [{ type: "leaf", title: "Step B", status: "not-started", href: "/b" }] },
];

describe("TimelineNav", () => {
  it("renders a section header for each parent", () => {
    render(
      <MemoryRouter initialEntries={["/other"]}>
        <TimelineNav className={undefined} data={data} />
      </MemoryRouter>,
    );

    expect(screen.getByRole("button", { name: /Setup/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Review/ })).toBeInTheDocument();
  });

  it("expands only the section that contains the active route", () => {
    render(
      <MemoryRouter initialEntries={["/b"]}>
        <TimelineNav className={undefined} data={data} />
      </MemoryRouter>,
    );

    // "/b" lives in the second parent ("Review"), so its leaf shows and the first stays collapsed.
    expect(screen.getByText("Step B")).toBeInTheDocument();
    expect(screen.queryByText("Step A")).not.toBeInTheDocument();
  });
});
