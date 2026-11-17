import { useLanguage, useSetLanguage, useTranslation } from "@neckar-hub/typescript-react-i18n";
import { isDevelopmentStage } from "@neckar-hub/typescript-utils/runtime-context";
import { GlobeIcon } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "../../primitives/dropdown-menu";
import { SidebarMenuButton } from "../../primitives/sidebar";

const languageLabels: Record<string, string> = {
  de: "Deutsch",
  en: "English",
  dev: "Dev (Keys)",
  large: "Large (Stress)",
};

function getLanguageLabel(code: string): string {
  return languageLabels[code] ?? code.toUpperCase();
}

/**
 * Dropdown to switch the active UI language.
 */
export function LanguageSwitcher(): React.ReactElement | undefined {
  const { i18n } = useTranslation();
  const currentLanguage = useLanguage();
  const setLanguage = useSetLanguage();

  // On a development stage, the `dev` and `large` QA pseudo-languages have no resource bundles — the
  // transforms are applied on-the-fly in the `useTranslation` wrapper. Inject the entries here so
  // the language switcher keeps exposing them. Same gate as the three parts in
  // typescript-react-i18n: without it the transforms would be active but unreachable.
  const resourceLanguages = Object.keys(i18n.store.data);
  const availableLanguages = isDevelopmentStage() ? [...resourceLanguages, "dev", "large"] : resourceLanguages;

  if (availableLanguages.length <= 1) {
    return undefined;
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <SidebarMenuButton className="w-full">
          <GlobeIcon className="h-4 w-4" />
          <span>{getLanguageLabel(currentLanguage)}</span>
        </SidebarMenuButton>
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" align="start">
        <DropdownMenuRadioGroup value={currentLanguage} onValueChange={setLanguage}>
          {availableLanguages.map((code) => (
            <DropdownMenuRadioItem key={code} value={code}>
              {getLanguageLabel(code)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
