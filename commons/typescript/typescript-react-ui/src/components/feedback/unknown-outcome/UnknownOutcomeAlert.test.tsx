import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { AxiosError, AxiosHeaders } from "axios";
import { FormProvider, useForm } from "react-hook-form";
import { afterEach, describe, expect, it, vi } from "vitest";
import { FormSubmitActionButton } from "../../actions/FormSubmitActionButton";
import { withActionFailedToast } from "../toast";
import { clearUnknownOutcome } from "./unknownOutcomeStore";

function errorWithStatus(status: number): AxiosError {
  const config = { headers: new AxiosHeaders(), url: "/api/patterns/publishers" };
  return new AxiosError("failed", AxiosError.ERR_BAD_RESPONSE, config, undefined, {
    data: undefined,
    status,
    statusText: "",
    headers: {},
    config,
  });
}

function PublisherForm(props: { save: () => Promise<unknown> }): React.ReactElement {
  const form = useForm({ defaultValues: { name: "" } });
  return (
    <FormProvider {...form}>
      <form>
        <input aria-label="Name" {...form.register("name")} />
        <FormSubmitActionButton
          action={{
            icon: undefined,
            label: "Speichern",
            onTrigger: () =>
              void form.handleSubmit(async () => {
                await withActionFailedToast("Speichern", props.save()).catch(() => undefined);
              })(),
          }}
        />
      </form>
    </FormProvider>
  );
}

function renderForm(save: () => Promise<unknown>) {
  const queryClient = createQueryClient();
  const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
  render(
    <QueryClientProvider client={queryClient}>
      <PublisherForm save={save} />
    </QueryClientProvider>,
  );
  return { invalidateQueries };
}

afterEach(() => {
  clearUnknownOutcome();
});

describe("UnknownOutcomeAlert", () => {
  it("shows `UnknownOutcomeAlert` with its three actions for a save failing with 504 and reloads the server state", async () => {
    const user = userEvent.setup();
    const save = vi.fn(() => Promise.reject(errorWithStatus(504)));
    const { invalidateQueries } = renderForm(save);

    await user.type(screen.getByRole("textbox", { name: "Name" }), "Hanser");
    await user.click(screen.getByRole("button", { name: "Speichern" }));

    expect(await screen.findByText("‘Speichern’: outcome unclear")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Load current state" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Send again" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Close form" })).toBeInTheDocument();
    expect(await screen.findByText(/^State as of /)).toBeInTheDocument();
    expect(invalidateQueries).toHaveBeenCalled();
    expect(save).toHaveBeenCalledOnce();
  });

  it("keeps the entered values after reloading the state and sends again on request", async () => {
    const user = userEvent.setup();
    const save = vi.fn(() => Promise.reject(errorWithStatus(504)));
    renderForm(save);

    await user.type(screen.getByRole("textbox", { name: "Name" }), "Hanser");
    await user.click(screen.getByRole("button", { name: "Speichern" }));
    await user.click(await screen.findByRole("button", { name: "Load current state" }));

    expect(screen.getByRole("textbox", { name: "Name", hidden: true })).toHaveValue("Hanser");

    await user.click(screen.getByRole("button", { name: "Send again" }));

    expect(save).toHaveBeenCalledTimes(2);
  });

  it("stays away for a save failing with 500", async () => {
    const user = userEvent.setup();
    renderForm(() => Promise.reject(errorWithStatus(500)));

    await user.click(screen.getByRole("button", { name: "Speichern" }));

    expect(screen.queryByText("‘Speichern’: outcome unclear")).not.toBeInTheDocument();
  });
});
