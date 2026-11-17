/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../../data-display/code-block";
import { Progress as ProgressComponent } from "./Progress";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Progress",
} satisfies Meta;

export default meta;

/**
 * Story
 */

export const Progress: StoryObj<typeof meta> = {
  render: () => (
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
  ),
};
