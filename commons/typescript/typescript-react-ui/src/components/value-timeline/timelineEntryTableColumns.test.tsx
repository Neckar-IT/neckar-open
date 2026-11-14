import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { ActionsDropdownItem } from "../actions";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import { rowActionsColumn } from "./timelineEntryTableColumns";

const now = new Date("2026-06-01T00:00:00Z");
const past: TimelineEntryLike = { validFrom: "2026-04-01T00:00:00Z" };
const current: TimelineEntryLike = { validFrom: "2026-05-01T00:00:00Z" };
const future: TimelineEntryLike = { validFrom: "2026-07-01T00:00:00Z" };
const summary: TimelineSummaryLike<TimelineEntryLike> = { currentEntry: current, nextFutureEntry: future };

function rowActions(entry: TimelineEntryLike): ReactNode {
  return <ActionsDropdownItem action={{ icon: undefined, label: `Delete ${entry.validFrom}`, onTrigger: vi.fn() }} />;
}

describe("rowActionsColumn", () => {
  it("renders the row actions of a future entry in the dropdown", async () => {
    const user = userEvent.setup();
    const column = rowActionsColumn({ summary, now, rowActions });
    render(<>{column.cell(future)}</>);

    await user.click(screen.getByRole("button"));

    expect(screen.getByRole("menuitem", { name: "Delete 2026-07-01T00:00:00Z" })).toBeInTheDocument();
  });

  it("renders nothing for the current and a past entry", () => {
    const renderRowActions = vi.fn(rowActions);
    const column = rowActionsColumn({ summary, now, rowActions: renderRowActions });

    expect(column.cell(current)).toBeNull();
    expect(column.cell(past)).toBeNull();
    expect(renderRowActions).not.toHaveBeenCalled();
  });
});
