import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { FormProvider, useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import { recordUnknownOutcome } from "../feedback/unknown-outcome/unknownOutcomeStore";
import type { Action } from "./Action";
import { ActionLockProvider } from "./ActionLockProvider";
import { FormSubmitActionButton } from "./FormSubmitActionButton";

function FormWithSubmit({ action }: { action: Action }): React.ReactElement {
  const form = useForm();
  return (
    <FormProvider {...form}>
      <form>
        <input aria-label="Name" />
        <FormSubmitActionButton action={action} />
      </form>
    </FormProvider>
  );
}

function actionOf(overrides: Partial<Action> = {}): Action {
  return { icon: undefined, label: "Speichern", onTrigger: vi.fn(), ...overrides };
}

describe("FormSubmitActionButton", () => {
  it("triggers the action on Enter in an input", async () => {
    const action = actionOf();
    render(<FormWithSubmit action={action} />);

    await userEvent.setup().type(screen.getByRole("textbox", { name: "Name" }), "{Enter}");

    expect(action.onTrigger).toHaveBeenCalledOnce();
  });

  it("ignores Enter while the action is running", async () => {
    const action = actionOf({ loading: true });
    render(<FormWithSubmit action={action} />);

    await userEvent.setup().type(screen.getByRole("textbox", { name: "Name" }), "{Enter}");

    expect(action.onTrigger).not.toHaveBeenCalled();
  });

  it("ignores Enter inside an ActionLockProvider", async () => {
    const action = actionOf();
    render(
      <ActionLockProvider reason="Gerät offline">
        <FormWithSubmit action={action} />
      </ActionLockProvider>,
    );

    await userEvent.setup().type(screen.getByRole("textbox", { name: "Name" }), "{Enter}");

    expect(action.onTrigger).not.toHaveBeenCalled();
  });

  it("claims no 504 while hidden, so the toast reports it", async () => {
    let finishSubmit: () => void = () => undefined;
    function HiddenSubmitForm(): React.ReactElement {
      const form = useForm();
      const submit = form.handleSubmit(() => new Promise<void>((resolve) => (finishSubmit = resolve)));
      return (
        <FormProvider {...form}>
          <form>
            <button type="button" onClick={() => void submit()}>
              Submit
            </button>
            <FormSubmitActionButton action={actionOf({ hidden: true })} />
          </form>
        </FormProvider>
      );
    }
    render(<HiddenSubmitForm />);

    await userEvent.setup().click(screen.getByRole("button", { name: "Submit" }));

    expect(recordUnknownOutcome("Speichern")).toBe(false);
    finishSubmit();
  });
});
