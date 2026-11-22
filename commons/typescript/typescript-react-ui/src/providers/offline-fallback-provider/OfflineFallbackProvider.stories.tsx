/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { OfflineFallbackProvider, type OfflineFallbackProviderProps } from "./OfflineFallbackProvider";

const meta = {
  title: "Providers / Offline Fallback Provider",
  component: OfflineFallbackProvider,
} satisfies Meta;

export default meta;

export const Online: StoryObj<OfflineFallbackProviderProps> = {
  args: {
    mockOnline: true,
  },
  render: (args: OfflineFallbackProviderProps) => (
    <OfflineFallbackProvider {...args}>
      <div>Content</div>
    </OfflineFallbackProvider>
  ),
};

export const Offline: StoryObj<OfflineFallbackProviderProps> = {
  args: {
    mockOnline: false,
  },
  render: (args: OfflineFallbackProviderProps) => (
    <OfflineFallbackProvider {...args}>
      <div>Content</div>
    </OfflineFallbackProvider>
  ),
};
