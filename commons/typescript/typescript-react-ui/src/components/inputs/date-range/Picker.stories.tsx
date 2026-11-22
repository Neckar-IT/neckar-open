/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { type DateRange, DateRangePicker, type DateRangePickerProps } from "./Picker";

const meta = {
  title: "Inputs / Date Range Picker",
  component: DateRangePicker,
  args: {
    defaultMonth: new Date(2024, 5, 28),
    disabled: false,
    initiallyOpen: true,
  },
} satisfies Meta<DateRangePickerProps>;

export default meta;

export const Default: StoryObj<DateRangePickerProps> = {
  render: (args: DateRangePickerProps) => {
    const [value, setValue] = useState<DateRange | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DateRangePicker {...args} value={value} onChange={setValue} className="w-96" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const QuickSelects: StoryObj<DateRangePickerProps> = {
  args: {
    quickSelects: [
      { title: "2024, Q1", from: new Date(2024, 0, 1), to: new Date(2024, 2, 31) },
      { title: "2024, Q2", from: new Date(2024, 3, 1), to: new Date(2024, 5, 30) },
    ],
  },
  render: (args: DateRangePickerProps) => {
    const [value, setValue] = useState<DateRange | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DateRangePicker {...args} value={value} onChange={setValue} className="w-96" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const MinAndMaxDate: StoryObj<DateRangePickerProps> = {
  args: {
    min: new Date(2024, 5, 17),
    max: new Date(2024, 5, 23),
  },
  render: (args: DateRangePickerProps) => {
    const [value, setValue] = useState<DateRange | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DateRangePicker {...args} value={value} onChange={setValue} className="w-96" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
