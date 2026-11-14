/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { QueryClient, QueryClientProvider, useMutation } from "@neckar-hub/typescript-react-query";
import { delay } from "es-toolkit";
import { useState } from "react";
import { type UseFormReturn } from "react-hook-form";
import { z as zod } from "zod";
import { TextInputFormField } from "../../forms/text-input-form-field";
import { Button } from "../../primitives/button";
import { FormDialog } from "./FormDialog";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Overlays / FormDialog",
  decorators: [
    (Story) => {
      const [queryClient] = useState(() => new QueryClient());

      return (
        <QueryClientProvider client={queryClient}>
          <Story />
        </QueryClientProvider>
      );
    },
  ],
};

export default meta;

/**
 * Stories
 */

const formSchema = zod.object({
  name: zod.string().nonempty(),
  email: zod.email(),
});

type FormSchema = zod.infer<typeof formSchema>;

export const FormDialogStory: Story = () => {
  const [open, setOpen] = useState(true);

  const defaultValues = {
    name: "",
    email: "",
  };

  const { mutateAsync } = useMutation({
    mutationFn: async (values: FormSchema) => {
      await delay(1000);
      console.log("Form submitted", values);
      return { type: "good" };
    },
  });

  const handleSubmit = async (values: FormSchema, form: UseFormReturn<FormSchema>) => {
    await mutateAsync(values, {
      onSuccess: (response) => {
        if (response.type === "good") {
          form.reset();
          setOpen(false);
        }
      },
    });
  };

  return (
    <>
      <Button onClick={() => setOpen(true)}>Open Dialog</Button>

      <FormDialog
        defaultValues={defaultValues}
        formSchema={formSchema}
        open={open}
        textKeys={{ title: "Contact Details" }}
        onOpenChange={setOpen}
        onSubmit={handleSubmit}
      >
        <div className="space-y-4">
          <TextInputFormField formSchema={formSchema} label="Name" fieldName="name" />
          <TextInputFormField formSchema={formSchema} label="Email" fieldName="email" />
        </div>
      </FormDialog>
    </>
  );
};
FormDialogStory.storyName = "Form Dialog";
