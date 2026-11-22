import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../../forms/form";
import { EuroInput, EuroInputFormField, EuroOutput } from "./euros";

const meta = {
  title: "Quantities / Money",
} satisfies Meta;

export default meta;

export const EurosStory: StoryObj<typeof meta> = {
  render: () => {
    const [value, setValue] = useState<number | undefined>(1234);

    const formSchema = z.object({
      price: z.number().min(0),
    });

    const form = useForm({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit((values) => {
      window.alert(JSON.stringify(values, null, 2));
    });

    return (
      <div className="mx-auto flex w-80 flex-col gap-12">
        <div>
          <pre className="font-bold">Euro Output</pre>
          <EuroOutput value={1234} />
        </div>

        <div>
          <pre className="font-bold">Euro Input</pre>
          <EuroInput value={value} onChange={setValue} />
          <CodeBlock className="mt-1">{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
        </div>

        <div>
          <pre className="font-bold">Euro Input Form Field</pre>
          <Form {...form} onSubmit={handleSubmit}>
            <EuroInputFormField formSchema={formSchema} fieldName="price" label="Preis" />
            <FormSubmitButton className="mt-1" />
          </Form>
        </div>
      </div>
    );
  },
};
