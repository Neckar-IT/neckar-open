import type { Story, StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import type { ArticleConditionsTimelineTableEntry } from "@neckar-hub/typescript-utils/conditions-timeline";
import type { FullArticleConditions } from "@neckar-hub/typescript-utils/openapi-types";
import { DataTable } from "../data-display/table";
import { TruncateTextContent } from "../text/text-overflow";
import { fullArticleConditionsColumns } from "./articleConditionsTimelineEntryTableColumns";
import { fullConditionsTableEntry, materialOnlyTableEntry } from "./conditionsFixtures";

export default {
  title: "Conditions timeline / ArticleConditionsTimelineTableColumns",
  decorators: [
    (Story) => (
      <WithMemoryRouter>
        <Story />
      </WithMemoryRouter>
    ),
  ],
} satisfies StoryDefault;

/** Four-digit amounts and a three-digit rate: the widest figures the part and accessory columns show whole. */
const widestEntry: ArticleConditionsTimelineTableEntry<FullArticleConditions> = {
  ...fullConditionsTableEntry,
  validFrom: "2024-01-01T00:00:00Z",
  priceSummary: {
    unit: "Quadratmeter",
    part: { inputPrice: 188888, sellingPrice: 988888, surcharge: 800000, surchargeRate: 4.235 },
    accessory: { inputPrice: 188888, sellingPrice: 988888, surcharge: 800000, surchargeRate: 4.235 },
    labour: { installationTime: "PT88H30M", hourlyRate: 18888, sellingPrice: 888888 },
    customerPrice: 2866664,
  },
};

export const ArticleConditionsTimelineTableColumnsStory: Story = () => (
  <div className="max-w-5xl">
    {/* The truncation zone every app installs on its body: it turns each cell's marker into a flex container. */}
    <TruncateTextContent>
      <DataTable
        data={[fullConditionsTableEntry, materialOnlyTableEntry, widestEntry]}
        config={{ columns: fullArticleConditionsColumns() }}
      />
    </TruncateTextContent>
  </div>
);
ArticleConditionsTimelineTableColumnsStory.storyName = "Table columns";
