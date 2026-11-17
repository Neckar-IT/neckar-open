import { Line, LineChart, ResponsiveContainer, Tooltip } from "recharts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/primitives/card";

const data = [
  {
    average: 400,
    today: 240,
  },
  {
    average: 300,
    today: 139,
  },
  {
    average: 200,
    today: 980,
  },
  {
    average: 278,
    today: 390,
  },
  {
    average: 189,
    today: 480,
  },
  {
    average: 239,
    today: 380,
  },
  {
    average: 349,
    today: 430,
  },
];

export function CardsMetric() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Exercise Minutes</CardTitle>
        <CardDescription>Your exercise minutes are ahead of where you normally are.</CardDescription>
      </CardHeader>
      <CardContent className="pb-4">
        <div className="h-[200px]">
          <ResponsiveContainer height="100%" width="100%">
            <LineChart
              data={data}
              margin={{
                top: 5,
                right: 10,
                left: 10,
                bottom: 0,
              }}
            >
              <Tooltip
                content={({ active, payload }) => {
                  const [averagePayload, todayPayload] = payload;

                  if (!active || averagePayload === undefined || todayPayload === undefined) {
                    return null;
                  }

                  return (
                    <div className="bg-background rounded-lg border p-2 shadow-sm">
                      <div className="grid grid-cols-2 gap-2">
                        <div className="flex flex-col">
                          <span className="text-muted-foreground text-[0.70rem] uppercase">Average</span>
                          <span className="text-muted-foreground font-bold">{averagePayload.value}</span>
                        </div>
                        <div className="flex flex-col">
                          <span className="text-muted-foreground text-[0.70rem] uppercase">Today</span>
                          <span className="font-bold">{todayPayload.value}</span>
                        </div>
                      </div>
                    </div>
                  );
                }}
              />
              <Line
                activeDot={{
                  r: 6,
                  style: { fill: "var(--primary)", opacity: 0.25 },
                }}
                dataKey="average"
                strokeWidth={2}
                style={
                  {
                    stroke: "var(--primary)",
                    opacity: 0.25,
                  } as React.CSSProperties
                }
                type="monotone"
              />
              <Line
                activeDot={{
                  r: 8,
                  style: { fill: "var(--primary)" },
                }}
                dataKey="today"
                strokeWidth={2}
                style={
                  {
                    stroke: "var(--primary)",
                  } as React.CSSProperties
                }
                type="monotone"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
