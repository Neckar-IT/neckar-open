/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GlobalErrorHandler } from "../../GlobalErrorHandler";
import { DefaultSupportContactEmail } from "../../SupportContactContext";
import type { VersionInfoData } from "../../VersionInfoContext";
import { MaxMailtoUrlLength } from "./supportMailtoUrl";

/**
 * Helpers
 */

const versionInfo: VersionInfoData = {
  imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
  imageDigestShort: "ae4608c7c590",
};

async function renderContactSupportHref(error: Error, contactEmail?: string): Promise<string> {
  vi.spyOn(console, "error").mockImplementation(() => undefined);
  const Children = () => {
    throw error;
  };

  render(
    <GlobalErrorHandler contactEmail={contactEmail} versionInfo={versionInfo}>
      <Children />
    </GlobalErrorHandler>,
  );

  return (await screen.findByRole("link", { name: "Contact Support" })).getAttribute("href") ?? "";
}

/**
 * Tests
 */

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe("ContactSupportButton", () => {
  it("addresses the error page's support mail to the contact GlobalErrorHandler configures", async () => {
    const href = await renderContactSupportHref(new Error("error from children"), "support@example.com");

    expect(href).toMatch(/^mailto:support@example\.com\?subject=/);
    expect(decodeURIComponent(href)).toContain("Error Message: error from children");
  });

  it("addresses the support mail to the default contact when GlobalErrorHandler configures none", async () => {
    const href = await renderContactSupportHref(new Error("error from children"));

    expect(href.startsWith(`mailto:${DefaultSupportContactEmail}?subject=`)).toBe(true);
  });

  it("names the time the error page appeared", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-09-11T09:30:00.000Z"));

    const href = await renderContactSupportHref(new Error("error from children"));

    expect(decodeURIComponent(href)).toContain("Occurred At: 2026-09-11T09:30:00.000Z");
  });

  it("keeps the time of the error when the page renders again", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-09-11T09:30:00.000Z"));
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    const Children = () => {
      throw new Error("error from children");
    };
    const { rerender } = render(
      <GlobalErrorHandler versionInfo={versionInfo}>
        <Children />
      </GlobalErrorHandler>,
    );
    await screen.findByRole("link", { name: "Contact Support" });

    // A new context value re-renders ErrorSupportActions, which must not take the time again.
    vi.setSystemTime(new Date("2026-09-11T10:00:00.000Z"));
    rerender(
      <GlobalErrorHandler versionInfo={{ ...versionInfo }}>
        <Children />
      </GlobalErrorHandler>,
    );

    const href = screen.getByRole("link", { name: "Contact Support" }).getAttribute("href") ?? "";
    expect(decodeURIComponent(href)).toContain("Occurred At: 2026-09-11T09:30:00.000Z");
  });

  it("closes a truncated support mail with the translated hint", async () => {
    const error = new Error("error from children");
    error.stack = Array.from({ length: 500 }, (_, index) => `    at frame${index} (app.js:${index}:1)`).join("\n");

    const href = await renderContactSupportHref(error);

    expect(href.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
    expect(
      decodeURIComponent(href).endsWith(
        'The copy button next to "Contact Support" on the error page copies the complete error information.]',
      ),
    ).toBe(true);
  });
});
