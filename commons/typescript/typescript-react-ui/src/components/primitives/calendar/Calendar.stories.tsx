/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Calendar as CalendarComponent } from "./Calendar";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Calendar",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Calendar: StoryObj<typeof meta> = {
  render: () => {
    const [date, setDate] = useState<Date | undefined>(() => new Date(2024, 5, 27, 13, 40));

    return <CalendarComponent className="rounded-md border" defaultMonth={date} mode="single" selected={date} onSelect={setDate} />;
  },
};
