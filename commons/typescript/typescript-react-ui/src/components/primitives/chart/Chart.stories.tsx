/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { isTestMode } from "@neckar-hub/typescript-react-utils/is-test-mode";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "./Chart";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Chart",
} satisfies StoryDefault;

/**
 * Stories
 */

const chartData = [
  { month: "January", desktop: 186, mobile: 80 },
  { month: "February", desktop: 305, mobile: 200 },
  { month: "March", desktop: 237, mobile: 120 },
  { month: "April", desktop: 73, mobile: 190 },
  { month: "May", desktop: 209, mobile: 130 },
  { month: "June", desktop: 214, mobile: 140 },
];

const chartConfig = {
  desktop: {
    label: "Desktop",
    color: "var(--chart-1)",
  },
  mobile: {
    label: "Mobile",
    color: "var(--chart-2)",
  },
} satisfies ChartConfig;

export const Chart: Story = () => {
  // disable animations for screenshot tests
  const testMode = isTestMode();

  return (
    <ChartContainer className="min-h-[200px] w-full" config={chartConfig}>
      <BarChart accessibilityLayer data={chartData}>
        <CartesianGrid vertical={false} />
        <XAxis axisLine={true} dataKey="month" tickFormatter={(value: string) => value.slice(0, 3)} tickLine={false} tickMargin={10} />
        <YAxis />
        <ChartTooltip content={<ChartTooltipContent />} />
        <Bar dataKey="desktop" fill="var(--color-desktop)" isAnimationActive={!testMode} radius={4} />
        <Bar dataKey="mobile" fill="var(--color-mobile)" isAnimationActive={!testMode} radius={4} />
      </BarChart>
    </ChartContainer>
  );
};
