import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { RatingInputFormField } from "./RatingInputFormField";

interface Args {
  disabled: boolean;
}

const meta = {
  title: "Forms / RatingInputFormField",
} satisfies Meta;

export default meta;

const formSchema = z.object({
  rating: z.number().int().min(1).max(5),
});

type FormSchema = z.infer<typeof formSchema>;

export const RatingInputFormFieldStory: StoryObj<Args> = {
  name: "RatingInputFormField",
  args: {
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
      defaultValues: {
        rating: 3,
      },
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(300);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <RatingInputFormField label="Rating" fieldName="rating" formSchema={formSchema} disabled={args.disabled} />

          <Button type="submit" loading={form.formState.isSubmitting} className="mt-4">
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
