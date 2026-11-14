import { useEffect, useRef } from "react";
import { composeDocumentTitle } from "./documentTitle";

/**
 * Sets document.title to "{pageTitle} · {appName}" while the component is mounted and
 * restores the previous title on unmount.
 *
 * The app name is the document title found on the first run — the static title from index.html.
 *
 * For screens rendered outside the app layout (login, registration). Pages inside the layout
 * are covered by useAutoPageTitle.
 */
export function useDocumentTitle(pageTitle: string): void {
  const titleBeforeMount = useRef<string | null>(null);

  useEffect(() => {
    // Captured once: on later runs document.title is what this hook itself wrote.
    titleBeforeMount.current ??= document.title;
    const appName = titleBeforeMount.current;

    document.title = composeDocumentTitle(pageTitle, appName);

    return () => {
      document.title = appName;
    };
  }, [pageTitle]);
}
