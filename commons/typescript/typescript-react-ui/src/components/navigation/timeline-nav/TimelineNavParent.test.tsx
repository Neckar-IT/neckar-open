/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { describe, expect, it } from "vitest";
import { TimelineNavParent } from "./TimelineNavParent";
import type { TimelineNavParentType } from "./TimelineNavTypes";

/**
 * Tests
 *
 * A parent section is initially expanded when one of its leaves matches the current route, otherwise
 * collapsed. The collapsed state hides the leaves and can be toggled via the section header.
 */

const parent: TimelineNavParentType = {
  title: "Setup",
  status: "in-progress",
  leafs: [
    { type: "leaf", title: "Step A", status: "completed", href: "/a" },
    { type: "leaf", title: "Step B", status: "not-started", href: "/b" },
  ],
};

const renderAt = (pathname: string) =>
  render(
    <MemoryRouter initialEntries={[pathname]}>
      <TimelineNavParent firstElement lastElement title={parent.title} status={parent.status} leafs={parent.leafs} />
    </MemoryRouter>,
  );

describe("TimelineNavParent", () => {
  it("always renders the section header", () => {
    renderAt("/other");

    expect(screen.getByRole("button", { name: /Setup/ })).toBeInTheDocument();
  });

  it("starts collapsed when no leaf matches the current route", () => {
    renderAt("/other");

    expect(screen.queryByText("Step A")).not.toBeInTheDocument();
    expect(screen.queryByText("Step B")).not.toBeInTheDocument();
  });

  it("starts expanded when a leaf matches the current route", () => {
    renderAt("/a");

    expect(screen.getByText("Step A")).toBeInTheDocument();
    expect(screen.getByText("Step B")).toBeInTheDocument();
  });

  it("expands and collapses again when the header is clicked", async () => {
    const user = userEvent.setup();
    renderAt("/other");

    const header = screen.getByRole("button", { name: /Setup/ });

    await user.click(header);
    expect(screen.getByText("Step A")).toBeInTheDocument();

    await user.click(header);
    expect(screen.queryByText("Step A")).not.toBeInTheDocument();
  });
});
