import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import {
  storybookColorMode,
  storybookColorModeGlobalTypes,
  storybookInitialGlobals,
  storybookTailwindViewports,
  useStorybookStoryReadyFlag,
} from "@neckar-hub/typescript-react-utils/storybook";
import type { Preview } from "@storybook/react-vite";
import { useEffect } from "react";
import { ThemeSwitcher } from "../src/components/actions/theme-switcher";
import { ToastProvider } from "../src/components/feedback/toast";
import { TooltipProvider } from "../src/components/primitives/tooltip";
import { type ColorMode, type ThemeDefinition, ThemeProvider, useTheme } from "../src/providers/theme";
import "./styles.css";

const themes: ThemeDefinition[] = [
  { id: "neckar", label: "Neckar", colorScheme: "selectable" },
  { id: "terminal", label: "Terminal", colorScheme: "dark" },
  { id: "paper", label: "Paper", colorScheme: "light" },
  { id: "aurora", label: "Aurora", colorScheme: "dark" },
  { id: "fjord", label: "Fjord", colorScheme: "light" },
];

/**
 * Wraps every story: the global providers, the color mode of the toolbar global `theme` and the
 * ready flag the screenshot suite waits for.
 */
function StoryFrame({ colorMode, children }: { colorMode: ColorMode; children: React.ReactNode }): React.ReactElement {
  useStorybookStoryReadyFlag();

  return (
    <JotaiProvider store={jotaiStore}>
      <I18nProvider>
        <ThemeProvider themes={themes}>
          <ColorModeOfToolbar colorMode={colorMode} />
          <ThemedStoryFrame>{children}</ThemedStoryFrame>
        </ThemeProvider>
      </I18nProvider>
    </JotaiProvider>
  );
}

/** Applies the toolbar's light/dark switch to the color mode of the active theme; the switcher picks the theme. */
function ColorModeOfToolbar({ colorMode }: { colorMode: ColorMode }): React.ReactElement {
  const { setColorMode } = useTheme();

  useEffect(() => {
    setColorMode(colorMode);
  }, [colorMode, setColorMode]);

  return (
    <div className="fixed top-2 right-2 z-50">
      <ThemeSwitcher />
    </div>
  );
}

function ThemedStoryFrame({ children }: { children: React.ReactNode }): React.ReactElement {
  const { resolvedColorScheme } = useTheme();

  return (
    <TooltipProvider>
      <div className="bg-background text-foreground">
        <div className="container mx-auto max-w-screen-xl px-4 pt-8 pb-12">{children}</div>
      </div>
      <ToastProvider theme={resolvedColorScheme} />
    </TooltipProvider>
  );
}

const preview: Preview = {
  decorators: [
    (Story, context) => (
      <StoryFrame colorMode={storybookColorMode(context.globals)}>
        <Story />
      </StoryFrame>
    ),
  ],
  globalTypes: storybookColorModeGlobalTypes,
  initialGlobals: storybookInitialGlobals,
  parameters: {
    // The story frame sets its own padding.
    layout: "fullscreen",
    // The theme preview first: the showcase opens on the first story.
    options: { storySort: { order: ["Theme", "*"] } },
    viewport: storybookTailwindViewports,
  },
};

export default preview;
