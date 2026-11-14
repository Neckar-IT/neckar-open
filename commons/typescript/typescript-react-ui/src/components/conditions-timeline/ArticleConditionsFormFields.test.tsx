import { fireEvent, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { type FieldErrors, useForm } from "react-hook-form";
import { MemoryRouter } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { FormCard, FormCardContent } from "../forms/form";
import {
  ArticleConditionsAccessoryFormFields,
  ArticleConditionsFormFields,
  ArticleConditionsInstallationTimeFormFields,
  ArticleConditionsUnitFormField,
} from "./ArticleConditionsFormFields";
import { toFullArticleConditions } from "./toArticleConditions";
import {
  articleConditionsTimelineEntryUpsertDefaultValues,
  useArticleConditionsTimelineEntryFormResolver,
  type ArticleConditionsTimelineEntryUpsertRequestSchema,
} from "./useArticleConditionsTimelineEntryFormResolver";

type OnValid = (values: ArticleConditionsTimelineEntryUpsertRequestSchema) => void;
type OnInvalid = (errors: FieldErrors<ArticleConditionsTimelineEntryUpsertRequestSchema>) => void;

/** Which optional blocks a harness composes after the mandatory {@link ArticleConditionsFormFields} core. */
type OptionalBlock = "unit" | "accessory" | "installationTime";

/**
 * Harness composing the core field group with the chosen optional blocks in the form context every call site
 * provides — the way a call site assembles a page from the building blocks. Submits through `form.handleSubmit`,
 * so the resolver decides whether [onValid] is reached at all; [onInvalid] receives the field errors that
 * rejected the submit, which is where the distinction between a schema type error and the resolver's
 * required-field message shows up.
 */
function renderComposition(optionalBlocks: readonly OptionalBlock[], onValid: OnValid = () => {}, onInvalid: OnInvalid = () => {}) {
  function Harness() {
    const form = useForm<ArticleConditionsTimelineEntryUpsertRequestSchema>({
      mode: "onTouched",
      resolver: useArticleConditionsTimelineEntryFormResolver(),
      defaultValues: articleConditionsTimelineEntryUpsertDefaultValues(
        { withAccessory: false, withInstallationTime: false },
        { conditions: null },
      ),
    });
    return (
      <FormCard form={form}>
        <FormCardContent>
          <ArticleConditionsFormFields form={form} />
          {optionalBlocks.includes("unit") && <ArticleConditionsUnitFormField />}
          {optionalBlocks.includes("accessory") && <ArticleConditionsAccessoryFormFields form={form} />}
          {optionalBlocks.includes("installationTime") && <ArticleConditionsInstallationTimeFormFields form={form} />}
          <button type="button" onClick={() => void form.handleSubmit(onValid, onInvalid)()}>
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

const allOptionalBlocks: readonly OptionalBlock[] = ["unit", "accessory", "installationTime"];

/**
 * Fills the two mandatory part fields. The euro inputs carry no `name` on the DOM node, so they are addressed by
 * their position among the text inputs: purchase price, selling price, [unit].
 */
async function fillPartFields(container: HTMLElement): Promise<void> {
  const [purchasePrice, sellingPrice] = Array.from(container.querySelectorAll<HTMLInputElement>('input[type="text"]'));

  await typeAndLeave(purchasePrice!, "120");
  await typeAndLeave(sellingPrice!, "168");
}

/** The unit input — third among the text inputs, after the two part euro inputs. */
function unitInput(container: HTMLElement): HTMLInputElement {
  return Array.from(container.querySelectorAll<HTMLInputElement>('input[type="text"]'))[2]!;
}

/** The accessory euro inputs — fourth and fifth among the text inputs, revealed by the accessory checkbox. */
function accessoryPriceInputs(container: HTMLElement): { purchasePrice: HTMLInputElement; sellingPrice: HTMLInputElement } {
  const [, , , purchasePrice, sellingPrice] = Array.from(container.querySelectorAll<HTMLInputElement>('input[type="text"]'));

  return { purchasePrice: purchasePrice!, sellingPrice: sellingPrice! };
}

/** The installation-time input, revealed by its checkbox — the harness renders its block last. */
function installationTimeInput(container: HTMLElement): HTMLInputElement {
  return Array.from(container.querySelectorAll<HTMLInputElement>('input[type="text"]')).at(-1)!;
}

/**
 * `NumberInput` reports its value on blur, so a typed number only reaches the form state once the field is left.
 * The value is set with one change event instead of one keystroke per character: the keystrokes are not under test and
 * cost about 3 events plus a re-render each.
 */
async function typeAndLeave(input: HTMLInputElement, value: string): Promise<void> {
  fireEvent.focus(input);
  fireEvent.change(input, { target: { value } });
  fireEvent.blur(input);
}

/** Empties a number input and leaves it: the emptied field reports `null` to the form state. */
async function clearAndLeave(input: HTMLInputElement): Promise<void> {
  fireEvent.focus(input);
  fireEvent.change(input, { target: { value: "" } });
  fireEvent.blur(input);
}

describe("ArticleConditionsFormFields", () => {
  it("renders the mandatory part fields and none of the optional blocks by itself", () => {
    renderComposition([]);

    expect(screen.getByText(/^purchase price/i)).toBeInTheDocument();
    expect(screen.getByText(/^selling price/i)).toBeInTheDocument();
    expect(screen.queryByText(/^unit/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: /specify accessory/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: /specify installation time/i })).not.toBeInTheDocument();
  });

  it("submits the core alone once the mandatory part fields are filled", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderComposition([], onValid);

    await fillPartFields(container);
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).toHaveBeenCalledTimes(1);
    expect(onValid.mock.calls[0]![0].partUnit).toBeNull();
  });

  it("rejects an empty form instead of submitting it", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    renderComposition([], onValid);

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).not.toHaveBeenCalled();
  });

  it("shows no surcharge beside the prices, which the price overview states instead", async () => {
    const user = userEvent.setup();
    const { container } = renderComposition(allOptionalBlocks);

    await fillPartFields(container);
    await user.click(screen.getByRole("checkbox", { name: /specify accessory/i }));
    const { purchasePrice, sellingPrice } = accessoryPriceInputs(container);
    await typeAndLeave(purchasePrice, "100");
    await typeAndLeave(sellingPrice, "125");

    expect(screen.queryByText(/^surcharge/i)).not.toBeInTheDocument();
  });
});

