/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useState } from "react";
import { useForm, useFormContext } from "react-hook-form";
import { z } from "zod";
import { Form } from "../../forms/form";
import { NumberInputFormField } from "../../forms/number-input-form-field";
import { TextInputFormField } from "../../forms/text-input-form-field";
import { MultiStep } from "./MultiStep";
import { useOnNext, useOnPrevious } from "./MultiStepControl";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Navigation / MultiStep",
  decorators: [
    (Story) => (
      <div className="mx-auto w-full max-w-screen-md">
        <Story />
      </div>
    ),
  ],
};

export default meta;

/**
 * Form Schema
 */

const formSchema = z.object({
  firstName: z.string().nonempty(),
  lastName: z.string().nonempty(),
  email: z.email(),
  age: z.number().min(18),
});

type FormValues = z.infer<typeof formSchema>;

/**
 * Mock Steps
 */

function StepOne() {
  const form = useFormContext<FormValues>();

  useOnNext(async () => {
    const isValid = await form.trigger(["firstName", "lastName"]);
    return isValid;
  });
  useOnPrevious(() => {
    window.alert("You are at the first step and cannot go back.");
    return false;
  });

  return (
    <div className="flex flex-col gap-4">
      <TextInputFormField label="Vorname" fieldName="firstName" formSchema={formSchema} />
      <TextInputFormField label="Nachname" fieldName="lastName" formSchema={formSchema} />
    </div>
  );
}

function StepTwo() {
  const form = useFormContext<FormValues>();

  useOnNext(async () => {
    const isValid = await form.trigger("email");
    await delay(1000);
    return isValid;
  });
  useOnPrevious(() => true);

  return (
    <div>
      <TextInputFormField label="E-Mail" fieldName="email" formSchema={formSchema} />
    </div>
  );
}

function StepThree() {
  useOnNext(() => false);
  useOnPrevious(() => false);

  return (
    <div>
      <h1>Step Three</h1>
    </div>
  );
}

function StepFour() {
  const form = useFormContext<FormValues>();

  const handleSubmit = form.handleSubmit(async (values) => {
    await delay(1000);
    window.alert(`Form submitted: ${JSON.stringify(values, null, 2)}`);
  });

  useOnNext(async () => {
    await handleSubmit();
    return false;
  });
  useOnPrevious(() => true);

  return (
    <div>
      <NumberInputFormField formSchema={formSchema} fieldName="age" label="Alter" />
    </div>
  );
}

/**
 * Stories
 */

export const MultiStepStory: Story = () => {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      age: 0,
    },
  });

  return (
    <Form {...form}>
      <MultiStep
        steps={[
          { title: "Name", component: <StepOne /> },
          { title: "E-Mail", component: <StepTwo /> },
          { title: "Disabled", disabled: true, component: <StepThree /> },
          { title: "Alter", component: <StepFour /> },
        ]}
        currentStepIndex={currentStepIndex}
        onNext={setCurrentStepIndex}
        onPrevious={setCurrentStepIndex}
        isSubmitting={form.formState.isSubmitting}
      />
    </Form>
  );
};
MultiStepStory.storyName = "MultiStep";
