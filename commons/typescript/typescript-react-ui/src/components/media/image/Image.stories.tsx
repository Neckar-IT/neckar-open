/**
 * Imports
 */

import { msw, type Story, type StoryDefault } from "@ladle/react";
import { RefreshCwIcon } from "lucide-react";
import { useState } from "react";
import exampleImage from "../../../assets/example-image.jpg";
import { Button } from "../../primitives/button";
import { Image } from "./Image";

/**
 * Meta Information
 */

export default {
  title: "Media / Image",
  msw: [
    msw.http.get("/slow-image", async () => {
      await new Promise((resolve) => window.setTimeout(resolve, 5000));
      const response = await window.fetch(exampleImage);
      const arrayBuffer = await response.arrayBuffer();
      return new msw.HttpResponse(arrayBuffer, {
        headers: { "Content-Type": "image/jpeg" },
      });
    }),
  ],
} satisfies StoryDefault;

/**
 * Stories
 */

export const ImageStory: Story = () => {
  const [key, setKey] = useState(0);

  return (
    <div className="grid grid-cols-4 gap-8">
      <div>
        <pre>src=&#123;url&#125;</pre>
        <Image className="h-64 rounded-md" src={exampleImage} alt="Matrix" />
      </div>
      <div>
        <pre>src="non-existing-image.jpg"</pre>
        <Image className="h-64 rounded-md" src="non-existing-image.jpg" alt="Matrix" />
      </div>
      <div>
        <pre>src=&#123;null&#125;</pre>
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
};
ImageStory.storyName = "Image";
