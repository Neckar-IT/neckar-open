import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import {
  type StorybookColorMode,
  storybookColorMode,
  storybookColorModeGlobalTypes,
  storybookInitialGlobals,
  storybookTailwindViewports,
  useStorybookStoryReadyFlag,
} from "@neckar-hub/typescript-react-utils/storybook";
import type { Preview } from "@storybook/react-vite";
import { useEffect } from "react";
import { VersionInfoProvider } from "../src/global-error-handler/VersionInfoContext";
import "./styles.css";

/** The image the error screens name; the stories run in no image. */
const storyVersionInfo = {
  imageDigest: "sha256:1ad1e00000000000000000000000000000000000000000000000000000000000",
  imageDigestShort: "1ad1e0000000",
};

/** Wraps every story: the global providers, the color mode of the toolbar global `theme` and the ready flag the screenshot suite waits for. */
function StoryFrame({ colorMode, children }: { colorMode: StorybookColorMode; children: React.ReactNode }): React.ReactElement {
  useStorybookStoryReadyFlag();

  // On the html element, like the ThemeProvider of an app: a class on body would override the html-scoped data-theme token blocks.
  useEffect(() => {
    window.document.documentElement.classList.remove("light", "dark");
    window.document.documentElement.classList.add(colorMode);
  }, [colorMode]);

  return (
    <JotaiProvider store={jotaiStore}>
      <I18nProvider>
        <VersionInfoProvider versionInfo={storyVersionInfo}>
          <div className="bg-background text-foreground">
            <div className="container mx-auto max-w-screen-xl px-4 pt-8 pb-12">{children}</div>
          </div>
        </VersionInfoProvider>
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
    // The story frame sets its own padding.
    layout: "fullscreen",
    viewport: storybookTailwindViewports,
  },
};

export default preview;
