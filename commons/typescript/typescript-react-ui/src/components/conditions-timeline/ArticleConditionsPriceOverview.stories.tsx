import type { Story, StoryDefault } from "@ladle/react";
import type { ArticleConditionsPriceSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { ArticleConditionsPriceOverview } from "./ArticleConditionsPriceOverview";

export default {
  title: "Conditions timeline / ArticleConditionsPriceOverview",
} satisfies StoryDefault;

const fullSummary: ArticleConditionsPriceSummary = {
  unit: null,
  part: { inputPrice: 2000, sellingPrice: 2500, surcharge: 500, surchargeRate: 0.25 },
  accessory: { inputPrice: 300, sellingPrice: 450, surcharge: 150, surchargeRate: 0.5 },
  labour: { installationTime: "PT30M", hourlyRate: 7500, sellingPrice: 3750 },
  customerPrice: 6700,
};

/** Part, accessory and labour — the article that ships with small material and is mounted. */
export const Full: Story = () => <ArticleConditionsPriceOverview summary={fullSummary} />;

/** A book: the part row alone. */
export const PartOnly: Story = () => (
  <ArticleConditionsPriceOverview summary={{ ...fullSummary, accessory: null, labour: null, customerPrice: 2500 }} />
);
PartOnly.storyName = "Part only";

/** An accessory price is still empty, so its surcharge and the customer price are too. */
export const Incomplete: Story = () => (
  <ArticleConditionsPriceOverview
    summary={{
      ...fullSummary,
      accessory: { inputPrice: 300, sellingPrice: null, surcharge: null, surchargeRate: null },
      customerPrice: null,
    }}
  />
);

/** Before the companion first answered. */
export const Pending: Story = () => <ArticleConditionsPriceOverview summary={undefined} />;
