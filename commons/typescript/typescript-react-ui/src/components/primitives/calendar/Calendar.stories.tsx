/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Calendar as CalendarComponent } from "./Calendar";

const meta = {
  title: "Primitives / Calendar",
  component: CalendarComponent,
} satisfies Meta;

export default meta;

export const Calendar: StoryObj = {
  render: () => {
    const [date, setDate] = useState<Date | undefined>(() => new Date(2024, 5, 27, 13, 40));

    return <CalendarComponent className="rounded-md border" defaultMonth={date} mode="single" selected={date} onSelect={setDate} />;
  },
};
