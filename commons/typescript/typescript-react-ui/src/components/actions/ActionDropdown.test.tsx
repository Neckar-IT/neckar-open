import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { Action } from "./Action";
import { ActionsDropdown, ActionsDropdownItem } from "./ActionDropdown";
import { ActionLockProvider } from "./ActionLockProvider";

function renderDropdown(action: Action, lockReason?: string): void {
  render(
    <ActionLockProvider reason={lockReason}>
      <ActionsDropdown>
        <ActionsDropdownItem action={action} />
      </ActionsDropdown>
    </ActionLockProvider>,
  );
}

describe("ActionsDropdownItem", () => {
  it("triggers an enabled action", async () => {
    const user = userEvent.setup();
    const onTrigger = vi.fn();
    renderDropdown({ icon: undefined, label: "Apply", onTrigger });

    await user.click(screen.getByRole("button"));
    await user.click(screen.getByRole("menuitem", { name: "Apply" }));

    expect(onTrigger).toHaveBeenCalledOnce();
  });

  it("does not trigger a disabled action", async () => {
    const user = userEvent.setup();
    const onTrigger = vi.fn();
    renderDropdown({ icon: undefined, label: "Apply", disabled: true, onTrigger });

    await user.click(screen.getByRole("button"));
    const item = screen.getByRole("menuitem", { name: "Apply" });
    await user.click(item);

    expect(item).toHaveAttribute("aria-disabled", "true");
    expect(onTrigger).not.toHaveBeenCalled();
  });

  it("names the reason of a disabled action below its label", async () => {
    const user = userEvent.setup();
    renderDropdown({ icon: undefined, label: "Edit", disabled: true, disabledReason: "Archived", onTrigger: vi.fn() });

    await user.click(screen.getByRole("button"));

    expect(screen.getByRole("menuitem")).toHaveTextContent("EditArchived");
  });

  it("does not trigger an action inside an ActionLockProvider and names the lock reason", async () => {
    const user = userEvent.setup();
    const onTrigger = vi.fn();
    renderDropdown({ icon: undefined, label: "Apply", onTrigger }, "Offline");

    await user.click(screen.getByRole("button"));
    const item = screen.getByRole("menuitem");
    await user.click(item);

    expect(item).toHaveAttribute("aria-disabled", "true");
    expect(item).toHaveTextContent("ApplyOffline");
    expect(onTrigger).not.toHaveBeenCalled();
  });

  it("does not trigger a running action", async () => {
    const user = userEvent.setup();
    const onTrigger = vi.fn();
    renderDropdown({ icon: undefined, label: "Apply", loading: true, onTrigger });

    await user.click(screen.getByRole("button"));
    await user.click(screen.getByRole("menuitem", { name: "Apply" }));

    expect(onTrigger).not.toHaveBeenCalled();
  });
});
