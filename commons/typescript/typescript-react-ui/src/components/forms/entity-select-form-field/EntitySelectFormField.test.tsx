/**
 * Imports
 */

import { elementAt, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { EntitySelectFormField } from "./EntitySelectFormField";

/**
 * Tests
 *
 * The field renders a trigger that opens a caller-supplied dialog. Selecting an entity writes its
 * id (or `null`) into react-hook-form; a probe renders the current form value.
 */

const schema = z.object({ entityId: z.string().nullable() });

interface Entity {
  id: string;
  name: string;
}

const alice: Entity = { id: "1", name: "Alice" };
const bob: Entity = { id: "2", name: "Bob" };

function EntityProbe() {
  const value = useWatch({ name: "entityId" });
  return <output data-testid="entity-value">{JSON.stringify(value ?? null)}</output>;
}

function Harness({ initialEntity }: { initialEntity?: Entity }) {
  const methods = useForm<{ entityId: string | null }>({ defaultValues: { entityId: null } });
  return (
    <FormProvider {...methods}>
      <EntitySelectFormField<typeof schema, Entity>
        fieldName="entityId"
        formSchema={schema}
        label="entitySelect.label"
        initialEntity={initialEntity}
        placeholder="entitySelect.placeholder"
        itemRenderer={(entity) => entity.name}
        renderDialog={({ onSelectEntity }) => (
          <button type="button" onClick={() => onSelectEntity(bob)}>
            pick-bob
          </button>
        )}
      />
      <EntityProbe />
    </FormProvider>
  );
}

const entityValue = () => screen.getByTestId("entity-value");

describe("EntitySelectFormField", () => {
  it("shows the selected entity via the item renderer", () => {
    render(<Harness initialEntity={alice} />);

    expect(screen.getByText("Alice")).toBeInTheDocument();
  });

  it("shows the placeholder when no entity is selected", () => {
    render(<Harness />);

    expect(screen.getByText("entitySelect.placeholder")).toBeInTheDocument();
  });

  it("opens the dialog when the trigger is clicked", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    expect(screen.queryByText("pick-bob")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button"));

    expect(screen.getByText("pick-bob")).toBeInTheDocument();
  });

  it("writes the picked entity's id to the form, updates the trigger, and closes the dialog", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByRole("button"));
    await user.click(screen.getByText("pick-bob"));

    expect(entityValue()).toHaveTextContent('"2"');
    expect(screen.getByText("Bob")).toBeInTheDocument();
    expect(screen.queryByText("pick-bob")).not.toBeInTheDocument();
  });

  it("puts the id and aria attributes FormControl slots in on the trigger", () => {
    // The field renders the trigger next to the dialog. Handing that pair to FormControl as a
    // fragment made React drop the props with a warning, leaving the caption pointing at nothing.
    render(<Harness />);

    const trigger = screen.getByRole("button");
    expect(screen.getByLabelText(/entitySelect.label/)).toBe(trigger);
    expect(trigger).toHaveAttribute("aria-describedby");
    expect(trigger).toHaveAttribute("aria-invalid", "false");
  });

  it("writes null to the form when the selection is cleared", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntity={alice} />);

    // With a selection the buttons are [trigger, clear]; the clear button is last.
    const buttons = screen.getAllByRole("button");
    await user.click(elementAt(buttons, -1));

    expect(entityValue()).toHaveTextContent("null");
    expect(screen.queryByText("Alice")).not.toBeInTheDocument();
  });
});
