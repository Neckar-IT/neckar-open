import type { Story, StoryDefault } from "@ladle/react";
import type { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { CodeBlock } from "../data-display/code-block";
import { FilterByConfirmationStatus } from "./FilterByConfirmationStatus";

export default {
  title: "Draft confirmed / FilterByConfirmationStatus",
} satisfies StoryDefault;

export const FilterByConfirmationStatusStory: Story = () => {
  const [filter, setFilter] = useState<ConfirmationStatus | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <FilterByConfirmationStatus value={filter} onChange={setFilter} />
      <FilterByConfirmationStatus form="Panel" value={filter} onChange={setFilter} />
      <CodeBlock>{`filter: ${JSON.stringify(filter, null, 2)}`}</CodeBlock>
    </div>
  );
};
