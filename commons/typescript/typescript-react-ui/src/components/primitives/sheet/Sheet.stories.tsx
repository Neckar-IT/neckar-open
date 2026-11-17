/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../button";
import { Input } from "../input";
import { Label } from "../label";
import {
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  Sheet as SheetRoot,
  SheetTitle,
  SheetTrigger,
} from "./Sheet";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Sheet",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Sheet: StoryObj<typeof meta> = {
  render: () => {
    const [isOpen, setIsOpen] = useState(true);

    return (
      <SheetRoot open={isOpen} onOpenChange={setIsOpen}>
        <SheetTrigger asChild>
          <Button variant="outline">Open</Button>
        </SheetTrigger>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>Edit profile</SheetTitle>
            <SheetDescription>Make changes to your profile here. Click save when you are done.</SheetDescription>
          </SheetHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label className="justify-end" htmlFor="name">
                Name
              </Label>
              <Input className="col-span-3" id="name" defaultValue="Pedro Duarte" />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label className="justify-end" htmlFor="username">
                Username
              </Label>
              <Input className="col-span-3" id="username" defaultValue="@peduarte" />
            </div>
          </div>
          <SheetFooter>
            <SheetClose asChild>
              <Button type="submit">Save changes</Button>
            </SheetClose>
          </SheetFooter>
        </SheetContent>
      </SheetRoot>
    );
  },
};
