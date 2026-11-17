/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { AdjustableValueDisplay } from "./AdjustableValueDisplay";

/**
 * Meta Information
 */
const meta = {
  title: "Data display / AdjustableValueDisplay",
} satisfies Meta;

export default meta;

/**
 * Stories
 */
export const AdjustableValueDisplayStory: StoryObj<typeof meta> = {
  name: "AdjustableValueDisplay",
  render: () => (
    <div className="flex max-w-md flex-col gap-4">
      <Row label="Auto — calculated, untouched">
        <AdjustableValueDisplay value={{ type: "Auto", value: "John Ronald Reuel Tolkien" }} />
      </Row>

      <Row label="Adjusted — overridden, click the icon for the calculated value">
        <AdjustableValueDisplay value={{ type: "Adjusted", calculated: "George Orwell", value: "Orwell, George" }} />
      </Row>

      <Row label="Manual — provided by hand, nothing calculated behind it">
        <AdjustableValueDisplay value={{ type: "Manual", value: "Anonymus" }} />
      </Row>

      <Row label="Absent">
        <AdjustableValueDisplay value={null} />
      </Row>

      <Row label="Adjusted, non-string value rendered through renderValue">
        <AdjustableValueDisplay value={{ type: "Adjusted", calculated: 1200, value: 999 }} renderValue={(amount) => `${amount} €`} />
      </Row>
    </div>
  ),
};

function Row({ label, children }: { label: string; children: React.ReactNode }): React.ReactElement {
  return (
    <div className="flex flex-col gap-1">
      <div className="text-muted-foreground text-xs">{label}</div>
      <div>{children}</div>
    </div>
  );
}
