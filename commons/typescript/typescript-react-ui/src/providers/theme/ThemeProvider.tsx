import { createContext, useCallback, useLayoutEffect, useMemo, useState, useSyncExternalStore } from "react";
import type { ColorMode, ThemeDefinition } from "./themeDefinition";

export interface ThemeContextValue {
  /** All themes registered on the provider, in registration order. */
  themes: ThemeDefinition[];
  /** The currently active theme. */
  theme: ThemeDefinition;
  /** Activates the theme with the given id. Throws for ids not registered on the provider. */
  setThemeId: (themeId: string) => void;
  /** The user's color mode choice — only effective while the active theme's colorScheme is "selectable". */
  colorMode: ColorMode;
  setColorMode: (colorMode: ColorMode) => void;
  /** The color scheme that is effectively applied (theme override or resolved color mode). */
  resolvedColorScheme: "light" | "dark";
}

export const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

interface ThemeProviderProps {
  /** Available themes. Must not be empty. The theme ids must match the data-theme selectors of the imported theme CSS. */
  themes: ThemeDefinition[];
  /** Theme used when nothing is persisted yet. Defaults to the first entry of `themes`. */
  defaultThemeId?: string;
  /** Color mode used when nothing is persisted yet. */
  defaultColorMode?: ColorMode;
  children: React.ReactNode;
}

/** The keys the provider persists. `theme-init.js` of `@neckar-hub/vite-config` reads the theme and the resolved color scheme before the first paint. */
const ThemeStorageKey = "nit.theme";
const ColorModeStorageKey = "nit.color-mode";
const ResolvedColorSchemeStorageKey = "nit.resolved-color-scheme";

const colorModes: Set<string> = new Set<ColorMode>(["light", "dark", "system"]);

function subscribeToSystemColorScheme(callback: () => void): () => void {
  // matchMedia is unavailable in jsdom — nothing to subscribe to there
  if (window.matchMedia === undefined) {
    return () => {};
  }
  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  mediaQuery.addEventListener("change", callback);
  return () => {
    mediaQuery.removeEventListener("change", callback);
  };
}

function getSystemColorScheme(): "light" | "dark" {
  if (window.matchMedia === undefined) {
    return "light";
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

/**
 * Makes the registered themes available via [useTheme] and applies the active theme to the html element:
 * sets the `data-theme` attribute, the `light`/`dark` class (matching the `@custom-variant dark` from
 * the tailwind-config), and `style.colorScheme` for native form controls.
 *
 * Theme and color mode are persisted in localStorage.
 *
 * The theme CSS itself is not managed here — apps import the theme files in their styles.css
 * (e.g. `@import "@neckar-hub/tailwind-config/themes/terminal.css";`).
 */
export function ThemeProvider(props: ThemeProviderProps): React.ReactElement {
  const [firstTheme] = props.themes;

  if (firstTheme === undefined) {
    throw new Error("ThemeProvider requires at least one theme");
  }

  const [themeId, setThemeIdState] = useState<string>(() => {
    const storedThemeId = window.localStorage.getItem(ThemeStorageKey);
    if (storedThemeId !== null && props.themes.some((theme) => theme.id === storedThemeId)) {
      return storedThemeId;
    }
    return props.defaultThemeId ?? firstTheme.id;
  });

  const [colorMode, setColorModeState] = useState<ColorMode>(() => {
    const storedColorMode = window.localStorage.getItem(ColorModeStorageKey);
    if (storedColorMode !== null && colorModes.has(storedColorMode)) {
      return storedColorMode as ColorMode;
    }
    return props.defaultColorMode ?? "system";
  });

  const systemColorScheme = useSyncExternalStore(subscribeToSystemColorScheme, getSystemColorScheme);

  const theme = props.themes.find((candidate) => candidate.id === themeId);
  if (theme === undefined) {
    throw new Error(`Unknown theme id: ${themeId}`);
  }

  const resolvedColorScheme: "light" | "dark" =
    theme.colorScheme === "selectable" ? (colorMode === "system" ? systemColorScheme : colorMode) : theme.colorScheme;

  const setThemeId = useCallback(
    (newThemeId: string) => {
      if (!props.themes.some((candidate) => candidate.id === newThemeId)) {
        throw new Error(`Unknown theme id: ${newThemeId}`);
      }
      setThemeIdState(newThemeId);
      window.localStorage.setItem(ThemeStorageKey, newThemeId);
    },
    [props.themes],
  );

  const setColorMode = useCallback((newColorMode: ColorMode) => {
    setColorModeState(newColorMode);
    window.localStorage.setItem(ColorModeStorageKey, newColorMode);
  }, []);

  useLayoutEffect(() => {
    const root = window.document.documentElement;
    root.setAttribute("data-theme", theme.id);
    root.classList.remove("light", "dark");
    root.classList.add(resolvedColorScheme);
    root.style.colorScheme = resolvedColorScheme;
    // read by theme-init.js of @neckar-hub/vite-config, which applies the theme before the first paint
    window.localStorage.setItem(ResolvedColorSchemeStorageKey, resolvedColorScheme);

    // clean up the global html mutations so an unmounted provider (e.g. a single story
    // in Ladle) does not leak its theme into unrelated content
    return () => {
      root.removeAttribute("data-theme");
      root.classList.remove("light", "dark");
      root.style.colorScheme = "";
    };
  }, [theme.id, resolvedColorScheme]);

  const contextValue = useMemo<ThemeContextValue>(
    () => ({
      themes: props.themes,
      theme,
      setThemeId,
      colorMode,
      setColorMode,
      resolvedColorScheme,
    }),
    [props.themes, theme, setThemeId, colorMode, setColorMode, resolvedColorScheme],
  );

  return <ThemeContext.Provider value={contextValue}>{props.children}</ThemeContext.Provider>;
}
