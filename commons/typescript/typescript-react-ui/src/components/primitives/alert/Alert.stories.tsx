/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Terminal } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "./Alert";

const meta = {
  title: "Primitives / Alert",
  component: Alert,
} satisfies Meta;

export default meta;

export const AlertStory: StoryObj = {
  name: "Alert",
  render: () => {
    return (
      <div className="space-y-8">
        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="default"</pre>
          <Alert>
            <Terminal className="h-4 w-4" />
            <AlertTitle>Heads up!</AlertTitle>
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>

        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="destructive"</pre>
          <Alert variant="destructive">
            <Terminal className="h-4 w-4" />
            <AlertTitle>Heads up!</AlertTitle>
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>

        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="info"</pre>
          <Alert variant="info">
            <Terminal className="h-4 w-4" />
            <AlertTitle>Heads up!</AlertTitle>
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>

        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="muted"</pre>
          <Alert variant="muted">
            <Terminal className="h-4 w-4" />
            <AlertTitle>Heads up!</AlertTitle>
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  },
};

export const AlertWithoutTitleStory: StoryObj = {
  name: "Alert (description only)",
  render: () => {
    return (
      <div className="space-y-8">
        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="default"</pre>
          <Alert>
            <Terminal className="h-4 w-4" />
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>

        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="destructive"</pre>
          <Alert variant="destructive">
            <Terminal className="h-4 w-4" />
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>

        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="info"</pre>
          <Alert variant="info">
            <Terminal className="h-4 w-4" />
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>

        <div>
          <pre className="bg-muted mb-2 rounded-sm px-2 py-1 text-sm">variant="muted"</pre>
          <Alert variant="muted">
            <Terminal className="h-4 w-4" />
            <AlertDescription>You can add components to your app using the cli.</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  },
};
