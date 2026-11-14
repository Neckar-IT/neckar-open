import type { Story, StoryDefault } from "@ladle/react";
import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { ArchiveScope } from "./ArchiveScope";

export default {
  title: "Entities / ArchiveScope",
} satisfies StoryDefault;

export const ArchiveScopeStory: Story = () => {
  const [value, setValue] = useState<ArchiveFilter>(ArchiveFilter.OnlyActive);

  return (
    <div className="flex flex-col gap-8">
      <ArchiveScope label="Archiv" value={value} onChange={setValue} />

      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
