import { useEffect, useRef } from "react";
import { composeDocumentTitle } from "./documentTitle";

interface UseAutoPageTitleOptions {
  /**
   * App name appended as suffix: "{page title} · {appName}"
   * When the derived title equals appName, no suffix is added.
   */
  appName: string;

  /**
   * CSS selector for the heading element to extract text from.
   * Defaults to "h1" (the first h1 inside the observed container).
   */
  headingSelector?: string;

  /**
   * CSS selector for breadcrumb items (excluding separators).
   * Defaults to `nav[aria-label='breadcrumb'] ol > li:not([aria-hidden])`.
   * The last matching element's textContent is used.
   */
  breadcrumbItemSelector?: string;
}

const DEFAULT_HEADING_SELECTOR = "h1";
const DEFAULT_BREADCRUMB_ITEM_SELECTOR = "nav[aria-label='breadcrumb'] ol > li:not([aria-hidden])";

/**
 * Observes DOM content within a container ref and automatically derives
 * document.title from the first h1 or last breadcrumb item.
 *
 * Priority:
 * 1. Explicit title via data-explicit-page-title attribute (set by Page component)
 * 2. First h1 textContent
 * 3. Last breadcrumb item textContent
 * 4. appName alone (fallback)
 *
 * Place this hook in a layout component that wraps both the breadcrumbs
 * and page content. Pass a ref to the container element.
 *
 * On unmount the title found before the first run is restored, so a layout that is torn
 * down (for example on logout) does not leave the title of its last page behind.
 */
export function useAutoPageTitle(containerRef: React.RefObject<HTMLElement | null>, options: UseAutoPageTitleOptions): void {
  const { appName, headingSelector = DEFAULT_HEADING_SELECTOR, breadcrumbItemSelector = DEFAULT_BREADCRUMB_ITEM_SELECTOR } = options;
  const previousTitle = useRef<string | null>(null);
  const titleBeforeMount = useRef<string | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    // Captured once: on later runs document.title is what this hook itself wrote.
    titleBeforeMount.current ??= document.title;
    const titleToRestore = titleBeforeMount.current;

    function deriveTitle(observed: HTMLElement): string {
      const explicitMarker = observed.querySelector<HTMLElement>("[data-explicit-page-title]");
      if (explicitMarker) {
        return explicitMarker.dataset.explicitPageTitle ?? appName;
      }

      const heading = observed.querySelector(headingSelector);
      if (heading?.textContent?.trim()) {
        return heading.textContent.trim();
      }

      const breadcrumbItems = observed.querySelectorAll(breadcrumbItemSelector);
      if (breadcrumbItems.length > 0) {
        const lastItem = breadcrumbItems[breadcrumbItems.length - 1];
        if (lastItem?.textContent?.trim()) {
          return lastItem.textContent.trim();
        }
      }

      return appName;
    }

    const updateTitle = (): void => {
      const full = composeDocumentTitle(deriveTitle(container), appName);
      if (full !== previousTitle.current) {
        document.title = full;
        previousTitle.current = full;
      }
    };

    updateTitle();

    const observer = new MutationObserver(updateTitle);
    observer.observe(container, { childList: true, subtree: true, characterData: true });

    return () => {
      observer.disconnect();
      document.title = titleToRestore;
      // Without this a re-run of this effect skips the write when it derives the same title.
      previousTitle.current = null;
    };
  }, [containerRef, appName, headingSelector, breadcrumbItemSelector]);
}
