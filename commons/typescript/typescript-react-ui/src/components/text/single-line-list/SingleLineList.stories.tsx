import type { Meta, StoryObj } from "@storybook/react-vite";
import { TooltipProvider } from "../../primitives/tooltip";
import { TruncateTextContent } from "../text-overflow";
import { SingleLineList } from "./SingleLineList";

const meta = {
  title: "Text / SingleLineList",
  component: SingleLineList,
} satisfies Meta;

export default meta;

const images: string[] = [
  "turboangebot.de/turboangebot-service@2f5a9c1d3e7b",
  "docker.io/postgres@898c60cb390d",
  "quay.io/keycloak/keycloak@1f3a6b2c4d5e",
  "agents.neckar.it/agents-job-runner@895348e22f47",
  "agents.neckar.it/agents-service@fa63b2b55958",
  "agents.neckar.it/agents-ui@acf400cafeb1",
  "docker.io/mongo@dc28ed5a9d0f",
  "ghcr.io/umami-software/umami@950133e41589",
  "docker.io/amir20/dozzle@4c2e8f1a9b3d",
];

/** One table row per list length; every row keeps the height of one line. */
export const SingleLineListStory: StoryObj = {
  name: "SingleLineList",
  render: () => (
    <TooltipProvider>
      <TruncateTextContent>
        <table className="w-full table-fixed border-collapse text-sm">
          <tbody>
            {[0, 1, 3, 9].map((count) => (
              <tr key={count} className="border-b">
                <td className="w-24 px-2 py-1 text-gray-500">{count} entries</td>
                <td className="w-48 px-2 py-1">
                  <span data-text-content>
                    <SingleLineList className="font-mono text-xs" items={images.slice(0, count)} />
                  </span>
                </td>
                <td className="px-2 py-1">
                  <span data-text-content>
                    <SingleLineList className="font-mono text-xs" items={images.slice(0, count)} />
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TruncateTextContent>
    </TooltipProvider>
  ),
};
