/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { CodeBlock } from "@neckar-hub/typescript-react-ui/data-display";
import { Form, FormSubmitButton } from "@neckar-hub/typescript-react-ui/forms";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { AdjustableValueNumberInputFormField, adjustableValueNumberSchema } from "./AdjustableValueNumberInputFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / AdjustableValueNumberInputFormField",
};

export default meta;

/**
 * Form Schema
 */

const formSchema = z.object({
  price: adjustableValueNumberSchema.refine((data) => data.type !== "Adjusted" || data.value > 18, {
    path: ["overridden"],
    error: "Mindestens 18",
  }),
});

type FormValues = z.infer<typeof formSchema>;

/**
 * Args
 */

interface Args {
  hint: string;
}

/**
 * Stories
 */

export const AdjustableValueNumberFormFieldStory: Story<Args> = (args) => {
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      price: {
        type: "Auto",
        value: 20.4,
      },
    },
  });

  const handleSubmit = form.handleSubmit(async (data) => {
    await delay(500);
    window.alert(JSON.stringify(data, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-96 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <AdjustableValueNumberInputFormField
          {...args}
          formSchema={formSchema}
          fieldName="price"
          label="Einspeisevergütung"
          description="The price can be overridden."
          unit="ct/kWh"
          fractionDigits={2}
        />
        <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
      </Form>
      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
AdjustableValueNumberFormFieldStory.storyName = "AdjustableValueNumberFormField";
AdjustableValueNumberFormFieldStory.args = {
  hint: "Die Einspeisevergütung wurde entsprechend der gesetzlichen Vorgaben und der Leitungsdaten der PV-Anlage berechnet.",
};
