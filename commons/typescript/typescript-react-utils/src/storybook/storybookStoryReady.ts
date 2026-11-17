import type { Page } from "@playwright/test";
import { useEffect } from "react";

/**
 * Extend the windows object with a flag that signals when the story is ready, and with what the
 * ready condition is still waiting for while the flag is false.
 */
declare global {
  interface Window {
    __STORYBOOK_STORY_READY__?: boolean;
    __STORYBOOK_STORY_PENDING__?: string;
  }
}

/** How long a screenshot test waits for a story to signal readiness. */
const defaultReadyTimeoutMilliseconds = 20_000;

/**
 * Frames in a row without anything pending and without a DOM change before the content counts as
 * settled. Three frames cover the React commit, the layout that follows it and the paint — a
 * component that measures its own box (a chart sizing itself through a `ResizeObserver`) draws in
 * the frame after the measurement, and a story whose subtree renders one turn after its decorator
 * would otherwise be photographed while it is still empty.
 */
const settledFramesRequired = 3;

/**
 * Hook to set the flag when the story is fully ready
 *
 * Call it in a decorator of `.storybook/preview.tsx`, which wraps every story:
 * ```
 * function StoryReadyFlag({ children }: PropsWithChildren) {
 *   useStorybookStoryReadyFlag();
 *
 *   return <>{children}</>;
 * }
 *
 * const preview: Preview = {
 *   decorators: [(Story) => <StoryReadyFlag><Story /></StoryReadyFlag>],
 * };
 * ```
 */
export function useStorybookStoryReadyFlag() {
  useEffect(() => {
    // Reset as soon as the component that sets the flag mounts
    window.__STORYBOOK_STORY_READY__ = false;
    window.__STORYBOOK_STORY_PENDING__ = "the story to render";

    let cancelled = false;

    async function markReady() {
      window.__STORYBOOK_STORY_PENDING__ = "the document fonts to load";

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

      window.__STORYBOOK_STORY_PENDING__ = undefined;
      window.__STORYBOOK_STORY_READY__ = true;
    }

    void markReady();

    return () => {
      cancelled = true;
      window.__STORYBOOK_STORY_READY__ = false;
      window.__STORYBOOK_STORY_PENDING__ = "the component that sets the ready flag to mount";
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
 * The DOM watch is what makes the condition hold for content that arrives after the decorator: a
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
      window.__STORYBOOK_STORY_PENDING__ = pending;
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
interface StoryDiagnostics {
  readyFlag: boolean | undefined;
  pending: string | undefined;
  errorOverlayText: string | undefined;
  errorDisplayText: string | undefined;
}

/**
 * Waits for the flag to be set
 *
 * Use this inside the playwright screenshot tests before taking the screenshot
 */
export async function waitForStorybookStoryReady(page: Page, options?: { timeoutMilliseconds?: number }): Promise<void> {
  const timeoutMilliseconds = options?.timeoutMilliseconds ?? defaultReadyTimeoutMilliseconds;

  try {
    await page.waitForFunction(() => window.__STORYBOOK_STORY_READY__ === true, undefined, { timeout: timeoutMilliseconds });
  } catch (cause) {
    throw new Error(describeStoryThatIsNotReady(await collectDiagnostics(page), timeoutMilliseconds), { cause });
  }
}

async function collectDiagnostics(page: Page): Promise<StoryDiagnostics> {
  return page.evaluate(() => ({
    readyFlag: window.__STORYBOOK_STORY_READY__,
    pending: window.__STORYBOOK_STORY_PENDING__,
    errorOverlayText: window.document.querySelector("vite-error-overlay")?.shadowRoot?.textContent?.trim().slice(0, 800),
    // Storybook shows a story that throws in its error display and marks the body with this class.
    errorDisplayText: window.document.body.classList.contains("sb-show-errordisplay")
      ? `${window.document.querySelector("#error-message")?.textContent ?? ""} ${window.document.querySelector("#error-stack")?.textContent ?? ""}`
          .trim()
          .slice(0, 800) || undefined
      : undefined,
  }));
}

/**
 * Turns the page state into the sentence the failing test shows.
 *
 * A story whose modules fail to load never calls `useStorybookStoryReadyFlag` and therefore never
 * touches the flag; that reads as the same timeout as a story that renders forever, which is why the
 * two cases are told apart here.
 */
function describeStoryThatIsNotReady(diagnostics: StoryDiagnostics, timeoutMilliseconds: number): string {
  const errorText = diagnostics.errorDisplayText ?? diagnostics.errorOverlayText;
  const pageError = errorText === undefined ? "the page shows no error" : `the page shows: ${errorText}`;

  if (diagnostics.readyFlag === undefined) {
    return `The story never mounted the component that sets the ready flag within ${timeoutMilliseconds}ms — it failed to load rather than to render, and ${pageError}`;
  }

  return `The story did not become ready within ${timeoutMilliseconds}ms — it is waiting for ${diagnostics.pending ?? "an unnamed condition"}, and ${pageError}`;
}
