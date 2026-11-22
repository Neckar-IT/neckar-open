/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { DatePicker, type DatePickerProps } from "./Picker";

const meta = {
  title: "Inputs / Date Picker",
  component: DatePicker,
  args: {
    defaultMonth: new Date(2024, 5, 28),
    disabled: false,
    initiallyOpen: true,
  },
} satisfies Meta<DatePickerProps>;

export default meta;

export const Default: StoryObj<DatePickerProps> = {
  render: (args: DatePickerProps) => {
    const [value, setValue] = useState<Date | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const QuickSelects: StoryObj<DatePickerProps> = {
  args: {
    quickSelects: [
      { title: "Today", date: new Date(2024, 5, 28) },
      { title: "March, 1st", date: new Date(2024, 2, 1) },
    ],
  },
  render: (args: DatePickerProps) => {
    const [value, setValue] = useState<Date | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const MinAndMaxDate: StoryObj<DatePickerProps> = {
  args: {
    min: new Date(2024, 5, 17),
    max: new Date(2024, 5, 23),
  },
  render: (args: DatePickerProps) => {
    const [value, setValue] = useState<Date | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};

export const Disabled: StoryObj<DatePickerProps> = {
  args: {
    disabled: true,
    initiallyOpen: false,
  },
  render: (args: DatePickerProps) => {
    const [value, setValue] = useState<Date | undefined>(undefined);

    return (
      <div className="flex flex-col gap-8">
        <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
