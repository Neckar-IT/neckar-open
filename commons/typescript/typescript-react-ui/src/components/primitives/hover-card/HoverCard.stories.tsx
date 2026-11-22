/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { CalendarDays } from "lucide-react";
import { avatar1 } from "../../../assets/avatars";
import { Avatar, AvatarFallback, AvatarImage } from "../avatar";
import { Button } from "../button";
import { HoverCardContent, HoverCard as HoverCardRoot, HoverCardTrigger } from "./HoverCard";

const meta = {
  title: "Primitives / HoverCard",
  component: HoverCardRoot,
} satisfies Meta;

export default meta;

export const HoverCard: StoryObj = {
  render: () => {
    return (
      <HoverCardRoot>
        <HoverCardTrigger asChild>
          <Button variant="link">@jd</Button>
        </HoverCardTrigger>
        <HoverCardContent className="w-80">
          <div className="flex space-x-4">
            <Avatar>
              <AvatarImage src={avatar1} />
              <AvatarFallback>VC</AvatarFallback>
            </Avatar>
            <div className="space-y-1">
              <h4 className="text-sm font-semibold">Jane Doe</h4>
              <div className="flex items-center">
                <CalendarDays className="mr-2 h-4 w-4 opacity-70" />{" "}
                <span className="text-muted-foreground text-xs">Joined December 2021</span>
              </div>
            </div>
          </div>
        </HoverCardContent>
      </HoverCardRoot>
    );
  },
};
