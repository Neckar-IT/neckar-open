import type { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../data-display/code-block";
import { FilterByConfirmationStatus } from "./FilterByConfirmationStatus";

const meta = {
  title: "Draft confirmed / FilterByConfirmationStatus",
} satisfies Meta;

export default meta;

export const FilterByConfirmationStatusStory: StoryObj<typeof meta> = {
  render: () => {
    const [filter, setFilter] = useState<ConfirmationStatus | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <FilterByConfirmationStatus value={filter} onChange={setFilter} />
        <FilterByConfirmationStatus form="Panel" value={filter} onChange={setFilter} />
        <CodeBlock>{`filter: ${JSON.stringify(filter, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
