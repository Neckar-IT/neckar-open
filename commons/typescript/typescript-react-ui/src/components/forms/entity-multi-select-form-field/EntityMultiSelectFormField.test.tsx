/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { EntityMultiSelectFormField } from "./EntityMultiSelectFormField";

/**
 * Tests
 *
 * The field renders one badge per selected entity plus a button that opens a caller-supplied
 * dialog. Picking an entity appends its id to the react-hook-form array; a probe renders the
 * current form value.
 */

const schema = z.object({ entityIds: z.array(z.string()) });

interface Entity {
  id: string;
  name: string;
}

const alice: Entity = { id: "1", name: "Alice" };
const bob: Entity = { id: "2", name: "Bob" };

function EntityProbe() {
  const value = useWatch({ name: "entityIds" });
  return <output data-testid="entity-values">{JSON.stringify(value ?? [])}</output>;
}

/**
 * Stands in for a real select dialog: one button per pickable entity, disabling the ones the field
 * reports as already selected — what the PV select tables do with their per-row Select button.
 */
function Harness({ initialEntities = [] }: { initialEntities?: Entity[] }) {
  const methods = useForm<{ entityIds: string[] }>({ defaultValues: { entityIds: initialEntities.map((entity) => entity.id) } });
  return (
    <FormProvider {...methods}>
      <EntityMultiSelectFormField<typeof schema, Entity>
        fieldName="entityIds"
        formSchema={schema}
        label="entityMultiSelect.label"
        initialEntities={initialEntities}
        addButtonLabel="entityMultiSelect.add"
        itemRenderer={(entity) => entity.name}
        renderDialog={({ selectedEntityIds, onSelectEntity }) => (
          <>
            {[alice, bob].map((entity) => (
              <button key={entity.id} type="button" disabled={selectedEntityIds.includes(entity.id)} onClick={() => onSelectEntity(entity)}>
                pick-{entity.name}
              </button>
            ))}
          </>
        )}
      />
      <EntityProbe />
    </FormProvider>
  );
}

/**
 * A dialog that ignores `selectedEntityIds` entirely, so a click on an already-selected entity
 * still reaches the field. Covers the dedupe guard independently of the dialog's cooperation.
 */
function UncooperativeHarness({ initialEntities = [] }: { initialEntities?: Entity[] }) {
  const methods = useForm<{ entityIds: string[] }>({ defaultValues: { entityIds: initialEntities.map((entity) => entity.id) } });
  return (
    <FormProvider {...methods}>
      <EntityMultiSelectFormField<typeof schema, Entity>
        fieldName="entityIds"
        formSchema={schema}
        initialEntities={initialEntities}
        addButtonLabel="entityMultiSelect.add"
        itemRenderer={(entity) => entity.name}
        renderDialog={({ onSelectEntity }) => (
          <button type="button" onClick={() => onSelectEntity(bob)}>
            pick-Bob
          </button>
        )}
      />
      <EntityProbe />
    </FormProvider>
  );
}

const entityValues = () => screen.getByTestId("entity-values");
const addButton = () => screen.getByText("entityMultiSelect.add").closest("button")!;
const pickButton = (name: string) => screen.getByText(`pick-${name}`);

describe("EntityMultiSelectFormField", () => {
  it("renders one badge per initial entity via the item renderer", () => {
    render(<Harness initialEntities={[alice, bob]} />);

    expect(screen.getByText("Alice")).toBeInTheDocument();
    expect(screen.getByText("Bob")).toBeInTheDocument();
  });

  it("renders no badges when nothing is selected", () => {
    render(<Harness />);

    expect(screen.queryByText("Alice")).not.toBeInTheDocument();
    expect(entityValues()).toHaveTextContent("[]");
  });

  it("opens the dialog when the add button is clicked", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    expect(screen.queryByText("pick-Bob")).not.toBeInTheDocument();
    await user.click(addButton());

    expect(screen.getByText("pick-Bob")).toBeInTheDocument();
  });

  it("appends the picked entity's id to the form, renders its badge, and closes the dialog", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(addButton());
    await user.click(pickButton("Bob"));

    expect(entityValues()).toHaveTextContent('["2"]');
    expect(screen.getByText("Bob")).toBeInTheDocument();
    expect(screen.queryByText("pick-Bob")).not.toBeInTheDocument();
  });

  it("puts the id and aria attributes FormControl slots in on the add button", () => {
    // The field renders the badge row next to the dialog. Handing that pair to FormControl as a
    // fragment made React drop the props with a warning, leaving the caption pointing at nothing.
    render(<Harness />);

    expect(screen.getByLabelText(/entityMultiSelect.label/)).toBe(addButton());
    expect(addButton()).toHaveAttribute("aria-describedby");
    expect(addButton()).toHaveAttribute("aria-invalid", "false");
  });

  it("reports the selected ids to the dialog so it can disable them", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntities={[bob]} />);

    await user.click(addButton());

    expect(pickButton("Bob")).toBeDisabled();
    expect(pickButton("Alice")).toBeEnabled();
  });

  it("reports a newly added id to the dialog on the next open", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(addButton());
    await user.click(pickButton("Bob"));
    await user.click(addButton());

    expect(pickButton("Bob")).toBeDisabled();
  });

  it("stops reporting an id once its badge is dismissed, re-enabling it in the dialog", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntities={[bob]} />);

    // The buttons are [bob-remove, add]; the badge's dismiss button comes first.
    await user.click(screen.getAllByRole("button")[0]!);
    await user.click(addButton());

    expect(pickButton("Bob")).toBeEnabled();
  });

  it("ignores an entity that is already selected even when the dialog offers it", async () => {
    const user = userEvent.setup();
    render(<UncooperativeHarness initialEntities={[bob]} />);

    await user.click(addButton());
    await user.click(pickButton("Bob"));

    expect(entityValues()).toHaveTextContent('["2"]');
  });

  it("drops the removed entity's id when its badge is dismissed", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntities={[alice, bob]} />);

    // Each badge carries a dismiss button; the buttons are [alice-remove, bob-remove, add].
    await user.click(screen.getAllByRole("button")[0]!);

    expect(entityValues()).toHaveTextContent('["2"]');
    expect(screen.queryByText("Alice")).not.toBeInTheDocument();
  });

  it("keeps the initial entities when a further one is picked", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntities={[alice]} />);

    await user.click(addButton());
    await user.click(pickButton("Bob"));

    expect(entityValues()).toHaveTextContent('["1","2"]');
    expect(screen.getByText("Alice")).toBeInTheDocument();
    expect(screen.getByText("Bob")).toBeInTheDocument();
  });
});
