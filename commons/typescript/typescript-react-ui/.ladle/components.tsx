/**
 * Imports
 */

import { type GlobalProvider, ThemeState } from "@ladle/react";
import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import { useLadleStoryReadyFlag } from "@neckar-hub/typescript-react-utils/ladle";
import { useEffect } from "react";
import { ThemeSwitcher } from "../src/components/actions/theme-switcher";
import { ToastProvider } from "../src/components/feedback/toast";
import { TooltipProvider } from "../src/components/primitives/tooltip";
import { type ColorMode, type ThemeDefinition, ThemeProvider, useTheme } from "../src/providers/theme";
import "./styles.css";

/**
 * Themes
 */

const themes: ThemeDefinition[] = [
  { id: "neckar", label: "Neckar", colorScheme: "selectable" },
  { id: "terminal", label: "Terminal", colorScheme: "dark" },
  { id: "paper", label: "Paper", colorScheme: "light" },
  { id: "aurora", label: "Aurora", colorScheme: "dark" },
  { id: "fjord", label: "Fjord", colorScheme: "light" },
];

const colorModeOfLadleTheme: Record<ThemeState, ColorMode> = {
  [ThemeState.Auto]: "system",
  [ThemeState.Light]: "light",
  [ThemeState.Dark]: "dark",
};

/**
 * Applies the light/dark switch of Ladle to the color mode of the active theme and keeps the `data-theme` attribute on the theme id,
 * because Ladle writes its own `light`/`dark` value into the same attribute.
 */
function LadleThemeBridge({ ladleTheme }: { ladleTheme: ThemeState }): React.ReactElement {
  const { theme, setColorMode, resolvedColorScheme } = useTheme();

  useEffect(() => {
    setColorMode(colorModeOfLadleTheme[ladleTheme]);
  }, [ladleTheme, setColorMode]);

  useEffect(() => {
    const root = window.document.documentElement;
    const restoreThemeId = () => {
      if (root.getAttribute("data-theme") !== theme.id) {
        root.setAttribute("data-theme", theme.id);
      }
    };
    restoreThemeId();
    const observer = new MutationObserver(restoreThemeId);
    observer.observe(root, { attributes: true, attributeFilter: ["data-theme"] });
    return () => {
      observer.disconnect();
    };
  }, [theme.id, resolvedColorScheme]);

  return (
    <div className="fixed top-2 right-2 z-50">
      <ThemeSwitcher />
    </div>
  );
}

/**
 * Global Provider
 */

export const Provider: GlobalProvider = ({ children, globalState }) => {
  useLadleStoryReadyFlag();

  return (
    <JotaiProvider store={jotaiStore}>
      <I18nProvider>
        <ThemeProvider themes={themes}>
          <LadleThemeBridge ladleTheme={globalState.theme} />
          <ThemedStoryFrame>{children}</ThemedStoryFrame>
        </ThemeProvider>
      </I18nProvider>
    </JotaiProvider>
  );
};

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
