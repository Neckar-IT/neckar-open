import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { MemoryRouter } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { FormCard, FormCardContent } from "../forms/form";
import { PriceTimelineEntryFormFields } from "./PriceTimelineEntryFormFields";
import {
  priceTimelineEntryUpsertDefaultValues,
  usePriceTimelineEntryFormResolver,
  type PriceTimelineEntryUpsertRequestSchema,
} from "./usePriceTimelineEntryFormResolver";

/**
 * Harness wrapping the field group in the form context every call site provides. Submits through
 * `form.handleSubmit`, the way the app's action hooks do, so the resolver decides whether [onValid]
 * is reached at all.
 */
function renderFields(onValid: (values: PriceTimelineEntryUpsertRequestSchema) => void = () => {}) {
  function Harness() {
    const form = useForm<PriceTimelineEntryUpsertRequestSchema>({
      mode: "onTouched",
      resolver: usePriceTimelineEntryFormResolver(),
      defaultValues: priceTimelineEntryUpsertDefaultValues,
    });
    return (
      <FormCard form={form}>
        <FormCardContent>
          <PriceTimelineEntryFormFields form={form} />
          <button type="button" onClick={() => void form.handleSubmit(onValid)()}>
            Save
          </button>
        </FormCardContent>
      </FormCard>
    );
  }

  return render(
    <MemoryRouter>
      <Harness />
    </MemoryRouter>,
  );
}

describe("PriceTimelineEntryFormFields", () => {
  it("renders the timing choice and the price field", () => {
    renderFields();

    expect(screen.getByText(/^price$/i)).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /immediately/i })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: /a specific date/i })).toBeInTheDocument();
  });

  it("rejects an empty form instead of submitting it", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    renderFields(onValid);

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).not.toHaveBeenCalled();
  });

  it("submits once a price is entered", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderFields(onValid);

    const [price] = Array.from(container.querySelectorAll<HTMLInputElement>('input[type="text"]'));
    await user.type(price!, "1990");

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).toHaveBeenCalledTimes(1);
  });
});
