/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { ImageUploadFormField } from "./ImageUploadFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / ImageUploadFormField",
  decorators: [
    (Story) => (
      <div className="mx-auto max-w-screen-sm">
        <Story />
      </div>
    ),
  ],
};

export default meta;

/**
 * Args
 */

interface Args {
  disabled: boolean;
}

/**
 * Form Schema
 */

const ACCEPTED_FILE_TYPES = ["image/png", "image/jpeg"];

const formSchema = zod.object({
  image: zod
    .instanceof(File, { error: "Please select an image." })
    .refine((file) => ACCEPTED_FILE_TYPES.includes(file.type), "Es sind nur Bilddateien erlaubt."),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const ImageUploadFormFieldStory: Story<Args> = (args) => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      image: undefined,
    },
  });

  const handleSubmit = form.handleSubmit(async (data: FormSchema) => {
    await delay(1000);
    window.alert(data.image.name);
  });

  return (
    <Form {...form} onSubmit={handleSubmit}>
      <ImageUploadFormField
        {...args}
        accept={ACCEPTED_FILE_TYPES.join(",")}
        description="png and jpg, max 3 MB"
        formSchema={formSchema}
        label="Upload image"
        fieldName="image"
      />

      <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
        Upload
      </Button>
    </Form>
  );
};
ImageUploadFormFieldStory.storyName = "ImageUploadFormField";
ImageUploadFormFieldStory.args = {
  disabled: false,
};
