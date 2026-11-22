import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useFieldArray, useForm } from "react-hook-form";
import * as zod from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form } from "../../forms/form";
import { NumberInputFormField } from "../../forms/number-input-form-field";
import { TextInputFormField } from "../../forms/text-input-form-field";
import { FieldArray, FieldArrayRow, FieldArrayRowFields, FieldArrayRowHeader, FieldArrayRowTitle } from "./FieldArray";
import { FieldArrayAddButton, FieldArrayRemoveButton } from "./FieldArrayButtons";

const meta = {
  title: "Layout / FieldArray",
  component: FieldArray,
} satisfies Meta;

export default meta;

const formSchema = zod.object({
  areas: zod.array(
    zod.object({
      label: zod.string().min(1),
      x: zod.number(),
      y: zod.number(),
      width: zod.number(),
      height: zod.number(),
    }),
  ),
});

type FormSchema = zod.infer<typeof formSchema>;

const emptyArea: FormSchema["areas"][number] = { label: "", x: 0, y: 0, width: 0, height: 0 };

/**
 * The card layout for a `useFieldArray` whose rows carry several fields: each row is a bordered card
 * with a title, a field grid, and a remove button; a full-width add button follows. For a compact
 * one-line list of narrow value objects, use `FormArrayRow` instead.
 */
export const MultiFieldCards: StoryObj = {
  render: () => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
      defaultValues: { areas: [{ label: "Roof south", x: 0, y: 0, width: 4, height: 2 }] },
    });
    const { fields, append, remove } = useFieldArray({ control: form.control, name: "areas" });
    const values = form.watch();

    return (
      <div className="flex w-2xl flex-col gap-8">
        <Form {...form}>
          <FieldArray>
            {fields.map((field, index) => (
              <FieldArrayRow key={field.id}>
                <FieldArrayRowHeader>
                  <FieldArrayRowTitle>Area {index + 1}</FieldArrayRowTitle>
                  <FieldArrayRemoveButton onClick={() => remove(index)} label="Remove area" />
                </FieldArrayRowHeader>
                <FieldArrayRowFields className="md:grid-cols-3">
                  <TextInputFormField formSchema={formSchema} fieldName={`areas.${index}.label`} label="Label" />
                  <NumberInputFormField formSchema={formSchema} fieldName={`areas.${index}.x`} label="X" />
                  <NumberInputFormField formSchema={formSchema} fieldName={`areas.${index}.y`} label="Y" />
                  <NumberInputFormField formSchema={formSchema} fieldName={`areas.${index}.width`} label="Width" />
                  <NumberInputFormField formSchema={formSchema} fieldName={`areas.${index}.height`} label="Height" />
                </FieldArrayRowFields>
              </FieldArrayRow>
            ))}
            <FieldArrayAddButton onClick={() => append(emptyArea)} label="Add area" />
          </FieldArray>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
