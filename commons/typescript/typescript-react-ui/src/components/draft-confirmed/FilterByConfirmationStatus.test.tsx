/**
 * Imports
 */

import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { elementAt, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { FilterByConfirmationStatus } from "./FilterByConfirmationStatus";

/**
 * Tests
 *
 * FilterByConfirmationStatus builds a single-select filter from the ConfirmationStatus enum and maps
 * the picked option string back to the typed enum value.
 */

describe("FilterByConfirmationStatus", () => {
  it("shows the placeholder when nothing is selected", () => {
    render(<FilterByConfirmationStatus value={undefined} onChange={vi.fn()} />);

    expect(screen.getByText("Confirmation")).toBeInTheDocument();
  });

  it("shows the selected status in the trigger", () => {
    render(<FilterByConfirmationStatus value={ConfirmationStatus.Draft} onChange={vi.fn()} />);

    expect(screen.getByText("Draft")).toBeInTheDocument();
  });

  it("reports the Draft enum value when its option is selected", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<FilterByConfirmationStatus value={undefined} onChange={onChange} />);

    await user.click(screen.getByRole("button"));
    await user.click(screen.getByRole("option", { name: "Draft" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(ConfirmationStatus.Draft);
  });

  it("reports the Confirmed enum value when its option is selected", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<FilterByConfirmationStatus value={undefined} onChange={onChange} />);

    await user.click(screen.getByRole("button"));
    await user.click(screen.getByRole("option", { name: "Confirmed" }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(ConfirmationStatus.Confirmed);
  });

  it("reports undefined when the inline clear button is clicked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<FilterByConfirmationStatus value={ConfirmationStatus.Confirmed} onChange={onChange} />);

    const buttons = screen.getAllByRole("button");
    await user.click(elementAt(buttons, -1));

    expect(onChange).toHaveBeenCalledExactlyOnceWith(undefined);
  });
});
