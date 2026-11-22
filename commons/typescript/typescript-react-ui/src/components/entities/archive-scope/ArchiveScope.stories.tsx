import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { ArchiveScope } from "./ArchiveScope";

const meta = {
  title: "Entities / ArchiveScope",
  component: ArchiveScope,
} satisfies Meta;

export default meta;

export const ArchiveScopeStory: StoryObj = {
  render: () => {
    const [value, setValue] = useState<ArchiveFilter>(ArchiveFilter.OnlyActive);

    return (
      <div className="flex flex-col gap-8">
        <ArchiveScope label="Archiv" value={value} onChange={setValue} />

        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
