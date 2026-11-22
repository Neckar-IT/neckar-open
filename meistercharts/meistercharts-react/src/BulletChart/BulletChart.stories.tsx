/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { BulletChart } from "./BulletChart";

/**
 * Default Config
 */

const config = {
  categories: [
    { current: 15, barStart: 10, barEnd: 20, label: "Revenue" },
    { current: 30, barStart: 27, barEnd: 45, label: "Expenses" },
  ],
};

const meta = { title: "Bullet Chart", component: BulletChart } satisfies Meta;

export default meta;

export const Minimal: StoryObj = {
  render: () => <BulletChart configuration={config} />,
};

export const CustomHeight: StoryObj = {
  render: () => <BulletChart configuration={config} style={{ height: "300px" }} />,
};
