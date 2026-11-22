import { zodResolver } from "@hookform/resolvers/zod";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { SaveIcon } from "lucide-react";
import { useEffect } from "react";
import { useForm, type UseFormReturn } from "react-hook-form";
import { z } from "zod";
import { type Action, FormSubmitActionButton } from "../../actions";
import { ButtonBar } from "../../layout/button-bar";
import { NumberInputFormField } from "../number-input-form-field";
import { TextInputFormField } from "../text-input-form-field";
import { FormCancelButton } from "./FormCancelButton";
import { FormCard, FormCardContent, FormCardFooter } from "./FormCard";

const meta = {
  title: "Forms / Form",
  component: FormCard,
} satisfies Meta;

export default meta;

const formSchema = z.object({
  firstName: z.string().nonempty().nullable(),
  lastName: z.string().nonempty().nullable(),
  age: z.number().int().positive().nullable(),
});

type FormSchema = z.infer<typeof formSchema>;

function useSubmitFormAction(form: UseFormReturn<FormSchema>): Action {
  return {
    label: "Submit",
    icon: <SaveIcon />,
    onTrigger: () => {
      void form.handleSubmit((values) => {
        window.alert(`Form submitted with values:\n${JSON.stringify(values, null, 2)}`);
      })();
    },
  };
}

function PersonFormCard({ rootError }: { rootError?: string }): React.ReactElement {
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      firstName: "John",
      lastName: "Doe",
      age: 30,
    },
  });

  useEffect(() => {
    if (isDefined(rootError)) {
      form.setError("root", { type: "server", message: rootError });
    }
  }, [form, rootError]);

  return (
    <FormCard form={form}>
      <FormCardContent>
        <TextInputFormField formSchema={formSchema} fieldName="firstName" label="First Name" />
        <TextInputFormField formSchema={formSchema} fieldName="lastName" label="Last Name" />
        <NumberInputFormField formSchema={formSchema} fieldName="age" label="Age" />
      </FormCardContent>
      <FormCardFooter>
        <ButtonBar>
          <FormCancelButton onClick={() => window.alert("Form cancelled")} />
          <FormSubmitActionButton action={useSubmitFormAction(form)} />
        </ButtonBar>
      </FormCardFooter>
    </FormCard>
  );
}

export const FormCardStory: StoryObj = {
  name: "FormCard",
  render: () => <PersonFormCard />,
};

export const FormCardWithRootErrorStory: StoryObj = {
  name: "FormCard with root error",
  render: () => <PersonFormCard rootError="The age does not match the birth date on file." />,
};
