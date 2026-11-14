/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { UserIcon } from "lucide-react";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { Form } from ".";
import { TextInputFormField } from "../text-input-form-field";

/**
 * Meta Information
 */

const meta: StoryDefault<{ disabled: boolean; required: boolean }> = {
  title: "Forms / Form",
  args: {
    disabled: false,
    required: false,
  },
  argTypes: {
    disabled: {
      control: { type: "boolean" },
    },
    required: {
      control: { type: "boolean" },
    },
  },
};

export default meta;

/**
 * Stories
 */

export const FormFieldLabelStory: Story<{ disabled: boolean; required: boolean }> = (props) => {
  const formSchema = zod.object({
    name: zod.string().nonempty(),
  });

  type FormSchema = zod.infer<typeof formSchema>;

  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "John Doe",
    },
  });

  return (
    <Form {...form}>
      <div className="flex flex-col gap-8">
        <dl className="flex flex-col gap-2">
          <dt>
            <pre className="bg-accent w-min rounded-sm px-2 py-1 text-sm">string label</pre>
          </dt>
          <dd>
            <TextInputFormField formSchema={formSchema} fieldName="name" label="Name" className="w-80" disabled={props.disabled} />
          </dd>
        </dl>
        <dl className="flex flex-col gap-2">
          <dt>
            <pre className="bg-accent w-min rounded-sm px-2 py-1 text-sm">label component</pre>
          </dt>
          <dd>
            <TextInputFormField
              className="w-80"
              disabled={props.disabled}
              formSchema={formSchema}
              fieldName="name"
              labelComponent={
                <div className="flex items-center gap-1.5">
                  <UserIcon className="h-4 w-4" />
                  <span>Name</span>
                </div>
              }
            />
          </dd>
        </dl>
        <dl className="flex flex-col gap-2">
          <dt>
            <pre className="bg-accent w-min rounded-sm px-2 py-1 text-sm">no label</pre>
          </dt>
          <dd>
            <TextInputFormField className="w-80" formSchema={formSchema} fieldName="name" disabled={props.disabled} />
          </dd>
        </dl>
      </div>
    </Form>
  );
};
FormFieldLabelStory.storyName = "FormFieldLabel";
