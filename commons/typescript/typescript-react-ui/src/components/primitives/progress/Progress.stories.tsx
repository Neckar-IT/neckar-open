/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { CodeBlock } from "../../data-display/code-block";
import { Progress as ProgressComponent } from "./Progress";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Progress",
} satisfies StoryDefault;

/**
 * Story
 */

export const Progress: Story = () => (
  <div className="mx-auto flex w-3/5 flex-col gap-8">
    <div className="flex flex-col gap-2">
      <CodeBlock>{`<Progress value={62} />`}</CodeBlock>
      <ProgressComponent value={62} />
    </div>
    <div className="flex flex-col gap-2">
      <CodeBlock>{`<Progress value={62} label="Datei wird hochgeladen" />`}</CodeBlock>
      <ProgressComponent value={62} label="Datei wird hochgeladen" />
    </div>
    <div className="flex flex-col gap-2">
      <CodeBlock>{`<Progress value="indeterminate" />`}</CodeBlock>
      <ProgressComponent value="indeterminate" />
    </div>
    <div className="flex flex-col gap-2">
      <CodeBlock>{`<Progress value="indeterminate" label="Datei wird verarbeitet" />`}</CodeBlock>
      <ProgressComponent value="indeterminate" label="Datei wird verarbeitet" />
    </div>
  </div>
);
