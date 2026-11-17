import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { Trash2Icon } from "lucide-react";
import { useFieldArray, useForm } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { ButtonBar } from "../../layout/button-bar";
import { Button } from "../../primitives/button";
import { NumberInputFormField } from "../number-input-form-field";
import { TextInputFormField } from "../text-input-form-field";
import { FormArrayErrorMessage } from "./FormArrayErrorMessage";
import { FormArrayRow } from "./FormArrayRow";
import { FormCard, FormCardContent, FormCardFooter } from "./FormCard";
import { FormSectionHeader } from "./FormSectionHeader";
import { FormSubmitButton } from "./FormSubmitButton";

const meta = {
  title: "Forms / Form",
} satisfies Meta;

export default meta;

/**
 * The repeatable rows are a `z.array(...)` of an object schema: the element schema validates one
 * row, `.min(1)` validates the list as a whole.
 */
const formSchema = z.object({
  title: z.string().nonempty("Enter a recipe title"),
  ingredients: z
    .array(
      z.object({
        name: z.string().nonempty("Enter an ingredient name"),
        amountInGrams: z.number().int().positive("Enter an amount greater than 0"),
      }),
    )
    .min(1, "Add at least one ingredient"),
});

type FormSchema = z.infer<typeof formSchema>;

const emptyIngredient: FormSchema["ingredients"][number] = { name: "", amountInGrams: 0 };

export const ArrayFormFieldsStory: StoryObj<typeof meta> = {
  name: "Array Fields",
  render: () => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
      defaultValues: {
        title: "Pancakes",
        ingredients: [
          { name: "Flour", amountInGrams: 300 },
          { name: "Milk", amountInGrams: 500 },
        ],
      },
    });

    /**
     * `fields` carries a stable `id` per row — it is the React key, and it is what a row action
     * resolves the row index by. Never key rows by their index or their values.
     */
    const { fields, append, remove } = useFieldArray({ control: form.control, name: "ingredients" });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const ingredients = form.watch("ingredients");

    return (
      <div className="flex w-2xl flex-col gap-8">
        <FormCard form={form} onSubmit={handleSubmit}>
          <FormCardContent>
            <TextInputFormField formSchema={formSchema} fieldName="title" label="Recipe" />

            <FormSectionHeader>Ingredients</FormSectionHeader>

            {fields.map((field, index) => (
              <FormArrayRow
                key={field.id}
                action={
                  <Button variant="ghost" size="icon" type="button" onClick={() => remove(index)} aria-label="Remove ingredient">
                    <Trash2Icon />
                  </Button>
                }
              >
                <TextInputFormField
                  className="flex-1"
                  formSchema={formSchema}
                  fieldName={`ingredients.${index}.name`}
                  label="Name"
                  placeholder="Sugar"
                />
                <NumberInputFormField
                  className="w-36"
                  formSchema={formSchema}
                  fieldName={`ingredients.${index}.amountInGrams`}
                  label="Amount"
                  unit="g"
                />
              </FormArrayRow>
            ))}

            <FormArrayErrorMessage formSchema={formSchema} fieldName="ingredients" />

            <Button className="self-start" variant="secondary" type="button" onClick={() => append(emptyIngredient)}>
              Add ingredient
            </Button>
          </FormCardContent>

          <FormCardFooter>
            <ButtonBar>
              <FormSubmitButton />
            </ButtonBar>
          </FormCardFooter>
        </FormCard>

        <CodeBlock>{`ingredients: ${JSON.stringify(ingredients, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
