/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import type { ReactElement } from "react";
import { Breadcrumbs } from "./Breadcrumbs";

const meta = {
  title: "Navigation / Breadcrumbs",
  component: Breadcrumbs,
} satisfies Meta;

export default meta;

export const Default: StoryObj = {
  render: () => {
    return (
      <Breadcrumbs>
        <a data-text-content href="#overview">
          Overview
        </a>
        <span data-text-content className="font-mono">
          f247c3742da
        </span>
      </Breadcrumbs>
    );
  },
};

/**
 * A renderer item wider than its share of the trail ends with an ellipsis; the separator keeps its size.
 */
export const LongItem: StoryObj = {
  render: () => {
    return (
      <div className="flex w-[480px] items-center">
        <Breadcrumbs>
          <a data-text-content href="#overview">
            Overview
          </a>
          <span data-text-content className="font-mono text-sm">
            <span>:services:orchestration:agent-orchestrator-deployment-with-a-long-module-path</span>
          </span>
        </Breadcrumbs>
      </div>
    );
  },
};

/**
 * Trail of a customer project, two short items and two long ones, in a flex row like the app header.
 */
function CustomerProjectTrail({ widthClassName }: { widthClassName: string }): ReactElement {
  return (
    <div className={`flex items-center ${widthClassName}`} data-testid="breadcrumbs-container">
      <Breadcrumbs>
        <a data-text-content href="#dashboard">
          Dashboard
        </a>
        <a data-text-content href="#customers">
          Kunden
        </a>
        <a data-text-content href="#customer">
          <span>Elektro Müller GmbH &amp; Co. KG Niederlassung Stuttgart-Feuerbach</span>
        </a>
        <span data-text-content>
          <span>PV-Projekt 2026-0142 Dachanlage Süd</span>
        </span>
      </Breadcrumbs>
    </div>
  );
}

/**
 * At 640 px the trail does not fit: the long items end with an ellipsis, the short items keep their text.
 */
export const TrailAt640: StoryObj = {
  render: () => {
    return <CustomerProjectTrail widthClassName="w-[640px]" />;
  },
};

/**
 * At 1280 px the trail fits: every item shows its full text.
 */
export const TrailAt1280: StoryObj = {
  render: () => {
    return <CustomerProjectTrail widthClassName="w-[1280px]" />;
  },
};
