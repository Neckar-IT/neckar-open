/**
 * Imports
 */

import { type GlobalProvider, ThemeState } from "@ladle/react";
import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import { useLadleStoryReadyFlag } from "@neckar-hub/typescript-react-utils/ladle";
import { useEffect, useState } from "react";
import { VersionInfoProvider } from "../src/global-error-handler/VersionInfoContext";
import "./styles.css";

/**
 * Dummy Version Info
 */

const ladleVersionInfo = {
  imageDigest: "sha256:1ad1e00000000000000000000000000000000000000000000000000000000000",
  imageDigestShort: "1ad1e0000000",
};

/**
 * Global Provider
 */

export const Provider: GlobalProvider = ({ children, globalState }) => {
  const [theme, setTheme] = useState<"system" | "light" | "dark">("light");

  useLadleStoryReadyFlag();

  useEffect(() => {
    if (globalState.theme === ThemeState.Dark) {
      setTheme("dark");
    } else {
      setTheme("light");
    }
  }, [globalState.theme]);

  // add the class to the html element (like the ThemeProvider does in real apps) — a class
  // on body would override html-scoped data-theme token blocks for all story content
  useEffect(() => {
    window.document.documentElement.classList.remove("light", "dark");
    window.document.documentElement.classList.add(theme);
  }, [theme]);

  return (
    <JotaiProvider store={jotaiStore}>
      <I18nProvider>
        <VersionInfoProvider versionInfo={ladleVersionInfo}>
          <div className="bg-background text-foreground">
            <div className="container mx-auto max-w-screen-xl px-4 pt-8 pb-12">{children}</div>
          </div>
        </VersionInfoProvider>
      </I18nProvider>
    </JotaiProvider>
  );
};
