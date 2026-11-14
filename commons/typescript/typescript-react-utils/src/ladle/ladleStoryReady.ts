import type { Page } from "@playwright/test";
import { useEffect } from "react";

/**
 * Extend the windows object with a flag that signals when the story is ready, and with what the
 * ready condition is still waiting for while the flag is false.
 */
declare global {
  interface Window {
    __LADLE_STORY_READY__?: boolean;
    __LADLE_STORY_PENDING__?: string;
  }
}

/** How long a screenshot test waits for a story to signal readiness. */
const defaultReadyTimeoutMilliseconds = 20_000;

/**
 * Frames in a row without anything pending and without a DOM change before the content counts as
 * settled. Three frames cover the React commit, the layout that follows it and the paint — a
 * component that measures its own box (a chart sizing itself through a `ResizeObserver`) draws in
 * the frame after the measurement, and a story whose subtree renders one turn after its provider
 * would otherwise be photographed while it is still empty.
 */
const settledFramesRequired = 3;

/**
 * Hook to set the flag when the story is fully ready
 *
 * Add this to the components.tsx file for ladle like this:
 * ```
 * export const Provider: GlobalProvider = ({ children }) => {
 *   useLadleStoryReadyFlag();
 *
 *   return <>{children}</>;
 * }
 * ```
 */
export function useLadleStoryReadyFlag() {
  useEffect(() => {
    // Reset as soon as the story provider mounts
    window.__LADLE_STORY_READY__ = false;
    window.__LADLE_STORY_PENDING__ = "the story to render";

    let cancelled = false;

    async function markReady() {
      window.__LADLE_STORY_PENDING__ = "the document fonts to load";

      try {
        await window.document.fonts.ready;
      } catch {
        /* a browser without the font loading API cannot report unloaded fonts */
      }

      if (cancelled) {
        return;
      }

      await waitForContentToSettle(() => cancelled);

      if (cancelled) {
        return;
      }

      window.__LADLE_STORY_PENDING__ = undefined;
      window.__LADLE_STORY_READY__ = true;
    }

    void markReady();

    return () => {
      cancelled = true;
      window.__LADLE_STORY_READY__ = false;
      window.__LADLE_STORY_PENDING__ = "the story provider to mount";
    };
  }, []);
}

/**
 * What the story is still waiting for, or `undefined` when its content stands.
 *
 * Fonts are awaited before this runs; what remains is everything that keeps changing the picture
 * after the first paint — an `<img>` whose source is still on the wire is the one the screenshot
 * suite trips over, because it paints as a placeholder and turns into an image moments later.
 */
function describePendingContent(): string | undefined {
  const loadingImages = Array.from(window.document.images).filter((image) => !image.complete);

  if (loadingImages.length > 0) {
    const sources = loadingImages.map((image) => image.getAttribute("src") ?? "<no src>").join(", ");
    return `${loadingImages.length} image(s) to finish loading: ${sources}`;
  }

  return undefined;
}

/**
 * Resolves once nothing is pending and the DOM has stopped changing for
 * {@link settledFramesRequired} frames in a row, publishing what it waits for on every frame so
 * that a run that never settles can say what held it up.
 *
 * The DOM watch is what makes the condition hold for content that arrives after the provider: a
 * story rendering one turn later, a query resolving into a table, a chart filling its measured
 * area. Without it an empty document counts as settled and the screenshot shows nothing.
 */
function waitForContentToSettle(isCancelled: () => boolean): Promise<void> {
  return new Promise((resolve) => {
    let settledFrames = 0;
    let documentChanged = true;

    const observer = new window.MutationObserver(() => {
      documentChanged = true;
    });
    observer.observe(window.document.documentElement, { subtree: true, childList: true, attributes: true, characterData: true });

    function finish() {
      observer.disconnect();
      resolve();
    }

    function checkFrame() {
      if (isCancelled()) {
        finish();
        return;
      }

      const changedSinceLastFrame = documentChanged;
      documentChanged = false;

      const pending = describePendingContent() ?? (changedSinceLastFrame ? "the document to stop changing" : undefined);
      window.__LADLE_STORY_PENDING__ = pending;
      settledFrames = pending === undefined ? settledFrames + 1 : 0;

      if (settledFrames >= settledFramesRequired) {
        finish();
        return;
      }

      window.requestAnimationFrame(checkFrame);
    }

    window.requestAnimationFrame(checkFrame);
  });
}

/**
 * What the page says about itself when it has not become ready.
 */
interface LadleStoryDiagnostics {
  readyFlag: boolean | undefined;
  pending: string | undefined;
  errorOverlayText: string | undefined;
}

/**
 * Waits for the flag to be set
 *
 * Use this inside the playwright screenshot tests before taking the screenshot
 */
export async function waitForLadleStoryReady(page: Page, options?: { timeoutMilliseconds?: number }): Promise<void> {
  const timeoutMilliseconds = options?.timeoutMilliseconds ?? defaultReadyTimeoutMilliseconds;

  try {
    await page.waitForFunction(() => window.__LADLE_STORY_READY__ === true, undefined, { timeout: timeoutMilliseconds });
  } catch (cause) {
    throw new Error(describeStoryThatIsNotReady(await collectDiagnostics(page), timeoutMilliseconds), { cause });
  }
}

async function collectDiagnostics(page: Page): Promise<LadleStoryDiagnostics> {
  return page.evaluate(() => ({
    readyFlag: window.__LADLE_STORY_READY__,
    pending: window.__LADLE_STORY_PENDING__,
    errorOverlayText: window.document.querySelector("vite-error-overlay")?.shadowRoot?.textContent?.trim().slice(0, 800),
  }));
}

/**
 * Turns the page state into the sentence the failing test shows.
 *
 * A story whose modules fail to load never mounts the provider and therefore never touches the
 * flag — that reads as the same 30 second timeout as a story that renders forever, which is why the
 * two cases are told apart here.
 */
function describeStoryThatIsNotReady(diagnostics: LadleStoryDiagnostics, timeoutMilliseconds: number): string {
  const errorOverlay =
    diagnostics.errorOverlayText === undefined ? "the page shows no error overlay" : `the page shows: ${diagnostics.errorOverlayText}`;

  if (diagnostics.readyFlag === undefined) {
    return `The story never mounted the Ladle provider within ${timeoutMilliseconds}ms — it failed to load rather than to render, and ${errorOverlay}`;
  }

  return `The story did not become ready within ${timeoutMilliseconds}ms — it is waiting for ${diagnostics.pending ?? "an unnamed condition"}, and ${errorOverlay}`;
}
