/** The color mode the toolbar global `theme` selects; `.storybook/preview.tsx` applies it in a decorator. */
export type StorybookColorMode = "light" | "dark";

/** `globalTypes` of a `.storybook/preview.tsx`: the toolbar switch between the light and the dark color mode. */
export const storybookColorModeGlobalTypes = {
  theme: {
    description: "Color mode of the active theme",
    toolbar: {
      title: "Color mode",
      icon: "mirror" as const,
      items: [
        { value: "light", title: "Light", icon: "sun" as const },
        { value: "dark", title: "Dark", icon: "moon" as const },
      ],
      dynamicTitle: true,
    },
  },
};

/** `initialGlobals` of a `.storybook/preview.tsx`: every story opens in the light color mode. */
export const storybookInitialGlobals = { theme: "light" };

/** The color mode of the toolbar global `theme`, from the `globals` of a decorator's story context. */
export function storybookColorMode(globals: Record<string, unknown>): StorybookColorMode {
  return globals.theme === "dark" ? "dark" : "light";
}

const TailwindBreakpointWidths = { base: 400, sm: 640, md: 768, lg: 1024, xl: 1280, "2xl": 1536 };

/** `parameters.viewport` of a `.storybook/preview.tsx`: the Tailwind breakpoints, below them the narrowest phone width a layout supports. */
export const storybookTailwindViewports = {
  options: Object.fromEntries(
    Object.entries(TailwindBreakpointWidths).map(([name, width]) => [
      name,
      { name: `${name} (${width}px)`, styles: { width: `${width}px`, height: "100%" }, type: width < 768 ? "mobile" : "desktop" },
    ]),
  ),
};
