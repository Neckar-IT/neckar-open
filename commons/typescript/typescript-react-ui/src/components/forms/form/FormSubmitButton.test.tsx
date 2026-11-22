import { fireEvent, render, screen, tooltipTriggerOf, waitFor } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { FormProvider, useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import { ActionLockProvider } from "../../actions/ActionLockProvider";
import { FormSubmitButton } from "./FormSubmitButton";

function FormWithSubmit({ onSubmit }: { onSubmit: () => void }): React.ReactElement {
  const form = useForm();
  return (
    <FormProvider {...form}>
      <form onSubmit={(event) => void form.handleSubmit(onSubmit)(event)}>
        <FormSubmitButton>Speichern</FormSubmitButton>
      </form>
    </FormProvider>
  );
}

describe("FormSubmitButton", () => {
  // user-event dispatches no submit event in this jsdom, so the click goes through fireEvent.
  it("submits the form on click", async () => {
    const onSubmit = vi.fn();
    render(<FormWithSubmit onSubmit={onSubmit} />);

    fireEvent.click(screen.getByRole("button", { name: "Speichern" }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledOnce());
  });

  it("is disabled and names the lock reason inside an ActionLockProvider", async () => {
    render(
      <ActionLockProvider reason="Gerät offline">
        <FormWithSubmit onSubmit={vi.fn()} />
      </ActionLockProvider>,
    );
    const button = screen.getByRole("button", { name: "Speichern" });

    await userEvent.setup().hover(tooltipTriggerOf(button));

    expect(button).toBeDisabled();
    expect(await screen.findByRole("tooltip")).toHaveTextContent("Gerät offline");
  });
});
