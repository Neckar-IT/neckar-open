"use client";

import { addDays } from "date-fns";
import { Calendar } from "../components/primitives/calendar";
import { Card, CardContent } from "../components/primitives/card";

const start = new Date(2023, 5, 5);

export function CardsCalendar() {
  return (
    <Card className="max-w-[280px]">
      <CardContent className="p-0">
        <Calendar
          defaultMonth={start}
          mode="range"
          numberOfMonths={1}
          selected={{
            from: start,
            to: addDays(start, 8),
          }}
        />
      </CardContent>
    </Card>
  );
}
