import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../data-display/code-block";
import { ConfirmationStatusBadge } from "./ConfirmationStatusBadge";

const meta = {
  title: "Draft confirmed / ConfirmationStatusBadge",
} satisfies Meta;

export default meta;

const variants = ["icon", "with-text"] as const;

export const ConfirmationStatusBadgeStory: StoryObj<typeof meta> = {
  render: () => {
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
  },
};
