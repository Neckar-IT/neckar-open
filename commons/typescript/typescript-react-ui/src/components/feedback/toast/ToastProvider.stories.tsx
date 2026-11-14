/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Button } from "../../primitives/button";
import { toast } from "./toast";

/**
 * Meta Information
 */

export default {
  title: "Feedback / ToastProvider",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Toast: Story = () => {
  return (
    <div className="flex flex-col items-start gap-2">
      <Button variant="outline" onClick={() => toast.error("Could not save changes — please retry")}>
        Show Error Toast
      </Button>
      <Button variant="outline" onClick={() => toast.success("Changes saved")}>
        Show Success Toast
      </Button>
      <Button variant="outline" onClick={() => toast.info("New version available")}>
        Show Info Toast
      </Button>
      <Button variant="outline" onClick={() => toast.warning("Connection unstable")}>
        Show Warning Toast
      </Button>
      <Button
        variant="outline"
        onClick={() =>
          toast("Event has been created", {
            description: "Sunday, December 03, 2023 at 9:00 AM",
            action: {
              label: "Undo",
              onClick: () => toast.info("Undone"),
            },
          })
        }
      >
        Show Toast with Action
      </Button>
    </div>
  );
};
