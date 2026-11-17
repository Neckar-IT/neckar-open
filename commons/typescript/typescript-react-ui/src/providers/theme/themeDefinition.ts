/**
 * Color mode the user can choose when a theme supports both light and dark ("selectable").
 */
export type ColorMode = "light" | "dark" | "system";

/**
 * The color scheme a theme is designed for:
 * - "light" / "dark": fixed — the matching class is always applied alongside the theme
 * - "selectable": the user may choose light, dark, or system via the color mode
 */
export type ThemeColorScheme = "light" | "dark" | "selectable";

/**
 * Definition of a UI theme that can be activated at runtime.
 *
 * A theme is a set of CSS custom property overrides scoped to a `data-theme`
 * attribute selector (see `@neckar-hub/tailwind-config/themes/`).
 * Components are not aware of themes — they keep using semantic Tailwind
 * utilities (bg-background, text-primary, …).
 */
export interface ThemeDefinition {
  /**
   * Identifier written to the `data-theme` attribute on the html element.
   * Must match the selector of the theme's CSS file, e.g. `html[data-theme="terminal"]`.
   */
  id: string;
  /** Human-readable theme name shown in the theme switcher (a proper noun, not translated). */
  label: string;
  colorScheme: ThemeColorScheme;
}
