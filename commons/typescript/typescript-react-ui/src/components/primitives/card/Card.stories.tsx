/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { CheckIcon } from "lucide-react";
import { Button } from "../button";
import { CardContent, CardDescription, CardFooter, CardHeader, Card as CardRoot, CardTitle } from "./Card";

const meta = {
  title: "Primitives / Card",
  component: CardRoot,
} satisfies Meta;

export default meta;

/**
 * Mock Data
 */

const notifications = [
  {
    id: "EduOsuMEVR",
    title: "Your call has been confirmed.",
    description: "1 hour ago",
  },
  {
    id: "Yh1CK4Szb4",
    title: "You have a new message!",
    description: "1 hour ago",
  },
  {
    id: "EacoVIELxV",
    title: "Your subscription is expiring soon!",
    description: "2 hours ago",
  },
];

export const Card: StoryObj = {
  render: () => {
    return (
      <CardRoot className="w-96">
        <CardHeader>
          <CardTitle>Notifications</CardTitle>
          <CardDescription>You have 3 unread messages.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <div>
            {notifications.map((notification) => (
              <div key={notification.id} className="mb-4 grid grid-cols-[25px_1fr] items-start pb-4 last:mb-0 last:pb-0">
                <span className="flex h-2 w-2 translate-y-1 rounded-full bg-sky-500" />
                <div className="space-y-1">
                  <p className="text-sm leading-none font-medium">{notification.title}</p>
                  <p className="text-muted-foreground text-sm">{notification.description}</p>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
        <CardFooter>
          <Button className="w-full">
            <CheckIcon />
            <span>Mark all as read</span>
          </Button>
        </CardFooter>
      </CardRoot>
    );
  },
};
