import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { MemoryRouter } from "react-router";
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { FormCard, FormCardContent } from "../forms/form";
import { TimelineEntryTimingFormFields } from "./TimelineEntryTimingFormFields";
import { type TimelineEntryTimingSchema } from "./timelineEntryTimingSchema";

// Wednesday, mid-month: every quick-select lands on a different date than a plain offset would.
const wednesday = new Date(2026, 6, 29, 15, 30);

beforeAll(() => {
  vi.setSystemTime(wednesday);
});

afterAll(() => {
  vi.useRealTimers();
});

/**
 * Harness wrapping the field group in the form context every call site provides. The timing is
 * fixed to `"scheduled"` because only that variant renders the date picker.
 */
function renderTimingFields() {
  function Harness() {
    const form = useForm<TimelineEntryTimingSchema>({ defaultValues: { type: "scheduled" } });
    return (
      <FormCard form={form}>
        <FormCardContent>
          <TimelineEntryTimingFormFields timingLabel="Conditions from" type="scheduled" />
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

async function clickQuickSelect(title: string) {
  const user = userEvent.setup();
  await user.click(screen.getByText("Pick a Date"));
  await user.click(screen.getByRole("button", { name: title }));
}

describe("TimelineEntryTimingFormFields", () => {
  it("offers the three quick-selects next to the calendar", async () => {
    const user = userEvent.setup();
    renderTimingFields();

    await user.click(screen.getByText("Pick a Date"));

    expect(screen.getByRole("button", { name: "Tomorrow" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next week" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next month" })).toBeInTheDocument();
  });

  it("picks the following day for 'Tomorrow'", async () => {
    renderTimingFields();

    await clickQuickSelect("Tomorrow");

    expect(screen.getByText("07/30/2026")).toBeInTheDocument();
  });

  it("picks the Monday of the following week for 'Next week', not the same weekday", async () => {
    renderTimingFields();

    await clickQuickSelect("Next week");

    // Monday 2026-08-03 — a plain +7 days would land on Wednesday 2026-08-05.
    expect(screen.getByText("08/03/2026")).toBeInTheDocument();
  });

  it("picks the first of the following month for 'Next month', not the same day of month", async () => {
    renderTimingFields();

    await clickQuickSelect("Next month");

    // 2026-08-01 — a plain +1 month would land on 2026-08-29.
    expect(screen.getByText("08/01/2026")).toBeInTheDocument();
  });
});
