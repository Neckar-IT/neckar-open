/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { OfflineFallbackProvider, type OfflineFallbackProviderProps } from "./OfflineFallbackProvider";

/**
 * Meta Information
 */

export default {
  title: "Providers / Offline Fallback Provider",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Online: Story<OfflineFallbackProviderProps> = (args) => (
  <OfflineFallbackProvider {...args}>
    <div>Content</div>
  </OfflineFallbackProvider>
);
Online.args = {
  mockOnline: true,
};

export const Offline: Story<OfflineFallbackProviderProps> = (args) => (
  <OfflineFallbackProvider {...args}>
    <div>Content</div>
  </OfflineFallbackProvider>
);
Offline.args = {
  mockOnline: false,
};
