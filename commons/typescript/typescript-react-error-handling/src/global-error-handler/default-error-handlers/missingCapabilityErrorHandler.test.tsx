/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { ExpectedApplicationErrorWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import { render as renderWithoutProviders } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Suspense } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { makeAxiosError } from "../axiosErrorFixture";
import { missingCapabilityErrorHandler } from "./missingCapabilityErrorHandler";

/**
 * The handler bows out with `null` on five separate conditions, and every one of them is
 * silent: the next handler in the chain takes over and the user reads "Oops, something went
 * wrong" instead of learning which permission they lack. Each condition therefore gets its own
 * test — a single happy-path test would keep passing while the handler quietly stopped matching.
 *
 * The counterpart matters just as much: the handler must not claim 403s that belong to someone
 * else, so a wrong error code has to leave it untouched.
 */

/**
 * A 403 body as `StatusPageRouting` serialises `MissingCapabilityException`.
 *
 * The `type` discriminator is what `isExpectedApplicationError` keys on — without it the handler
 * declines the error no matter how right the rest of the payload looks.
 */
function missingCapabilityResponse(capability: string) {
  return {
    type: ExpectedApplicationErrorWithTypeType.ExpectedError,
    errorCode: "AUTHZ-1",
    fallbackMessage: `Missing capability: ${capability}`,
    attributes: { missingCapability: capability },
  };
}

describe("missingCapabilityErrorHandler", () => {
  it("claims a capability 403", () => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({ status: 403, responseData: missingCapabilityResponse("core:operate-product") }),
      "global-error-boundary",
    );

    expect(handled).not.toBeNull();
  });

  it("names the missing capability, so the user can ask for the right permission", async () => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({ status: 403, responseData: missingCapabilityResponse("core:operate-product") }),
      "global-error-boundary",
    );

    render(<Suspense fallback={null}>{handled}</Suspense>);

    // Only a product bundle labels `capabilities.core.operate-product`, so the page shows the raw key
    // in the message and in the error-code line. The translations arrive asynchronously.
    const capabilityMentions = await screen.findAllByText(/core:operate-product/);
    expect(capabilityMentions).toHaveLength(2);
    expect(screen.getByText("HTTP 403")).toBeInTheDocument();
  });

  // `<Trans>` resolves its key outside the embedded `t`; a wrong namespace renders the raw key.
  it.each([
    ["en", "You need the permission core:operate-product to perform this action."],
    ["de", "Sie benötigen die Berechtigung core:operate-product, um diese Aktion auszuführen."],
  ])("renders the permission message from the error-handling bundle in %s", async (language, message) => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({ status: 403, responseData: missingCapabilityResponse("core:operate-product") }),
      "global-error-boundary",
    );

    renderWithoutProviders(
      <I18nProvider options={{ lng: language }}>
        <Suspense fallback={null}>{handled}</Suspense>
      </I18nProvider>,
    );

    const paragraph = await screen.findByText((_, element) => element?.tagName === "P" && element.textContent === message);
    expect(paragraph.querySelector("strong")).toHaveTextContent("core:operate-product");
  });

  it("offers a way back and a way to ask an administrator", async () => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({ status: 403, responseData: missingCapabilityResponse("core:operate-product") }),
      "global-error-boundary",
    );

    render(<Suspense fallback={null}>{handled}</Suspense>);

    expect(await screen.findByRole("button", { name: "Back" })).toBeInTheDocument();
    const contactLink = screen.getByRole("link");
    expect(contactLink.getAttribute("href")).toMatch(/^mailto:\?subject=/);
    expect(contactLink.getAttribute("href")).toContain(encodeURIComponent("core:operate-product"));
  });

  it("ignores an error that is not an AxiosError", () => {
    expect(missingCapabilityErrorHandler(new Error("plain failure"), "global-error-boundary")).toBeNull();
  });

  it("ignores a status other than 403", () => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({ status: 500, responseData: missingCapabilityResponse("core:operate-product") }),
      "global-error-boundary",
    );

    expect(handled).toBeNull();
  });

  it("leaves a 403 that carries a different error code to the other handlers", () => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({
        status: 403,
        responseData: {
          type: ExpectedApplicationErrorWithTypeType.ExpectedError,
          errorCode: "DB-100",
          fallbackMessage: "Something else entirely",
          attributes: undefined,
        },
      }),
      "global-error-boundary",
    );

    expect(handled).toBeNull();
  });

  it("ignores a capability 403 whose payload lacks the capability attribute", () => {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({
        status: 403,
        responseData: {
          type: ExpectedApplicationErrorWithTypeType.ExpectedError,
          errorCode: "AUTHZ-1",
          fallbackMessage: "Missing capability",
          attributes: undefined,
        },
      }),
      "global-error-boundary",
    );

    expect(handled).toBeNull();
  });

  it("ignores a 403 whose body is not an ExpectedApplicationError at all", () => {
    const handled = missingCapabilityErrorHandler(makeAxiosError({ status: 403, responseData: "Forbidden" }), "global-error-boundary");

    expect(handled).toBeNull();
  });
});

/**
 * Leaving this page takes a full document load. The error boundary above it renders the page
 * *instead of* the application, so a history-only navigation swaps the address bar and leaves the
 * 403 page on screen. The two steps below are what makes the button work,
 * and each is easy to drop while the other keeps looking right, so both are pinned.
 */
describe("PermissionDeniedPage — going back", () => {
  const back = vi.fn();
  const reload = vi.fn();
  let originalHistory: History;
  let originalLocation: Location;

  function replaceWindowProperty(name: "history" | "location", value: unknown): void {
    Object.defineProperty(window, name, { value, writable: true, configurable: true });
  }

  /** Renders the page the handler builds for a capability 403 and returns its "Back" button. */
  async function renderPermissionDeniedPage(): Promise<HTMLElement> {
    const handled = missingCapabilityErrorHandler(
      makeAxiosError({ status: 403, responseData: missingCapabilityResponse("core:operate-product") }),
      "global-error-boundary",
    );

    render(<Suspense fallback={null}>{handled}</Suspense>);

    return screen.findByRole("button", { name: "Back" });
  }

  beforeEach(() => {
    vi.clearAllMocks();
    originalHistory = window.history;
    originalLocation = window.location;
    replaceWindowProperty("location", { href: "https://patterns.localdev/permission-demo", reload });
  });

  // Restored here rather than at the end of a test so a failing assertion does not leak into the next one.
  afterEach(() => {
    replaceWindowProperty("history", originalHistory);
    replaceWindowProperty("location", originalLocation);
  });

  it("returns to the previous entry and reloads the document it lands on", async () => {
    const user = userEvent.setup();
    replaceWindowProperty("history", { back, length: 3 });

    await user.click(await renderPermissionDeniedPage());

    expect(back).toHaveBeenCalledOnce();
    // The reload waits for the address bar to carry the previous URL — reloading any earlier would
    // fetch the forbidden route again and land the user right back on this page.
    expect(reload).not.toHaveBeenCalled();

    window.dispatchEvent(new PopStateEvent("popstate"));
    expect(reload).toHaveBeenCalledOnce();
  });

  it("starts over at the application entry point when there is no previous entry", async () => {
    const user = userEvent.setup();
    replaceWindowProperty("history", { back, length: 1 });

    await user.click(await renderPermissionDeniedPage());

    expect(back).not.toHaveBeenCalled();
    // `BASE_URL` rather than "/" so an application served under a base path reaches its own entry.
    expect(window.location.href).toBe(import.meta.env.BASE_URL);
  });
});
