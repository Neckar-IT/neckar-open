import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { Action } from "./Action";
import { ActionButton } from "./ActionButton";
import { ActionLockProvider } from "./ActionLockProvider";

function actionOf(overrides: Partial<Action> = {}): Action {
  return { icon: undefined, label: "Archivieren", onTrigger: vi.fn(), ...overrides };
}

// A disabled button fires no pointer events; the tooltip trigger is the wrapper around it.
function tooltipTriggerOf(button: HTMLElement): HTMLElement {
  const wrapper = button.parentElement;
  if (wrapper === null) {
    throw new Error("The button has no tooltip wrapper");
  }
  return wrapper;
}

describe("ActionButton", () => {
  it("triggers an enabled action", async () => {
    const action = actionOf();
    render(<ActionButton action={action} />);

    await userEvent.setup().click(screen.getByRole("button", { name: "Archivieren" }));

    expect(action.onTrigger).toHaveBeenCalledOnce();
  });

  it("shows the reason of a disabled action as tooltip", async () => {
    render(<ActionButton action={actionOf({ disabled: true, disabledReason: "Bereits archiviert" })} />);

    await userEvent.setup().hover(tooltipTriggerOf(screen.getByRole("button", { name: "Archivieren" })));

    expect(await screen.findByRole("tooltip")).toHaveTextContent("Bereits archiviert");
  });

  it("is disabled and names the lock reason inside an ActionLockProvider", async () => {
    const action = actionOf();
    render(
      <ActionLockProvider reason="Gerät offline">
        <ActionButton action={action} />
      </ActionLockProvider>,
    );
    const button = screen.getByRole("button", { name: "Archivieren" });

    await userEvent.setup().hover(tooltipTriggerOf(button));

    expect(button).toBeDisabled();
    expect(await screen.findByRole("tooltip")).toHaveTextContent("Gerät offline");
  });

  it("ignores a click while loading, even when the usage site passes disabled={false}", () => {
    const action = actionOf({ loading: true });
    render(<ActionButton action={action} disabled={false} />);

    screen.getByRole("button").click();

    expect(action.onTrigger).not.toHaveBeenCalled();
  });

  it("stays locked below an inner ActionLockProvider without a reason", () => {
    render(
      <ActionLockProvider reason="Gerät offline">
        <ActionLockProvider reason={undefined}>
          <ActionButton action={actionOf()} />
        </ActionLockProvider>
      </ActionLockProvider>,
    );

    expect(screen.getByRole("button", { name: "Archivieren" })).toBeDisabled();
  });
});
