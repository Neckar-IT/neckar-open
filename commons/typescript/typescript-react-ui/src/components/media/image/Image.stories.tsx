/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { RefreshCwIcon } from "lucide-react";
import { http, HttpResponse } from "msw";
import { useState } from "react";
import exampleImage from "../../../assets/example-image.jpg";
import { Button } from "../../primitives/button";
import { Image } from "./Image";

const meta = {
  title: "Media / Image",
  component: Image,
  beforeEach: ({ msw }) => {
    msw.use(
      http.get("/slow-image", async () => {
        await new Promise((resolve) => window.setTimeout(resolve, 5000));
        const response = await window.fetch(exampleImage);
        const arrayBuffer = await response.arrayBuffer();
        return new HttpResponse(arrayBuffer, {
          headers: { "Content-Type": "image/jpeg" },
        });
      }),
    );
  },
} satisfies Meta;

export default meta;

export const ImageStory: StoryObj = {
  name: "Image",
  render: () => {
    const [key, setKey] = useState(0);

    return (
      <div className="grid grid-cols-4 gap-8">
        <div>
          <pre>{"src={url}"}</pre>
          <Image className="h-64 rounded-md" src={exampleImage} alt="Matrix" />
        </div>
        <div>
          <pre>src="non-existing-image.jpg"</pre>
          <Image className="h-64 rounded-md" src="non-existing-image.jpg" alt="Matrix" />
        </div>
        <div>
          <pre>{"src={null}"}</pre>
          <Image className="h-64 rounded-md" src={null} alt="Matrix" />
        </div>
        <div>
          <div className="flex items-center justify-between">
            <pre>Slow Server Response (5s)</pre>
            <Button variant="ghost" size="sm" className="h-6" onClick={() => setKey((k) => k + 1)}>
              <RefreshCwIcon />
            </Button>
          </div>
          <Image key={key} className="h-64 rounded-md" src="/slow-image" alt="Slow loading image" />
        </div>
      </div>
    );
  },
};
