import type { Page } from "@playwright/test";
import { cleanup, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { useStorybookStoryReadyFlag, waitForStorybookStoryReady } from "./storybookStoryReady";

/**
 * Appends an image that reports itself as still loading, together with the switch that finishes it.
 * jsdom fetches nothing, so `complete` is set by hand — the ready condition reads exactly that property.
 */
function appendLoadingImage(source: string): { finishLoading: () => void } {
  const image = window.document.createElement("img");
  image.setAttribute("src", source);
  let complete = false;
  Object.defineProperty(image, "complete", { get: () => complete });
  window.document.body.append(image);

  return {
    finishLoading: () => {
      complete = true;
    },
  };
}

/**
 * A page whose `waitForFunction` never succeeds and whose `evaluate` runs against the test's own
 * jsdom window — enough to drive the diagnostics `waitForStorybookStoryReady` reports on a timeout.
 */
function pageStubThatNeverBecomesReady(): Page {
  return {
    waitForFunction: () => Promise.reject(new Error("Timeout 20000ms exceeded.")),
    evaluate: <Result>(pageFunction: () => Result) => Promise.resolve(pageFunction()),
  } as unknown as Page;
}

afterEach(() => {
  // Vitest runs without globals, so Testing Library does not unmount the rendered hooks by itself.
  cleanup();
  window.document.body.innerHTML = "";
  window.document.body.className = "";
  window.__STORYBOOK_STORY_READY__ = undefined;
  window.__STORYBOOK_STORY_PENDING__ = undefined;
});

describe("useStorybookStoryReadyFlag()", () => {
  it("signals readiness once the content has settled", async () => {
    renderHook(() => useStorybookStoryReadyFlag());

    await waitFor(() => expect(window.__STORYBOOK_STORY_READY__).toBe(true));
    expect(window.__STORYBOOK_STORY_PENDING__).toBeUndefined();
  });

  it("withholds readiness while an image is still loading and names it", async () => {
    const slowImage = appendLoadingImage("/slow-image");

    renderHook(() => useStorybookStoryReadyFlag());

    await waitFor(() => expect(window.__STORYBOOK_STORY_PENDING__).toContain("/slow-image"));
    expect(window.__STORYBOOK_STORY_READY__).toBe(false);

    slowImage.finishLoading();

    await waitFor(() => expect(window.__STORYBOOK_STORY_READY__).toBe(true));
  });

  it("withholds readiness from an empty document whose story renders one turn later", async () => {
    renderHook(() => useStorybookStoryReadyFlag());
    const slowImage = appendLoadingImage("/slow-image");

    await waitFor(() => expect(window.__STORYBOOK_STORY_PENDING__).toContain("/slow-image"));
    expect(window.__STORYBOOK_STORY_READY__).toBe(false);

    slowImage.finishLoading();

    await waitFor(() => expect(window.__STORYBOOK_STORY_READY__).toBe(true));
  });

  it("withdraws readiness when the component that sets the flag unmounts", async () => {
    const { unmount } = renderHook(() => useStorybookStoryReadyFlag());
    await waitFor(() => expect(window.__STORYBOOK_STORY_READY__).toBe(true));

    unmount();

    expect(window.__STORYBOOK_STORY_READY__).toBe(false);
  });
});

describe("waitForStorybookStoryReady()", () => {
  it("reports a story that never mounted, with what the story browser put on the page instead", async () => {
    window.document.body.innerHTML = `<vite-error-overlay></vite-error-overlay>`;
    const overlay = window.document.querySelector("vite-error-overlay");
    overlay
      ?.attachShadow({ mode: "open" })
      .append(window.document.createTextNode(`Failed to resolve import "@neckar-hub/typescript-utils/runtime-context"`));

    await expect(waitForStorybookStoryReady(pageStubThatNeverBecomesReady())).rejects.toThrow(/never mounted/);
    await expect(waitForStorybookStoryReady(pageStubThatNeverBecomesReady())).rejects.toThrow(/Failed to resolve import/);
  });

  it("reports the error Storybook displays for a story that throws", async () => {
    window.__STORYBOOK_STORY_READY__ = false;
    window.document.body.classList.add("sb-show-errordisplay");
    window.document.body.innerHTML = `<h1 id="error-message">Cannot read properties of undefined</h1><code id="error-stack">at Chart (Chart.tsx:12)</code>`;

    await expect(waitForStorybookStoryReady(pageStubThatNeverBecomesReady())).rejects.toThrow(
      /Cannot read properties of undefined at Chart/,
    );
  });

  it("falls back to the Vite error overlay when Storybook's error display stays empty", async () => {
    window.__STORYBOOK_STORY_READY__ = false;
    window.document.body.classList.add("sb-show-errordisplay");
    window.document.body.innerHTML = `<h1 id="error-message"></h1><code id="error-stack"></code><vite-error-overlay></vite-error-overlay>`;
    window.document
      .querySelector("vite-error-overlay")
      ?.attachShadow({ mode: "open" })
      .append(window.document.createTextNode("Transform failed with 1 error"));

    await expect(waitForStorybookStoryReady(pageStubThatNeverBecomesReady())).rejects.toThrow(
      /the page shows: Transform failed with 1 error/,
    );
  });

  it("reports what a mounted story is still waiting for", async () => {
    window.__STORYBOOK_STORY_READY__ = false;
    window.__STORYBOOK_STORY_PENDING__ = "1 image to finish loading: /slow-image";

    await expect(waitForStorybookStoryReady(pageStubThatNeverBecomesReady())).rejects.toThrow(/1 image to finish loading: \/slow-image/);
  });
});