describe("ArticleConditionsUnitFormField", () => {
  it("submits with a unit that was typed and cleared again", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderComposition(["unit"], onValid);

    await fillPartFields(container);

    // Clearing a TextInput yields `null`, not an empty string — the schema has to accept it, or a
    // per-piece article becomes unsubmittable once the unit was touched.
    await user.type(unitInput(container), "Meter");
    await user.clear(unitInput(container));
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).toHaveBeenCalledTimes(1);
  });

  it("submits an explicit unit", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderComposition(["unit"], onValid);

    await fillPartFields(container);
    await user.type(unitInput(container), "Meter");
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).toHaveBeenCalledTimes(1);
    expect(onValid.mock.calls[0]![0].partUnit).toEqual("Meter");
  });
});

describe("ArticleConditionsAccessoryFormFields", () => {
  it("leaves the gate checkbox unchecked by default", () => {
    renderComposition(allOptionalBlocks);

    expect(screen.getByRole("checkbox", { name: /specify accessory/i })).not.toBeChecked();
  });

  it("reveals both accessory fields once the checkbox is checked", async () => {
    const user = userEvent.setup();
    renderComposition(allOptionalBlocks);

    await user.click(screen.getByRole("checkbox", { name: /specify accessory/i }));

    expect(screen.getByText(/accessory purchase price/i)).toBeInTheDocument();
    expect(screen.getByText(/accessory selling price/i)).toBeInTheDocument();
  });

  it("groups the revealed accessory fields under their checkbox", async () => {
    const user = userEvent.setup();
    renderComposition(allOptionalBlocks);

    await user.click(screen.getByRole("checkbox", { name: /specify accessory/i }));

    // CheckboxDependentFields indents the group to the checkbox label.
    expect(screen.getByText(/accessory selling price/i).closest("div.pl-6")).toBeInTheDocument();
  });

  it("hides the accessory fields again when the checkbox is unchecked", async () => {
    const user = userEvent.setup();
    renderComposition(allOptionalBlocks);

    const checkbox = screen.getByRole("checkbox", { name: /specify accessory/i });
    await user.click(checkbox);
    await user.click(checkbox);

    expect(screen.queryByText(/accessory purchase price/i)).not.toBeInTheDocument();
  });

  it("rejects a checked accessory whose fields are empty", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderComposition(allOptionalBlocks, onValid);

    await fillPartFields(container);
    await user.click(screen.getByRole("checkbox", { name: /specify accessory/i }));
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).not.toHaveBeenCalled();
  });

  it("reports the required purchase price when a checked accessory's price was entered and cleared again", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const onInvalid = vi.fn();
    const { container } = renderComposition(allOptionalBlocks, onValid, onInvalid);

    await fillPartFields(container);
    await user.click(screen.getByRole("checkbox", { name: /specify accessory/i }));

    const { purchasePrice, sellingPrice } = accessoryPriceInputs(container);
    await typeAndLeave(purchasePrice, "100");
    await typeAndLeave(sellingPrice, "125");
    await clearAndLeave(purchasePrice);
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).not.toHaveBeenCalled();
    // The cleared field reports `null`, which the schema accepts — so the checkbox rule reports the
    // missing value instead of zod rejecting the type before the rule ever runs.
    expect(onInvalid.mock.calls[0]![0].accessoryPurchasePrice?.message).toBe("Accessory purchase price is required");
  });

  it("reports the required selling price when a checked accessory's price was entered and cleared again", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const onInvalid = vi.fn();
    const { container } = renderComposition(allOptionalBlocks, onValid, onInvalid);

    await fillPartFields(container);
    await user.click(screen.getByRole("checkbox", { name: /specify accessory/i }));

    const { purchasePrice, sellingPrice } = accessoryPriceInputs(container);
    await typeAndLeave(purchasePrice, "100");
    await typeAndLeave(sellingPrice, "125");
    await clearAndLeave(sellingPrice);
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).not.toHaveBeenCalled();
    expect(onInvalid.mock.calls[0]![0].accessorySellingPrice?.message).toBe("Accessory selling price is required");
  });

  it("submits an unchecked accessory whose prices were entered and cleared again", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderComposition(allOptionalBlocks, onValid);

    await fillPartFields(container);
    const accessoryCheckbox = screen.getByRole("checkbox", { name: /specify accessory/i });
    await user.click(accessoryCheckbox);

    const { purchasePrice, sellingPrice } = accessoryPriceInputs(container);
    await typeAndLeave(purchasePrice, "100");
    await typeAndLeave(sellingPrice, "125");
    await clearAndLeave(purchasePrice);
    await clearAndLeave(sellingPrice);
    await user.click(accessoryCheckbox);
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).toHaveBeenCalledTimes(1);
    expect(toFullArticleConditions(onValid.mock.calls[0]![0]).accessory).toBeNull();
  });
});

