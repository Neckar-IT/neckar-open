/**
 * Imports
 */

import { elementAt, render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DeploymentInfoDialog } from "./DeploymentInfoDialog";
import { FullyInjectedDeploymentInfo, NothingInjectedDeploymentInfo, PartiallyInjectedDeploymentInfo } from "./deploymentInfoExamples";

/**
 * Helpers
 *
 * The test i18n environment resolves to English (`en.json`).
 */

const NotInjected = "not injected";

function renderedValueOf(name: string): string {
  const label = screen.getByText(`${name}:`);
  const value = label.nextElementSibling;

  if (value === null) {
    throw new Error(`No value rendered next to [${name}]`);
  }

  return value.textContent ?? "";
}

/**
 * Tests
 */

describe("DeploymentInfoDialog", () => {
  it("shows which image runs on which stage", () => {
    render(<DeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(renderedValueOf("deploymentStage")).toBe("Demo");
    expect(renderedValueOf("imageDigestShort")).toBe("7c5587b1ca26");
  });

  it("links the short digest to the page naming its commit", () => {
    render(<DeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(screen.getByRole("link", { name: "7c5587b1ca26" })).toHaveAttribute(
      "href",
      "https://deployments.neckar.it/digests/sha256:7c5587b1ca26e1af8c1a0a21a6d4bc8fc9fa726a7c5587b1ca26e1af8c1a0a21",
    );
  });

  it("leaves the values a support request only needs in writing off the screen", () => {
    render(<DeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(screen.queryByText("executionEnvironment:")).not.toBeInTheDocument();
    expect(screen.queryByText("effectiveDeploymentStage:")).not.toBeInTheDocument();
    expect(screen.queryByText("serviceHost:")).not.toBeInTheDocument();
    expect(screen.queryByText("imageDigest:")).not.toBeInTheDocument();
  });

  it("says nothing was injected instead of reporting a fallback as a value", () => {
    render(<DeploymentInfoDialog deploymentInfo={NothingInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(renderedValueOf("deploymentStage")).toBe(NotInjected);
    expect(renderedValueOf("imageDigestShort")).toBe(NotInjected);
    expect(screen.queryByText("unknown")).not.toBeInTheDocument();
  });

  it("names the stage the application works with where the injected stage does not decide it", () => {
    render(<DeploymentInfoDialog deploymentInfo={NothingInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(screen.getByText("The application assumes the Production stage.")).toBeInTheDocument();
  });

  it("keeps that note away where the deployment reported its stage", () => {
    render(<DeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(screen.queryByText(/The application assumes/)).not.toBeInTheDocument();
  });

  it("tells reported and missing values apart within the one section", () => {
    render(<DeploymentInfoDialog deploymentInfo={PartiallyInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(renderedValueOf("deploymentStage")).toBe(NotInjected);
    expect(renderedValueOf("imageDigestShort")).toBe("7c5587b1ca26");
  });

  it("offers one copy step, so a support request carries every value in one paste", () => {
    render(<DeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} onOpenChange={vi.fn()} />);

    expect(screen.getAllByRole("button", { name: "Copy as JSON" })).toHaveLength(1);
  });

  it("closes on the footer button, for a reader who does not aim for the corner", async () => {
    const onOpenChange = vi.fn();
    render(<DeploymentInfoDialog deploymentInfo={FullyInjectedDeploymentInfo} onOpenChange={onOpenChange} />);

    // the dialog's own corner button carries the same accessible name and is rendered after the body
    await userEvent.click(elementAt(screen.getAllByRole("button", { name: "Close" }), 0));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
