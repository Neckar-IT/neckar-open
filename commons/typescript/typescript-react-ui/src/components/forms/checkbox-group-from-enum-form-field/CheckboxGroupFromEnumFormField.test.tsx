import { zodResolver } from "@hookform/resolvers/zod";
import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createZodEnumFromConstObject } from "@neckar-hub/typescript-utils/zod-utils";
import { fireEvent, render, screen } from "@testing-library/react";
import { useForm, useWatch } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { Form } from "../form";
import { CheckboxGroupFromEnumFormField } from "./CheckboxGroupFromEnumFormField";

/**
 * Test Setup
 *
 * A probe renders the current form value so the assertions observe what the field wrote.
 */

const ContentWarning = {
  Violence: "Violence",
  Death: "Death",
  SelfHarm: "SelfHarm",
} as const;

const languageResources = {
  de: {
    translation: {
      enums: {
        ContentWarning: {
          Violence: "Gewalt",
          Death: "Tod",
        },
      },
    },
  },
};

const schema = z.object({
  contentWarnings: z.array(createZodEnumFromConstObject(ContentWarning)).min(1, "Mindestens eine Warnung"),
});

type FormValues = z.infer<typeof schema>;

function ValueProbe(): React.ReactElement {
  const value = useWatch<FormValues>({ name: "contentWarnings" });
  return <output data-testid="value">{JSON.stringify(value)}</output>;
}

function Harness({ initial }: { initial: FormValues["contentWarnings"] }): React.ReactElement {
  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { contentWarnings: initial } });
  return (
    <Form {...form} onSubmit={form.handleSubmit(() => undefined)}>
      <CheckboxGroupFromEnumFormField formSchema={schema} fieldName="contentWarnings" enumName="ContentWarning" label="Warnungen" />
      <ValueProbe />
      <button type="submit">Speichern</button>
    </Form>
  );
}

function UnlabelledHarness(): React.ReactElement {
  const form = useForm<FormValues>({ defaultValues: { contentWarnings: [] } });
  return (
    <Form {...form} onSubmit={form.handleSubmit(() => undefined)}>
      <CheckboxGroupFromEnumFormField formSchema={schema} fieldName="contentWarnings" enumName="ContentWarning" />
    </Form>
  );
}

function renderField(initial: FormValues["contentWarnings"] = []): void {
  render(
    <I18nProvider languageResources={languageResources} options={{ lng: "de" }}>
      <Harness initial={initial} />
    </I18nProvider>,
  );
}

const value = () => screen.getByTestId("value").textContent;

describe("CheckboxGroupFromEnumFormField", () => {
  it("names the group with the field label", () => {
    renderField();

    expect(screen.getByRole("group", { name: /^Warnungen/ })).toBeInTheDocument();
  });

  it("leaves the group unnamed without a label instead of pointing at a missing element", () => {
    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "de" }}>
        <UnlabelledHarness />
      </I18nProvider>,
    );

    expect(screen.getByRole("group")).not.toHaveAttribute("aria-labelledby");
  });

  it("renders one checkbox per enum value", () => {
    renderField();

    expect(screen.getAllByRole("checkbox")).toHaveLength(3);
  });

  it("labels each checkbox with the translation of the named enum and falls back to the key", () => {
    renderField();

    expect(screen.getByRole("checkbox", { name: "Gewalt" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "enums.SelfHarm" })).toBeInTheDocument();
  });

  it("checks the values the field holds", () => {
    renderField(["Death"]);

    expect(screen.getByRole("checkbox", { name: "Tod" })).toHaveAttribute("aria-checked", "true");
    expect(screen.getByRole("checkbox", { name: "Gewalt" })).toHaveAttribute("aria-checked", "false");
  });

  it("writes the checked values in enum order", () => {
    renderField(["Death"]);

    fireEvent.click(screen.getByRole("checkbox", { name: "Gewalt" }));

    expect(value()).toBe(JSON.stringify(["Violence", "Death"]));
  });

  it("removes an unchecked value", () => {
    renderField(["Violence", "Death"]);

    fireEvent.click(screen.getByRole("checkbox", { name: "Gewalt" }));

    expect(value()).toBe(JSON.stringify(["Death"]));
  });

  it("renders the field error", async () => {
    renderField();

    fireEvent.click(screen.getByRole("button", { name: "Speichern" }));

    expect(await screen.findByText("Mindestens eine Warnung")).toBeInTheDocument();
  });
});