describe("ArticleConditionsInstallationTimeFormFields", () => {
  it("leaves the gate checkbox unchecked by default", () => {
    renderComposition(allOptionalBlocks);

    expect(screen.getByRole("checkbox", { name: /specify installation time/i })).not.toBeChecked();
  });

  it("reveals the installation-time field once its checkbox is checked", async () => {
    const user = userEvent.setup();
    renderComposition(allOptionalBlocks);

    await user.click(screen.getByRole("checkbox", { name: /specify installation time/i }));

    expect(screen.getByText(/^installation time/i)).toBeInTheDocument();
  });

  it("reports the required installation time when it was entered and cleared again", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const onInvalid = vi.fn();
    const { container } = renderComposition(allOptionalBlocks, onValid, onInvalid);

    await fillPartFields(container);
    await user.click(screen.getByRole("checkbox", { name: /specify installation time/i }));

    await typeAndLeave(installationTimeInput(container), "8");
    await clearAndLeave(installationTimeInput(container));
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).not.toHaveBeenCalled();
    expect(onInvalid.mock.calls[0]![0].installationTimeHours?.message).toBe("Installation time is required");
  });

  it("submits an unchecked installation time that was entered and cleared again", async () => {
    const user = userEvent.setup();
    const onValid = vi.fn();
    const { container } = renderComposition(allOptionalBlocks, onValid);

    await fillPartFields(container);
    const installationTimeCheckbox = screen.getByRole("checkbox", { name: /specify installation time/i });
    await user.click(installationTimeCheckbox);

    await typeAndLeave(installationTimeInput(container), "8");
    await clearAndLeave(installationTimeInput(container));
    await user.click(installationTimeCheckbox);
    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(onValid).toHaveBeenCalledTimes(1);
    expect(toFullArticleConditions(onValid.mock.calls[0]![0]).installationTime).toBeNull();
  });
});
