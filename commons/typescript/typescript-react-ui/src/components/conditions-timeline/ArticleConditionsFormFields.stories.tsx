import type { Meta, StoryObj } from "@storybook/react-vite";
import { useForm } from "react-hook-form";
import { MemoryRouter } from "react-router";
import { CodeBlock } from "../data-display/code-block";
import { FormCard, FormCardContent } from "../forms/form";
import {
  ArticleConditionsAccessoryFormFields,
  ArticleConditionsFormFields,
  ArticleConditionsInstallationTimeFormFields,
  ArticleConditionsUnitFormField,
} from "./ArticleConditionsFormFields";
import {
  articleConditionsTimelineEntryUpsertDefaultValues,
  useArticleConditionsTimelineEntryFormResolver,
  type ArticleConditionsTimelineEntryUpsertRequestSchema,
} from "./useArticleConditionsTimelineEntryFormResolver";

const meta = {
  title: "Conditions timeline / ArticleConditionsFormFields",
} satisfies Meta;

export default meta;

function useConditionsForm() {
  return useForm<ArticleConditionsTimelineEntryUpsertRequestSchema>({
    mode: "onTouched",
    resolver: useArticleConditionsTimelineEntryFormResolver(),
    defaultValues: articleConditionsTimelineEntryUpsertDefaultValues(
      { withAccessory: false, withInstallationTime: false },
      { conditions: null },
    ),
  });
}

/** A per-piece article (a book): the mandatory core alone, no optional parts. */
export const CoreOnly: StoryObj<typeof meta> = {
  name: "Core only",
  render: () => {
    const form = useConditionsForm();
    return (
      <MemoryRouter>
        <div className="max-w-md">
          <CodeBlock className="mb-4">core only (per-piece article, e.g. a book)</CodeBlock>
          <FormCard form={form}>
            <FormCardContent>
              <ArticleConditionsFormFields form={form} />
            </FormCardContent>
          </FormCard>
        </div>
      </MemoryRouter>
    );
  },
};

/** A fully-featured article: the core composed with every optional block. */
export const FullComposition: StoryObj<typeof meta> = {
  name: "Full composition",
  render: () => {
    const form = useConditionsForm();
    return (
      <MemoryRouter>
        <div className="max-w-md">
          <CodeBlock className="mb-4">core + unit + accessory + installation time</CodeBlock>
          <FormCard form={form}>
            <FormCardContent>
              <ArticleConditionsFormFields form={form} />
              <ArticleConditionsUnitFormField />
              <ArticleConditionsAccessoryFormFields form={form} />
              <ArticleConditionsInstallationTimeFormFields form={form} />
            </FormCardContent>
          </FormCard>
        </div>
      </MemoryRouter>
    );
  },
};
