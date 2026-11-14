import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { CancelButton } from ".";
import { Form, FormBody, FormCancelButton } from "../../forms/form";
import { TextInputFormField } from "../../forms/text-input-form-field";
import { ButtonBar } from "../../layout/button-bar";
import { Button } from "../../primitives/button";

const meta: StoryDefault = {
  title: "Actions / CancelButton",
};

export default meta;

export const Default: Story = () => (
  <div className="p-8">
    <CancelButton onClick={() => {}} />
  </div>
);

const schema = z.object({ name: z.string().min(1, "Darf nicht leer sein") });

/**
 * Repro harness for issue #2431: an `onTouched` form with a required field. Clicking a button
 * while the empty field is focused blurs it, the validation message renders, the layout shifts —
 * and a raw button loses the click (`mousedown`/`mouseup` on different elements), while the shared
 * `FormCancelButton` keeps focus on the field (`preventDefault` on mousedown) and always receives it.
 *
 * The click counters make the outcome observable for automated (Playwright) verification.
 */
function CancelClickVsBlurValidationStory(): React.ReactElement {
  const form = useForm<z.infer<typeof schema>>({
    mode: "onTouched",
    resolver: zodResolver(schema),
    defaultValues: { name: "" },
  });
  const [sharedCancelClicks, setSharedCancelClicks] = useState(0);
  const [rawCancelClicks, setRawCancelClicks] = useState(0);

  return (
    <div className="max-w-md p-8">
      <Form {...form}>
        <FormBody>
          <TextInputFormField formSchema={schema} fieldName="name" label="Name" />
          <ButtonBar>
            <FormCancelButton data-testid="shared-cancel" onClick={() => setSharedCancelClicks((count) => count + 1)} />
            <Button variant="outline" data-testid="raw-cancel" onClick={() => setRawCancelClicks((count) => count + 1)}>
              Roher Abbrechen-Button
            </Button>
          </ButtonBar>
          <div data-testid="click-counters" className="text-muted-foreground text-sm">
            shared: {sharedCancelClicks} / raw: {rawCancelClicks}
          </div>
        </FormBody>
      </Form>
    </div>
  );
}

export const CancelClickVsBlurValidation: Story = () => <CancelClickVsBlurValidationStory />;
