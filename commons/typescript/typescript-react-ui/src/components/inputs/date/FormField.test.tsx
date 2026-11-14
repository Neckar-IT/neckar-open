/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { userEvent } from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { Form } from "../../forms/form";
import { DatePickerFormField } from "./FormField";

/**
 * Pins the wire-format contract: `DatePickerFormField` writes the user's calendar-day click
 * to the form state as an ISO-8601 date string `yyyy-MM-dd` matching the picked day. The
 * conversion uses local `Date` methods (`getFullYear/getMonth/getDate`); it must not pass
 * through `toISOString()`, which would shift the day by ±1 in non-UTC timezones.
 *
 * See ADR 0150 (frontend wire-format section) and issue #1737.
 */

/**
 * Test Fixtures
 */

const formSchema = z.object({
  dob: z.iso.date(),
});

type FormSchema = z.infer<typeof formSchema>;

function FormHarness({
  onValuesChange,
  defaultValues,
}: {
  onValuesChange: (values: Partial<FormSchema>) => void;
  defaultValues?: Partial<FormSchema>;
}) {
  const form = useForm<FormSchema>({ defaultValues });
  onValuesChange(form.watch());
  return (
    <Form {...form} onSubmit={form.handleSubmit(() => undefined)}>
      <DatePickerFormField formSchema={formSchema} fieldName="dob" label="Date of Birth" defaultMonth={new Date(2024, 5, 28)} />
    </Form>
  );
}

/**
 * Tests
 */

describe("DatePickerFormField — wire-format contract", () => {
  it("writes the picked calendar day to form state as ISO yyyy-MM-dd", async () => {
    const user = userEvent.setup();
    let latestValues: Partial<FormSchema> = {};
    render(<FormHarness onValuesChange={(values) => (latestValues = values)} />);

    await user.click(screen.getByText("Pick a Date"));
    await user.click(screen.getByText("10"));

    expect(latestValues.dob).toBe("2024-06-10");
  });

  it("shows no day for the empty text a required field starts in", () => {
    render(<FormHarness onValuesChange={() => undefined} defaultValues={{ dob: "" }} />);

    expect(screen.getByText("Pick a Date")).toBeInTheDocument();
  });
});
