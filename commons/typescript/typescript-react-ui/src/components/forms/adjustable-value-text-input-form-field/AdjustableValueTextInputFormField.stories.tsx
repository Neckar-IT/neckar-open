/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { CodeBlock } from "@neckar-hub/typescript-react-ui/data-display";
import { Form, FormSubmitButton } from "@neckar-hub/typescript-react-ui/forms";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { AdjustableValueTextInputFormField, adjustableValueTextSchema } from "./AdjustableValueTextInputFormField";

/**
 * Meta Information
 */

const meta = {
  title: "Forms / AdjustableValueTextInputFormField",
} satisfies Meta;

export default meta;

/**
 * Form Schema
 */

const formSchema = z.object({
  username: adjustableValueTextSchema,
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

export const AdjustableValueTextInputFormFieldStory: StoryObj<Args> = {
  name: "AdjustableValueTextInputFormField",
  args: {
    hint: "Dieser Benutzername wurde aus Ihrem Namen und den verfügbaren Benutzernamen ermittelt.",
  },
  render: (args: Args) => {
    const form = useForm<FormValues>({
      resolver: zodResolver(formSchema),
      defaultValues: {
        username: {
          type: "Auto",
          value: "john.doe",
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
          <AdjustableValueTextInputFormField
            {...args}
            formSchema={formSchema}
            fieldName="username"
            label="Einspeisevergütung"
            description="The price can be overridden."
          />
          <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
        </Form>
        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
