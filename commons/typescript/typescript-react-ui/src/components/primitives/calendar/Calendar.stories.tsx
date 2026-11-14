/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { Calendar as CalendarComponent } from "./Calendar";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Calendar",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Calendar: Story = () => {
  const [date, setDate] = useState<Date | undefined>(() => new Date(2024, 5, 27, 13, 40));

  return <CalendarComponent className="rounded-md border" defaultMonth={date} mode="single" selected={date} onSelect={setDate} />;
};
