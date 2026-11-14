/**
 * Imports
 */

import { type GlobalProvider, ThemeState } from "@ladle/react";
import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { ToastProvider } from "@neckar-hub/typescript-react-ui/feedback";
import { TooltipProvider } from "@neckar-hub/typescript-react-ui/overlays";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import { useLadleStoryReadyFlag } from "@neckar-hub/typescript-react-utils/ladle";
import { useEffect, useState } from "react";
import "./styles.css";

/**
 * Query Client
 */

const queryClient = createQueryClient();

/**
 * Global Provider
 */

export const Provider: GlobalProvider = ({ children, globalState }) => {
  const [theme, setTheme] = useState<"system" | "light" | "dark">("light");

  useLadleStoryReadyFlag();

  // listen for changes of the ladle theme state
  useEffect(() => {
    if (globalState.theme === ThemeState.Dark) {
      setTheme("dark");
    } else {
      setTheme("light");
    }
  }, [globalState.theme]);

  // add the class to the body
  useEffect(() => {
    window.document.body.classList.remove("light", "dark");
    window.document.body.classList.add(theme);
  }, [theme]);

  return (
    <JotaiProvider store={jotaiStore}>
      <I18nProvider options={{ lng: "en" }}>
        <QueryClientProvider client={queryClient}>
          <TooltipProvider>
            <div className="bg-background text-foreground">
              <div className="container mx-auto max-w-screen-xl">{children}</div>
            </div>
            <ToastProvider theme={theme} />
          </TooltipProvider>
        </QueryClientProvider>
      </I18nProvider>
    </JotaiProvider>
  );
};
