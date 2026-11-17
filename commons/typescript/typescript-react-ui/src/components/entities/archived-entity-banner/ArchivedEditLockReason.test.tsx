import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ArchivedEditLockReason } from "./ArchivedEditLockReason";

describe("ArchivedEditLockReason", () => {
  it("names the unarchive as the way to edit", () => {
    render(<ArchivedEditLockReason />);

    expect(screen.getByText("Archived — unarchive it to edit.")).toBeInTheDocument();
  });
});
