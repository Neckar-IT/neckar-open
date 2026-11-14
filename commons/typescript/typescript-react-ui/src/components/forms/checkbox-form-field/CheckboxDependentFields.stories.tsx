import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../form";
import { TextInputFormField } from "../text-input-form-field";
import { CheckboxDependentFields } from "./CheckboxDependentFields";
import { CheckboxFormField } from "./CheckboxFormField";

export default {
  title: "Forms / CheckboxDependentFields",
} satisfies StoryDefault;

const formSchema = z.object({
  withDeliveryAddress: z.boolean(),
  street: z.string().nullish(),
  city: z.string().nullish(),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * Stories
 */

// The gated fields are mounted only while the box is checked, and indented to its label.
export const CheckboxDependentFieldsStory: Story = () => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: { withDeliveryAddress: false },
  });

  const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
    await delay(500);
    window.alert(JSON.stringify(values, null, 2));
  });

  const withDeliveryAddress = form.watch("withDeliveryAddress");
  const values = form.watch();

  return (
    <div className="flex w-96 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <CheckboxFormField
          formSchema={formSchema}
          fieldName="withDeliveryAddress"
          label="Different delivery address"
          description="Deliver to an address other than the billing address."
        />

        {withDeliveryAddress && (
          <CheckboxDependentFields>
            <TextInputFormField formSchema={formSchema} fieldName="street" label="Street" />
            <TextInputFormField formSchema={formSchema} fieldName="city" label="City" />
          </CheckboxDependentFields>
        )}

        <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
      </Form>
      <CodeBlock>{`values: ${JSON.stringify(values)}`}</CodeBlock>
    </div>
  );
};
CheckboxDependentFieldsStory.storyName = "CheckboxDependentFields";
