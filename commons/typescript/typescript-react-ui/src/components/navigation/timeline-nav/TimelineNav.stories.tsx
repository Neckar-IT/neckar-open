/**
 * Imports
 */

import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { TimelineNav, type TimelineNavProps } from "./TimelineNav";

const meta = {
  title: "Navigation / Timeline Nav",
  component: TimelineNav,
  decorators: [
    (Story) => {
      return (
        <WithMemoryRouter path="/:stepId" params={{ stepId: "step-7" }}>
          <div className="mx-auto max-w-80 overflow-hidden">
            <Story />
          </div>
        </WithMemoryRouter>
      );
    },
  ],
} satisfies Meta<TimelineNavProps>;

export default meta;

export const TimelineNavStory: StoryObj<TimelineNavProps> = {
  name: "Timeline Nav",
  args: {
    data: [
      {
        title: "Vertrieb",
        leafs: [
          { type: "leaf", title: "Kundenerfassung", status: "completed", href: "/step-1" },
          { type: "leaf", title: "Projekterfassung", status: "completed", href: "/step-2" },
          { type: "leaf", title: "Bearbeitung", status: "warning", href: "/step-3" },
          { type: "leaf", title: "Angebot", status: "completed", href: "/step-4" },
          { type: "leaf", title: "Auftragsbestätigung", status: "completed", href: "/step-5" },
        ],
        status: "warning",
      },
      {
        title: "Arbeitsvorbereitung",
        leafs: [
          {
            type: "parallel-leafs",
            leafsOnTheLeft: [{ type: "leaf", title: "Netzvoranfrage", status: "completed", href: "/step-6-1" }],
            leafsOnTheRight: [{ type: "leaf", title: "Sondermaterial Bestellung", status: "completed", href: "/step-6-2" }],
          },
          { type: "leaf", title: "Montagemappe erstellen", status: "completed", href: "/step-7" },
          {
            type: "parallel-leafs",
            leafsOnTheLeft: [{ type: "leaf", title: "Montagetermin Dach einplanen", status: "completed", href: "/step-8-1" }],
            leafsOnTheRight: [
              { type: "leaf", title: "Montagetermin Keller einplanen", status: "completed", href: "/step-8-2" },
              { type: "leaf", title: "Wechselrichter bestellen", status: "completed", href: "/step-9-2" },
              { type: "leaf", title: "Kabel bestellen", status: "completed", href: "/step-9-3" },
            ],
          },
        ],
        status: "completed",
      },
      {
        title: "Montage",
        leafs: [
          {
            type: "parallel-leafs",
            leafsOnTheLeft: [
              { type: "leaf", title: "Montage Dach Vorbereitung test", status: "completed", href: "/step-10-1" },
              { type: "leaf", title: "Montage Dach", status: "in-progress", href: "/step-11-1" },
              { type: "leaf", title: "Kabel anschließen", status: "not-started", href: "/step-12-1" },
            ],
            leafsOnTheRight: [
              { type: "leaf", title: "Montage Keller Vorbereitung", status: "in-progress", href: "/step-10-2" },
              { type: "leaf", title: "Montage Keller", status: "not-started", href: "/step-11-2" },
            ],
          },
          { type: "leaf", title: "Zählertausch", status: "not-started", href: "/step-13" },
          { type: "leaf", title: "Inbetriebnahme", status: "not-started", href: "/step-14" },
          { type: "leaf", title: "Fertigmeldung", status: "not-started", href: "/step-15" },
        ],
        status: "in-progress",
      },
      {
        title: "Projektabschluss",
        leafs: [
          { type: "leaf", title: "Abschlussrechnung", status: "not-started", href: "/step-16" },
          { type: "leaf", title: "Dokumentation", status: "not-started", href: "/step-17" },
        ],
        status: "not-started",
      },
    ],
  },
  render: (props: TimelineNavProps) => <TimelineNav {...props} />,
};
