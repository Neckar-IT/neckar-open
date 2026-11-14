/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AddressSection } from "./AddressSection";

/**
 * Tests
 *
 * AddressSection shows the address with its country and confirmation status, or the missing label, and offers
 * the edit action and the confirm action.
 */

const labels = { title: "Business address", missingLabel: "No address yet" };
const confirmAction = { icon: null, label: "Confirm", onTrigger: vi.fn() };

describe("AddressSection", () => {
  it("shows the missing label and triggers the edit action without an address", async () => {
    const user = userEvent.setup();
    const editAction = { icon: null, label: "Capture", onTrigger: vi.fn() };
    render(<AddressSection {...labels} address={null} editAction={editAction} confirmAction={confirmAction} />);

    expect(screen.getByText("No address yet")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Capture" }));

    expect(editAction.onTrigger).toHaveBeenCalledTimes(1);
  });

  it("shows the address with its country and status and offers edit and confirm", async () => {
    const user = userEvent.setup();
    render(
      <AddressSection
        {...labels}
        address={{ type: "Draft", street: "Neckarstraße", houseNumber: "12", zipCode: "72072", city: "Tübingen", country: "Germany" }}
        editAction={{ icon: null, label: "Edit", onTrigger: vi.fn() }}
        confirmAction={confirmAction}
      />,
    );

    expect(screen.getByText(/Neckarstraße 12/)).toBeInTheDocument();
    expect(screen.getByText(/Germany/)).toBeInTheDocument();
    expect(screen.getByText("Draft")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Edit" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Confirm" }));

    expect(confirmAction.onTrigger).toHaveBeenCalledTimes(1);
  });

  it("renders no edit button for a hidden edit action", () => {
    render(
      <AddressSection
        {...labels}
        address={null}
        editAction={{ icon: null, label: "Capture", hidden: true, onTrigger: vi.fn() }}
        confirmAction={confirmAction}
      />,
    );

    expect(screen.queryByRole("button", { name: "Capture" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Confirm" })).toBeInTheDocument();
  });
});
