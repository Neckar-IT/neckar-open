/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { type DateRange, DateRangePicker, type DateRangePickerProps } from "./Picker";

/**
 * Meta Information
 */

export default {
  title: "Inputs / Date Range Picker",
  args: {
    defaultMonth: new Date(2024, 5, 28),
    disabled: false,
    initiallyOpen: true,
  },
} satisfies StoryDefault<DateRangePickerProps>;

/**
 * Stories
 */

export const Default: Story<DateRangePickerProps> = (args) => {
  const [value, setValue] = useState<DateRange | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DateRangePicker {...args} value={value} onChange={setValue} className="w-96" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};

export const QuickSelects: Story<DateRangePickerProps> = (args) => {
  const [value, setValue] = useState<DateRange | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DateRangePicker {...args} value={value} onChange={setValue} className="w-96" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
QuickSelects.args = {
  quickSelects: [
    { title: "2024, Q1", from: new Date(2024, 0, 1), to: new Date(2024, 2, 31) },
    { title: "2024, Q2", from: new Date(2024, 3, 1), to: new Date(2024, 5, 30) },
  ],
};

export const MinAndMaxDate: Story<DateRangePickerProps> = (args) => {
  const [value, setValue] = useState<DateRange | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DateRangePicker {...args} value={value} onChange={setValue} className="w-96" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
MinAndMaxDate.args = {
  min: new Date(2024, 5, 17),
  max: new Date(2024, 5, 23),
};
