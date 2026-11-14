import type { Story, StoryDefault } from "@ladle/react";
import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { CodeBlock } from "../data-display/code-block";
import { ConfirmationStatusBadge } from "./ConfirmationStatusBadge";

export default {
  title: "Draft confirmed / ConfirmationStatusBadge",
} satisfies StoryDefault;

const variants = ["icon", "with-text"] as const;

export const ConfirmationStatusBadgeStory: Story = () => {
  return (
    <div className="grid grid-cols-6 gap-4">
      {Object.values(ConfirmationStatus).map((status) =>
        variants.map((variant) => (
          <div key={`${status}-${variant}`}>
            <CodeBlock className="mb-4">{`${status} - ${variant}`}</CodeBlock>
            <ConfirmationStatusBadge status={status} variant={variant} />
          </div>
        )),
      )}
    </div>
  );
};
