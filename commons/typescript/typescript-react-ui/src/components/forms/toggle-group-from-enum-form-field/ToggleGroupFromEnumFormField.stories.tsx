/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { createZodEnumFromConstObject } from "@neckar-hub/typescript-utils/zod-utils";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { ToggleGroupFromEnumFormField } from "./ToggleGroupFromEnumFormField";

/**
 * Args
 */

interface Args {
  disabled: boolean;
}

const meta = {
  title: "Forms / ToggleGroupFromEnumFormField",
  component: ToggleGroupFromEnumFormField,
} satisfies Meta;

export default meta;

/**
 * Example Enum
 */

const BatteryRequirementKind = {
  Maybe: "Maybe",
  WithBattery: "WithBattery",
  WithoutBattery: "WithoutBattery",
} as const;

/**
 * Form Schema
 */

const formSchema = z.object({
  batteryKind: createZodEnumFromConstObject(BatteryRequirementKind),
});

type FormValues = z.infer<typeof formSchema>;

export const ToggleGroupFromEnumFormFieldStory: StoryObj<Args> = {
  name: "ToggleGroupFromEnumFormField",
  args: {
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormValues>({
      resolver: zodResolver(formSchema),
      defaultValues: {
        batteryKind: BatteryRequirementKind.Maybe,
      },
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(500);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <ToggleGroupFromEnumFormField
            formSchema={formSchema}
            fieldName="batteryKind"
            label="Batterie gewünscht?"
            disabled={args.disabled}
          />
          <Button type="submit" className="mt-4" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
