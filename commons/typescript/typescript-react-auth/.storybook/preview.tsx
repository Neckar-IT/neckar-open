import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { ToastProvider } from "@neckar-hub/typescript-react-ui/feedback";
import { TooltipProvider } from "@neckar-hub/typescript-react-ui/overlays";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import {
  type StorybookColorMode,
  storybookColorMode,
  storybookColorModeGlobalTypes,
  storybookInitialGlobals,
  storybookTailwindViewports,
  useStorybookColorModeClass,
  useStorybookStoryReadyFlag,
} from "@neckar-hub/typescript-react-utils/storybook";
import type { Preview } from "@storybook/react-vite";
import "./styles.css";

const queryClient = createQueryClient();

/** Wraps every story: the global providers, the color mode of the toolbar global `theme` and the ready flag the screenshot suite waits for. */
function StoryFrame({ colorMode, children }: { colorMode: StorybookColorMode; children: React.ReactNode }): React.ReactElement {
  useStorybookStoryReadyFlag();

  useStorybookColorModeClass(colorMode);

  return (
    <JotaiProvider store={jotaiStore}>
      <I18nProvider options={{ lng: "en" }}>
        <QueryClientProvider client={queryClient}>
          <TooltipProvider>
            <div className="bg-background text-foreground">
              <div className="container mx-auto max-w-screen-xl">{children}</div>
            </div>
            <ToastProvider theme={colorMode} />
          </TooltipProvider>
        </QueryClientProvider>
      </I18nProvider>
    </JotaiProvider>
  );
}

const preview: Preview = {
  // A docs page per story file next to its stories, with the props table of its `component`.
  tags: ["autodocs"],
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
    // The story frame sets its own width.
    layout: "fullscreen",
    viewport: storybookTailwindViewports,
  },
};

export default preview;
