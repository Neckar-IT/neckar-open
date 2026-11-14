import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../../forms/form";
import { SelectVatRate, SelectVatRateFormField, VatRateOutput } from "./vatRate";

/**
 * Meta Information
 */
export default {
  title: "Quantities / vatRate",
} satisfies StoryDefault;

/**
 * Stories
 */
export const VatRateStory: Story = () => {
  const [value, setValue] = useState<number | null>(0.19);

  const formSchema = z.object({
    vatRate: z.number(),
  });

  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      vatRate: 0.19,
    },
  });

  const handleSubmit = form.handleSubmit((values) => {
    window.alert(JSON.stringify(values, null, 2));
  });

  return (
    <div className="mx-auto flex w-80 flex-col gap-12">
      <div>
        <pre className="font-bold">VAT Rate Output</pre>
        <VatRateOutput value={0.19} />
      </div>

      <div>
        <pre className="font-bold">Select VAT Rate</pre>
        <SelectVatRate value={value} onChange={setValue} />
        <CodeBlock className="mt-1">{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>

      <div>
        <pre className="font-bold">Select VAT Rate Form Field</pre>
        <Form {...form} onSubmit={handleSubmit}>
          <SelectVatRateFormField formSchema={formSchema} fieldName="vatRate" label="MwSt-Satz" />
          <FormSubmitButton className="mt-1" />
        </Form>
      </div>
    </div>
  );
};
VatRateStory.storyName = "VAT Rate";
