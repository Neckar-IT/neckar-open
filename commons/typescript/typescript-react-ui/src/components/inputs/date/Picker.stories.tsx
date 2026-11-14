/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { DatePicker, type DatePickerProps } from "./Picker";

/**
 * Meta Information
 */

export default {
  title: "Inputs / Date Picker",
  args: {
    defaultMonth: new Date(2024, 5, 28),
    disabled: false,
    initiallyOpen: true,
  },
} satisfies StoryDefault<DatePickerProps>;

/**
 * Stories
 */

export const Default: Story<DatePickerProps> = (args) => {
  const [value, setValue] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};

export const QuickSelects: Story<DatePickerProps> = (args) => {
  const [value, setValue] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
QuickSelects.args = {
  quickSelects: [
    { title: "Today", date: new Date(2024, 5, 28) },
    { title: "March, 1st", date: new Date(2024, 2, 1) },
  ],
};

export const MinAndMaxDate: Story<DatePickerProps> = (args) => {
  const [value, setValue] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
MinAndMaxDate.args = {
  min: new Date(2024, 5, 17),
  max: new Date(2024, 5, 23),
};

export const Disabled: Story<DatePickerProps> = (args) => {
  const [value, setValue] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DatePicker {...args} value={value} onChange={setValue} className="w-80" />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
Disabled.args = {
  disabled: true,
  initiallyOpen: false,
};
