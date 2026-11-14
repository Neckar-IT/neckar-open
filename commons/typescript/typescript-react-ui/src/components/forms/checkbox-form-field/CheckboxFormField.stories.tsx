import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../form";
import { CheckboxFormField } from "./CheckboxFormField";

export default {
  title: "Forms / CheckboxFormField",
} satisfies StoryDefault;

interface Args {
  label: string;
  description: string;
  disabled: boolean;
}

const formSchema = z.object({
  agree: z.boolean(),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const CheckboxFormFieldStory: Story<Args> = (args) => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
  });

  const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
    await delay(500);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-96 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <CheckboxFormField {...args} formSchema={formSchema} fieldName="agree" />
        <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
      </Form>
      <CodeBlock>{`values: ${JSON.stringify(values)}`}</CodeBlock>
    </div>
  );
};
CheckboxFormFieldStory.storyName = "CheckboxFormField";
CheckboxFormFieldStory.args = {
  label: "I agree to the terms and conditions",
  description: "Our terms and conditions apply always and everywhere.",
  disabled: false,
};
