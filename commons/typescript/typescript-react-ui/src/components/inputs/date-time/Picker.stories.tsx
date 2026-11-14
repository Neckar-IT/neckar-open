import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { DateTimePicker, type DateTimePickerProps } from "./Picker";

export default {
  title: "Inputs / Date Time Picker",
  args: {
    defaultMonth: new Date(2024, 5, 28),
    disabled: false,
  },
} satisfies StoryDefault<DateTimePickerProps>;

export const Default: Story<DateTimePickerProps> = (args) => {
  const [value, setValue] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DateTimePicker {...args} value={value} onChange={setValue} className="w-80" />
      <CodeBlock>{`value: ${value === undefined ? "undefined" : value.toISOString()}`}</CodeBlock>
    </div>
  );
};

export const FromAndTo: Story<DateTimePickerProps> = (args) => {
  const [from, setFrom] = useState<Date | undefined>(undefined);
  const [to, setTo] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center gap-4">
        <DateTimePicker {...args} value={from} onChange={setFrom} defaultTimeOfDay="start" max={to} className="w-80" />
        <span className="text-muted-foreground text-sm">→</span>
        <DateTimePicker {...args} value={to} onChange={setTo} defaultTimeOfDay="end" min={from} className="w-80" />
      </div>
      <CodeBlock>{`from: ${from === undefined ? "undefined" : from.toISOString()}\nto:   ${to === undefined ? "undefined" : to.toISOString()}`}</CodeBlock>
    </div>
  );
};

export const QuickSelects: Story<DateTimePickerProps> = (args) => {
  const [value, setValue] = useState<Date | undefined>(undefined);

  return (
    <div className="flex flex-col gap-8">
      <DateTimePicker {...args} value={value} onChange={setValue} className="w-80" />
      <CodeBlock>{`value: ${value === undefined ? "undefined" : value.toISOString()}`}</CodeBlock>
    </div>
  );
};
QuickSelects.args = {
  quickSelects: [
    { title: "Tomorrow 09:00", date: new Date(2024, 5, 29, 9, 0, 0, 0) },
    { title: "Next Monday 09:00", date: new Date(2024, 6, 1, 9, 0, 0, 0) },
  ],
};
