import { Minus, Plus } from "lucide-react";
import { useState } from "react";
import { Bar, BarChart, ResponsiveContainer } from "recharts";
import { Button } from "../components/primitives/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../components/primitives/card";

const data = [
  {
    goal: 400,
  },
  {
    goal: 300,
  },
  {
    goal: 200,
  },
  {
    goal: 300,
  },
  {
    goal: 200,
  },
  {
    goal: 278,
  },
  {
    goal: 189,
  },
  {
    goal: 239,
  },
  {
    goal: 300,
  },
  {
    goal: 200,
  },
  {
    goal: 278,
  },
  {
    goal: 189,
  },
  {
    goal: 349,
  },
];

export function CardsActivityGoal() {
  const [goal, setGoal] = useState(350);

  function onClick(adjustment: number) {
    setGoal(Math.max(200, Math.min(400, goal + adjustment)));
  }

  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-base">Move Goal</CardTitle>
        <CardDescription>Set your daily activity goal.</CardDescription>
      </CardHeader>
      <CardContent className="pb-2">
        <div className="flex items-center justify-center space-x-2">
          <Button
            className="h-8 w-8 shrink-0 rounded-full"
            disabled={goal <= 200}
            size="icon"
            variant="outline"
            onClick={() => {
              onClick(-10);
            }}
          >
            <Minus className="h-4 w-4" />
            <span className="sr-only">Decrease</span>
          </Button>
          <div className="flex-1 text-center">
            <div className="text-5xl font-bold tracking-tighter">{goal}</div>
            <div className="text-muted-foreground text-[0.70rem] uppercase">Calories/day</div>
          </div>
          <Button
            className="h-8 w-8 shrink-0 rounded-full"
            disabled={goal >= 400}
            size="icon"
            variant="outline"
            onClick={() => {
              onClick(10);
            }}
          >
            <Plus className="h-4 w-4" />
            <span className="sr-only">Increase</span>
          </Button>
        </div>
        <div className="my-3 h-[60px]">
          <ResponsiveContainer height="100%" width="100%">
            <BarChart data={data}>
              <Bar
                dataKey="goal"
                style={
                  {
                    fill: "var(--primary)",
                    opacity: 0.2,
                  } as React.CSSProperties
                }
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
      <CardFooter>
        <Button className="w-full">Set Goal</Button>
      </CardFooter>
    </Card>
  );
}
