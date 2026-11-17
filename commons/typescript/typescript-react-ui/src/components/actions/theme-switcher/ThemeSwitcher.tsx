import { MonitorIcon, MoonIcon, SunIcon, SwatchBookIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { type ColorMode, useTheme } from "../../../providers/theme";
import { Button } from "../../primitives/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../../primitives/dropdown-menu";
import { SidebarMenuButton } from "../../primitives/sidebar";

interface ThemeSwitcherProps {
  /**
   * Visual variant of the trigger:
   * - "icon-button": ghost icon button for headers/toolbars (default)
   * - "sidebar": full-width [SidebarMenuButton] showing the active theme label — requires a SidebarProvider
   */
  variant?: "icon-button" | "sidebar";
}

/**
 * Dropdown to switch the active theme and — for themes with a selectable color
 * scheme — the color mode (light / dark / system).
 *
 * Renders nothing when only a single theme with a fixed color scheme is
 * registered, since there would be nothing to switch.
 *
 * Must be used inside a [ThemeProvider].
 */
export function ThemeSwitcher({ variant = "icon-button" }: ThemeSwitcherProps): React.ReactElement | undefined {
  const { t } = useEmbeddedTranslation();
  const { themes, theme, setThemeId, colorMode, setColorMode } = useTheme();

  const colorModeSelectable = theme.colorScheme === "selectable";

  if (themes.length <= 1 && !colorModeSelectable) {
    return undefined;
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        {variant === "sidebar" ? (
          <SidebarMenuButton className="w-full">
            <SwatchBookIcon className="h-4 w-4" />
            <span>{theme.label}</span>
          </SidebarMenuButton>
        ) : (
          <Button size="icon" variant="ghost">
            <SwatchBookIcon className="h-4 w-4" />
            <span className="sr-only">{t("themeSwitcher.openMenu")}</span>
          </Button>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align={variant === "sidebar" ? "start" : "end"}
        className="min-w-44"
        side={variant === "sidebar" ? "top" : "bottom"}
      >
        {themes.length > 1 && (
          <>
            <DropdownMenuLabel>{t("themeSwitcher.theme")}</DropdownMenuLabel>
            <DropdownMenuRadioGroup value={theme.id} onValueChange={setThemeId}>
              {themes.map((themeDefinition) => (
                <DropdownMenuRadioItem key={themeDefinition.id} value={themeDefinition.id}>
                  {themeDefinition.label}
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuRadioGroup>
          </>
        )}

        {colorModeSelectable && (
          <>
            {themes.length > 1 && <DropdownMenuSeparator />}
            <DropdownMenuLabel>{t("themeSwitcher.colorMode")}</DropdownMenuLabel>
            <DropdownMenuRadioGroup value={colorMode} onValueChange={(value) => setColorMode(value as ColorMode)}>
              <DropdownMenuRadioItem value="light">
                <SunIcon className="mr-2 h-4 w-4" />
                {t("themeSwitcher.light")}
              </DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="dark">
                <MoonIcon className="mr-2 h-4 w-4" />
                {t("themeSwitcher.dark")}
              </DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="system">
                <MonitorIcon className="mr-2 h-4 w-4" />
                {t("themeSwitcher.system")}
              </DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
