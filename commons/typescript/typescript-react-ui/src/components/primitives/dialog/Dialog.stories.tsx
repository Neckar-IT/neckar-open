/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../button";
import { Input } from "../input";
import { Label } from "../label";
import {
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  Dialog as DialogRoot,
  DialogTitle,
  DialogTrigger,
} from "./Dialog";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Dialog",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Dialog: StoryObj<typeof meta> = {
  render: () => {
    const [isOpen, setIsOpen] = useState(true);

    return (
      <DialogRoot open={isOpen} onOpenChange={setIsOpen}>
        <DialogTrigger asChild>
          <Button variant="outline">Edit Profile</Button>
        </DialogTrigger>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Edit profile</DialogTitle>
            <DialogDescription>Make changes to your profile here. Click save when you are done.</DialogDescription>
          </DialogHeader>

          <DialogBody>
            <div className="grid gap-4">
              <div className="grid grid-cols-4 items-center gap-4">
                <Label className="justify-end" htmlFor="name">
                  Name
                </Label>
                <Input className="col-span-3" defaultValue="Pedro Duarte" id="name" />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label className="justify-end" htmlFor="username">
                  Username
                </Label>
                <Input className="col-span-3" defaultValue="@peduarte" id="username" />
              </div>
            </div>
          </DialogBody>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsOpen(false)}>
              Cancel
            </Button>
            <Button>Save changes</Button>
          </DialogFooter>
        </DialogContent>
      </DialogRoot>
    );
  },
};

export const DialogWithLongContent: StoryObj<typeof meta> = {
  render: () => {
    const [isOpen, setIsOpen] = useState(true);

    return (
      <DialogRoot open={isOpen} onOpenChange={setIsOpen}>
        <DialogTrigger asChild>
          <Button variant="outline">Open Long Dialog</Button>
        </DialogTrigger>
        <DialogContent size="md">
          <DialogHeader>
            <DialogTitle>Terms and Conditions</DialogTitle>
            <DialogDescription>Please review the following terms before proceeding.</DialogDescription>
          </DialogHeader>
          <DialogBody className="space-y-4">
            {Array.from({ length: 20 }, (_, i) => (
              <div key={i}>
                <h4 className="font-medium">Section {i + 1}</h4>
                <p className="text-muted-foreground text-sm">
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna
                  aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.
                </p>
              </div>
            ))}
          </DialogBody>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsOpen(false)}>
              Decline
            </Button>
            <Button>Accept</Button>
          </DialogFooter>
        </DialogContent>
      </DialogRoot>
    );
  },
};
