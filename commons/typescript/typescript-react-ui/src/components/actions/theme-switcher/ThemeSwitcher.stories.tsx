import type { Story, StoryDefault } from "@ladle/react";
import { ThemeProvider, type ThemeDefinition } from "../../../providers/theme";
import { Button } from "../../primitives/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../primitives/card";
import { ThemeSwitcher } from "./ThemeSwitcher";

export default {
  title: "Actions / ThemeSwitcher",
} satisfies StoryDefault;

const themes: ThemeDefinition[] = [
  { id: "neckar", label: "Neckar", colorScheme: "selectable" },
  { id: "terminal", label: "Terminal", colorScheme: "dark" },
  { id: "aurora", label: "Aurora", colorScheme: "dark" },
  { id: "paper", label: "Paper", colorScheme: "light" },
  { id: "fjord", label: "Fjord", colorScheme: "light" },
];

export const ThemeSwitcherStory: Story = () => {
  return (
    <ThemeProvider themes={themes}>
      <div className="flex flex-col gap-4">
        <div className="flex justify-end">
          <ThemeSwitcher />
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Theme preview</CardTitle>
            <CardDescription>Switch the theme with the button above.</CardDescription>
          </CardHeader>
          <CardContent className="flex gap-2">
            <Button>Primary</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="outline">Outline</Button>
            <Button variant="destructive">Destructive</Button>
          </CardContent>
        </Card>
      </div>
    </ThemeProvider>
  );
};
ThemeSwitcherStory.storyName = "ThemeSwitcher";
